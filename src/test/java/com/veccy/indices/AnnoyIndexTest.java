package com.veccy.indices;

import com.veccy.base.SearchResult;
import com.veccy.config.AnnoyConfig;
import com.veccy.config.Metric;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnnoyIndex.
 */
class AnnoyIndexTest {

    private AnnoyIndex index;
    private StorageBackend storage;

    @BeforeEach
    void setUp() {
        AnnoyConfig config = AnnoyConfig.builder()
                .metric(Metric.COSINE)
                .numTrees(5)
                .maxLeafSize(10)
                .build();

        index = new AnnoyIndex(config);
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
        assertFalse(index.isBuilt());

        Map<String, Object> stats = index.getStats();
        assertEquals("AnnoyIndex", stats.get("type"));
        assertEquals("cosine", stats.get("metric"));
        assertEquals(5, stats.get("num_trees"));
        assertEquals(10, stats.get("max_leaf_size"));
    }

    @Test
    void testInsertAndBuild() {
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());
        assertFalse(index.isBuilt());

        index.build();
        assertTrue(index.isBuilt());
    }

    @Test
    void testAutoBuild() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        assertFalse(index.isBuilt());

        // Search should trigger auto-build
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertTrue(index.isBuilt());
        assertFalse(results.isEmpty());
    }

    @Test
    void testInsertAndSearch() {
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();

        // Search for similar vector
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        // First result should be the query vector itself (distance ~0)
        assertTrue(results.get(0).getDistance() < 0.01);
    }

    @Test
    void testSearchBeforeBuild() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        // Should auto-build and search
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(index.isBuilt());
    }

    @Test
    void testBuildWithoutVectors() {
        assertThrows(Exception.class, () -> {
            index.build();
        });
    }

    @Test
    void testInsertWithMetadata() {
        double[][] vectors = generateRandomVectors(20, 64);
        List<Map<String, Object>> metadata = new ArrayList<>();

        for (int i = 0; i < vectors.length; i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", i);
            meta.put("label", "vector_" + i);
            metadata.add(meta);
        }

        List<String> ids = index.insert(vectors, metadata);

        assertEquals(20, ids.size());

        index.build();

        // Search and verify metadata
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 1);

        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).getMetadata());
        assertEquals("vector_0", results.get(0).getMetadata().get("label"));
    }

    @Test
    void testUpdate() {
        double[][] vectors = generateRandomVectors(20, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();

        String idToUpdate = ids.get(0);
        double[] newVector = generateRandomVector(64);

        // Update vector
        boolean updated = index.update(idToUpdate, newVector, null);
        assertTrue(updated);

        // Index should need rebuild
        assertFalse(index.isBuilt());

        // Rebuild and search
        index.build();

        List<SearchResult> results = index.search(newVector, 1);
        assertEquals(idToUpdate, results.get(0).getId());
        assertTrue(results.get(0).getDistance() < 0.01);
    }

    @Test
    void testDelete() {
        double[][] vectors = generateRandomVectors(20, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();

        String idToDelete = ids.get(0);
        double[] deletedVector = vectors[0];

        // Delete vector
        boolean deleted = index.delete(Collections.singletonList(idToDelete));
        assertTrue(deleted);

        // Index should need rebuild
        assertFalse(index.isBuilt());

        // Rebuild and search
        index.build();

        List<SearchResult> results = index.search(deletedVector, 20);

        // Should not find deleted vector
        for (SearchResult result : results) {
            assertNotEquals(idToDelete, result.getId());
        }

        Map<String, Object> stats = index.getStats();
        assertEquals(19, stats.get("vector_count"));
    }

    @Test
    void testDeleteMultiple() {
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();

        List<String> idsToDelete = ids.subList(0, 10);

        boolean deleted = index.delete(idsToDelete);
        assertTrue(deleted);

        index.build();

        Map<String, Object> stats = index.getStats();
        assertEquals(20, stats.get("vector_count"));
    }

    @Test
    void testDifferentMetrics() {
        // Test with Euclidean distance
        AnnoyConfig config = AnnoyConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .numTrees(5)
                .build();

        AnnoyIndex euclideanIndex = new AnnoyIndex(config);
        euclideanIndex.initialize(storage);

        double[][] vectors = generateRandomVectors(30, 64);
        euclideanIndex.insert(vectors, null);

        euclideanIndex.build();

        double[] query = vectors[0];
        List<SearchResult> results = euclideanIndex.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 1.0);

        euclideanIndex.close();
    }

    @Test
    void testDifferentTreeConfigurations() {
        // Test with different num_trees
        int[] treeCounts = {3, 5, 10};

        for (int numTrees : treeCounts) {
            AnnoyConfig config = AnnoyConfig.builder()
                    .numTrees(numTrees)
                    .maxLeafSize(10)
                    .build();

            AnnoyIndex testIndex = new AnnoyIndex(config);
            testIndex.initialize(storage);

            double[][] vectors = generateRandomVectors(30, 64);
            testIndex.insert(vectors, null);

            testIndex.build();

            double[] query = vectors[0];
            List<SearchResult> results = testIndex.search(query, 5);

            assertFalse(results.isEmpty(),
                    "Should find results with num_trees=" + numTrees);

            testIndex.close();
        }
    }

    @Test
    void testDifferentLeafSizes() {
        // Test with different max_leaf_size
        int[] leafSizes = {5, 10, 20};

        for (int leafSize : leafSizes) {
            AnnoyConfig config = AnnoyConfig.builder()
                    .numTrees(5)
                    .maxLeafSize(leafSize)
                    .build();

            AnnoyIndex testIndex = new AnnoyIndex(config);
            testIndex.initialize(storage);

            double[][] vectors = generateRandomVectors(50, 64);
            testIndex.insert(vectors, null);

            testIndex.build();

            double[] query = vectors[0];
            List<SearchResult> results = testIndex.search(query, 5);

            assertFalse(results.isEmpty(),
                    "Should find results with max_leaf_size=" + leafSize);

            testIndex.close();
        }
    }

    @Test
    void testSearchAccuracy() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        index.build();

        // Query with one of the inserted vectors
        double[] query = vectors[25];
        List<SearchResult> results = index.search(query, 20);

        // The query vector itself should be in the results
        boolean found = results.stream()
                .anyMatch(r -> r.getDistance() < 0.01);

        assertTrue(found, "Query vector should be found in results");
    }

    @Test
    void testTreeStatistics() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        index.build();

        Map<String, Object> stats = index.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> treeStats = (Map<String, Object>) stats.get("tree_stats");

        assertNotNull(treeStats);

        int totalNodes = (Integer) treeStats.get("total_nodes");
        int totalLeaves = (Integer) treeStats.get("total_leaves");
        double avgNodesPerTree = (Double) treeStats.get("avg_nodes_per_tree");

        assertTrue(totalNodes > 0);
        assertTrue(totalLeaves > 0);
        assertTrue(avgNodesPerTree > 0);
    }

    @Test
    void testLargeDataset() {
        double[][] vectors = generateRandomVectors(500, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(500, ids.size());

        index.build();

        double[] query = vectors[100];
        List<SearchResult> results = index.search(query, 20);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 20);
    }

    @Test
    void testMultipleOperations() {
        // Insert initial batch
        double[][] vectors1 = generateRandomVectors(30, 64);
        List<String> ids1 = index.insert(vectors1, null);

        index.build();

        // Insert more vectors
        double[][] vectors2 = generateRandomVectors(20, 64);
        List<String> ids2 = index.insert(vectors2, null);

        // Should need rebuild
        assertFalse(index.isBuilt());

        index.build();

        // Update some vectors
        for (int i = 0; i < 5; i++) {
            double[] newVector = generateRandomVector(64);
            index.update(ids1.get(i), newVector, null);
        }

        index.build();

        // Delete some vectors
        index.delete(ids2.subList(0, 5));

        index.build();

        // Search
        double[] query = vectors1[10];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());

        Map<String, Object> stats = index.getStats();
        assertEquals(45, stats.get("vector_count"));  // 30 + 20 - 5 deleted
    }

    @Test
    void testClose() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        index.build();

        index.close();

        assertFalse(index.isInitialized());
        assertFalse(index.isBuilt());
    }

    @Test
    void testReinitialize() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        index.build();

        index.close();

        // Reinitialize
        index.initialize(storage);
        assertTrue(index.isInitialized());
        assertFalse(index.isBuilt());  // Build state not preserved
    }

    @Test
    void testConsistency() {
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();

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
    void testCustomSearchK() {
        AnnoyConfig config = AnnoyConfig.builder()
                .numTrees(5)
                .maxLeafSize(10)
                .searchK(100)  // Custom search_k
                .build();

        AnnoyIndex testIndex = new AnnoyIndex(config);
        testIndex.initialize(storage);

        double[][] vectors = generateRandomVectors(50, 64);
        testIndex.insert(vectors, null);

        testIndex.build();

        Map<String, Object> stats = testIndex.getStats();
        assertEquals(100, stats.get("search_k"));

        double[] query = vectors[0];
        List<SearchResult> results = testIndex.search(query, 5);

        assertFalse(results.isEmpty());

        testIndex.close();
    }

    @Test
    void testAutoComputedSearchK() {
        AnnoyConfig config = AnnoyConfig.builder()
                .numTrees(5)
                .maxLeafSize(10)
                .build();
        // search_k not specified, should auto-compute

        AnnoyIndex testIndex = new AnnoyIndex(config);
        testIndex.initialize(storage);

        double[][] vectors = generateRandomVectors(30, 64);
        testIndex.insert(vectors, null);

        testIndex.build();

        Map<String, Object> stats = testIndex.getStats();
        assertEquals(50, stats.get("search_k"));  // 5 trees * 10 leaf_size

        testIndex.close();
    }

    @Test
    void testHighDimensionalVectors() {
        // Test with high-dimensional vectors
        double[][] vectors = generateRandomVectors(50, 512);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

        index.build();

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testLowDimensionalVectors() {
        // Test with low-dimensional vectors
        double[][] vectors = generateRandomVectors(30, 16);
        List<String> ids = index.insert(vectors, null);

        assertEquals(30, ids.size());

        index.build();

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testRecall() {
        // Test recall: how many of the true nearest neighbors are found
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        index.build();

        double[] query = vectors[0];

        // Annoy search
        List<SearchResult> annoyResults = index.search(query, 10);

        // At minimum, should find the query vector itself
        boolean foundSelf = annoyResults.stream()
                .anyMatch(r -> r.getDistance() < 0.01);

        assertTrue(foundSelf, "Should at least find the query vector itself");
    }

    @Test
    void testStatsBeforeAndAfterBuild() {
        Map<String, Object> statsBefore = index.getStats();
        assertEquals(0, statsBefore.get("vector_count"));
        assertEquals(false, statsBefore.get("built"));
        assertNull(statsBefore.get("tree_stats"));

        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        index.build();

        Map<String, Object> statsAfter = index.getStats();
        assertEquals(30, statsAfter.get("vector_count"));
        assertEquals(true, statsAfter.get("built"));
        assertEquals(64, statsAfter.get("dimensions"));
        assertNotNull(statsAfter.get("tree_stats"));
    }

    @Test
    void testRebuildAfterModification() {
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = index.insert(vectors, null);

        index.build();
        assertTrue(index.isBuilt());

        // Modify index
        double[] newVector = generateRandomVector(64);
        index.update(ids.get(0), newVector, null);

        // Should need rebuild
        assertFalse(index.isBuilt());

        // Rebuild
        index.build();
        assertTrue(index.isBuilt());

        // Search should work
        List<SearchResult> results = index.search(newVector, 5);
        assertFalse(results.isEmpty());
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
