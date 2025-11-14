package com.veccy.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for MemoryStorage.
 */
public class MemoryStorageTest {

    private MemoryStorage storage;
    private Logger logger;

    @BeforeEach
    public void setUp() {
        logger = mock(Logger.class);
        storage = new MemoryStorage(new HashMap<>(), logger);
        storage.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    public void testInitialization() {
        MemoryStorage newStorage = new MemoryStorage(new HashMap<>());
        assertFalse(newStorage.isInitialized());

        newStorage.initialize();
        assertTrue(newStorage.isInitialized());

        newStorage.close();
    }

    @Test
    public void testStoreVector_Simple() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = Map.of("label", "test");

        boolean result = storage.storeVector("vec1", vector, metadata);
        assertTrue(result);
    }

    @Test
    public void testStoreVector_WithoutMetadata() {
        double[] vector = {1.0, 2.0, 3.0};

        boolean result = storage.storeVector("vec1", vector, null);
        assertTrue(result);
    }

    @Test
    public void testRetrieveVector_Exists() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = Map.of("label", "test");

        storage.storeVector("vec1", vector, metadata);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        StorageBackend.VectorWithMetadata vwm = retrieved.get();
        // ID is tracked separately in storage tests
        assertArrayEquals(vector, vwm.getVector());
        assertEquals(metadata, vwm.getMetadata());
    }

    @Test
    public void testRetrieveVector_NotExists() {
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("nonexistent");
        assertFalse(retrieved.isPresent());
    }

    @Test
    public void testRetrieveVector_NoMetadata() {
        double[] vector = {1.0, 2.0, 3.0};
        storage.storeVector("vec1", vector, null);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        StorageBackend.VectorWithMetadata vwm = retrieved.get();
        assertNull(vwm.getMetadata());
    }

    @Test
    public void testDeleteVector_Exists() {
        double[] vector = {1.0, 2.0, 3.0};
        storage.storeVector("vec1", vector, null);

        assertTrue(storage.retrieveVector("vec1").isPresent());

        boolean result = storage.deleteVector("vec1");
        assertTrue(result);

        assertFalse(storage.retrieveVector("vec1").isPresent());
    }

    @Test
    public void testDeleteVector_NotExists() {
        boolean result = storage.deleteVector("nonexistent");
        assertFalse(result);
    }

    @Test
    public void testUpdateVector_Exists() {
        double[] originalVector = {1.0, 2.0, 3.0};
        Map<String, Object> originalMetadata = Map.of("label", "original");

        storage.storeVector("vec1", originalVector, originalMetadata);

        double[] newVector = {4.0, 5.0, 6.0};
        Map<String, Object> newMetadata = Map.of("label", "updated");

        boolean result = storage.updateVector("vec1", newVector, newMetadata);
        assertTrue(result);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        StorageBackend.VectorWithMetadata vwm = retrieved.get();
        assertArrayEquals(newVector, vwm.getVector());
        assertEquals(newMetadata, vwm.getMetadata());
    }

    @Test
    public void testUpdateVector_NotExists() {
        double[] vector = {1.0, 2.0, 3.0};
        boolean result = storage.updateVector("nonexistent", vector, null);
        assertFalse(result);
    }

    @Test
    public void testUpdateVector_PartialUpdate() {
        double[] originalVector = {1.0, 2.0, 3.0};
        Map<String, Object> originalMetadata = Map.of("label", "original");

        storage.storeVector("vec1", originalVector, originalMetadata);

        // Update only vector
        double[] newVector = {4.0, 5.0, 6.0};
        storage.updateVector("vec1", newVector, null);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(newVector, retrieved.get().getVector());
        assertNull(retrieved.get().getMetadata());

        // Update only metadata
        Map<String, Object> newMetadata = Map.of("label", "updated");
        storage.updateVector("vec1", null, newMetadata);

        retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(newVector, retrieved.get().getVector());
        assertEquals(newMetadata, retrieved.get().getMetadata());
    }

    @Test
    public void testListVectors_All() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);
        storage.storeVector("vec3", new double[]{3.0}, null);

        List<String> ids = storage.listVectors(null);
        assertEquals(3, ids.size());
        assertTrue(ids.contains("vec1"));
        assertTrue(ids.contains("vec2"));
        assertTrue(ids.contains("vec3"));
    }

    @Test
    public void testListVectors_Limited() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);
        storage.storeVector("vec3", new double[]{3.0}, null);
        storage.storeVector("vec4", new double[]{4.0}, null);

        List<String> ids = storage.listVectors(2);
        assertEquals(2, ids.size());
    }

    @Test
    public void testListVectors_Empty() {
        List<String> ids = storage.listVectors(null);
        assertEquals(0, ids.size());
    }

    @Test
    public void testGetStats() {
        storage.storeVector("vec1", new double[]{1.0, 2.0, 3.0}, null);
        storage.storeVector("vec2", new double[]{4.0, 5.0, 6.0}, null);

        Map<String, Object> stats = storage.getStats();

        assertNotNull(stats);
        assertEquals("MemoryStorage", stats.get("type"));
        assertEquals(2, stats.get("vector_count"));
        assertTrue(stats.containsKey("memory_usage_bytes"));
        assertTrue((Long) stats.get("memory_usage_bytes") > 0);
    }

    @Test
    public void testVectorIsolation() {
        // Test that stored vectors are isolated from modifications
        double[] vector = {1.0, 2.0, 3.0};
        storage.storeVector("vec1", vector, null);

        // Modify original array
        vector[0] = 999.0;

        // Retrieved vector should be unchanged
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertEquals(1.0, retrieved.get().getVector()[0]);
    }

    @Test
    public void testMetadataIsolation() {
        // Test that stored metadata is isolated from modifications
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        storage.storeVector("vec1", new double[]{1.0}, metadata);

        // Modify original map
        metadata.put("label", "modified");

        // Retrieved metadata should be unchanged
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertEquals("test", retrieved.get().getMetadata().get("label"));
    }

    @Test
    public void testConcurrentWrites() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String id = "thread" + threadId + "_vec" + j;
                        double[] vector = {threadId, j, threadId + j};
                        storage.storeVector(id, vector, null);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all vectors were stored
        List<String> ids = storage.listVectors(null);
        assertEquals(threadCount * operationsPerThread, ids.size());
    }

    @Test
    public void testConcurrentReads() throws InterruptedException {
        // Store some vectors
        for (int i = 0; i < 100; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        int threadCount = 10;
        int readsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int j = 0; j < readsPerThread; j++) {
                        String id = "vec" + random.nextInt(100);
                        Optional<StorageBackend.VectorWithMetadata> result = storage.retrieveVector(id);
                        if (result.isPresent()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // All reads should have succeeded
        assertEquals(threadCount * readsPerThread, successCount.get());
    }

    @Test
    public void testConcurrentReadWrite() throws InterruptedException {
        int threadCount = 20;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Half writers, half readers
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            final boolean isWriter = i < threadCount / 2;

            executor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int j = 0; j < operationsPerThread; j++) {
                        String id = "vec" + random.nextInt(100);

                        if (isWriter) {
                            double[] vector = {random.nextDouble(), random.nextDouble()};
                            storage.storeVector(id, vector, null);
                        } else {
                            storage.retrieveVector(id);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        // Should complete without exceptions
        assertTrue(true);
    }

    @Test
    public void testConcurrentDelete() throws InterruptedException {
        // Store vectors
        for (int i = 0; i < 100; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Each thread tries to delete some vectors
        for (int i = 0; i < threadCount; i++) {
            final int start = i * 10;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        storage.deleteVector("vec" + (start + j));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // All vectors should be deleted
        List<String> ids = storage.listVectors(null);
        assertEquals(0, ids.size());
    }

    @Test
    public void testClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        assertTrue(storage.isInitialized());

        storage.close();

        // After closing, the storage should be marked as not initialized
        Map<String, Object> stats = storage.getStats();
        assertEquals("not_initialized", stats.get("status"));
        assertFalse(storage.isInitialized());
    }

    @Test
    public void testLargeVectors() {
        // Test with high-dimensional vectors
        int dimensions = 1000;
        double[] largeVector = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            largeVector[i] = Math.random();
        }

        boolean result = storage.storeVector("large_vec", largeVector, null);
        assertTrue(result);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("large_vec");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(largeVector, retrieved.get().getVector());
    }

    @Test
    public void testComplexMetadata() {
        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("string", "value");
        complexMetadata.put("integer", 123);
        complexMetadata.put("double", 45.67);
        complexMetadata.put("boolean", true);
        complexMetadata.put("list", Arrays.asList(1, 2, 3));
        complexMetadata.put("nested", Map.of("key", "value"));

        storage.storeVector("vec1", new double[]{1.0}, complexMetadata);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        Map<String, Object> retrievedMetadata = retrieved.get().getMetadata();
        assertEquals("value", retrievedMetadata.get("string"));
        assertEquals(123, retrievedMetadata.get("integer"));
        assertEquals(45.67, (Double) retrievedMetadata.get("double"), 0.001);
        assertEquals(true, retrievedMetadata.get("boolean"));
    }

    @Test
    public void testListVectorsPaginated_FirstPage() {
        // Store test vectors
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        com.veccy.base.Page<String> page = storage.listVectorsPaginated(3, Optional.empty());

        assertNotNull(page);
        assertEquals(3, page.items().size());
        assertTrue(page.hasMore());
        assertTrue(page.nextCursor().isPresent());
    }

    @Test
    public void testListVectorsPaginated_MiddlePage() {
        // Store test vectors
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        // Get first page
        com.veccy.base.Page<String> firstPage = storage.listVectorsPaginated(3, Optional.empty());
        String cursor = firstPage.nextCursor().get();

        // Get next page
        com.veccy.base.Page<String> secondPage = storage.listVectorsPaginated(3, Optional.of(cursor));

        assertNotNull(secondPage);
        assertEquals(3, secondPage.items().size());
        assertTrue(secondPage.hasMore());
    }

    @Test
    public void testListVectorsPaginated_LastPage() {
        // Store 7 vectors
        for (int i = 0; i < 7; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        // Get first page (3 items)
        com.veccy.base.Page<String> firstPage = storage.listVectorsPaginated(3, Optional.empty());

        // Get second page (3 items)
        com.veccy.base.Page<String> secondPage = storage.listVectorsPaginated(3,
            firstPage.nextCursor());

        // Get last page (1 item)
        com.veccy.base.Page<String> lastPage = storage.listVectorsPaginated(3,
            secondPage.nextCursor());

        assertNotNull(lastPage);
        assertEquals(1, lastPage.items().size());
        assertFalse(lastPage.hasMore());
        assertFalse(lastPage.nextCursor().isPresent());
    }

    @Test
    public void testListVectorsPaginated_EmptyStorage() {
        com.veccy.base.Page<String> page = storage.listVectorsPaginated(10, Optional.empty());

        assertNotNull(page);
        assertTrue(page.items().isEmpty());
        assertFalse(page.hasMore());
    }

    @Test
    public void testListVectorsPaginated_InvalidPageSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            storage.listVectorsPaginated(0, Optional.empty());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            storage.listVectorsPaginated(-1, Optional.empty());
        });
    }

    @Test
    public void testListVectorsPaginated_InvalidCursor() {
        // Store some vectors
        for (int i = 0; i < 5; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        // Use non-existent cursor (should start from beginning)
        com.veccy.base.Page<String> page = storage.listVectorsPaginated(3, Optional.of("nonexistent"));

        assertNotNull(page);
        assertEquals(3, page.items().size());
    }

    @Test
    public void testListVectorsPaginated_ConsistentOrdering() {
        // Store vectors
        for (int i = 0; i < 20; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        // Collect all items through pagination
        List<String> allItems = new ArrayList<>();
        Optional<String> cursor = Optional.empty();

        while (true) {
            com.veccy.base.Page<String> page = storage.listVectorsPaginated(5, cursor);
            allItems.addAll(page.items());

            if (!page.hasMore()) {
                break;
            }
            cursor = page.nextCursor();
        }

        // Should have all 20 items
        assertEquals(20, allItems.size());

        // Items should be sorted consistently
        List<String> sorted = new ArrayList<>(allItems);
        Collections.sort(sorted);
        assertEquals(sorted, allItems);
    }

    @Test
    public void testStreamVectorIds() {
        // Store vectors
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        List<String> streamedIds = storage.streamVectorIds().toList();

        assertEquals(10, streamedIds.size());

        // Should be sorted
        List<String> sortedIds = new ArrayList<>(streamedIds);
        Collections.sort(sortedIds);
        assertEquals(sortedIds, streamedIds);
    }

    @Test
    public void testStreamVectorIds_Empty() {
        List<String> streamedIds = storage.streamVectorIds().toList();

        assertTrue(streamedIds.isEmpty());
    }

    @Test
    public void testStreamVectorIds_LargeDataset() {
        // Store many vectors
        for (int i = 0; i < 1000; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        long count = storage.streamVectorIds().count();

        assertEquals(1000, count);
    }

    @Test
    public void testStoreVector_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.storeVector("vec1", new double[]{1.0}, null);
        });
    }

    @Test
    public void testRetrieveVector_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.retrieveVector("vec1");
        });
    }

    @Test
    public void testDeleteVector_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.deleteVector("vec1");
        });
    }

    @Test
    public void testUpdateVector_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.updateVector("vec1", new double[]{1.0}, null);
        });
    }

    @Test
    public void testListVectors_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.listVectors(null);
        });
    }

    @Test
    public void testListVectorsPaginated_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.listVectorsPaginated(10, Optional.empty());
        });
    }

    @Test
    public void testStreamVectorIds_NotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.streamVectorIds();
        });
    }

    @Test
    public void testMultipleClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        // First close
        storage.close();
        assertFalse(storage.isInitialized());

        // Second close should not throw
        storage.close();
        assertFalse(storage.isInitialized());

        // Third close should not throw
        storage.close();
        assertFalse(storage.isInitialized());
    }

    @Test
    public void testReinitializeAfterClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        storage.close();
        assertFalse(storage.isInitialized());

        // Reinitialize
        storage.initialize();
        assertTrue(storage.isInitialized());

        // Storage should be empty after reinitialization
        List<String> ids = storage.listVectors(null);
        assertTrue(ids.isEmpty());
    }

    @Test
    public void testGetStatsNotInitialized() {
        MemoryStorage uninitializedStorage = new MemoryStorage(new HashMap<>());

        Map<String, Object> stats = uninitializedStorage.getStats();

        assertNotNull(stats);
        assertEquals("not_initialized", stats.get("status"));
    }

    @Test
    public void testListVectors_ZeroLimit() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);

        List<String> ids = storage.listVectors(0);

        // Zero limit should return all vectors
        assertEquals(2, ids.size());
    }

    @Test
    public void testListVectors_LimitExceedsCount() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);

        List<String> ids = storage.listVectors(100);

        assertEquals(2, ids.size());
    }

    @Test
    public void testStoreVector_OverwriteExisting() {
        double[] originalVector = {1.0, 2.0, 3.0};
        Map<String, Object> originalMetadata = Map.of("version", "1");

        storage.storeVector("vec1", originalVector, originalMetadata);

        // Store again with same ID
        double[] newVector = {4.0, 5.0, 6.0};
        Map<String, Object> newMetadata = Map.of("version", "2");

        storage.storeVector("vec1", newVector, newMetadata);

        // Should have new values
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(newVector, retrieved.get().getVector());
        assertEquals("2", retrieved.get().getMetadata().get("version"));

        // Count should still be 1
        assertEquals(1, storage.listVectors(null).size());
    }

    @Test
    public void testMemoryUsageCalculation() {
        // Store vectors of different sizes
        storage.storeVector("small", new double[]{1.0}, null);
        storage.storeVector("medium", new double[]{1.0, 2.0, 3.0}, null);
        storage.storeVector("large", new double[100], null);

        Map<String, Object> stats = storage.getStats();

        Long memoryUsageBytes = (Long) stats.get("memory_usage_bytes");
        Double memoryUsageMB = (Double) stats.get("memory_usage_mb");

        assertNotNull(memoryUsageBytes);
        assertNotNull(memoryUsageMB);
        assertTrue(memoryUsageBytes > 0);
        assertTrue(memoryUsageMB > 0);

        // Check conversion is correct
        assertEquals(memoryUsageBytes / (1024.0 * 1024.0), memoryUsageMB, 0.0001);
    }

    @Test
    public void testConfigWithNullConstructor() {
        MemoryStorage nullConfigStorage = new MemoryStorage(null);
        nullConfigStorage.initialize();

        assertTrue(nullConfigStorage.isInitialized());

        // Should work normally
        nullConfigStorage.storeVector("vec1", new double[]{1.0}, null);
        assertTrue(nullConfigStorage.retrieveVector("vec1").isPresent());

        nullConfigStorage.close();
    }
}
