package com.veccy.client;

import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.indices.FlatIndex;
import com.veccy.indices.HNSWIndex;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for VectorDBClient.
 */
public class VectorDBClientTest {

    private VectorDBClient client;
    private StorageBackend storage;
    private FlatIndex index;

    @BeforeEach
    public void setUp() {
        storage = new MemoryStorage(new HashMap<>());
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        index = new FlatIndex(config);
        client = new VectorDBClient(storage, index);
        client.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testInitialization() {
        StorageBackend newStorage = new MemoryStorage(new HashMap<>());
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        FlatIndex newIndex = new FlatIndex(config);
        VectorDBClient newClient = new VectorDBClient(newStorage, newIndex);

        assertFalse(newClient.isInitialized());

        newClient.initialize();
        assertTrue(newClient.isInitialized());

        newClient.close();
    }

    @Test
    public void testInsert_Single() {
        double[][] vectors = {{1.0, 0.0, 0.0}};

        List<String> ids = client.insert(vectors, null);

        assertEquals(1, ids.size());
        assertNotNull(ids.get(0));
    }

    @Test
    public void testInsert_Multiple() {
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };

        List<String> ids = client.insert(vectors, null);

        assertEquals(3, ids.size());
        assertEquals(3, new HashSet<>(ids).size()); // All unique
    }

