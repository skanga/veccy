package com.veccy.indices;

import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlatIndex.
 */
public class FlatIndexTest {

    private FlatIndex index;
    private StorageBackend storage;

    @BeforeEach
    public void setUp() {
        storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        FlatConfig indexConfig = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        index = new FlatIndex(indexConfig);
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

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .build();
        FlatIndex newIndex = new FlatIndex(config);

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
            {0.0, 0.0, 1.0}
        };

        List<String> ids = index.insert(vectors, null);

        assertEquals(3, ids.size());
        // IDs should be unique
        assertEquals(3, new HashSet<>(ids).size());
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
        assertEquals(0.0, results.get(0).getDistance(), 1e-6);
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

        // Results should be sorted by distance (ascending)
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getDistance() <= results.get(i + 1).getDistance());
        }
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
        assertEquals(3, results.size());
    }

    @Test
    public void testSearch_CosineMetric() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        FlatIndex cosineIndex = new FlatIndex(config);
        cosineIndex.initialize(storage);

        try {
            // Insert vectors
            double[][] vectors = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.7071, 0.7071, 0.0}  // 45 degrees from first vector
            };
            cosineIndex.insert(vectors, null);

            // Search
            double[] query = {1.0, 0.0, 0.0};
            List<SearchResult> results = cosineIndex.search(query, 3);

            assertEquals(3, results.size());

            // First result should be exact match (distance ~0)
            assertTrue(results.get(0).getDistance() < 0.01);

            // Third result should be orthogonal vector (distance ~1)
            assertTrue(results.get(2).getDistance() > 0.9);
        } finally {
            cosineIndex.close();
        }
    }

    @Test
    public void testSearch_EuclideanMetric() {
        StorageBackend eucStorage = new MemoryStorage(new HashMap<>());
        eucStorage.initialize();

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .build();
        FlatIndex eucIndex = new FlatIndex(config);
        eucIndex.initialize(eucStorage);

        try {
            // Insert vectors
            double[][] vectors = {
                {0.0, 0.0, 0.0},
                {1.0, 0.0, 0.0},
                {3.0, 4.0, 0.0}
            };
            eucIndex.insert(vectors, null);

            // Search from origin
            double[] query = {0.0, 0.0, 0.0};
            List<SearchResult> results = eucIndex.search(query, 3);

            assertEquals(3, results.size());

            // Distances should be 0, 1, and 5
            assertEquals(0.0, results.get(0).getDistance(), 1e-6);
            assertEquals(1.0, results.get(1).getDistance(), 1e-6);
            assertEquals(5.0, results.get(2).getDistance(), 1e-6);
        } finally {
            eucIndex.close();
            eucStorage.close();
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

        // Search should now return only 2 results
        double[] query = {0.5, 0.5, 0.5};
        List<SearchResult> results = index.search(query, 10);
        assertEquals(2, results.size());
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

        // Should have 2 vectors left
        double[] query = {0.5, 0.5, 0.5};
        List<SearchResult> results = index.search(query, 10);
        assertEquals(2, results.size());
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
        assertEquals(0.0, results.get(0).getDistance(), 1e-6);
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
        assertEquals("FlatIndex", stats.get("type"));
        assertEquals("cosine", stats.get("metric"));
        assertEquals(3, stats.get("vector_count"));
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
        assertEquals("x-axis", results.get(0).getMetadata().get("label"));
    }

    @Test
    public void testHighDimensionalVectors() {
        // Test with 128-dimensional vectors
        int dimensions = 128;
        int count = 10;

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
        List<SearchResult> results = index.search(vectors[0], 3);
        assertEquals(3, results.size());

        // First result should be exact match
        assertTrue(results.get(0).getDistance() < 0.01);
    }

    @Test
    public void testSearchResultOrdering() {
        // Insert vectors with known distances
        double[][] vectors = {
            {1.0, 0.0, 0.0},    // distance 0 from query
            {0.9, 0.1, 0.0},    // small distance
            {0.5, 0.5, 0.0},    // medium distance
            {0.0, 1.0, 0.0}     // large distance
        };
        index.insert(vectors, null);

        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = index.search(query, 4);

        assertEquals(4, results.size());

        // Verify ascending order
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).getDistance() <= results.get(i + 1).getDistance(),
                String.format("Results not in order: %f > %f",
                    results.get(i).getDistance(), results.get(i + 1).getDistance()));
        }
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
        assertEquals(2, results1.size());

        // Insert more vectors
        double[][] vectors2 = {
            {0.0, 0.0, 1.0},
            {1.0, 1.0, 0.0}
        };
        List<String> ids2 = index.insert(vectors2, null);

        // Search again
        List<SearchResult> results2 = index.search(new double[]{1.0, 0.0, 0.0}, 5);
        assertEquals(4, results2.size());

        // Delete one
        index.delete(List.of(ids1.get(0)));

        // Search again
        List<SearchResult> results3 = index.search(new double[]{1.0, 0.0, 0.0}, 5);
        assertEquals(3, results3.size());

        // Update one
        index.update(ids2.get(0), new double[]{0.5, 0.5, 0.5}, null);

        // Final search
        List<SearchResult> results4 = index.search(new double[]{0.5, 0.5, 0.5}, 1);
        assertEquals(1, results4.size());
        assertTrue(results4.get(0).getDistance() < 0.1);
    }

    @Test
    public void testClose() {
        index.insert(new double[][]{{1.0, 0.0, 0.0}}, null);
        assertTrue(index.isInitialized());

        index.close();

        // Index should still report stats (implementation specific)
        Map<String, Object> stats = index.getStats();
        assertNotNull(stats);
    }
}
