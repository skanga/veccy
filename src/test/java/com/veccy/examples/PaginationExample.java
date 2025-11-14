package com.veccy.examples;

import com.veccy.base.Page;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Demonstrates pagination features in Veccy.
 * <p>
 * This example shows:
 * - Basic cursor-based pagination
 * - Iterating through all pages
 * - Streaming API
 * - Export by pages
 * - Pattern matching with pagination
 * - Rate-limited batch processing
 */
public class PaginationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=" + "=".repeat(79));
        System.out.println("Veccy Pagination Examples");
        System.out.println("=" + "=".repeat(79));
        System.out.println();

        // Create and initialize client with test data
        VectorDBClient client = setupTestData();

        try {
            // Example 1: Basic pagination
            System.out.println("Example 1: Basic Pagination");
            System.out.println("-".repeat(80));
            basicPaginationExample(client);
            System.out.println();

            // Example 2: Iterate through all pages
            System.out.println("Example 2: Iterate Through All Pages");
            System.out.println("-".repeat(80));
            iterateAllPagesExample(client);
            System.out.println();

            // Example 3: Streaming API
            System.out.println("Example 3: Streaming API");
            System.out.println("-".repeat(80));
            streamingExample(client);
            System.out.println();

            // Example 4: Export by pages
            System.out.println("Example 4: Export by Pages");
            System.out.println("-".repeat(80));
            exportByPagesExample(client);
            System.out.println();

            // Example 5: Pattern matching
            System.out.println("Example 5: Find Vectors Matching Pattern");
            System.out.println("-".repeat(80));
            patternMatchingExample(client);
            System.out.println();

            // Example 6: Rate-limited processing
            System.out.println("Example 6: Rate-Limited Batch Processing");
            System.out.println("-".repeat(80));
            rateLimitedProcessingExample(client);
            System.out.println();

            System.out.println("=" + "=".repeat(79));
            System.out.println("All pagination examples completed successfully!");
            System.out.println("=" + "=".repeat(79));

        } finally {
            client.close();
        }
    }

    /**
     * Setup test database with sample vectors.
     */
    private static VectorDBClient setupTestData() {
        System.out.println("Setting up test data...");

        VectorDBClient client = VectorDBFactory.createSimple();
        client.initialize();

        // Insert 500 test vectors
        int numVectors = 500;
        for (int i = 0; i < numVectors; i++) {
            double[] vector = {i * 0.1, i * 0.2, i * 0.3};
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("index", i);
            metadata.put("category", "category_" + (i % 5));

            client.insert(new double[][]{vector}, List.of(metadata));
        }

        System.out.println("✓ Created database with " + numVectors + " vectors");
        System.out.println();

        return client;
    }

    /**
     * Example 1: Basic pagination - fetching first two pages.
     */
    private static void basicPaginationExample(VectorDBClient client) {
        int pageSize = 20;

        // First page
        System.out.println("Fetching first page (page size: " + pageSize + ")...");
        Page<String> page1 = client.listVectorIdsPaginated(pageSize, Optional.empty());

        System.out.println("Page 1:");
        System.out.println("  Items: " + page1.size());
        System.out.println("  Has more: " + page1.hasMore());
        System.out.println("  Sample IDs: " + page1.items().subList(0, Math.min(5, page1.size())));

        if (page1.hasMore()) {
            // Second page
            System.out.println("\nFetching second page...");
            Page<String> page2 = client.listVectorIdsPaginated(pageSize, page1.nextCursor());

            System.out.println("Page 2:");
            System.out.println("  Items: " + page2.size());
            System.out.println("  Has more: " + page2.hasMore());
            System.out.println("  Sample IDs: " + page2.items().subList(0, Math.min(5, page2.size())));
            System.out.println("  Cursor: " + page2.nextCursor().orElse("none"));
        }
    }

    /**
     * Example 2: Iterate through all pages with progress reporting.
     */
    private static void iterateAllPagesExample(VectorDBClient client) {
        int pageSize = 50;
        int totalProcessed = 0;
        int pageNumber = 1;
        Optional<String> cursor = Optional.empty();

        System.out.println("Iterating through all pages (page size: " + pageSize + ")...");
        System.out.println();

        long startTime = System.currentTimeMillis();

        while (true) {
            Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

            if (page.isEmpty()) {
                break;
            }

            totalProcessed += page.size();
            System.out.printf("Page %2d: %3d items (total: %d)%n",
                    pageNumber, page.size(), totalProcessed);

            if (!page.hasMore()) {
                break;
            }

            cursor = page.nextCursor();
            pageNumber++;
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("\n✓ Processed " + totalProcessed + " vectors in " +
                pageNumber + " pages (" + duration + " ms)");
    }

    /**
     * Example 3: Using streaming API for simple iteration.
     */
    private static void streamingExample(VectorDBClient client) {
        System.out.println("Streaming all vector IDs...");

        long startTime = System.currentTimeMillis();
        long count;

        try (Stream<String> stream = client.streamVectorIds()) {
            count = stream
                    .peek(id -> {
                        // Process each ID
                        if (Math.random() < 0.001) { // Sample output
                            System.out.println("  Processing: " + id);
                        }
                    })
                    .count();
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("✓ Streamed " + count + " vectors (" + duration + " ms)");
    }

    /**
     * Example 4: Export vectors by pages to separate files.
     */
    private static void exportByPagesExample(VectorDBClient client) throws IOException {
        Path tempDir = Files.createTempDirectory("veccy_export");
        int pageSize = 100;
        int pageNumber = 1;
        Optional<String> cursor = Optional.empty();

        System.out.println("Exporting vectors to: " + tempDir);
        System.out.println();

        while (true) {
            Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

            if (page.isEmpty()) {
                break;
            }

            // Export page to file
            Path pageFile = tempDir.resolve("vectors_page_" + pageNumber + ".txt");
            Files.write(pageFile, page.items());

            System.out.printf("  Exported page %d: %d vectors to %s%n",
                    pageNumber, page.size(), pageFile.getFileName());

            if (!page.hasMore()) {
                break;
            }

            cursor = page.nextCursor();
            pageNumber++;
        }

        System.out.println("\n✓ Export complete: " + (pageNumber) + " files in " + tempDir);
    }

    /**
     * Example 5: Find vectors matching a pattern using pagination.
     */
    private static void patternMatchingExample(VectorDBClient client) {
        // Find all vectors with IDs containing "42"
        String pattern = ".*42.*";
        List<String> matches = new ArrayList<>();
        int pageSize = 100;
        Optional<String> cursor = Optional.empty();

        System.out.println("Finding vectors matching pattern: " + pattern);

        while (true) {
            Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

            if (page.isEmpty()) {
                break;
            }

            // Filter matching IDs
            page.items().stream()
                    .filter(id -> id.matches(pattern))
                    .forEach(matches::add);

            if (!page.hasMore()) {
                break;
            }

            cursor = page.nextCursor();
        }

        System.out.println("✓ Found " + matches.size() + " matching vectors");
        if (!matches.isEmpty()) {
            System.out.println("  Sample matches: " +
                    matches.subList(0, Math.min(5, matches.size())));
        }
    }

    /**
     * Example 6: Rate-limited batch processing.
     */
    private static void rateLimitedProcessingExample(VectorDBClient client)
            throws InterruptedException {
        int pageSize = 50;
        int rateLimit = 100; // vectors per second
        int processed = 0;
        Optional<String> cursor = Optional.empty();
        long startTime = System.currentTimeMillis();

        System.out.println("Processing with rate limit: " + rateLimit + " vectors/second");
        System.out.println();

        // Process only first 150 for demo
        int maxToProcess = 150;

        while (processed < maxToProcess) {
            Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

            if (page.isEmpty()) {
                break;
            }

            for (String id : page.items()) {
                if (processed >= maxToProcess) {
                    break;
                }

                // Simulate processing
                processVector(id);
                processed++;

                // Rate limiting
                if (processed % 25 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    long expectedTime = (processed * 1000L) / rateLimit;

                    if (elapsed < expectedTime) {
                        long sleepTime = expectedTime - elapsed;
                        Thread.sleep(sleepTime);
                    }

                    double actualRate = processed * 1000.0 / elapsed;
                    System.out.printf("  Processed: %3d vectors (rate: %.1f/sec)%n",
                            processed, actualRate);
                }
            }

            if (!page.hasMore() || processed >= maxToProcess) {
                break;
            }

            cursor = page.nextCursor();
        }

        long duration = System.currentTimeMillis() - startTime;
        double actualRate = processed * 1000.0 / duration;
        System.out.println("\n✓ Processed " + processed + " vectors in " + duration +
                " ms (avg rate: " + String.format("%.1f", actualRate) + "/sec)");
    }

    /**
     * Simulate vector processing.
     */
    private static void processVector(String id) {
        // Simulate some work
        try {
            Thread.sleep(1); // 1ms per vector
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Display pagination statistics.
     */
    private static void displayPaginationStats(VectorDBClient client) {
        System.out.println("\nPagination Performance Statistics:");
        System.out.println("-".repeat(80));

        int[] pageSizes = {10, 50, 100, 500};

        for (int pageSize : pageSizes) {
            long startTime = System.currentTimeMillis();
            int pageCount = 0;
            int totalItems = 0;
            Optional<String> cursor = Optional.empty();

            while (true) {
                Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

                if (page.isEmpty()) {
                    break;
                }

                pageCount++;
                totalItems += page.size();

                if (!page.hasMore()) {
                    break;
                }

                cursor = page.nextCursor();
            }

            long duration = System.currentTimeMillis() - startTime;
            double itemsPerMs = totalItems / (double) duration;

            System.out.printf("Page size %4d: %3d pages, %4d items, %4d ms (%.1f items/ms)%n",
                    pageSize, pageCount, totalItems, duration, itemsPerMs);
        }
    }
}
