package com.veccy.cli.commands;

import com.veccy.base.Page;
import com.veccy.cli.CLIContext;

import java.util.List;
import java.util.Optional;

/**
 * CLI command to list vector IDs with pagination support.
 * <p>
 * Supports both simple listing with limit and cursor-based pagination
 * for efficient iteration over large datasets.
 * <p>
 * Examples:
 * <pre>
 *   list-vectors                    # List all vectors
 *   list-vectors --limit 10         # List first 10 vectors
 *   list-vectors --page-size 50     # First page of 50 vectors
 *   list-vectors --page-size 50 --cursor abc123  # Next page after cursor
 *   list-vectors --format json      # Output as JSON
 * </pre>
 */
public class ListVectorsCommand implements Command {

    @Override
    public String getName() {
        return "list-vectors";
    }

    @Override
    public String getDescription() {
        return "List vector IDs with optional pagination";
    }

    @Override
    public String getUsage() {
        return "list-vectors [--limit <n>] [--page-size <n>] [--cursor <cursor>] [--format <fmt>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        if (!context.hasOpenDatabase()) {
            System.err.println("Error: No database is open. Use 'open' command first.");
            return;
        }

        // Parse arguments
        Integer limit = null;
        Integer pageSize = null;
        String cursor = null;
        String format = context.getOutputFormat();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--limit":
                    if (i + 1 < args.length) {
                        try {
                            limit = Integer.parseInt(args[++i]);
                            if (limit <= 0) {
                                System.err.println("Error: Limit must be positive");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid limit: " + args[i]);
                            return;
                        }
                    } else {
                        System.err.println("Error: --limit requires a value");
                        return;
                    }
                    break;

                case "--page-size":
                    if (i + 1 < args.length) {
                        try {
                            pageSize = Integer.parseInt(args[++i]);
                            if (pageSize <= 0) {
                                System.err.println("Error: Page size must be positive");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("Error: Invalid page size: " + args[i]);
                            return;
                        }
                    } else {
                        System.err.println("Error: --page-size requires a value");
                        return;
                    }
                    break;

                case "--cursor":
                    if (i + 1 < args.length) {
                        cursor = args[++i];
                    } else {
                        System.err.println("Error: --cursor requires a value");
                        return;
                    }
                    break;

                case "--format":
                    if (i + 1 < args.length) {
                        format = args[++i];
                        if (!format.equals("table") && !format.equals("json") && !format.equals("csv")) {
                            System.err.println("Error: Invalid format. Must be: table, json, or csv");
                            return;
                        }
                    } else {
                        System.err.println("Error: --format requires a value");
                        return;
                    }
                    break;

                default:
                    System.err.println("Error: Unknown option: " + args[i]);
                    System.err.println("Usage: " + getUsage());
                    return;
            }
        }

        try {
            // Use pagination if page-size is specified
            if (pageSize != null) {
                Page<String> page = context.getClient().listVectorIdsPaginated(
                        pageSize,
                        cursor != null ? Optional.of(cursor) : Optional.empty()
                );
                displayPaginatedResults(page, format, context.isVerbose());
            } else {
                // Simple listing with optional limit
                List<String> vectorIds = context.getClient().listVectorIds(limit);
                displaySimpleResults(vectorIds, format, context.isVerbose());
            }
        } catch (Exception e) {
            System.err.println("Error listing vectors: " + e.getMessage());
            if (context.isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    private void displayPaginatedResults(Page<String> page, String format, boolean verbose) {
        switch (format) {
            case "json":
                displayPaginatedJson(page);
                break;
            case "csv":
                displayPaginatedCsv(page);
                break;
            default:
                displayPaginatedTable(page, verbose);
                break;
        }
    }

    private void displayPaginatedTable(Page<String> page, boolean verbose) {
        System.out.println("Vector IDs (Page):");
        System.out.println("─".repeat(80));

        if (page.isEmpty()) {
            System.out.println("No vectors found.");
        } else {
            int index = 1;
            for (String id : page.items()) {
                System.out.printf("%4d. %s%n", index++, id);
            }

            System.out.println("─".repeat(80));
            System.out.println("Page size: " + page.size());

            if (page.hasMore()) {
                System.out.println("Has more: Yes");
                page.nextCursor().ifPresent(cursor ->
                        System.out.println("Next cursor: " + cursor)
                );
                if (verbose) {
                    System.out.println("\nTo get next page, use:");
                    System.out.println("  list-vectors --page-size " + page.size() +
                            " --cursor " + page.nextCursor().orElse(""));
                }
            } else {
                System.out.println("Has more: No (last page)");
            }
        }
    }

    private void displayPaginatedJson(Page<String> page) {
        System.out.println("{");
        System.out.println("  \"items\": [");

        List<String> items = page.items();
        for (int i = 0; i < items.size(); i++) {
            System.out.print("    \"" + items.get(i) + "\"");
            if (i < items.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }

        System.out.println("  ],");
        System.out.println("  \"size\": " + page.size() + ",");
        System.out.println("  \"hasMore\": " + page.hasMore() + ",");

        if (page.nextCursor().isPresent()) {
            System.out.println("  \"nextCursor\": \"" + page.nextCursor().get() + "\"");
        } else {
            System.out.println("  \"nextCursor\": null");
        }

        System.out.println("}");
    }

    private void displayPaginatedCsv(Page<String> page) {
        // Header
        System.out.println("index,id");

        // Data
        int index = 1;
        for (String id : page.items()) {
            System.out.println(index++ + "," + id);
        }

        // Metadata as comment
        System.out.println("# Page size: " + page.size());
        System.out.println("# Has more: " + page.hasMore());
        page.nextCursor().ifPresent(cursor ->
                System.out.println("# Next cursor: " + cursor)
        );
    }

    private void displaySimpleResults(List<String> vectorIds, String format, boolean verbose) {
        switch (format) {
            case "json":
                displaySimpleJson(vectorIds);
                break;
            case "csv":
                displaySimpleCsv(vectorIds);
                break;
            default:
                displaySimpleTable(vectorIds, verbose);
                break;
        }
    }

    private void displaySimpleTable(List<String> vectorIds, boolean verbose) {
        System.out.println("Vector IDs:");
        System.out.println("─".repeat(80));

        if (vectorIds.isEmpty()) {
            System.out.println("No vectors found.");
        } else {
            int index = 1;
            for (String id : vectorIds) {
                System.out.printf("%4d. %s%n", index++, id);
            }

            System.out.println("─".repeat(80));
            System.out.println("Total: " + vectorIds.size());

            if (verbose) {
                System.out.println("\nTip: Use --page-size for paginated listing");
            }
        }
    }

    private void displaySimpleJson(List<String> vectorIds) {
        System.out.println("{");
        System.out.println("  \"vectorIds\": [");

        for (int i = 0; i < vectorIds.size(); i++) {
            System.out.print("    \"" + vectorIds.get(i) + "\"");
            if (i < vectorIds.size() - 1) {
                System.out.println(",");
            } else {
                System.out.println();
            }
        }

        System.out.println("  ],");
        System.out.println("  \"total\": " + vectorIds.size());
        System.out.println("}");
    }

    private void displaySimpleCsv(List<String> vectorIds) {
        System.out.println("index,id");
        int index = 1;
        for (String id : vectorIds) {
            System.out.println(index++ + "," + id);
        }
        System.out.println("# Total: " + vectorIds.size());
    }
}
