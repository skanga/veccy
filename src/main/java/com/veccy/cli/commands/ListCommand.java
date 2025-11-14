package com.veccy.cli.commands;

import com.veccy.base.Page;
import com.veccy.cli.CLIContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * List vectors in the database.
 */
public class ListCommand implements Command {

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"ls"};
    }

    @Override
    public String getDescription() {
        return "List vectors in the database";
    }

    @Override
    public String getUsage() {
        return "list [--limit <n>] [--format <format>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        requireOpenDatabase(context);

        int limit = 100;
        String format = context.getOutputFormat();

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--limit":
                case "-l":
                    if (i + 1 < args.length) {
                        limit = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--format":
                case "-f":
                    if (i + 1 < args.length) {
                        format = args[++i];
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        // Get vector IDs using pagination
        List<String> allIds = new ArrayList<>();
        Optional<String> cursor = Optional.empty();

        while (allIds.size() < limit) {
            int pageSize = Math.min(1000, limit - allIds.size());
            Page<String> page = context.getClient().listVectorIdsPaginated(pageSize, cursor);

            allIds.addAll(page.items());

            if (!page.hasMore() || allIds.size() >= limit) {
                break;
            }

            cursor = page.nextCursor();
        }

        // Get stats for total count
        Map<String, Object> stats = context.getClient().getStats();
        long totalCount = getTotalCount(stats);

        switch (format) {
            case "json" -> printJson(allIds, totalCount, limit);
            case "csv" -> printCsv(allIds);
            default -> printTable(allIds, totalCount, limit);
        }
    }

    private void printTable(List<String> ids, long totalCount, int limit) {
        if (ids.isEmpty()) {
            System.out.println("No vectors found in database");
            return;
        }

        System.out.println("Vector IDs (showing " + ids.size() + " of " + totalCount + "):");
        System.out.println();
        System.out.printf("%-6s %-36s%n", "Index", "ID");
        System.out.println("─".repeat(44));

        for (int i = 0; i < ids.size(); i++) {
            System.out.printf("%-6d %s%n", i + 1, ids.get(i));
        }

        System.out.println();
        if (ids.size() < totalCount) {
            System.out.println("Showing " + ids.size() + " of " + totalCount + " vectors. Use --limit to see more.");
        } else {
            System.out.println("Total: " + totalCount + " vector(s)");
        }
    }

    private void printJson(List<String> ids, long totalCount, int limit) {
        System.out.println("{");
        System.out.println("  \"total\": " + totalCount + ",");
        System.out.println("  \"limit\": " + limit + ",");
        System.out.println("  \"count\": " + ids.size() + ",");
        System.out.println("  \"ids\": [");
        for (int i = 0; i < ids.size(); i++) {
            System.out.print("    \"" + ids.get(i) + "\"");
            if (i < ids.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }
        System.out.println("  ]");
        System.out.println("}");
    }

    private void printCsv(List<String> ids) {
        System.out.println("index,id");
        for (int i = 0; i < ids.size(); i++) {
            System.out.println((i + 1) + "," + ids.get(i));
        }
    }

    @SuppressWarnings("unchecked")
    private long getTotalCount(Map<String, Object> stats) {
        try {
            if (stats.containsKey("storage")) {
                Map<String, Object> storage = (Map<String, Object>) stats.get("storage");
                if (storage.containsKey("vectorCount")) {
                    Object count = storage.get("vectorCount");
                    if (count instanceof Number) {
                        return ((Number) count).longValue();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return 0
        }
        return 0;
    }
}
