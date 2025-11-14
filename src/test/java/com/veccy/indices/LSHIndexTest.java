package com.veccy.indices;

import com.veccy.base.SearchResult;
import com.veccy.config.LSHConfig;
import com.veccy.config.Metric;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LSHIndex.
 */
class LSHIndexTest {

    private LSHIndex index;
    private StorageBackend storage;

    @BeforeEach
    void setUp() {
        LSHConfig config = LSHConfig.builder()
                .metric(Metric.COSINE)
                .numTables(5)
                .numHashBits(8)
                .build();

        index = new LSHIndex(config);
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

        Map<String, Object> stats = index.getStats();
        assertEquals("LSHIndex", stats.get("type"));
        assertEquals("cosine", stats.get("metric"));
        assertEquals(5, stats.get("num_tables"));
        assertEquals(8, stats.get("num_hash_bits"));
    }

    @Test
    void testInsertAndSearch() {
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

        // Search for similar vector
        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        // First result should be the query vector itself (distance ~0)
        assertTrue(results.get(0).getDistance() < 0.01);
    }

    @Test
    void testSearchBeforeInsert() {
        double[] query = generateRandomVector(64);

        assertThrows(Exception.class, () -> {
            index.search(query, 5);
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
        List<SearchResult> resultsAfter = index.search(newVector, 5);

        // Should find the updated vector in results
        boolean found = resultsAfter.stream()
                .anyMatch(r -> r.getId().equals(idToUpdate) && r.getDistance() < 0.01);
        assertTrue(found);
    }

    @Test
    void testDelete() {
        double[][] vectors = generateRandomVectors(20, 64);
        List<String> ids = index.insert(vectors, null);

        String idToDelete = ids.get(0);
        double[] deletedVector = vectors[0];

        // Delete vector
        boolean deleted = index.delete(Collections.singletonList(idToDelete));
        assertTrue(deleted);

        // Search should not find deleted vector
        List<SearchResult> results = index.search(deletedVector, 20);

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

        List<String> idsToDelete = ids.subList(0, 10);

        boolean deleted = index.delete(idsToDelete);
        assertTrue(deleted);

        Map<String, Object> stats = index.getStats();
        assertEquals(20, stats.get("vector_count"));
    }

    @Test
    void testDifferentMetrics() {
        // Test with Euclidean distance
        LSHConfig config = LSHConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .numTables(5)
                .numHashBits(8)
                .bucketWidth(4.0)
                .build();

        LSHIndex euclideanIndex = new LSHIndex(config);
        euclideanIndex.initialize(storage);

        double[][] vectors = generateRandomVectors(30, 64);
        euclideanIndex.insert(vectors, null);

        double[] query = vectors[0];
        List<SearchResult> results = euclideanIndex.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 1.0);

        euclideanIndex.close();
    }

    @Test
    void testDifferentHashConfigurations() {
        // Test with different num_tables and num_hash_bits
        int[][] configs = {{3, 6}, {5, 8}, {7, 10}};

        for (int[] config : configs) {
            int numTables = config[0];
            int numHashBits = config[1];

            LSHConfig hashConfig = LSHConfig.builder()
                    .numTables(numTables)
                    .numHashBits(numHashBits)
                    .build();

            LSHIndex testIndex = new LSHIndex(hashConfig);
            testIndex.initialize(storage);

            double[][] vectors = generateRandomVectors(30, 64);
            testIndex.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = testIndex.search(query, 5);

            assertFalse(results.isEmpty(),
                    "Should find results with tables=" + numTables + ", bits=" + numHashBits);

            testIndex.close();
        }
    }

    @Test
    void testSearchAccuracy() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        // Query with one of the inserted vectors
        double[] query = vectors[25];
        List<SearchResult> results = index.search(query, 20);

        // The query vector itself should be in the results
        boolean found = results.stream()
                .anyMatch(r -> r.getDistance() < 0.01);

        assertTrue(found, "Query vector should be found in results");
    }

    @Test
    void testHashCollisions() {
        // Insert many similar vectors to test hash collisions
        double[][] vectors = new double[50][64];
        Random random = new Random(42);

        // Create a base vector
        double[] baseVector = generateRandomVector(64);

        // Create similar vectors by adding small noise
        for (int i = 0; i < 50; i++) {
            for (int d = 0; d < 64; d++) {
                vectors[i][d] = baseVector[d] + (random.nextDouble() - 0.5) * 0.1;
            }
        }

        List<String> ids = index.insert(vectors, null);
        assertEquals(50, ids.size());

        // Search with base vector - should find many similar vectors
        List<SearchResult> results = index.search(baseVector, 20);

        // Should find at least some results due to hash collisions
        assertTrue(results.size() > 0);
    }

