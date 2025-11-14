package com.veccy.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HybridStorage.
 */
class HybridStorageTest {

    private HybridStorage storage;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        Map<String, Object> config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        config.put("cache_size", 10);
        config.put("cache_initial_capacity", 5);

        storage = new HybridStorage(config);
        storage.initialize();
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    @Test
    void testInitialization() {
        assertTrue(storage.isInitialized());

        Map<String, Object> stats = storage.getStats();
        assertEquals("HybridStorage", stats.get("type"));
        assertTrue((Boolean) stats.get("initialized"));
        assertNotNull(stats.get("cache"));
        assertNotNull(stats.get("disk"));
    }

    @Test
    void testStoreAndRetrieve() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = Map.of("key", "value");

        boolean stored = storage.storeVector("vec1", vector, metadata);
        assertTrue(stored);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(vector, retrieved.get().getVector());
        assertEquals("value", retrieved.get().getMetadata().get("key"));
    }

    @Test
    void testCacheHit() {
        double[] vector = {1.0, 2.0, 3.0};

        storage.storeVector("vec1", vector, null);

        // First retrieval - should be cache hit (just inserted)
        storage.retrieveVector("vec1");

        // Second retrieval - should definitely be cache hit
        storage.retrieveVector("vec1");

        Map<String, Object> stats = storage.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        long hits = (Long) cacheStats.get("hits");
        assertTrue(hits >= 1, "Should have at least one cache hit");
    }

    @Test
    void testCacheMiss() {
        double[] vector = {1.0, 2.0, 3.0};

        storage.storeVector("vec1", vector, null);

        // Invalidate cache to force disk read
        storage.invalidateCache();

        // This should be a cache miss
        storage.retrieveVector("vec1");

        Map<String, Object> stats = storage.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        long misses = (Long) cacheStats.get("misses");
        assertTrue(misses >= 1, "Should have at least one cache miss");
    }

    @Test
    void testCacheEviction() {
        // Insert more vectors than cache size (cache_size=10)
        for (int i = 0; i < 15; i++) {
            double[] vector = {i, i + 1.0, i + 2.0};
            storage.storeVector("vec" + i, vector, null);
        }

        // Access all vectors to trigger evictions
        for (int i = 0; i < 15; i++) {
            storage.retrieveVector("vec" + i);
        }

        Map<String, Object> stats = storage.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        long evictions = (Long) cacheStats.get("evictions");
        assertTrue(evictions > 0, "Should have cache evictions");
    }

    @Test
    void testUpdate() {
        double[] vector1 = {1.0, 2.0, 3.0};
        double[] vector2 = {4.0, 5.0, 6.0};

        storage.storeVector("vec1", vector1, null);

        boolean updated = storage.updateVector("vec1", vector2, null);
        assertTrue(updated);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(vector2, retrieved.get().getVector());
    }

    @Test
    void testDelete() {
        double[] vector = {1.0, 2.0, 3.0};

        storage.storeVector("vec1", vector, null);
        assertTrue(storage.retrieveVector("vec1").isPresent());

        boolean deleted = storage.deleteVector("vec1");
        assertTrue(deleted);

        assertFalse(storage.retrieveVector("vec1").isPresent());
    }

    @Test
    void testListVectors() {
        for (int i = 0; i < 5; i++) {
            double[] vector = {i, i + 1.0, i + 2.0};
            storage.storeVector("vec" + i, vector, null);
        }

        List<String> ids = storage.listVectors(null);
        assertEquals(5, ids.size());

        List<String> limitedIds = storage.listVectors(3);
        assertEquals(3, limitedIds.size());
    }

    @Test
    void testPreloadCache() {
        // Store vectors
        for (int i = 0; i < 5; i++) {
            double[] vector = {i, i + 1.0, i + 2.0};
            storage.storeVector("vec" + i, vector, null);
        }

        // Invalidate cache
        storage.invalidateCache();

        // Preload specific vectors
        List<String> toPreload = Arrays.asList("vec0", "vec2", "vec4");
        int preloaded = storage.preloadCache(toPreload);

        assertEquals(3, preloaded);

        // These should be cache hits
        storage.retrieveVector("vec0");
        storage.retrieveVector("vec2");

        Map<String, Object> stats = storage.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        long hits = (Long) cacheStats.get("hits");
        assertTrue(hits >= 2);
    }

    @Test
    void testCacheHitRate() {
        double[] vector = {1.0, 2.0, 3.0};

        storage.storeVector("vec1", vector, null);

        // Multiple retrievals should increase hit rate
        for (int i = 0; i < 10; i++) {
            storage.retrieveVector("vec1");
        }

        double hitRate = storage.getCacheHitRate();
        assertTrue(hitRate > 0.5, "Hit rate should be above 50%");

        Map<String, Object> stats = storage.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        double statsHitRate = (Double) cacheStats.get("hit_rate");
        assertEquals(hitRate, statsHitRate, 0.01);
    }

    @Test
    void testWriteThroughConsistency() {
        double[] vector = {1.0, 2.0, 3.0};

        storage.storeVector("vec1", vector, null);

        // Verify in cache
        storage.retrieveVector("vec1");

        // Invalidate cache
        storage.invalidateCache();

        // Should still be retrievable from disk
        Optional<StorageBackend.VectorWithMetadata> fromDisk = storage.retrieveVector("vec1");
        assertTrue(fromDisk.isPresent());
        assertArrayEquals(vector, fromDisk.get().getVector());
    }

    @Test
    void testRetrieveNonExistent() {
        Optional<StorageBackend.VectorWithMetadata> result = storage.retrieveVector("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateNonExistent() {
        double[] vector = {1.0, 2.0, 3.0};
        boolean updated = storage.updateVector("nonexistent", vector, null);
        assertFalse(updated);
    }

    @Test
    void testDeleteNonExistent() {
        boolean deleted = storage.deleteVector("nonexistent");
        assertFalse(deleted);
    }

    @Test
    void testMetadataPreservation() {
        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "test");
        metadata.put("value", 42);
        metadata.put("nested", Map.of("key", "value"));

        storage.storeVector("vec1", vector, metadata);

        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        Map<String, Object> retrievedMetadata = retrieved.get().getMetadata();
        assertEquals("test", retrievedMetadata.get("type"));
        assertEquals(42, retrievedMetadata.get("value"));
        assertNotNull(retrievedMetadata.get("nested"));
    }

    @Test
    void testListVectorsPaginated_FirstPage() {
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        com.veccy.base.Page<String> page = storage.listVectorsPaginated(3, Optional.empty());

        assertNotNull(page);
        assertEquals(3, page.items().size());
        assertTrue(page.hasMore());
    }

    @Test
    void testListVectorsPaginated_EmptyStorage() {
        com.veccy.base.Page<String> page = storage.listVectorsPaginated(10, Optional.empty());

        assertNotNull(page);
        assertTrue(page.items().isEmpty());
        assertFalse(page.hasMore());
    }

    @Test
    void testStreamVectorIds() {
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        List<String> streamedIds = storage.streamVectorIds().toList();

        assertEquals(10, streamedIds.size());
    }

    @Test
    void testStreamVectorIds_Empty() {
        List<String> streamedIds = storage.streamVectorIds().toList();

        assertTrue(streamedIds.isEmpty());
    }

    @Test
    void testStoreVector_NotInitialized() {
        Map<String, Object> config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        HybridStorage uninitializedStorage = new HybridStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.storeVector("vec1", new double[]{1.0}, null);
        });
    }

    @Test
    void testRetrieveVector_NotInitialized() {
        Map<String, Object> config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        HybridStorage uninitializedStorage = new HybridStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.retrieveVector("vec1");
        });
    }

    @Test
    void testMultipleClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        storage.close();
        assertFalse(storage.isInitialized());

        // Second close should not throw
        storage.close();
        assertFalse(storage.isInitialized());
    }

    @Test
    void testGetStatsNotInitialized() {
        Map<String, Object> config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        HybridStorage uninitializedStorage = new HybridStorage(config);

        Map<String, Object> stats = uninitializedStorage.getStats();

        assertNotNull(stats);
        assertEquals("not_initialized", stats.get("status"));
    }

    @Test
    void testVectorIsolation() {
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
    void testMetadataIsolation() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");

        storage.storeVector("vec1", new double[]{1.0}, metadata);

        // Modify original map after storing
        metadata.put("label", "modified");

        // Invalidate cache to ensure we read from disk
        storage.invalidateCache();

        // Retrieved metadata should be unchanged
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertEquals("test", retrieved.get().getMetadata().get("label"));
    }

    @Test
    void testStoreVector_OverwriteExisting() {
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
    void testCacheInvalidationOnUpdate() {
        double[] vector1 = {1.0, 2.0, 3.0};
        storage.storeVector("vec1", vector1, null);

        // Retrieve to cache
        storage.retrieveVector("vec1");

        // Update vector
        double[] vector2 = {4.0, 5.0, 6.0};
        storage.updateVector("vec1", vector2, null);

        // Should get updated vector (cache should be invalidated)
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(vector2, retrieved.get().getVector());
    }

    @Test
    void testCacheInvalidationOnDelete() {
        double[] vector = {1.0, 2.0, 3.0};
        storage.storeVector("vec1", vector, null);

        // Retrieve to cache
        storage.retrieveVector("vec1");

        // Delete vector
        storage.deleteVector("vec1");

        // Should not be retrievable (cache should be invalidated)
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testPreloadCacheWithNonExistentVectors() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        List<String> toPreload = Arrays.asList("vec1", "nonexistent1", "nonexistent2");
        int preloaded = storage.preloadCache(toPreload);

        // Only vec1 exists
        assertEquals(1, preloaded);
    }

    @Test
    void testPreloadCache_EmptyList() {
        int preloaded = storage.preloadCache(new ArrayList<>());

        assertEquals(0, preloaded);
    }

    @Test
    void testInvalidateCacheIdempotent() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        // Multiple invalidations should not throw
        storage.invalidateCache();
        storage.invalidateCache();
        storage.invalidateCache();

        // Storage should still work
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testCacheStatistics() {
        Map<String, Object> stats = storage.getStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("cache"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cacheStats = (Map<String, Object>) stats.get("cache");

        assertTrue(cacheStats.containsKey("hits"));
        assertTrue(cacheStats.containsKey("misses"));
        assertTrue(cacheStats.containsKey("evictions"));
        assertTrue(cacheStats.containsKey("hit_rate"));
        assertTrue(cacheStats.containsKey("size"));
    }

    @Test
    void testDiskStatistics() {
        Map<String, Object> stats = storage.getStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("disk"));

        @SuppressWarnings("unchecked")
        Map<String, Object> diskStats = (Map<String, Object>) stats.get("disk");

        assertTrue(diskStats.containsKey("vector_count"));
    }

    @Test
    void testUpdateVector_PartialUpdate() {
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
    }

    @Test
    void testLargeVectors() {
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
    void testReinitializeAfterClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        storage.close();
        assertFalse(storage.isInitialized());

        // Reinitialize
        storage.initialize();
        assertTrue(storage.isInitialized());

        // Vector should still be there (persisted to disk)
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
    }

    @Test
    void testConfigWithDefaultCacheSize() {
        Map<String, Object> config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        // No cache_size specified - should use default

        HybridStorage defaultCacheStorage = new HybridStorage(config);
        defaultCacheStorage.initialize();

        try {
            assertTrue(defaultCacheStorage.isInitialized());

            // Should work normally
            defaultCacheStorage.storeVector("vec1", new double[]{1.0}, null);
            assertTrue(defaultCacheStorage.retrieveVector("vec1").isPresent());
        } finally {
            defaultCacheStorage.close();
        }
    }
}
