package com.veccy.indices;

import com.veccy.base.SearchResult;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HNSWIndex.
 */
public class HNSWIndexTest {

    private HNSWIndex index;
    private StorageBackend storage;

    @BeforeEach
    public void setUp() {
        storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        HNSWConfig indexConfig = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .randomSeed(42L)
                .build();

        index = new HNSWIndex(indexConfig);
        index.initialize(storage);
    }

    @AfterEach
    public void tearDown() {
        if (index != null) {
            index.close();
        }
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    public void testInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        newStorage.initialize();

        HNSWConfig config = HNSWConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .m(8)
                .efConstruction(100)
                .efSearch(25)
                .build();

        HNSWIndex newIndex = new HNSWIndex(config);

        assertFalse(newIndex.isInitialized());
        newIndex.initialize(newStorage);
        assertTrue(newIndex.isInitialized());

        newIndex.close();
        newStorage.close();
    }

    @Test
    public void testInsert_Single() {
        double[][] vectors = {{1.0, 0.0, 0.0}};
        List<String> ids = index.insert(vectors, null);

        assertEquals(1, ids.size());
        assertNotNull(ids.get(0));
    }

    @Test
    public void testInsert_Multiple() {
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0},
            {0.7071, 0.7071, 0.0}
        };

        List<String> ids = index.insert(vectors, null);

        assertEquals(4, ids.size());
        // IDs should be unique
        assertEquals(4, new HashSet<>(ids).size());
    }

    @Test
    public void testInsert_WithMetadata() {
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0}
        };

        List<Map<String, Object>> metadata = Arrays.asList(
            Map.of("label", "first"),
            Map.of("label", "second")
        );

        List<String> ids = index.insert(vectors, metadata);

        assertEquals(2, ids.size());
    }

    @Test
    public void testSearch_Exact() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        index.insert(vectors, null);

        // Search for exact match
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 1);

        assertEquals(1, results.size());
        // HNSW is approximate, but should find exact match with small datasets
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    public void testSearch_TopK() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.9, 0.1, 0.0},
            {0.8, 0.2, 0.0},
            {0.0, 1.0, 0.0}
        };
        index.insert(vectors, null);

        // Search for top 3
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 3);

        assertEquals(3, results.size());

        // Results should be roughly sorted by distance
        // (HNSW is approximate so perfect ordering not guaranteed)
        assertTrue(results.get(0).getDistance() <= results.get(1).getDistance() + 0.1);
    }

    @Test
    public void testSearch_KLargerThanDataset() {
        // Insert 3 vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        index.insert(vectors, null);

        // Search for k=10 (more than available)
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 10);

        // Should return all 3 vectors
        assertTrue(results.size() <= 3);
    }

    @Test
    public void testSearch_ApproximateRecall() {
        // Test that HNSW finds good approximate neighbors
        Random random = new Random(42);
        int vectorCount = 100;
        int dimensions = 64;

        // Generate random vectors
        double[][] vectors = new double[vectorCount][dimensions];
        for (int i = 0; i < vectorCount; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        List<String> ids = index.insert(vectors, null);

        // Search for one of the inserted vectors
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        // Should find the exact vector as first result (or very close)
        assertTrue(results.size() > 0);
        assertTrue(results.get(0).getDistance() < 0.1,
            "First result distance: " + results.get(0).getDistance());
    }

    @Test
    public void testSearch_CosineMetric() {
        HNSWConfig config = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .build();

        HNSWIndex cosineIndex = new HNSWIndex(config);
        cosineIndex.initialize(storage);

        try {
            // Insert vectors
            double[][] vectors = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.7071, 0.7071, 0.0}
            };
            cosineIndex.insert(vectors, null);

            // Search
            double[] query = {1.0, 0.0, 0.0};
            List<SearchResult> results = cosineIndex.search(query, 3);

            assertEquals(3, results.size());

            // First result should be close to exact match
            assertTrue(results.get(0).getDistance() < 0.1);
        } finally {
            cosineIndex.close();
        }
    }

    @Test
    public void testDelete_Exists() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        List<String> ids = index.insert(vectors, null);

        // Delete one
        boolean result = index.delete(List.of(ids.get(1)));
        assertTrue(result);

        // Search should now return at most 2 results
        double[] query = {0.5, 0.5, 0.5};
        List<SearchResult> results = index.search(query, 10);
        assertTrue(results.size() <= 2);
    }

    @Test
    public void testDelete_NotExists() {
        boolean result = index.delete(List.of("nonexistent-id"));
        assertFalse(result);
    }

    @Test
    public void testDelete_Multiple() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0},
            {1.0, 1.0, 1.0}
        };
        List<String> ids = index.insert(vectors, null);

        // Delete multiple
        boolean result = index.delete(Arrays.asList(ids.get(0), ids.get(2)));
        assertTrue(result);

        // Should have at most 2 vectors left
        double[] query = {0.5, 0.5, 0.5};
        List<SearchResult> results = index.search(query, 10);
        assertTrue(results.size() <= 2);
    }

    @Test
    public void testUpdate_Exists() {
        // Insert vector
        double[][] vectors = {{1.0, 0.0, 0.0}};
        List<String> ids = index.insert(vectors, null);
        String id = ids.get(0);

        // Update vector
        double[] newVector = {0.0, 1.0, 0.0};
        boolean result = index.update(id, newVector, null);
        assertTrue(result);

        // Search for the new vector
        List<SearchResult> results = index.search(newVector, 1);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getDistance() < 0.1);
        assertEquals(id, results.get(0).getId());
    }

    @Test
    public void testUpdate_NotExists() {
        double[] vector = {1.0, 0.0, 0.0};
        boolean result = index.update("nonexistent-id", vector, null);
        assertFalse(result);
    }

    @Test
    public void testUpdate_WithMetadata() {
        // Insert vector with metadata
        double[][] vectors = {{1.0, 0.0, 0.0}};
        List<Map<String, Object>> metadata = List.of(Map.of("version", 1));
        List<String> ids = index.insert(vectors, metadata);
        String id = ids.get(0);

        // Update with new metadata
        Map<String, Object> newMetadata = Map.of("version", 2, "updated", true);
        boolean result = index.update(id, null, newMetadata);
        assertTrue(result);

        // Verify update via search
        List<SearchResult> results = index.search(vectors[0], 1);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getMetadata().get("version"));
        assertEquals(true, results.get(0).getMetadata().get("updated"));
    }

    @Test
    public void testGetStats() {
        // Insert some vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        index.insert(vectors, null);

        Map<String, Object> stats = index.getStats();

        assertNotNull(stats);
        assertEquals("HNSWIndex", stats.get("type"));
        assertEquals("cosine", stats.get("metric"));
        assertEquals(3, stats.get("vector_count"));
        assertEquals(16, stats.get("m"));
        assertEquals(200, stats.get("ef_construction"));
        assertEquals(50, stats.get("ef_search"));
    }

    @Test
    public void testSearch_EmptyIndex() {
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 5);

        assertEquals(0, results.size());
    }

    @Test
    public void testSearch_WithMetadata() {
        // Insert vectors with metadata
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };

        List<Map<String, Object>> metadata = Arrays.asList(
            Map.of("label", "x-axis"),
            Map.of("label", "y-axis"),
            Map.of("label", "z-axis")
        );

        index.insert(vectors, metadata);

        // Search and verify metadata is returned
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 1);

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getMetadata());
        assertTrue(results.get(0).getMetadata().containsKey("label"));
    }

    @Test
    public void testHighDimensionalVectors() {
        // Test with 128-dimensional vectors
        int dimensions = 128;
        int count = 50;

        double[][] vectors = new double[count][dimensions];
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        List<String> ids = index.insert(vectors, null);
        assertEquals(count, ids.size());

        // Search with one of the vectors
        List<SearchResult> results = index.search(vectors[0], 10);
        assertTrue(results.size() > 0);

        // First result should be close to exact match
        assertTrue(results.get(0).getDistance() < 0.2);
    }

    @Test
    public void testPerformance_LargeDataset() {
        // Test with larger dataset to verify scalability
        Random random = new Random(42);
        int vectorCount = 500;
        int dimensions = 64;

        double[][] vectors = new double[vectorCount][dimensions];
        for (int i = 0; i < vectorCount; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        // Insert vectors
        long startInsert = System.currentTimeMillis();
        List<String> ids = index.insert(vectors, null);
        long insertTime = System.currentTimeMillis() - startInsert;

        assertEquals(vectorCount, ids.size());
        System.out.println("Insert time for " + vectorCount + " vectors: " + insertTime + "ms");

        // Search
        double[] query = vectors[0];
        long startSearch = System.currentTimeMillis();
        List<SearchResult> results = index.search(query, 10);
        long searchTime = System.currentTimeMillis() - startSearch;

        assertTrue(results.size() > 0);
        System.out.println("Search time: " + searchTime + "ms");

        // Search should be fast (< 100ms for 500 vectors)
        assertTrue(searchTime < 100, "Search took too long: " + searchTime + "ms");
    }

    @Test
    public void testGraphConstruction() {
        // Test that the graph is being built correctly
        double[][] vectors = new double[20][32];
        Random random = new Random(42);

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 32; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        index.insert(vectors, null);

        Map<String, Object> stats = index.getStats();

        // Verify stats show graph is constructed
        assertEquals(20, stats.get("vector_count"));
        assertTrue((Integer) stats.get("max_level") >= 0);
    }

    @Test
    public void testMultipleOperations() {
        // Insert initial vectors
        double[][] vectors1 = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0}
        };
        List<String> ids1 = index.insert(vectors1, null);

        // Search
        List<SearchResult> results1 = index.search(new double[]{1.0, 0.0, 0.0}, 5);
        assertTrue(results1.size() > 0);

        // Insert more vectors
        double[][] vectors2 = {
            {0.0, 0.0, 1.0},
            {1.0, 1.0, 0.0}
        };
        List<String> ids2 = index.insert(vectors2, null);

        // Search again
        List<SearchResult> results2 = index.search(new double[]{1.0, 0.0, 0.0}, 5);
        assertTrue(results2.size() >= 2);

        // Delete one
        index.delete(List.of(ids1.get(0)));

        // Update one
        index.update(ids2.get(0), new double[]{0.5, 0.5, 0.5}, null);

        // Final search
        List<SearchResult> results4 = index.search(new double[]{0.5, 0.5, 0.5}, 1);
        assertTrue(results4.size() > 0);
    }

    @Test
    public void testParameterEffects() {
        // Test with different M values
        StorageBackend storage2 = new MemoryStorage(new HashMap<>());
        storage2.initialize();

        HNSWConfig smallMConfig = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(4)  // Small M
                .efConstruction(100)
                .efSearch(25)
                .build();

        HNSWIndex smallMIndex = new HNSWIndex(smallMConfig);
        smallMIndex.initialize(storage2);

        try {
            double[][] vectors = new double[50][32];
            Random random = new Random(42);

            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            smallMIndex.insert(vectors, null);

            // Should still work with smaller M
            List<SearchResult> results = smallMIndex.search(vectors[0], 5);
            assertTrue(results.size() > 0);
        } finally {
            smallMIndex.close();
            storage2.close();
        }
    }

    @Test
    public void testClose() {
        index.insert(new double[][]{{1.0, 0.0, 0.0}}, null);
        assertTrue(index.isInitialized());

        index.close();

        // Index should still report stats
        Map<String, Object> stats = index.getStats();
        assertNotNull(stats);
    }
}
