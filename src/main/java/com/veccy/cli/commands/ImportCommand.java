package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Import vectors from a file.
 */
public class ImportCommand implements Command {

    @Override
    public String getName() {
        return "import";
    }

    @Override
    public String getDescription() {
        return "Import vectors from a file";
    }

    @Override
    public String getUsage() {
        return "import <file> [--format <format>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        requireOpenDatabase(context);
        requireArgs(args, 1);

        String filePath = args[0];
        String format = null;

        // Parse arguments
        for (int i = 1; i < args.length; i++) {
            if ((args[i].equals("--format") || args[i].equals("-f")) && i + 1 < args.length) {
                format = args[++i];
            }
        }

        // Auto-detect format from extension if not specified
        if (format == null) {
            if (filePath.endsWith(".json")) {
                format = "json";
            } else if (filePath.endsWith(".csv")) {
                format = "csv";
            } else {
                throw new IllegalArgumentException("Cannot detect format. Please specify --format json or --format csv");
            }
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        long startTime = System.currentTimeMillis();
        int count;

        switch (format.toLowerCase()) {
            case "json" -> count = importJson(context, path);
            case "csv" -> count = importCsv(context, path);
            default -> throw new IllegalArgumentException("Unsupported format: " + format + ". Use 'json' or 'csv'");
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Successfully imported " + count + " vector(s)");
        System.out.println("Time: " + duration + "ms");
        if (count > 0) {
            System.out.println("Average: " + (duration / count) + "ms per vector");
        }
    }

    private int importCsv(CLIContext context, Path path) throws IOException {
        List<double[]> vectors = new ArrayList<>();
        List<Map<String, Object>> metadataList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("Empty CSV file");
            }

            // Check if first line is header
            boolean hasHeader = line.toLowerCase().contains("vector") || line.toLowerCase().contains("id");
            if (!hasHeader) {
                // Process first line as data
                processVectorLine(line, vectors, metadataList);
            }

            // Process remaining lines
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                processVectorLine(line, vectors, metadataList);
            }
        }

        if (vectors.isEmpty()) {
            return 0;
        }

        // Bulk insert
        double[][] vectorArray = vectors.toArray(new double[0][]);
        context.getClient().insert(vectorArray, metadataList.isEmpty() ? null : metadataList);

        return vectors.size();
    }

    private void processVectorLine(String line, List<double[]> vectors, List<Map<String, Object>> metadataList) {
        // Expected format: vector values separated by commas, optionally followed by metadata
        // Example: 1.0,2.0,3.0
        // Or with metadata: 1.0,2.0,3.0,key1=value1,key2=value2

        String[] parts = line.split(",");
        List<Double> vectorValues = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();

        for (String part : parts) {
            part = part.trim();
            if (part.contains("=")) {
                // This is metadata
                String[] kvPair = part.split("=", 2);
                if (kvPair.length == 2) {
                    metadata.put(kvPair[0].trim(), kvPair[1].trim());
                }
            } else {
                // This is a vector value
                try {
                    vectorValues.add(Double.parseDouble(part));
                } catch (NumberFormatException e) {
                    // Skip invalid values
                }
            }
        }

        if (!vectorValues.isEmpty()) {
            double[] vector = vectorValues.stream().mapToDouble(Double::doubleValue).toArray();
            vectors.add(vector);
            metadataList.add(metadata.isEmpty() ? null : metadata);
        }
    }

    private int importJson(CLIContext context, Path path) throws IOException {
        String content = Files.readString(path);
        content = content.trim();

        if (!content.startsWith("[")) {
            throw new IllegalArgumentException("JSON file must contain an array of vectors");
        }

        List<double[]> vectors = new ArrayList<>();
        List<Map<String, Object>> metadataList = new ArrayList<>();

        // Simple JSON parsing for array of objects
        // Format: [{"vector": [1.0, 2.0], "metadata": {"key": "value"}}, ...]
        String arrayContent = content.substring(1, content.length() - 1);

        // Split by },{ to get individual objects
        String[] objects = arrayContent.split("\\},\\s*\\{");

        for (String obj : objects) {
            obj = obj.trim();
            if (obj.startsWith("{")) {
                obj = obj.substring(1);
            }
            if (obj.endsWith("}")) {
                obj = obj.substring(0, obj.length() - 1);
            }

            double[] vector = extractVector(obj);
            Map<String, Object> metadata = extractMetadata(obj);

            if (vector != null) {
                vectors.add(vector);
                metadataList.add(metadata);
            }
        }

        if (vectors.isEmpty()) {
            return 0;
        }

        // Bulk insert
        double[][] vectorArray = vectors.toArray(new double[0][]);
        context.getClient().insert(vectorArray, metadataList.isEmpty() ? null : metadataList);

        return vectors.size();
    }

    private double[] extractVector(String jsonObject) {
        // Extract vector array from "vector": [1.0, 2.0, 3.0]
        int vectorStart = jsonObject.indexOf("\"vector\"");
        if (vectorStart == -1) {
            return null;
        }

        int arrayStart = jsonObject.indexOf("[", vectorStart);
        int arrayEnd = jsonObject.indexOf("]", arrayStart);

        if (arrayStart == -1 || arrayEnd == -1) {
            return null;
        }

        String vectorStr = jsonObject.substring(arrayStart + 1, arrayEnd);
        String[] parts = vectorStr.split(",");
        double[] vector = new double[parts.length];

        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i].trim());
        }

        return vector;
    }

    private Map<String, Object> extractMetadata(String jsonObject) {
        Map<String, Object> metadata = new HashMap<>();

        // Extract metadata object from "metadata": {"key": "value"}
        int metadataStart = jsonObject.indexOf("\"metadata\"");
        if (metadataStart == -1) {
            return metadata;
        }

        int objStart = jsonObject.indexOf("{", metadataStart);
        int objEnd = jsonObject.indexOf("}", objStart);

        if (objStart == -1 || objEnd == -1) {
            return metadata;
        }

        String metadataStr = jsonObject.substring(objStart + 1, objEnd);
        if (metadataStr.trim().isEmpty()) {
            return metadata;
        }

        // Simple key-value parsing
        String[] pairs = metadataStr.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");
                metadata.put(key, value);
            }
        }

        return metadata;
    }
}
