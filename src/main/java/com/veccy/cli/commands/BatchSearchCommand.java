package com.veccy.cli.commands;

import com.veccy.base.SearchResult;
import com.veccy.cli.CLIContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI command for batch searching multiple query vectors.
 * <p>
 * Supports reading queries from a file or command line arguments.
 * More efficient than running multiple individual searches.
 */
public class BatchSearchCommand implements Command {

    @Override
    public String getName() {
        return "batch-search";
    }

    @Override
    public String getDescription() {
        return "Search for similar vectors (batch mode)";
    }

    @Override
    public String getUsage() {
        return "batch-search <queries> [--top-k <k>] [--format <fmt>] [--show-vectors]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);

        if (args.length < 1) {
            System.err.println("Usage: batch-search <queries> [options]");
            System.err.println();
            System.err.println("Arguments:");
            System.err.println("  <queries>          Comma-separated queries or @file path");
            System.err.println();
            System.err.println("Options:");
            System.err.println("  --top-k <k>        Number of results per query (default: 10)");
            System.err.println("  --format <fmt>     Output format: table, json, csv (default: table)");
            System.err.println("  --show-vectors     Include vector values in output");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  batch-search \"[1,0,0],[0,1,0]\" --top-k 5");
            System.err.println("  batch-search @queries.txt --format json");
            return;
        }

        try {
            // Parse options
            int topK = 10;
            String format = context.getOutputFormat();
            boolean showVectors = false;

            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--top-k":
                        if (i + 1 < args.length) {
                            topK = Integer.parseInt(args[++i]);
                        }
                        break;
                    case "--format":
                        if (i + 1 < args.length) {
                            format = args[++i];
                        }
                        break;
                    case "--show-vectors":
                        showVectors = true;
                        break;
                }
            }

            // Parse query vectors
            List<double[]> queryVectors = new ArrayList<>();
            String queryArg = args[0];

            if (queryArg.startsWith("@")) {
                // Read from file
                String filename = queryArg.substring(1);
                queryVectors = readVectorsFromFile(filename);
                if (context.isVerbose()) {
                    System.out.println("Loaded " + queryVectors.size() + " query vectors from " + filename);
                }
            } else {
                // Parse from command line
                queryVectors = parseVectorsFromString(queryArg);
            }

            if (queryVectors.isEmpty()) {
                System.err.println("Error: No query vectors provided");
                return;
            }

            // Perform batch search
            long startTime = System.nanoTime();
            double[][] queryArray = queryVectors.toArray(new double[0][]);
            List<List<SearchResult>> results = context.getClient().batchSearch(queryArray, topK);
            long endTime = System.nanoTime();

            double totalTime = (endTime - startTime) / 1_000_000.0; // Convert to ms

            // Display results
            switch (format.toLowerCase()) {
                case "json":
                    printJsonResults(results, queryVectors, showVectors);
                    break;
                case "csv":
                    printCsvResults(results, queryVectors, showVectors);
                    break;
                default:
                    printTableResults(results, queryVectors, showVectors);
                    break;
            }

            // Print summary
            System.out.println();
            System.out.printf("Batch searched %d queries in %.2f ms (%.2f ms/query)%n",
                    queryVectors.size(), totalTime, totalTime / queryVectors.size());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (context.isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    private List<double[]> readVectorsFromFile(String filename) throws IOException {
        List<double[]> vectors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }
                vectors.add(parseVector(line));
            }
        }
        return vectors;
    }

    private List<double[]> parseVectorsFromString(String input) {
        List<double[]> vectors = new ArrayList<>();
        // Split by "]," to handle multiple vectors
        String[] parts = input.split("\\],");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (!part.endsWith("]")) {
                part = part + "]"; // Add back the removed bracket
            }
            vectors.add(parseVector(part));
        }
        return vectors;
    }

    private double[] parseVector(String vectorStr) {
        vectorStr = vectorStr.trim();
        if (vectorStr.startsWith("[")) {
            vectorStr = vectorStr.substring(1);
        }
        if (vectorStr.endsWith("]")) {
            vectorStr = vectorStr.substring(0, vectorStr.length() - 1);
        }

        String[] parts = vectorStr.split(",");
        double[] vector = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Double.parseDouble(parts[i].trim());
        }
        return vector;
    }

    private void printTableResults(List<List<SearchResult>> results, List<double[]> queries, boolean showVectors) {
        for (int i = 0; i < results.size(); i++) {
            System.out.println();
            System.out.printf("Query %d", i + 1);
            if (showVectors) {
                System.out.print(": " + formatVector(queries.get(i)));
            }
            System.out.println();
            System.out.println("─".repeat(80));

            List<SearchResult> queryResults = results.get(i);
            if (queryResults.isEmpty()) {
                System.out.println("No results found");
                continue;
            }

            System.out.printf("%-10s %-15s %-40s%n", "Rank", "Distance", "ID");
            System.out.println("─".repeat(80));

            for (int j = 0; j < queryResults.size(); j++) {
                SearchResult result = queryResults.get(j);
                System.out.printf("%-10d %-15.6f %-40s%n",
                        j + 1,
                        result.distance(),
                        result.id());
            }
        }
    }

    private void printJsonResults(List<List<SearchResult>> results, List<double[]> queries, boolean showVectors) {
        System.out.println("{");
        System.out.println("  \"results\": [");

        for (int i = 0; i < results.size(); i++) {
            System.out.println("    {");
            System.out.printf("      \"query_index\": %d,%n", i);
            if (showVectors) {
                System.out.printf("      \"query_vector\": %s,%n", formatJsonVector(queries.get(i)));
            }
            System.out.println("      \"matches\": [");

            List<SearchResult> queryResults = results.get(i);
            for (int j = 0; j < queryResults.size(); j++) {
                SearchResult result = queryResults.get(j);
                System.out.println("        {");
                System.out.printf("          \"rank\": %d,%n", j + 1);
                System.out.printf("          \"id\": \"%s\",%n", result.id());
                System.out.printf("          \"distance\": %.6f%n", result.distance());
                System.out.print("        }");
                if (j < queryResults.size() - 1) {
                    System.out.println(",");
                } else {
                    System.out.println();
                }
            }

            System.out.print("      ]\n    }");
            if (i < results.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }

        System.out.println("  ]");
        System.out.println("}");
    }

    private void printCsvResults(List<List<SearchResult>> results, List<double[]> queries, boolean showVectors) {
        // CSV header
        if (showVectors) {
            System.out.println("query_index,query_vector,rank,id,distance");
        } else {
            System.out.println("query_index,rank,id,distance");
        }

        for (int i = 0; i < results.size(); i++) {
            List<SearchResult> queryResults = results.get(i);
            String queryVector = showVectors ? "\"" + formatVector(queries.get(i)) + "\"" : "";

            for (int j = 0; j < queryResults.size(); j++) {
                SearchResult result = queryResults.get(j);
                if (showVectors) {
                    System.out.printf("%d,%s,%d,%s,%.6f%n",
                            i, queryVector, j + 1, result.id(), result.distance());
                } else {
                    System.out.printf("%d,%d,%s,%.6f%n",
                            i, j + 1, result.id(), result.distance());
                }
            }
        }
    }

    private String formatVector(double[] vector) {
        if (vector.length <= 5) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < vector.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("%.3f", vector[i]));
            }
            sb.append("]");
            return sb.toString();
        } else {
            return String.format("[%.3f, %.3f, ... %.3f, %.3f] (%d dims)",
                    vector[0], vector[1], vector[vector.length - 2], vector[vector.length - 1], vector.length);
        }
    }

    private String formatJsonVector(double[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
