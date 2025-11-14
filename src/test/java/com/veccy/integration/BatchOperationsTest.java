package com.veccy.integration;

import com.veccy.base.Index;
import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.indices.FlatIndex;
import com.veccy.indices.HNSWIndex;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for batch operations (batchUpdate and batchSearch).
 */
class BatchOperationsTest {

    private final List<AutoCloseable> resourcesToClose = new ArrayList<>();
    private final Random testRandom = new Random(42);

    @AfterEach
    void tearDown() {
        for (AutoCloseable resource : resourcesToClose) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignore close errors in tests
            }
        }
        resourcesToClose.clear();
    }

    @Test
    void testBatchUpdate_FlatIndex() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 64;
        int vectorCount = 50;

        // Insert initial vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        List<String> ids = index.insert(vectors, null);

        // Batch update with new vectors
        List<double[]> newVectors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            newVectors.add(generateRandomVector(dimensions));
        }

        List<String> idsToUpdate = ids.subList(0, 10);
        List<Boolean> results = index.batchUpdate(idsToUpdate, newVectors, null);

        // Verify all updates succeeded
        assertEquals(10, results.size());
        assertTrue(results.stream().allMatch(r -> r));

        // Verify vectors were actually updated by searching
        for (int i = 0; i < 10; i++) {
            List<SearchResult> searchResults = index.search(newVectors.get(i), 1);
            assertEquals(1, searchResults.size());
            assertEquals(idsToUpdate.get(i), searchResults.get(0).id());
            assertTrue(searchResults.get(0).distance() < 0.01); // Should be very close
        }
    }

    @Test
    void testBatchUpdate_HNSWIndex() {
        HNSWConfig config = HNSWConfig.builder()
                .m(8)
                .efConstruction(100)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();

        Index index = new HNSWIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 64;
        int vectorCount = 100;

        // Insert initial vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        List<String> ids = index.insert(vectors, null);

        // Batch update with new vectors
        List<double[]> newVectors = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            newVectors.add(generateRandomVector(dimensions));
        }

        List<String> idsToUpdate = ids.subList(0, 20);
        List<Boolean> results = index.batchUpdate(idsToUpdate, newVectors, null);

        // Verify all updates succeeded
        assertEquals(20, results.size());
        assertTrue(results.stream().allMatch(r -> r));

        // Verify graph structure is still valid
        Map<String, Object> stats = index.getStats();
        assertEquals(vectorCount, stats.get("vector_count"));
    }

    @Test
    void testBatchUpdate_WithMetadata() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;

        // Insert initial vectors with metadata
        double[][] vectors = generateRandomVectors(5, dimensions);
        List<Map<String, Object>> initialMetadata = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("category", "old");
            meta.put("index", i);
            initialMetadata.add(meta);
        }
        List<String> ids = index.insert(vectors, initialMetadata);

        // Batch update with new metadata
        List<Map<String, Object>> newMetadata = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("category", "new");
            meta.put("index", i);
            meta.put("updated", true);
            newMetadata.add(meta);
        }

        List<Boolean> results = index.batchUpdate(ids, null, newMetadata);

        // Verify updates succeeded
        assertTrue(results.stream().allMatch(r -> r));

        // Verify metadata was updated
        for (int i = 0; i < 5; i++) {
            List<SearchResult> searchResults = index.search(vectors[i], 1);
            assertEquals(1, searchResults.size());
            Map<String, Object> meta = searchResults.get(0).metadata();
            assertNotNull(meta);
            assertEquals("new", meta.get("category"));
            assertEquals(true, meta.get("updated"));
        }
    }

    @Test
    void testBatchUpdate_PartialFailure() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;

        // Insert initial vectors
        double[][] vectors = generateRandomVectors(5, dimensions);
        List<String> ids = index.insert(vectors, null);

        // Try to update with some non-existent IDs
        List<String> idsToUpdate = new ArrayList<>(ids);
        idsToUpdate.add("non-existent-id-1");
        idsToUpdate.add("non-existent-id-2");

        List<double[]> newVectors = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            newVectors.add(generateRandomVector(dimensions));
        }

        List<Boolean> results = index.batchUpdate(idsToUpdate, newVectors, null);

        // Verify results
        assertEquals(7, results.size());
        // First 5 should succeed
        for (int i = 0; i < 5; i++) {
            assertTrue(results.get(i), "Update " + i + " should succeed");
        }
        // Last 2 should fail
        assertFalse(results.get(5), "Update 5 should fail");
        assertFalse(results.get(6), "Update 6 should fail");
    }

    @Test
    void testBatchSearch_FlatIndex() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 64;
        int vectorCount = 100;

        // Insert vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        index.insert(vectors, null);

        // Batch search with multiple queries
        double[][] queryVectors = generateRandomVectors(10, dimensions);
        List<List<SearchResult>> results = index.batchSearch(queryVectors, 5);

        // Verify results
        assertEquals(10, results.size());
        for (List<SearchResult> queryResults : results) {
            assertEquals(5, queryResults.size());
            // Verify results are sorted by distance
            for (int i = 0; i < queryResults.size() - 1; i++) {
                assertTrue(queryResults.get(i).distance() <= queryResults.get(i + 1).distance());
            }
        }
    }

    @Test
    void testBatchSearch_HNSWIndex() {
        HNSWConfig config = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();

        Index index = new HNSWIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 128;
        int vectorCount = 200;

        // Insert vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        index.insert(vectors, null);

        // Batch search with multiple queries
        double[][] queryVectors = generateRandomVectors(20, dimensions);
        List<List<SearchResult>> results = index.batchSearch(queryVectors, 10);

        // Verify results
        assertEquals(20, results.size());
        for (List<SearchResult> queryResults : results) {
            assertEquals(10, queryResults.size());
            // Verify results are sorted by distance
            for (int i = 0; i < queryResults.size() - 1; i++) {
                assertTrue(queryResults.get(i).distance() <= queryResults.get(i + 1).distance());
            }
        }
    }

    @Test
    void testBatchSearch_EmptyIndex() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;

        // Batch search on empty index
        double[][] queryVectors = generateRandomVectors(5, dimensions);
        List<List<SearchResult>> results = index.batchSearch(queryVectors, 10);

        // Verify all results are empty
        assertEquals(5, results.size());
        for (List<SearchResult> queryResults : results) {
            assertTrue(queryResults.isEmpty());
        }
    }

    @Test
    void testBatchSearch_LargeK() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;
        int vectorCount = 20;

        // Insert vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        index.insert(vectors, null);

        // Batch search with k > vector count
        double[][] queryVectors = generateRandomVectors(3, dimensions);
        List<List<SearchResult>> results = index.batchSearch(queryVectors, 100);

        // Verify results are limited to available vectors
        assertEquals(3, results.size());
        for (List<SearchResult> queryResults : results) {
            assertEquals(vectorCount, queryResults.size());
        }
    }

    //@Disabled("This test is sensitive to performance variations and may fail with JaCoCo agent")
    @Test
    void testBatchOperations_Performance() {
        HNSWConfig config = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();

        Index index = new HNSWIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 128;
        int vectorCount = 500;

        // Insert vectors
        double[][] vectors = generateRandomVectors(vectorCount, dimensions);
        List<String> ids = index.insert(vectors, null);

        // Measure batch search performance
        double[][] queryVectors = generateRandomVectors(100, dimensions);

        long startBatch = System.nanoTime();
        List<List<SearchResult>> batchResults = index.batchSearch(queryVectors, 10);
        long batchTime = System.nanoTime() - startBatch;

        // Measure individual search performance
        long startIndividual = System.nanoTime();
        List<List<SearchResult>> individualResults = new ArrayList<>();
        for (double[] queryVector : queryVectors) {
            individualResults.add(index.search(queryVector, 10));
        }
        long individualTime = System.nanoTime() - startIndividual;

        // Verify results are correct
        assertEquals(100, batchResults.size());
        assertEquals(100, individualResults.size());

        // Log performance comparison
        System.out.printf("Batch search time: %dms%n", batchTime / 1_000_000);
        System.out.printf("Individual search time: %dms%n", individualTime / 1_000_000);
        System.out.printf("Ratio: %.2fx%n", (double) batchTime / individualTime);

        // Batch operations should be reasonably efficient (within 2x of individual)
        // The main benefit is simplified API and reduced lock acquisition overhead
        assertTrue(batchTime <= individualTime * 2.0,
                String.format("Batch operations should be reasonably efficient: batch=%dms, individual=%dms",
                        batchTime / 1_000_000, individualTime / 1_000_000));
    }

    @Test
    void testBatchUpdate_EmptyLists() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Batch update with empty lists
        List<Boolean> results = index.batchUpdate(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        assertTrue(results.isEmpty());
    }

    @Test
    void testBatchSearch_SingleQuery() {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;

        // Insert vectors
        double[][] vectors = generateRandomVectors(50, dimensions);
        index.insert(vectors, null);

        // Batch search with single query
        double[][] queryVectors = new double[][]{generateRandomVector(dimensions)};
        List<List<SearchResult>> results = index.batchSearch(queryVectors, 10);

        assertEquals(1, results.size());
        assertEquals(10, results.get(0).size());
    }

    private double[] generateRandomVector(int dimensions) {
        double[] vector = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = testRandom.nextDouble() * 10.0 - 5.0;
        }
        return vector;
    }

    private double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];
        for (int i = 0; i < count; i++) {
            vectors[i] = generateRandomVector(dimensions);
        }
        return vectors;
    }
}