    @Test
    void testBucketDistribution() {
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        Map<String, Object> stats = index.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> bucketStats = (Map<String, Object>) stats.get("bucket_stats");

        assertNotNull(bucketStats);

        int nonEmptyBuckets = (Integer) bucketStats.get("non_empty_buckets");
        int maxBucketSize = (Integer) bucketStats.get("max_bucket_size");

        assertTrue(nonEmptyBuckets > 0);
        assertTrue(maxBucketSize >= 1);
    }

    @Test
    void testLargeDataset() {
        double[][] vectors = generateRandomVectors(500, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(500, ids.size());

        double[] query = vectors[100];
        List<SearchResult> results = index.search(query, 20);

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 20);
    }

    @Test
    void testMultipleOperations() {
        // Insert initial vectors
        double[][] vectors1 = generateRandomVectors(30, 64);
        List<String> ids1 = index.insert(vectors1, null);

        // Insert more vectors
        double[][] vectors2 = generateRandomVectors(30, 64);
        List<String> ids2 = index.insert(vectors2, null);

        // Update some vectors
        for (int i = 0; i < 10; i++) {
            double[] newVector = generateRandomVector(64);
            index.update(ids1.get(i), newVector, null);
        }

        // Delete some vectors
        index.delete(ids2.subList(0, 10));

        // Search
        double[] query = vectors1[15];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());

        Map<String, Object> stats = index.getStats();
        assertEquals(50, stats.get("vector_count"));  // 30 + 30 - 10 deleted
    }

    @Test
    void testClose() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        index.close();

        assertFalse(index.isInitialized());
    }

    @Test
    void testReinitialize() {
        double[][] vectors = generateRandomVectors(30, 64);
        index.insert(vectors, null);

        index.close();

        // Reinitialize
        index.initialize(storage);
        assertTrue(index.isInitialized());
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
    void testRandomProjectionInitialization() {
        // Insert vectors to trigger projection initialization
        double[][] vectors = generateRandomVectors(20, 128);
        index.insert(vectors, null);

        Map<String, Object> stats = index.getStats();
        assertEquals(128, stats.get("dimensions"));

        // Verify bucket stats are available
        @SuppressWarnings("unchecked")
        Map<String, Object> bucketStats = (Map<String, Object>) stats.get("bucket_stats");
        assertNotNull(bucketStats);
    }

    @Test
    void testEmptySearch() {
        // Insert vectors
        double[][] vectors = generateRandomVectors(10, 64);
        index.insert(vectors, null);

        // Search with very dissimilar vector - might return empty or few results
        double[] query = new double[64];
        Arrays.fill(query, 100.0);  // Very different from training vectors

        List<SearchResult> results = index.search(query, 5);

        // Results might be empty or contain some vectors
        assertTrue(results.size() <= 5);
    }

    @Test
    void testUpdateMetadataOnly() {
        double[][] vectors = generateRandomVectors(10, 64);
        Map<String, Object> metadata = Map.of("original", true);
        List<String> ids = index.insert(vectors, Collections.nCopies(10, metadata));

        String idToUpdate = ids.get(0);

        // Update only metadata (null vector means keep existing)
        Map<String, Object> newMetadata = Map.of("updated", true);
        boolean updated = index.update(idToUpdate, null, newMetadata);
        assertTrue(updated);
    }

    @Test
    void testHighDimensionalVectors() {
        // Test with high-dimensional vectors
        double[][] vectors = generateRandomVectors(50, 512);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

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

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testDifferentBucketWidths() {
        // Test with different bucket widths for euclidean metric
        double[] bucketWidths = {1.0, 4.0, 10.0};

        for (double width : bucketWidths) {
            LSHConfig config = LSHConfig.builder()
                    .metric(Metric.EUCLIDEAN)
                    .bucketWidth(width)
                    .numTables(5)
                    .build();

            LSHIndex testIndex = new LSHIndex(config);
            testIndex.initialize(storage);

            double[][] vectors = generateRandomVectors(30, 64);
            testIndex.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = testIndex.search(query, 5);

            assertFalse(results.isEmpty(),
                    "Should find results with bucket_width=" + width);

            testIndex.close();
        }
    }

    @Test
    void testRecall() {
        // Test recall: how many of the true nearest neighbors are found
        double[][] vectors = generateRandomVectors(100, 64);
        index.insert(vectors, null);

        double[] query = vectors[0];

        // LSH search
        List<SearchResult> lshResults = index.search(query, 10);

        // At minimum, should find the query vector itself
        boolean foundSelf = lshResults.stream()
                .anyMatch(r -> r.getDistance() < 0.01);

        assertTrue(foundSelf, "Should at least find the query vector itself");
    }

    @Test
    void testStatsBeforeAndAfterInsert() {
        Map<String, Object> statsBefore = index.getStats();
        assertEquals(0, statsBefore.get("vector_count"));
        assertNull(statsBefore.get("dimensions"));

        double[][] vectors = generateRandomVectors(20, 64);
        index.insert(vectors, null);

        Map<String, Object> statsAfter = index.getStats();
        assertEquals(20, statsAfter.get("vector_count"));
        assertEquals(64, statsAfter.get("dimensions"));
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
