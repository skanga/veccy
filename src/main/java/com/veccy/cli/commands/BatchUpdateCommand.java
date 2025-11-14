package com.veccy.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veccy.cli.CLIContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI command for batch updating multiple vectors.
 * <p>
 * Supports reading updates from a file or command line arguments.
 * More efficient than running multiple individual updates.
 */
public class BatchUpdateCommand implements Command {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "batch-update";
    }

    @Override
    public String getDescription() {
        return "Update multiple vectors (batch mode)";
    }

    @Override
    public String getUsage() {
        return "batch-update <updates> [--format <fmt>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);

        if (args.length < 1) {
            System.err.println("Usage: batch-update <updates> [options]");
            System.err.println();
            System.err.println("Arguments:");
            System.err.println("  <updates>          Updates specification or @file path");
            System.err.println();
            System.err.println("Options:");
            System.err.println("  --format <fmt>     Input format: csv, json (default: csv)");
            System.err.println();
            System.err.println("CSV Format (id,vector,metadata):");
            System.err.println("  id1,[0.1,0.2,0.3],'{\"key\":\"value\"}'");
            System.err.println("  id2,[0.4,0.5,0.6],");
            System.err.println();
            System.err.println("JSON Format:");
            System.err.println("  [");
            System.err.println("    {\"id\":\"id1\", \"vector\":[0.1,0.2,0.3], \"metadata\":{\"key\":\"value\"}},");
            System.err.println("    {\"id\":\"id2\", \"vector\":[0.4,0.5,0.6]}");
            System.err.println("  ]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  batch-update @updates.csv");
            System.err.println("  batch-update @updates.json --format json");
            return;
        }

        try {
            // Parse options
            String format = "csv";

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("--format") && i + 1 < args.length) {
                    format = args[++i];
                }
            }

            // Parse updates
            List<String> ids = new ArrayList<>();
            List<double[]> vectors = new ArrayList<>();
            List<Map<String, Object>> metadata = new ArrayList<>();

            String updateArg = args[0];
            if (updateArg.startsWith("@")) {
                // Read from file
                String filename = updateArg.substring(1);
                if (format.equalsIgnoreCase("json")) {
                    readJsonUpdates(filename, ids, vectors, metadata);
                } else {
                    readCsvUpdates(filename, ids, vectors, metadata);
                }

                if (context.isVerbose()) {
                    System.out.println("Loaded " + ids.size() + " updates from " + filename);
                }
            } else {
                System.err.println("Error: Direct command-line updates not supported. Use @file");
                return;
            }

            if (ids.isEmpty()) {
                System.err.println("Error: No updates provided");
                return;
            }

            // Perform batch update
            long startTime = System.nanoTime();
            List<Boolean> results = context.getClient().batchUpdate(ids, vectors, metadata);
            long endTime = System.nanoTime();

            double totalTime = (endTime - startTime) / 1_000_000.0; // Convert to ms

            // Display results
            int successCount = 0;
            int failureCount = 0;

            System.out.println();
            System.out.println("Update Results:");
            System.out.println("─".repeat(80));
            System.out.printf("%-40s %-10s%n", "ID", "Status");
            System.out.println("─".repeat(80));

            for (int i = 0; i < results.size(); i++) {
                boolean success = results.get(i);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }

                if (!success || context.isVerbose()) {
                    System.out.printf("%-40s %-10s%n",
                            ids.get(i),
                            success ? "✓ SUCCESS" : "✗ FAILED");
                }
            }

            // Print summary
            System.out.println("─".repeat(80));
            System.out.printf("Total: %d | Success: %d | Failed: %d%n",
                    results.size(), successCount, failureCount);
            System.out.printf("Completed in %.2f ms (%.2f ms/update)%n",
                    totalTime, totalTime / results.size());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (context.isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    private void readCsvUpdates(String filename, List<String> ids, List<double[]> vectors,
                                List<Map<String, Object>> metadata) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                try {
                    parseCsvLine(line, ids, vectors, metadata);
                } catch (Exception e) {
                    System.err.println("Warning: Skipping line " + lineNum + ": " + e.getMessage());
                }
            }
        }
    }

    private void parseCsvLine(String line, List<String> ids, List<double[]> vectors,
                             List<Map<String, Object>> metadata) throws Exception {
        // Parse CSV: id,[vector],{metadata}
        // Find the vector part between []
        int vectorStart = line.indexOf('[');
        int vectorEnd = line.indexOf(']', vectorStart);

        if (vectorStart == -1 || vectorEnd == -1) {
            throw new IllegalArgumentException("Invalid CSV format: missing vector");
        }

        // Extract ID
        String id = line.substring(0, vectorStart).trim();
        if (id.endsWith(",")) {
            id = id.substring(0, id.length() - 1).trim();
        }
        ids.add(id);

        // Extract vector
        String vectorStr = line.substring(vectorStart + 1, vectorEnd);
        String[] parts = vectorStr.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i].trim());
        }
        vectors.add(vector);

        // Extract metadata (if present)
        String metadataStr = line.substring(vectorEnd + 1).trim();
        if (metadataStr.startsWith(",")) {
            metadataStr = metadataStr.substring(1).trim();
        }

        if (!metadataStr.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = objectMapper.readValue(metadataStr, Map.class);
            metadata.add(meta);
        } else {
            metadata.add(null);
        }
    }

    @SuppressWarnings("unchecked")
    private void readJsonUpdates(String filename, List<String> ids, List<double[]> vectors,
                                 List<Map<String, Object>> metadata) throws IOException {
        List<Map<String, Object>> updates = objectMapper.readValue(
                new FileReader(filename),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
        );

        for (Map<String, Object> update : updates) {
            // Extract ID
            String id = (String) update.get("id");
            if (id == null) {
                throw new IllegalArgumentException("Missing 'id' field in JSON update");
            }
            ids.add(id);

            // Extract vector
            List<Number> vectorList = (List<Number>) update.get("vector");
            if (vectorList == null) {
                throw new IllegalArgumentException("Missing 'vector' field in JSON update");
            }
            double[] vector = new double[vectorList.size()];
            for (int i = 0; i < vectorList.size(); i++) {
                vector[i] = vectorList.get(i).doubleValue();
            }
            vectors.add(vector);

            // Extract metadata
            Map<String, Object> meta = (Map<String, Object>) update.get("metadata");
            metadata.add(meta);
        }
    }
}