    @Test
    public void testInsert_WithMetadata() {
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0}
        };

        List<Map<String, Object>> metadata = Arrays.asList(
            Map.of("label", "first", "category", "A"),
            Map.of("label", "second", "category", "B")
        );

        List<String> ids = client.insert(vectors, metadata);

        assertEquals(2, ids.size());
    }

    @Test
    public void testSearch_Basic() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        client.insert(vectors, null);

        // Search
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 2);

        assertEquals(2, results.size());

        // First result should be exact match
        assertTrue(results.get(0).getDistance() < 0.1);
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
            Map.of("name", "x-axis"),
            Map.of("name", "y-axis"),
            Map.of("name", "z-axis")
        );

        client.insert(vectors, metadata);

        // Search and verify metadata
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 1);

        assertEquals(1, results.size());
        assertNotNull(results.get(0).getMetadata());
        assertEquals("x-axis", results.get(0).getMetadata().get("name"));
    }

    @Test
    public void testDelete_Single() {
        // Insert vectors
        double[][] vectors = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}};
        List<String> ids = client.insert(vectors, null);

        // Delete one
        boolean result = client.delete(List.of(ids.get(0)));
        assertTrue(result);

        // Verify deletion
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 10);
        assertEquals(1, results.size());
    }

    @Test
    public void testDelete_Multiple() {
        // Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };
        List<String> ids = client.insert(vectors, null);

        // Delete multiple
        boolean result = client.delete(Arrays.asList(ids.get(0), ids.get(2)));
        assertTrue(result);

        // Verify deletion
        double[] query = {0.5, 0.5, 0.5};
        List<SearchResult> results = client.search(query, 10);
        assertEquals(1, results.size());
    }

    @Test
    public void testUpdate_Vector() {
        // Insert vector
        double[][] vectors = {{1.0, 0.0, 0.0}};
        List<String> ids = client.insert(vectors, null);
        String id = ids.get(0);

        // Update vector
        double[] newVector = {0.0, 1.0, 0.0};
        boolean result = client.update(id, newVector, null);
        assertTrue(result);

        // Verify update
        List<SearchResult> results = client.search(newVector, 1);
        assertEquals(1, results.size());
        assertEquals(id, results.get(0).getId());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    public void testUpdate_Metadata() {
        // Insert vector with metadata
        double[][] vectors = {{1.0, 0.0, 0.0}};
        List<Map<String, Object>> metadata = List.of(Map.of("version", 1));
        List<String> ids = client.insert(vectors, metadata);
        String id = ids.get(0);

        // Update metadata only
        Map<String, Object> newMetadata = Map.of("version", 2, "updated", true);
        boolean result = client.update(id, null, newMetadata);
        assertTrue(result);

        // Verify update
        List<SearchResult> results = client.search(vectors[0], 1);
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
        client.insert(vectors, null);

        Map<String, Object> stats = client.getStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("storage"));
        assertTrue(stats.containsKey("index"));

        @SuppressWarnings("unchecked")
        Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
        assertEquals(3, storageStats.get("vector_count"));

        @SuppressWarnings("unchecked")
        Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
        assertEquals("FlatIndex", indexStats.get("type"));
    }

    @Test
    public void testEndToEndWorkflow() {
        // Complete workflow test

        // 1. Insert vectors
        double[][] vectors = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0},
            {0.7071, 0.7071, 0.0}
        };

        List<Map<String, Object>> metadata = Arrays.asList(
            Map.of("label", "x", "group", "axis"),
            Map.of("label", "y", "group", "axis"),
            Map.of("label", "z", "group", "axis"),
            Map.of("label", "diagonal", "group", "other")
        );

        List<String> ids = client.insert(vectors, metadata);
        assertEquals(4, ids.size());

        // 2. Search
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 2);
        assertEquals(2, results.size());

        // 3. Update one vector
        client.update(ids.get(3), new double[]{0.0, 0.0, 0.0}, null);

        // 4. Delete one vector
        client.delete(List.of(ids.get(2)));

        // 5. Final search
        results = client.search(query, 10);
        assertEquals(3, results.size()); // 4 - 1 deleted = 3

        // 6. Check stats
        Map<String, Object> stats = client.getStats();
        assertNotNull(stats);
    }

    @Test
    public void testWithHNSWIndex() {
        // Test with HNSW index instead of Flat
        StorageBackend hnswStorage = new MemoryStorage(new HashMap<>());

        HNSWConfig hnswConfig = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .build();

        HNSWIndex hnswIndex = new HNSWIndex(hnswConfig);
        VectorDBClient hnswClient = new VectorDBClient(hnswStorage, hnswIndex);
        hnswClient.initialize();

        try {
            // Insert vectors
            Random random = new Random(42);
            double[][] vectors = new double[100][64];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 64; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = hnswClient.insert(vectors, null);
            assertEquals(100, ids.size());

            // Search
            List<SearchResult> results = hnswClient.search(vectors[0], 10);
            assertTrue(results.size() > 0);
            assertTrue(results.get(0).getDistance() < 0.2);

            // Check stats
            Map<String, Object> stats = hnswClient.getStats();
            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            assertEquals("HNSWIndex", indexStats.get("type"));
        } finally {
            hnswClient.close();
        }
    }

    @Test
    public void testHighDimensionalVectors() {
        // Test with high-dimensional vectors
        int dimensions = 256;
        int count = 50;

        double[][] vectors = new double[count][dimensions];
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        List<String> ids = client.insert(vectors, null);
        assertEquals(count, ids.size());

        // Search
        List<SearchResult> results = client.search(vectors[0], 10);
        assertEquals(10, results.size());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    public void testEmptyDatabase() {
        // Search in empty database
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 5);

        assertEquals(0, results.size());

        // Stats of empty database
        Map<String, Object> stats = client.getStats();
        assertNotNull(stats);
    }

    @Test
    public void testLargeScale() {
        // Test with larger dataset
        int count = 500;
        int dimensions = 32;

        double[][] vectors = new double[count][dimensions];
        Random random = new Random(42);

        for (int i = 0; i < count; i++) {
            for (int j = 0; j < dimensions; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        // Insert
        long startInsert = System.currentTimeMillis();
        List<String> ids = client.insert(vectors, null);
        long insertTime = System.currentTimeMillis() - startInsert;

        assertEquals(count, ids.size());
        System.out.println("Insert time for " + count + " vectors: " + insertTime + "ms");

        // Search
        double[] query = vectors[0];
        long startSearch = System.currentTimeMillis();
        List<SearchResult> results = client.search(query, 10);
        long searchTime = System.currentTimeMillis() - startSearch;

        assertEquals(10, results.size());
        System.out.println("Search time: " + searchTime + "ms");

        // Delete half
        List<String> toDelete = ids.subList(0, count / 2);
        boolean deleted = client.delete(toDelete);
        assertTrue(deleted);

        // Search again
        results = client.search(query, 10);
        assertTrue(results.size() <= count / 2);
    }

    @Test
    public void testMetadataQuery() {
        // Insert vectors with rich metadata
        double[][] vectors = new double[10][32];
        List<Map<String, Object>> metadata = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 32; j++) {
                vectors[i][j] = random.nextGaussian();
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("id", i);
            meta.put("category", i % 3 == 0 ? "A" : "B");
            meta.put("score", i * 10.0);
            metadata.add(meta);
        }

        client.insert(vectors, metadata);

        // Search and filter results by metadata
        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 10);

        // Verify metadata is present in results
        for (SearchResult result : results) {
            assertNotNull(result.getMetadata());
            assertTrue(result.getMetadata().containsKey("category"));
        }
    }

    @Test
    public void testClose() {
        client.insert(new double[][]{{1.0, 0.0, 0.0}}, null);
        assertTrue(client.isInitialized());

        client.close();

        // After close, client is no longer initialized
        assertFalse(client.isInitialized());
    }

    @Test
    public void testMultipleInsertBatches() {
        // Test inserting multiple batches
        for (int batch = 0; batch < 5; batch++) {
            double[][] vectors = new double[20][16];
            Random random = new Random(42 + batch);

            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 16; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            List<String> ids = client.insert(vectors, null);
            assertEquals(20, ids.size());
        }

        // Should have 100 vectors total
        Map<String, Object> stats = client.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
        assertEquals(100, storageStats.get("vector_count"));
    }
}
