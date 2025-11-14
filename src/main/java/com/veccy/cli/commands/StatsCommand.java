package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

import java.util.Map;

/**
 * Display database statistics.
 */
public class StatsCommand implements Command {

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Display database statistics";
    }

    @Override
    public String getUsage() {
        return "stats [--format <format>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);

        String format = context.getOutputFormat();

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            if ((args[i].equals("--format") || args[i].equals("-f")) && i + 1 < args.length) {
                format = args[++i];
            }
        }

        Map<String, Object> stats = context.getClient().getStats();

        switch (format) {
            case "json" -> printJson(stats);
            case "table" -> printTable(stats);
            default -> printTable(stats);
        }
    }

    @SuppressWarnings("unchecked")
    private void printTable(Map<String, Object> stats) {
        System.out.println("Database Statistics:");
        System.out.println();

        if (stats.containsKey("storage")) {
            System.out.println("Storage:");
            Map<String, Object> storage = (Map<String, Object>) stats.get("storage");
            for (Map.Entry<String, Object> entry : storage.entrySet()) {
                System.out.printf("  %-20s: %s%n", entry.getKey(), entry.getValue());
            }
            System.out.println();
        }

        if (stats.containsKey("index")) {
            System.out.println("Index:");
            Map<String, Object> index = (Map<String, Object>) stats.get("index");
            for (Map.Entry<String, Object> entry : index.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    System.out.println("  " + entry.getKey() + ":");
                    Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                    for (Map.Entry<String, Object> nestedEntry : nested.entrySet()) {
                        System.out.printf("    %-18s: %s%n", nestedEntry.getKey(), nestedEntry.getValue());
                    }
                } else {
                    System.out.printf("  %-20s: %s%n", entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void printJson(Map<String, Object> stats) {
        // Simple JSON formatting
        System.out.println("{");
        printJsonObject(stats, "  ");
        System.out.println("}");
    }

    @SuppressWarnings("unchecked")
    private void printJsonObject(Map<String, Object> obj, String indent) {
        int i = 0;
        int size = obj.size();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            System.out.print(indent + "\"" + entry.getKey() + "\": ");
            Object value = entry.getValue();
            if (value instanceof Map) {
                System.out.println("{");
                printJsonObject((Map<String, Object>) value, indent + "  ");
                System.out.print(indent + "}");
            } else if (value instanceof String) {
                System.out.print("\"" + value + "\"");
            } else {
                System.out.print(value);
            }
            if (++i < size) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
    }
}
