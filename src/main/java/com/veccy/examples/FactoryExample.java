package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;

import java.util.List;
import java.util.Map;

/**
 * Example demonstrating the use of VectorDBFactory for easy database creation.
 */
public class FactoryExample {

    public static void main(String[] args) {
        System.out.println("=== Veccy Factory Example ===\n");

        // Example 1: Simple in-memory database
        simpleExample();

        // Example 2: High-performance HNSW database
        highPerformanceExample();

        // Example 3: Persistent database
        persistentExample();

        // Example 4: Memory-optimized with quantization
        memoryOptimizedExample();
    }

    private static void simpleExample() {
        System.out.println("1. Simple In-Memory Database");
        System.out.println("   Using FlatIndex with exact search\n");

        try (VectorDBClient client = VectorDBFactory.createSimple()) {
            // Insert vectors
            double[][] vectors = {
                    {1.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0},
                    {0.0, 0.0, 1.0},
                    {0.7, 0.7, 0.0}
            };

            List<String> ids = client.insert(vectors, null);
            System.out.println("   Inserted " + ids.size() + " vectors");

            // Search
            double[] query = {0.8, 0.6, 0.0};
            List<SearchResult> results = client.search(query, 2);

            System.out.println("   Top 2 results:");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                System.out.printf("     %d. Distance: %.4f%n", i + 1, result.getDistance());
            }
            System.out.println();

        }
    }

    private static void highPerformanceExample() {
        System.out.println("2. High-Performance HNSW Database");
        System.out.println("   Optimized for speed and accuracy\n");

        try (VectorDBClient client = VectorDBFactory.createHighPerformance()) {
            // Generate and insert many vectors
            int numVectors = 1000;
            double[][] vectors = generateRandomVectors(numVectors, 128);

            long startTime = System.currentTimeMillis();
            List<String> ids = client.insert(vectors, null);
            long insertTime = System.currentTimeMillis() - startTime;

            System.out.println("   Inserted " + ids.size() + " vectors in " + insertTime + "ms");

            // Perform search
            double[] query = vectors[0];
            startTime = System.currentTimeMillis();
            List<SearchResult> results = client.search(query, 10);
            long searchTime = System.currentTimeMillis() - startTime;

            System.out.println("   Found " + results.size() + " results in " + searchTime + "ms");

            Map<String, Object> stats = client.getStats();
            System.out.println("   Index stats: " + stats.get("index"));
            System.out.println();

        }
    }

    private static void persistentExample() {
        System.out.println("3. Persistent Database");
        System.out.println("   Data saved to disk\n");

        String dataDir = "./veccy_example_data";

        try (VectorDBClient client = VectorDBFactory.createPersistent(dataDir, true)) {
            // Insert vectors with metadata
            double[][] vectors = {
                    {1.0, 2.0, 3.0},
                    {4.0, 5.0, 6.0},
                    {7.0, 8.0, 9.0}
            };

            List<Map<String, Object>> metadata = List.of(
                    Map.of("label", "first", "category", "A"),
                    Map.of("label", "second", "category", "B"),
                    Map.of("label", "third", "category", "A")
            );

            List<String> ids = client.insert(vectors, metadata);
            System.out.println("   Inserted " + ids.size() + " vectors with metadata");

            // Search and display metadata
            double[] query = {1.5, 2.5, 3.5};
            List<SearchResult> results = client.search(query, 2);

            System.out.println("   Results with metadata:");
            for (SearchResult result : results) {
                System.out.printf("     Distance: %.4f, Metadata: %s%n",
                        result.getDistance(), result.getMetadata());
            }
            System.out.println();

        }
    }

    private static void memoryOptimizedExample() {
        System.out.println("4. Memory-Optimized Database");
        System.out.println("   Using 8-bit quantization for compression\n");

        try (VectorDBClient client = VectorDBFactory.createMemoryOptimized(8)) {
            // Insert vectors
            double[][] vectors = generateRandomVectors(500, 64);

            List<String> ids = client.insert(vectors, null);
            System.out.println("   Inserted " + ids.size() + " vectors");

            Map<String, Object> stats = client.getStats();
            System.out.println("   Storage stats: " + stats.get("storage"));
            System.out.println("   Quantization stats: " + stats.get("quantization"));
            System.out.println();

        }
    }

    /**
     * Generate random vectors for testing.
     */
    private static double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        return vectors;
    }
}
