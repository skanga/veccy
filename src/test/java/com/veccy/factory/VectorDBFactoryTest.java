package com.veccy.factory;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for VectorDBFactory.
 */
public class VectorDBFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    public void testCreateSimple() {
        VectorDBClient client = VectorDBFactory.createSimple();

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Test basic operations
            double[][] vectors = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0}
            };

            List<String> ids = client.insert(vectors, null);
            assertEquals(2, ids.size());

            // Search
            List<SearchResult> results = client.search(new double[]{1.0, 0.0, 0.0}, 1);
            assertEquals(1, results.size());
            assertTrue(results.get(0).getDistance() < 0.1);

            // Check stats
            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("FlatIndex", indexStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateHighPerformance() {
        VectorDBClient client = VectorDBFactory.createHighPerformance();

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Test with more vectors
            Random random = new Random(42);
            double[][] vectors = new double[100][64];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 64; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = client.insert(vectors, null);
            assertEquals(100, ids.size());

            // Search should be fast
            long startTime = System.currentTimeMillis();
            List<SearchResult> results = client.search(vectors[0], 10);
            long searchTime = System.currentTimeMillis() - startTime;

            assertTrue(results.size() > 0);
            System.out.println("High-performance search time: " + searchTime + "ms");

            // Check it's using HNSW
            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("HNSWIndex", indexStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreatePersistent_WithFlat() {
        String dataDir = tempDir.resolve("persistent_flat").toString();
        VectorDBClient client = VectorDBFactory.createPersistent(dataDir, false);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            double[][] vectors = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.0, 0.0, 1.0}
            };
            client.insert(vectors, null);

            // Check stats
            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
            assertEquals("DiskStorage", storageStats.get("type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("FlatIndex", indexStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreatePersistent_WithHNSW() {
        String dataDir = tempDir.resolve("persistent_hnsw").toString();
        VectorDBClient client = VectorDBFactory.createPersistent(dataDir, true);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            double[][] vectors = new double[50][32];
            Random random = new Random(42);
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = client.insert(vectors, null);
            assertEquals(50, ids.size());

            // Search
            List<SearchResult> results = client.search(vectors[0], 5);
            assertTrue(results.size() > 0);

            // Check stats
            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("HNSWIndex", indexStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateMemoryOptimized() {
        VectorDBClient client = VectorDBFactory.createMemoryOptimized(8);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            Random random = new Random(42);
            double[][] vectors = new double[50][64];
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 64; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = client.insert(vectors, null);
            assertEquals(50, ids.size());

            // Search
            List<SearchResult> results = client.search(vectors[0], 5);
            assertTrue(results.size() > 0);

            // Check stats - should have quantization
            Map<String, Object> stats = client.getStats();
            assertTrue(stats.containsKey("quantization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> quantStats = (Map<String, Object>) stats.get("quantization");
            assertEquals("ScalarQuantizer", quantStats.get("type"));
            assertEquals(8, quantStats.get("bits"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateWithPersistence() {
        String dataDir = tempDir.resolve("with_persist_data").toString();
        String persistDir = tempDir.resolve("with_persist_state").toString();

        VectorDBClient client = VectorDBFactory.createWithPersistence(
            dataDir, persistDir, true);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            double[][] vectors = new double[20][32];
            Random random = new Random(42);
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = client.insert(vectors, null);
            assertEquals(20, ids.size());

            // Check stats - should have persistence
            Map<String, Object> stats = client.getStats();
            assertTrue(stats.containsKey("persistence"));

            @SuppressWarnings("unchecked")
            Map<String, Object> persistStats = (Map<String, Object>) stats.get("persistence");
            assertEquals("TensorPersistence", persistStats.get("type"));
            assertEquals(true, persistStats.get("compression_enabled"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateCustom_MemoryFlat() {
        Map<String, Object> storageConfig = Map.of("type", "memory");
        Map<String, Object> indexConfig = Map.of("type", "flat", "metric", "cosine");

        VectorDBClient client = VectorDBFactory.createCustom(
            storageConfig, indexConfig, null, null);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Test basic operations
            double[][] vectors = {{1.0, 0.0, 0.0}};
            List<String> ids = client.insert(vectors, null);
            assertEquals(1, ids.size());

            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
            assertEquals("MemoryStorage", storageStats.get("type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("FlatIndex", indexStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateCustom_DiskHNSW() {
        String dataDir = tempDir.resolve("custom_disk").toString();

        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "disk");
        storageConfig.put("data_dir", dataDir);

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "hnsw");
        indexConfig.put("metric", "euclidean");
        indexConfig.put("m", 8);
        indexConfig.put("ef_construction", 100);
        indexConfig.put("ef_search", 25);

        VectorDBClient client = VectorDBFactory.createCustom(
            storageConfig, indexConfig, null, null);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            double[][] vectors = new double[30][16];
            Random random = new Random(42);
            for (int i = 0; i < 30; i++) {
                for (int j = 0; j < 16; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            client.insert(vectors, null);

            // Search
            List<SearchResult> results = client.search(vectors[0], 5);
            assertTrue(results.size() > 0);

            Map<String, Object> stats = client.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
            assertEquals("DiskStorage", storageStats.get("type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("HNSWIndex", indexStats.get("type"));
            assertEquals("euclidean", indexStats.get("metric"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateCustom_WithQuantization() {
        Map<String, Object> storageConfig = Map.of("type", "memory");
        Map<String, Object> indexConfig = Map.of("type", "hnsw", "metric", "cosine");
        Map<String, Object> quantConfig = Map.of("type", "scalar", "bits", 16);

        VectorDBClient client = VectorDBFactory.createCustom(
            storageConfig, indexConfig, quantConfig, null);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            Random random = new Random(42);
            double[][] vectors = new double[30][32];
            for (int i = 0; i < 30; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            client.insert(vectors, null);

            // Check quantization
            Map<String, Object> stats = client.getStats();
            assertTrue(stats.containsKey("quantization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> quantStats = (Map<String, Object>) stats.get("quantization");
            assertEquals("ScalarQuantizer", quantStats.get("type"));
            assertEquals(16, quantStats.get("bits"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCreateCustom_WithPersistence() {
        String dataDir = tempDir.resolve("custom_data").toString();
        String persistDir = tempDir.resolve("custom_persist").toString();

        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "disk");
        storageConfig.put("data_dir", dataDir);

        Map<String, Object> indexConfig = Map.of("type", "flat", "metric", "cosine");

        Map<String, Object> persistConfig = new HashMap<>();
        persistConfig.put("type", "tensor");
        persistConfig.put("data_dir", persistDir);
        persistConfig.put("compression", false);

        VectorDBClient client = VectorDBFactory.createCustom(
            storageConfig, indexConfig, null, persistConfig);

        try {
            assertNotNull(client);
            assertTrue(client.isInitialized());

            // Insert vectors
            double[][] vectors = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
            client.insert(vectors, null);

            // Check persistence
            Map<String, Object> stats = client.getStats();
            assertTrue(stats.containsKey("persistence"));

            @SuppressWarnings("unchecked")
            Map<String, Object> persistStats = (Map<String, Object>) stats.get("persistence");
            assertEquals("TensorPersistence", persistStats.get("type"));
        } finally {
            client.close();
        }
    }

    @Test
    public void testCompareFactoryMethods() {
        // Compare performance characteristics of different factory methods

        // Simple (Flat + Memory)
        VectorDBClient simple = VectorDBFactory.createSimple();

        // High Performance (HNSW + Memory)
        VectorDBClient highPerf = VectorDBFactory.createHighPerformance();

        // Memory Optimized (HNSW + Memory + Quantization)
        VectorDBClient memOpt = VectorDBFactory.createMemoryOptimized(8);

        try {
            // Insert same vectors to all
            Random random = new Random(42);
            double[][] vectors = new double[50][32];
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            simple.insert(vectors, null);
            highPerf.insert(vectors, null);
            memOpt.insert(vectors, null);

            // All should work
            List<SearchResult> results1 = simple.search(vectors[0], 5);
            List<SearchResult> results2 = highPerf.search(vectors[0], 5);
            List<SearchResult> results3 = memOpt.search(vectors[0], 5);

            assertEquals(5, results1.size());
            assertTrue(results2.size() > 0);
            assertTrue(results3.size() > 0);

            // Verify different configurations
            Map<String, Object> stats1 = simple.getStats();
            Map<String, Object> stats2 = highPerf.getStats();
            Map<String, Object> stats3 = memOpt.getStats();

            assertFalse(stats1.containsKey("quantization"));
            assertFalse(stats2.containsKey("quantization"));
            assertTrue(stats3.containsKey("quantization"));
        } finally {
            simple.close();
            highPerf.close();
            memOpt.close();
        }
    }

    @Test
    public void testFactoryMethodsInitialized() {
        // Verify all factory methods return initialized clients
        VectorDBClient simple = VectorDBFactory.createSimple();
        VectorDBClient highPerf = VectorDBFactory.createHighPerformance();
        VectorDBClient persistent = VectorDBFactory.createPersistent(
            tempDir.resolve("persist1").toString(), false);
        VectorDBClient memOpt = VectorDBFactory.createMemoryOptimized(8);

        try {
            assertTrue(simple.isInitialized());
            assertTrue(highPerf.isInitialized());
            assertTrue(persistent.isInitialized());
            assertTrue(memOpt.isInitialized());
        } finally {
            simple.close();
            highPerf.close();
            persistent.close();
            memOpt.close();
        }
    }
}
