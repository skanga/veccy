package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Update a vector or its metadata.
 */
public class UpdateCommand implements Command {

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public String getDescription() {
        return "Update a vector or its metadata";
    }

    @Override
    public String getUsage() {
        return "update <id> --vector <vector> | --metadata <key=value>...";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        requireOpenDatabase(context);
        requireArgs(args, 1);

        String id = args[0];
        double[] vector = null;
        Map<String, Object> metadata = new HashMap<>();

        // Parse arguments
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--vector":
                case "-v":
                    if (i + 1 < args.length) {
                        vector = parseVector(args[++i]);
                    }
                    break;
                case "--metadata":
                case "-m":
                    if (i + 1 < args.length) {
                        String[] parts = args[++i].split("=", 2);
                        if (parts.length == 2) {
                            metadata.put(parts[0], parseValue(parts[1]));
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        if (vector == null && metadata.isEmpty()) {
            throw new IllegalArgumentException("Must provide --vector or --metadata. Usage: " + getUsage());
        }

        boolean success = context.getClient().update(id, vector, metadata.isEmpty() ? null : metadata);

        if (success) {
            System.out.println("Successfully updated vector: " + id);
            if (vector != null && context.isVerbose()) {
                System.out.println("  Updated vector dimension: " + vector.length);
            }
            if (!metadata.isEmpty()) {
                System.out.println("  Updated metadata: " + metadata);
            }
        } else {
            System.err.println("Failed to update vector: " + id);
            System.err.println("Vector may not exist in database");
        }
    }

    private double[] parseVector(String str) {
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

    private Object parseValue(String value) {
        // Try to parse as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Try to parse as boolean
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(value);
            }
            // Default to string
            return value;
        }
    }
}
