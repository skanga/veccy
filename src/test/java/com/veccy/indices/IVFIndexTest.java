package com.veccy.indices;

import com.veccy.base.SearchResult;
import com.veccy.config.IVFConfig;
import com.veccy.config.Metric;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for IVFIndex.
 */
class IVFIndexTest {

    private IVFIndex index;
    private StorageBackend storage;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        IVFConfig config = IVFConfig.builder()
                .metric(Metric.COSINE)
                .numClusters(10)  // Small for faster tests
                .numProbes(3)
                .maxIterations(50)
                .randomSeed(42L)
                .build();

        index = new IVFIndex(config, logger);
        storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
    }

    @AfterEach
    void tearDown() {
        if (index != null) {
            index.close();
        }
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    void testInitialization() {
        assertTrue(index.isInitialized());
        assertFalse(index.isTrained());

        Map<String, Object> stats = index.getStats();
        assertEquals("IVFIndex", stats.get("type"));
        assertEquals("cosine", stats.get("metric"));
        assertEquals(10, stats.get("num_clusters"));
        assertEquals(3, stats.get("num_probes"));
    }

    @Test
    void testTraining() {
        double[][] trainingData = generateRandomVectors(100, 64);

        index.train(trainingData);

        assertTrue(index.isTrained());

        Map<String, Object> stats = index.getStats();
        assertEquals(true, stats.get("trained"));
        assertNotNull(stats.get("cluster_stats"));
    }

    @Test
    void testAutoTraining() {
        // Insert without training - should auto-train
        double[][] vectors = generateRandomVectors(50, 64);

        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());
        assertTrue(index.isTrained());
    }

    @Test
    void testInsertAndSearch() {
        // Train on data
        double[][] trainingData = generateRandomVectors(100, 64);
        index.train(trainingData);

        // Insert vectors
        double[][] vectors = generateRandomVectors(20, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(20, ids.size());

        // Search for similar vector
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 5);

        // First result should be the query vector itself (distance ~0)
        assertTrue(results.get(0).getDistance() < 0.01);
    }

    @Test
    void testSearchBeforeTraining() {
        double[] query = generateRandomVector(64);

        assertThrows(Exception.class, () -> {
            index.search(query, 5);
        });
    }

    @Test
    void testInsertWithMetadata() {
        double[][] vectors = generateRandomVectors(10, 64);
        List<Map<String, Object>> metadata = new ArrayList<>();

        for (int i = 0; i < vectors.length; i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", i);
            meta.put("label", "vector_" + i);
            metadata.add(meta);
        }

        List<String> ids = index.insert(vectors, metadata);

        assertEquals(10, ids.size());

        // Search and verify metadata
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 1);

        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).getMetadata());
        assertEquals("vector_0", results.get(0).getMetadata().get("label"));
    }

    @Test
    void testUpdate() {
        double[][] vectors = generateRandomVectors(10, 64);
        List<String> ids = index.insert(vectors, null);

        String idToUpdate = ids.get(0);
        double[] originalVector = vectors[0];
        double[] newVector = generateRandomVector(64);

        // Search for original vector
        List<SearchResult> resultsBefore = index.search(originalVector, 1);
        assertEquals(idToUpdate, resultsBefore.get(0).getId());

        // Update vector
        boolean updated = index.update(idToUpdate, newVector, null);
        assertTrue(updated);

        // Search for new vector - should find updated one
        List<SearchResult> resultsAfter = index.search(newVector, 1);
        assertEquals(idToUpdate, resultsAfter.get(0).getId());
        assertTrue(resultsAfter.get(0).getDistance() < 0.01);
    }

    @Test
    void testUpdateBeforeTraining() {
        double[] vector = generateRandomVector(64);

        assertThrows(Exception.class, () -> {
            index.update("nonexistent", vector, null);
        });
    }

    @Test
    void testDelete() {
        double[][] vectors = generateRandomVectors(10, 64);
        List<String> ids = index.insert(vectors, null);

        String idToDelete = ids.get(0);
        double[] deletedVector = vectors[0];

        // Delete vector
        boolean deleted = index.delete(Collections.singletonList(idToDelete));
        assertTrue(deleted);

        // Search should not find deleted vector
        List<SearchResult> results = index.search(deletedVector, 10);

        for (SearchResult result : results) {
            assertNotEquals(idToDelete, result.getId());
        }

        Map<String, Object> stats = index.getStats();
        assertEquals(9, stats.get("vector_count"));
    }

    @Test
    void testDeleteMultiple() {
        double[][] vectors = generateRandomVectors(20, 64);
        List<String> ids = index.insert(vectors, null);

        List<String> idsToDelete = ids.subList(0, 5);

        boolean deleted = index.delete(idsToDelete);
        assertTrue(deleted);

        Map<String, Object> stats = index.getStats();
        assertEquals(15, stats.get("vector_count"));
    }

    @Test
    void testMultiProbeSearch() {
        // Train with distinct clusters
        double[][] trainingData = generateRandomVectors(100, 64);
        index.train(trainingData);

        // Insert vectors
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        // Search with multi-probe (num_probes=3)
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);
    }

    @Test
    void testClusterAssignment() {
        // Create vectors in distinct regions
        double[][] vectors = new double[30][64];

        // Cluster 1: vectors with large first dimension
        for (int i = 0; i < 10; i++) {
            vectors[i] = generateRandomVector(64);
            vectors[i][0] = 10.0;
        }

        // Cluster 2: vectors with large second dimension
        for (int i = 10; i < 20; i++) {
            vectors[i] = generateRandomVector(64);
            vectors[i][1] = 10.0;
        }

        // Cluster 3: vectors with large third dimension
        for (int i = 20; i < 30; i++) {
            vectors[i] = generateRandomVector(64);
            vectors[i][2] = 10.0;
        }

        index.train(vectors);
        List<String> ids = index.insert(vectors, null);

        assertEquals(30, ids.size());

        Map<String, Object> stats = index.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> clusterStats = (Map<String, Object>) stats.get("cluster_stats");

        assertNotNull(clusterStats);
        assertTrue((Integer) clusterStats.get("non_empty_clusters") > 0);
    }

    @Test
    void testDifferentMetrics() {
        // Test with Euclidean distance
        IVFConfig config = IVFConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .numClusters(5)
                .numProbes(3)  // Must be <= numClusters
                .randomSeed(42L)
                .build();

        IVFIndex euclideanIndex = new IVFIndex(config);
        euclideanIndex.initialize(storage);

        double[][] vectors = generateRandomVectors(20, 64);
        euclideanIndex.insert(vectors, null);

        double[] query = vectors[0];
        List<SearchResult> results = euclideanIndex.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 1.0);

        euclideanIndex.close();
    }

    @Test
    void testTrainingWithFewerVectorsThanClusters() {
        // Create index with more clusters than training vectors
        IVFConfig config = IVFConfig.builder()
                .numClusters(20)  // More clusters than vectors
                .randomSeed(42L)
                .build();

        IVFIndex testIndex = new IVFIndex(config, logger);
        testIndex.initialize(storage);

        double[][] trainingData = generateRandomVectors(20, 64);  // Only 20 vectors

        // Should automatically reduce num_clusters to match training size
        testIndex.train(trainingData);

        assertTrue(testIndex.isTrained());

        testIndex.close();
    }

    @Test
    void testEmptyTraining() {
        double[][] emptyData = new double[0][];

        assertThrows(Exception.class, () -> {
            index.train(emptyData);
        });
    }

    @Test
    void testNullTraining() {
        assertThrows(Exception.class, () -> {
            index.train(null);
        });
    }

    @Test
    void testLargeDataset() {
        double[][] trainingData = generateRandomVectors(500, 64);
        index.train(trainingData);

        double[][] vectors = generateRandomVectors(200, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(200, ids.size());

        double[] query = vectors[50];
        List<SearchResult> results = index.search(query, 20);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 20);
    }

    @Test
    void testSearchAccuracy() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        // Query with one of the inserted vectors
        double[] query = vectors[10];
        List<SearchResult> results = index.search(query, 10);

        // The query vector itself should be in the results
        boolean found = results.stream()
                .anyMatch(r -> r.getDistance() < 0.01);

        assertTrue(found, "Query vector should be found in results");
    }

    @Test
    void testClusterDistribution() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        Map<String, Object> stats = index.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> clusterStats = (Map<String, Object>) stats.get("cluster_stats");

        assertNotNull(clusterStats);

        int minSize = (Integer) clusterStats.get("min_cluster_size");
        int maxSize = (Integer) clusterStats.get("max_cluster_size");
        double avgSize = (Double) clusterStats.get("avg_cluster_size");

        assertTrue(minSize >= 0);
        assertTrue(maxSize >= minSize);
        assertTrue(avgSize > 0);
    }

    @Test
    void testMultipleOperations() {
        // Insert initial vectors
        double[][] vectors1 = generateRandomVectors(20, 64);
        List<String> ids1 = index.insert(vectors1, null);

        // Insert more vectors
        double[][] vectors2 = generateRandomVectors(20, 64);
        List<String> ids2 = index.insert(vectors2, null);

        // Update some vectors
        for (int i = 0; i < 5; i++) {
            double[] newVector = generateRandomVector(64);
            index.update(ids1.get(i), newVector, null);
        }

        // Delete some vectors
        index.delete(ids2.subList(0, 5));

        // Search
        double[] query = vectors1[10];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());

        Map<String, Object> stats = index.getStats();
        assertEquals(35, stats.get("vector_count"));  // 20 + 20 - 5 deleted
    }

    @Test
    void testClose() {
        double[][] vectors = generateRandomVectors(20, 64);
        index.insert(vectors, null);

        index.close();

        assertFalse(index.isInitialized());
        assertFalse(index.isTrained());
    }

    @Test
    void testReinitialize() {
        double[][] vectors = generateRandomVectors(20, 64);
        index.insert(vectors, null);

        index.close();

        // Reinitialize
        index.initialize(storage);
        assertTrue(index.isInitialized());
        assertFalse(index.isTrained());  // Training state not preserved
    }

    @Test
    void testConsistency() {
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        // Same query should give same results
        double[] query = vectors[0];

        List<SearchResult> results1 = index.search(query, 10);
        List<SearchResult> results2 = index.search(query, 10);

        assertEquals(results1.size(), results2.size());

        for (int i = 0; i < results1.size(); i++) {
            assertEquals(results1.get(i).getId(), results2.get(i).getId());
            assertEquals(results1.get(i).getDistance(), results2.get(i).getDistance(), 0.0001);
        }
    }

    @Test
    void testDifferentProbeSettings() {
        // Test with different num_probes values
        for (int numProbes : new int[]{1, 3, 5, 10}) {
            IVFConfig config = IVFConfig.builder()
                    .numClusters(10)
                    .numProbes(numProbes)
                    .randomSeed(42L)
                    .build();

            IVFIndex testIndex = new IVFIndex(config);
            testIndex.initialize(storage);

            double[][] vectors = generateRandomVectors(50, 64);
            testIndex.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = testIndex.search(query, 5);

            assertFalse(results.isEmpty());
            assertTrue(results.get(0).getDistance() < 0.1);

            testIndex.close();
        }
    }

    // Helper methods

    private double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];
        Random random = new Random(42);  // Fixed seed for reproducibility

        for (int i = 0; i < count; i++) {
            for (int d = 0; d < dimensions; d++) {
                vectors[i][d] = random.nextDouble() * 10.0 - 5.0;  // Range [-5, 5]
            }
        }

        return vectors;
    }

    private double[] generateRandomVector(int dimensions) {
        return generateRandomVectors(1, dimensions)[0];
    }
}
