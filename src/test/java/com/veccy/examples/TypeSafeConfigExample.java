package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.exceptions.ConfigurationException;
import com.veccy.indices.HNSWIndex;
import com.veccy.storage.MemoryStorage;

import java.util.*;

/**
 * Example demonstrating type-safe configuration for HNSW index.
 */
public class TypeSafeConfigExample {

    public static void main(String[] args) {
        System.out.println("=== Type-Safe Configuration Examples ===\n");

        // Example 1: Using builder pattern
        example1_BuilderPattern();

        // Example 2: Using defaults
        example2_Defaults();

        // Example 3: High-quality index configuration
        example3_HighQuality();

        // Example 4: Fast construction configuration
        example4_FastConstruction();

        // Example 5: Deterministic behavior with random seed
        example5_DeterministicBehavior();

        // Example 6: Validation examples
        example6_Validation();
    }

    /**
     * Example 1: Using builder pattern for type-safe configuration.
     */
    private static void example1_BuilderPattern() {
        System.out.println("=== Example 1: Builder Pattern ===");

        // Create type-safe configuration using builder
        HNSWConfig config = HNSWConfig.builder()
                .m(24)
                .efConstruction(300)
                .efSearch(75)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();

        System.out.println("Created config: m=" + config.m() +
                ", efConstruction=" + config.efConstruction() +
                ", efSearch=" + config.efSearch() +
                ", metric=" + config.metric());

        // Use the config
        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        try (VectorDBClient client = new VectorDBClient(storage, index)) {
            client.initialize();

            // Insert and search
            double[][] vectors = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}};
            List<String> ids = client.insert(vectors, null);
            System.out.println("Inserted " + ids.size() + " vectors");

            double[] query = {1.0, 0.0, 0.0};
            List<SearchResult> results = client.search(query, 1);
            System.out.println("Search found " + results.size() + " results\n");
        }
    }

    /**
     * Example 2: Using default configuration.
     */
    private static void example2_Defaults() {
        System.out.println("=== Example 2: Default Configuration ===");

        // Use defaults (m=16, efConstruction=200, efSearch=50, metric=COSINE)
        HNSWConfig config = HNSWConfig.defaults();

        System.out.println("Default config: m=" + config.m() +
                ", efConstruction=" + config.efConstruction() +
                ", efSearch=" + config.efSearch() +
                ", metric=" + config.metric());

        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        try (VectorDBClient client = new VectorDBClient(storage, index)) {
            client.initialize();

            double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
            client.insert(vectors, null);
            System.out.println("Successfully inserted vectors with default config\n");
        }
    }

    /**
     * Example 3: High-quality index for production use.
     */
    private static void example3_HighQuality() {
        System.out.println("=== Example 3: High-Quality Index ===");

        // High-quality configuration for production search
        HNSWConfig config = HNSWConfig.builder()
                .m(32)              // More connections for better recall
                .efConstruction(400) // High-quality construction
                .efSearch(100)      // Thorough search
                .metric(Metric.EUCLIDEAN)
                .build();

        System.out.println("High-quality config: m=" + config.m() +
                ", efConstruction=" + config.efConstruction() +
                ", efSearch=" + config.efSearch());

        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        try (VectorDBClient client = new VectorDBClient(storage, index)) {
            client.initialize();

            // Insert more vectors to demonstrate high-quality index
            Random random = new Random(42);
            double[][] vectors = new double[100][10];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 10; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            long startTime = System.nanoTime();
            List<String> ids = client.insert(vectors, null);
            long duration = (System.nanoTime() - startTime) / 1_000_000;

            System.out.println("Inserted " + ids.size() + " vectors in " + duration + "ms");
            System.out.println("High-quality construction trades speed for better search quality\n");
        }
    }

    /**
     * Example 4: Fast construction for development/testing.
     */
    private static void example4_FastConstruction() {
        System.out.println("=== Example 4: Fast Construction ===");

        // Fast construction configuration for development
        HNSWConfig config = HNSWConfig.builder()
                .m(8)               // Fewer connections
                .efConstruction(100) // Faster construction
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();

        System.out.println("Fast construction config: m=" + config.m() +
                ", efConstruction=" + config.efConstruction());

        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        try (VectorDBClient client = new VectorDBClient(storage, index)) {
            client.initialize();

            // Insert vectors
            Random random = new Random(42);
            double[][] vectors = new double[100][10];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 10; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            long startTime = System.nanoTime();
            List<String> ids = client.insert(vectors, null);
            long duration = (System.nanoTime() - startTime) / 1_000_000;

            System.out.println("Inserted " + ids.size() + " vectors in " + duration + "ms");
            System.out.println("Fast construction is ideal for development and testing\n");
        }
    }

    /**
     * Example 5: Deterministic behavior with random seed.
     */
    private static void example5_DeterministicBehavior() {
        System.out.println("=== Example 5: Deterministic Behavior ===");

        // Create two indices with same random seed
        HNSWConfig config1 = HNSWConfig.builder()
                .randomSeed(12345L)
                .build();

        HNSWConfig config2 = HNSWConfig.builder()
                .randomSeed(12345L)
                .build();

        System.out.println("Created two configs with same random seed: " +
                config1.randomSeed().get());

        // Create two clients
        HNSWIndex index1 = new HNSWIndex(config1);
        MemoryStorage storage1 = new MemoryStorage(new HashMap<>());
        storage1.initialize();

        HNSWIndex index2 = new HNSWIndex(config2);
        MemoryStorage storage2 = new MemoryStorage(new HashMap<>());
        storage2.initialize();

        try (VectorDBClient client1 = new VectorDBClient(storage1, index1);
             VectorDBClient client2 = new VectorDBClient(storage2, index2)) {

            client1.initialize();
            client2.initialize();

            // Insert same data
            Random random = new Random(999);
            double[][] vectors = new double[50][5];
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 5; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            client1.insert(vectors, null);
            client2.insert(vectors, null);

            // Search should produce identical results
            double[] query = {1.0, 0.0, 0.0, 0.0, 0.0};
            List<SearchResult> results1 = client1.search(query, 5);
            List<SearchResult> results2 = client2.search(query, 5);

            System.out.println("Both indices found " + results1.size() + " results");
            System.out.println("Results are deterministic (same random seed produces same behavior)\n");
        }
    }

    /**
     * Example 6: Validation examples showing error handling.
     */
    private static void example6_Validation() {
        System.out.println("=== Example 6: Validation ===");

        // Demonstrate validation errors
        try {
            HNSWConfig.builder().m(1).build();
        } catch (ConfigurationException e) {
            System.out.println("Validation error (m too low): " + e.getMessage());
        }

        try {
            HNSWConfig.builder().m(101).build();
        } catch (ConfigurationException e) {
            System.out.println("Validation error (m too high): " + e.getMessage());
        }

        try {
            HNSWConfig.builder()
                    .efConstruction(100)
                    .efSearch(200)
                    .build();
        } catch (ConfigurationException e) {
            System.out.println("Validation error (efSearch > efConstruction): " + e.getMessage());
        }

        try {
            HNSWConfig.builder().metric(null).build();
        } catch (ConfigurationException e) {
            System.out.println("Validation error (null metric): " + e.getMessage());
        }

        // Valid configuration
        HNSWConfig validConfig = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();
        System.out.println("Valid configuration created successfully\n");
    }
}
