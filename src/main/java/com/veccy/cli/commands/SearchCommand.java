package com.veccy.cli.commands;

import com.veccy.base.SearchResult;
import com.veccy.cli.CLIContext;

import java.util.List;

/**
 * Search for similar vectors.
 */
public class SearchCommand implements Command {

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"find", "query"};
    }

    @Override
    public String getDescription() {
        return "Search for similar vectors";
    }

    @Override
    public String getUsage() {
        return "search <vector> [--k <count>] [--format <format>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);
        requireArgs(args, 1);

        double[] query = null;
        int k = 10;
        String format = context.getOutputFormat();

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--k":
                case "-k":
                    if (i + 1 < args.length) {
                        k = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--format":
                case "-f":
                    if (i + 1 < args.length) {
                        format = args[++i];
                    }
                    break;
                default:
                    if (query == null) {
                        query = parseVector(args[i]);
                    }
                    break;
            }
        }

        if (query == null) {
            throw new IllegalArgumentException("No query vector provided");
        }

        List<SearchResult> results = context.getClient().search(query, k);

        switch (format) {
            case "json" -> printJson(results);
            case "csv" -> printCsv(results);
            default -> printTable(results);
        }
    }

    private void printTable(List<SearchResult> results) {
        if (results.isEmpty()) {
            System.out.println("No results found");
            return;
        }

        System.out.println("Search Results:");
        System.out.println();
        System.out.printf("%-4s %-36s %-12s %s%n", "Rank", "ID", "Distance", "Metadata");
        System.out.println("─".repeat(80));

        int rank = 1;
        for (SearchResult result : results) {
            String metadata = result.metadata() != null ? result.metadata().toString() : "";
            if (metadata.length() > 30) {
                metadata = metadata.substring(0, 27) + "...";
            }
            System.out.printf("%-4d %-36s %-12.6f %s%n",
                    rank++,
                    result.id(),
                    result.distance(),
                    metadata);
        }

        System.out.println();
        System.out.println("Found " + results.size() + " result(s)");
    }

    private void printJson(List<SearchResult> results) {
        System.out.println("[");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.print("  {");
            System.out.print("\"id\": \"" + result.id() + "\", ");
            System.out.print("\"distance\": " + result.distance());
            if (result.metadata() != null && !result.metadata().isEmpty()) {
                System.out.print(", \"metadata\": " + formatMetadataJson(result.metadata()));
            }
            System.out.print("}");
            if (i < results.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("]");
    }

    private void printCsv(List<SearchResult> results) {
        System.out.println("rank,id,distance,metadata");
        int rank = 1;
        for (SearchResult result : results) {
            String metadata = result.metadata() != null ? result.metadata().toString() : "";
            System.out.printf("%d,%s,%.6f,\"%s\"%n",
                    rank++,
                    result.id(),
                    result.distance(),
                    metadata.replace("\"", "\"\""));
        }
    }

    private String formatMetadataJson(java.util.Map<String, Object> metadata) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (java.util.Map.Entry<String, Object> entry : metadata.entrySet()) {
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
}
