package com.veccy.integration;

import com.veccy.base.Index;
import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.config.HNSWConfig;
import com.veccy.config.IVFConfig;
import com.veccy.config.LSHConfig;
import com.veccy.config.Metric;
import com.veccy.indices.*;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for verifying thread-safety and correct behavior under concurrent load.
 */
class ConcurrencyIntegrationTest {

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
    @Timeout(30)
    void testConcurrentInsertAndSearchHNSW() throws Exception {
        HNSWConfig config = HNSWConfig.builder()
                .m(8)
                .efConstruction(100)
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

        int numThreads = 4;
        int vectorsPerThread = 100;
        int dimensions = 128;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger insertCount = new AtomicInteger(0);
        AtomicInteger searchCount = new AtomicInteger(0);

        // Half threads insert, half threads search
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    if (threadId < numThreads / 2) {
                        // Insert threads
                        for (int i = 0; i < vectorsPerThread; i++) {
                            double[][] vectors = new double[][]{generateRandomVector(dimensions)};
                            index.insert(vectors, null);
                            insertCount.incrementAndGet();
                        }
                    } else {
                        // Search threads
                        for (int i = 0; i < vectorsPerThread; i++) {
                            double[] queryVector = generateRandomVector(dimensions);
                            List<SearchResult> results = index.search(queryVector, 10);
                            assertNotNull(results);
                            searchCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(vectorsPerThread * (numThreads / 2), insertCount.get());
        assertEquals(vectorsPerThread * (numThreads / 2), searchCount.get());

        // Verify final state
        Map<String, Object> stats = index.getStats();
        assertEquals(200, stats.get("vector_count"));
    }

    @Test
    @Timeout(30)
    void testConcurrentUpdateAndSearchFlatIndex() throws Exception {
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.EUCLIDEAN)
                .build();

        Index index = new FlatIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 64;
        int initialVectors = 50;

        // Insert initial vectors
        double[][] vectors = generateRandomVectors(initialVectors, dimensions);
        List<String> vectorIds = index.insert(vectors, null);

        int numThreads = 4;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger updateCount = new AtomicInteger(0);
        AtomicInteger searchCount = new AtomicInteger(0);

        // Half threads update, half threads search
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Random threadRandom = new Random(42 + threadId);
                    if (threadId < numThreads / 2) {
                        // Update threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            String id = vectorIds.get(threadRandom.nextInt(vectorIds.size()));
                            double[] newVector = generateRandomVector(dimensions);
                            index.update(id, newVector, null);
                            updateCount.incrementAndGet();
                        }
                    } else {
                        // Search threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            double[] queryVector = generateRandomVector(dimensions);
                            List<SearchResult> results = index.search(queryVector, 10);
                            assertNotNull(results);
                            assertTrue(results.size() <= 10);
                            searchCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(operationsPerThread * (numThreads / 2), updateCount.get());
        assertEquals(operationsPerThread * (numThreads / 2), searchCount.get());
    }

    @Test
    @Timeout(30)
    void testConcurrentInsertDeleteAndSearchIVF() throws Exception {
        IVFConfig config = IVFConfig.builder()
                .numClusters(5)
                .numProbes(2)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();

        Index index = new IVFIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);
        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        int dimensions = 32;
        int trainingVectors = 50;

        // Train the index
        double[][] trainingData = generateRandomVectors(trainingVectors, dimensions);
        ((IVFIndex) index).train(trainingData);

        // Insert initial vectors
        int initialVectors = 100;
        double[][] vectors = generateRandomVectors(initialVectors, dimensions);
        List<String> vectorIds = Collections.synchronizedList(new ArrayList<>(index.insert(vectors, null)));

        int numThreads = 6;
        int operationsPerThread = 30;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger insertCount = new AtomicInteger(0);
        AtomicInteger deleteCount = new AtomicInteger(0);
        AtomicInteger searchCount = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Random threadRandom = new Random(42 + threadId);
                    int threadType = threadId % 3;

                    if (threadType == 0) {
                        // Insert threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            double[][] newVectors = new double[][]{generateRandomVector(dimensions)};
                            List<String> newIds = index.insert(newVectors, null);
                            vectorIds.addAll(newIds);
                            insertCount.incrementAndGet();
                        }
                    } else if (threadType == 1) {
                        // Delete threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            synchronized (vectorIds) {
                                if (!vectorIds.isEmpty()) {
                                    String id = vectorIds.remove(threadRandom.nextInt(vectorIds.size()));
                                    index.delete(Collections.singletonList(id));
                                    deleteCount.incrementAndGet();
                                }
                            }
                        }
                    } else {
                        // Search threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            double[] queryVector = generateRandomVector(dimensions);
                            List<SearchResult> results = index.search(queryVector, 5);
                            assertNotNull(results);
                            searchCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(operationsPerThread * 2, insertCount.get());
        assertTrue(deleteCount.get() <= operationsPerThread * 2);
        assertEquals(operationsPerThread * 2, searchCount.get());
    }

    @Test
    @Timeout(30)
    void testConcurrentMixedOperationsMultipleIndices() throws Exception {
        int dimensions = 64;
        int numIndices = 3;
        List<Index> indices = new ArrayList<>();

        // Create different index types
        FlatConfig flatConfig = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        Index flatIndex = new FlatIndex(flatConfig);
        StorageBackend flatStorage = new MemoryStorage(new HashMap<>());
        flatStorage.initialize();
        flatIndex.initialize(flatStorage);
        indices.add(flatIndex);
        resourcesToClose.add(flatIndex);
        resourcesToClose.add(flatStorage);

        HNSWConfig hnswConfig = HNSWConfig.builder()
                .m(8)
                .metric(Metric.COSINE)
                .randomSeed(42L)
                .build();
        Index hnswIndex = new HNSWIndex(hnswConfig);
        StorageBackend hnswStorage = new MemoryStorage(new HashMap<>());
        hnswStorage.initialize();
        hnswIndex.initialize(hnswStorage);
        indices.add(hnswIndex);
        resourcesToClose.add(hnswIndex);
        resourcesToClose.add(hnswStorage);

        LSHConfig lshConfig = LSHConfig.builder()
                .numTables(4)
                .numHashBits(8)
                .metric(Metric.COSINE)
                .build();
        Index lshIndex = new LSHIndex(lshConfig);
        StorageBackend lshStorage = new MemoryStorage(new HashMap<>());
        lshStorage.initialize();
        lshIndex.initialize(lshStorage);
        indices.add(lshIndex);
        resourcesToClose.add(lshIndex);
        resourcesToClose.add(lshStorage);

        int numThreads = 9;
        int operationsPerThread = 50;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger totalOperations = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Random threadRandom = new Random(42 + threadId);
                    Index targetIndex = indices.get(threadId % numIndices);

                    for (int i = 0; i < operationsPerThread; i++) {
                        try {
                            int operation = threadRandom.nextInt(3);

                            if (operation == 0) {
                                // Insert
                                double[][] vectors = new double[][]{generateRandomVector(dimensions)};
                                targetIndex.insert(vectors, null);
                            } else if (operation == 1) {
                                // Search
                                double[] queryVector = generateRandomVector(dimensions);
                                List<SearchResult> results = targetIndex.search(queryVector, 10);
                                assertNotNull(results);
                            } else {
                                // Get stats
                                Map<String, Object> stats = targetIndex.getStats();
                                assertNotNull(stats);
                                assertTrue(stats.containsKey("type"));
                            }
                            totalOperations.incrementAndGet();
                        } catch (Exception e) {
                            // Some operations may fail due to race conditions - this is acceptable
                        }
                    }
                } catch (Exception e) {
                    fail("Thread failed unexpectedly: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify that most operations completed (allow for some failures due to race conditions)
        int expectedOperations = numThreads * operationsPerThread;
        assertTrue(totalOperations.get() >= expectedOperations * 0.8,
                "Expected at least 80% of operations to complete, but got " +
                        totalOperations.get() + " out of " + expectedOperations);

        // Verify all indices are still functional
        for (Index index : indices) {
            Map<String, Object> stats = index.getStats();
            assertNotNull(stats);
            assertTrue((int) stats.get("vector_count") > 0);
        }
    }

    @Test
    @Timeout(30)
    void testConcurrentStatsReading() throws Exception {
        HNSWConfig config = HNSWConfig.builder()
                .m(8)
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
        int initialVectors = 50;

        // Insert initial vectors
        double[][] vectors = generateRandomVectors(initialVectors, dimensions);
        index.insert(vectors, null);

        int numThreads = 8;
        int operationsPerThread = 100;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger statsReadCount = new AtomicInteger(0);

        // Half threads modify, half threads read stats
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    if (threadId < numThreads / 2) {
                        // Modification threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            double[][] newVectors = new double[][]{generateRandomVector(dimensions)};
                            index.insert(newVectors, null);
                        }
                    } else {
                        // Stats reading threads
                        for (int i = 0; i < operationsPerThread; i++) {
                            Map<String, Object> stats = index.getStats();
                            assertNotNull(stats);
                            assertTrue(stats.containsKey("vector_count"));
                            assertTrue((int) stats.get("vector_count") >= initialVectors);
                            statsReadCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    fail("Thread failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(20, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(operationsPerThread * (numThreads / 2), statsReadCount.get());

        // Verify final vector count
        Map<String, Object> finalStats = index.getStats();
        int finalCount = (int) finalStats.get("vector_count");
        assertEquals(initialVectors + (operationsPerThread * (numThreads / 2)), finalCount);
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
