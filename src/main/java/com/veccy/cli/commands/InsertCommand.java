package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

import java.util.*;

/**
 * Insert vectors into the database.
 */
public class InsertCommand implements Command {

    @Override
    public String getName() {
        return "insert";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"add"};
    }

    @Override
    public String getDescription() {
        return "Insert vectors into the database";
    }

    @Override
    public String getUsage() {
        return "insert <vector> [--id <id>] [--metadata <key=value>...]\n" +
                "       insert --file <path>";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);
        requireArgs(args, 1);

        if (args[0].equals("--file") || args[0].equals("-f")) {
            insertFromFile(context, args);
        } else {
            insertSingle(context, args);
        }
    }

    private void insertSingle(CLIContext context, String[] args) {
        double[] vector = null;
        String id = null;
        Map<String, Object> metadata = new HashMap<>();

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--id":
                    if (i + 1 < args.length) {
                        id = args[++i];
                    }
                    break;
                case "--metadata":
                case "-m":
                    if (i + 1 < args.length) {
                        String[] parts = args[++i].split("=", 2);
                        if (parts.length == 2) {
                            metadata.put(parts[0], parts[1]);
                        }
                    }
                    break;
                default:
                    if (vector == null) {
                        vector = parseVector(args[i]);
                    }
                    break;
            }
        }

        if (vector == null) {
            throw new IllegalArgumentException("No vector provided");
        }

        double[][] vectors = {vector};
        List<Map<String, Object>> metadataList = metadata.isEmpty() ? null : List.of(metadata);

        List<String> ids = context.getClient().insert(vectors, metadataList);

        System.out.println("✓ Inserted 1 vector");
        System.out.println("  ID: " + ids.get(0));
        if (!metadata.isEmpty()) {
            System.out.println("  Metadata: " + metadata);
        }
    }

    private void insertFromFile(CLIContext context, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("File path required");
        }

        String path = args[1];
        System.err.println("File import not yet implemented: " + path);
        System.err.println("Use 'import' command instead");
    }

    /**
     * Parse vector from string like "[1.0, 2.0, 3.0]" or "1.0,2.0,3.0"
     */
    private double[] parseVector(String str) {
        // Remove brackets if present
        str = str.trim();
        if (str.startsWith("[")) {
            str = str.substring(1);
        }
        if (str.endsWith("]")) {
            str = str.substring(0, str.length() - 1);
        }

        String[] parts = str.split(",");
        double[] vector = new double[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                vector[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid vector component: " + parts[i]);
            }
        }

        return vector;
    }
}
