package com.veccy.cli.commands;

import com.veccy.base.Page;
import com.veccy.cli.CLIContext;
import com.veccy.storage.StorageBackend;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Export vectors to a file.
 */
public class ExportCommand implements Command {

    @Override
    public String getName() {
        return "export";
    }

    @Override
    public String getDescription() {
        return "Export vectors to a file";
    }

    @Override
    public String getUsage() {
        return "export <file> [--format <format>]";
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

        // Check if file exists and prompt for overwrite
        if (Files.exists(path)) {
            System.out.println("Warning: File already exists and will be overwritten: " + filePath);
        }

        long startTime = System.currentTimeMillis();
        int count;

        switch (format.toLowerCase()) {
            case "json" -> count = exportJson(context, path);
            case "csv" -> count = exportCsv(context, path);
            default -> throw new IllegalArgumentException("Unsupported format: " + format + ". Use 'json' or 'csv'");
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Successfully exported " + count + " vector(s) to: " + filePath);
        System.out.println("Time: " + duration + "ms");
    }

    private int exportCsv(CLIContext context, Path path) throws IOException {
        StorageBackend storage = context.getClient().getStorageBackend();
        int count = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            // Write header
            writer.write("id,vector,metadata");
            writer.newLine();

            // Stream all vectors
            Optional<String> cursor = Optional.empty();
            boolean hasMore = true;

            while (hasMore) {
                Page<String> page = context.getClient().listVectorIdsPaginated(1000, cursor);

                for (String id : page.items()) {
                    // Get vector and metadata
                    Optional<StorageBackend.VectorWithMetadata> dataOpt = storage.retrieveVector(id);

                    if (dataOpt.isPresent()) {
                        StorageBackend.VectorWithMetadata data = dataOpt.get();
                        double[] vector = data.getVector();
                        Map<String, Object> metadata = data.getMetadata();

                        // Write ID
                        writer.write(id);
                        writer.write(",\"");

                        // Write vector
                        for (int i = 0; i < vector.length; i++) {
                            if (i > 0) writer.write(",");
                            writer.write(String.valueOf(vector[i]));
                        }
                        writer.write("\"");

                        // Write metadata
                        if (metadata != null && !metadata.isEmpty()) {
                            writer.write(",\"");
                            writer.write(formatMetadata(metadata));
                            writer.write("\"");
                        } else {
                            writer.write(",");
                        }

                        writer.newLine();
                        count++;
                    }
                }

                hasMore = page.hasMore();
                cursor = page.nextCursor();
            }
        }

        return count;
    }

    private int exportJson(CLIContext context, Path path) throws IOException {
        StorageBackend storage = context.getClient().getStorageBackend();
        int count = 0;

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("[");
            writer.newLine();

            // Stream all vectors
            Optional<String> cursor = Optional.empty();
            boolean hasMore = true;
            boolean first = true;

            while (hasMore) {
                Page<String> page = context.getClient().listVectorIdsPaginated(1000, cursor);

                for (String id : page.items()) {
                    // Get vector and metadata
                    Optional<StorageBackend.VectorWithMetadata> dataOpt = storage.retrieveVector(id);

                    if (dataOpt.isPresent()) {
                        if (!first) {
                            writer.write(",");
                            writer.newLine();
                        }
                        first = false;

                        StorageBackend.VectorWithMetadata data = dataOpt.get();
                        double[] vector = data.getVector();
                        Map<String, Object> metadata = data.getMetadata();

                        writer.write("  {");
                        writer.write("\"id\": \"" + id + "\", ");
                        writer.write("\"vector\": [");

                        for (int i = 0; i < vector.length; i++) {
                            if (i > 0) writer.write(", ");
                            writer.write(String.valueOf(vector[i]));
                        }
                        writer.write("]");

                        if (metadata != null && !metadata.isEmpty()) {
                            writer.write(", \"metadata\": ");
                            writer.write(formatMetadataJson(metadata));
                        }

                        writer.write("}");
                        count++;
                    }
                }

                hasMore = page.hasMore();
                cursor = page.nextCursor();
            }

            writer.newLine();
            writer.write("]");
            writer.newLine();
        }

        return count;
    }

    private String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (i++ > 0) sb.append(";");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString().replace("\"", "\"\"");
    }

    private String formatMetadataJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (i++ > 0) sb.append(", ");
            sb.append("\"").append(entry.getKey()).append("\": ");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
