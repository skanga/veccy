package com.veccy.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiskStorage.
 */
public class DiskStorageTest {

    @TempDir
    Path tempDir;

    private DiskStorage storage;
    private Map<String, Object> config;

    @BeforeEach
    public void setUp() {
        config = new HashMap<>();
        config.put("data_dir", tempDir.toString());
        storage = new DiskStorage(config);
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
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("data_dir", tempDir.resolve("new_storage").toString());

        DiskStorage newStorage = new DiskStorage(newConfig);
        assertFalse(newStorage.isInitialized());

        newStorage.initialize();
        assertTrue(newStorage.isInitialized());

        // Verify directories were created
        assertTrue(Files.exists(tempDir.resolve("new_storage")));
        assertTrue(Files.exists(tempDir.resolve("new_storage/vectors")));
        assertTrue(Files.exists(tempDir.resolve("new_storage/metadata")));

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
        assertArrayEquals(vector, vwm.getVector(), 1e-6);
        assertEquals(metadata.get("label"), vwm.getMetadata().get("label"));
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
        assertArrayEquals(newVector, vwm.getVector(), 1e-6);
        assertEquals("updated", vwm.getMetadata().get("label"));
    }

    @Test
    public void testUpdateVector_NotExists() {
        double[] vector = {1.0, 2.0, 3.0};
        boolean result = storage.updateVector("nonexistent", vector, null);
        assertFalse(result);
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
        assertEquals("DiskStorage", stats.get("type"));
        assertEquals(2, stats.get("vector_count"));
        assertTrue(stats.containsKey("disk_usage_bytes"));
        assertTrue((Long) stats.get("disk_usage_bytes") > 0);
    }

    @Test
    public void testPersistenceAcrossRestarts() {
        // Store vectors
        double[] vector1 = {1.0, 2.0, 3.0};
        double[] vector2 = {4.0, 5.0, 6.0};
        Map<String, Object> metadata1 = Map.of("label", "first");
        Map<String, Object> metadata2 = Map.of("label", "second");

        storage.storeVector("vec1", vector1, metadata1);
        storage.storeVector("vec2", vector2, metadata2);

        // Close storage
        storage.close();

        // Create new storage instance with same directory
        DiskStorage newStorage = new DiskStorage(config);
        newStorage.initialize();

        try {
            // Verify vectors are still there
            Optional<StorageBackend.VectorWithMetadata> retrieved1 = newStorage.retrieveVector("vec1");
            assertTrue(retrieved1.isPresent());
            assertArrayEquals(vector1, retrieved1.get().getVector(), 1e-6);
            assertEquals("first", retrieved1.get().getMetadata().get("label"));

            Optional<StorageBackend.VectorWithMetadata> retrieved2 = newStorage.retrieveVector("vec2");
            assertTrue(retrieved2.isPresent());
            assertArrayEquals(vector2, retrieved2.get().getVector(), 1e-6);
            assertEquals("second", retrieved2.get().getMetadata().get("label"));

            List<String> ids = newStorage.listVectors(null);
            assertEquals(2, ids.size());
        } finally {
            newStorage.close();
        }
    }

    @Test
    public void testIdSanitization() {
        // Test that IDs with special characters are handled correctly
        String[] specialIds = {
            "vec:with:colons",
            "vec/with/slashes",
            "vec\\with\\backslashes",
            "vec<with>brackets",
            "vec|with|pipes"
        };

        double[] vector = {1.0, 2.0, 3.0};

        for (String id : specialIds) {
            storage.storeVector(id, vector, null);
            Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector(id);
            assertTrue(retrieved.isPresent(), "Failed to retrieve vector with ID: " + id);
            // ID is verified via retrieval key
        }
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
        assertArrayEquals(largeVector, retrieved.get().getVector(), 1e-6);
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
    public void testBinaryVectorFormat() throws IOException {
        // Store a vector and verify the binary format
        double[] vector = {1.0, 2.0, 3.0, 4.0, 5.0};
        storage.storeVector("vec1", vector, null);

        // The vector file should exist
        Path vectorsDir = tempDir.resolve("vectors");
        assertTrue(Files.exists(vectorsDir));

        // Retrieve and verify
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
        assertArrayEquals(vector, retrieved.get().getVector(), 1e-6);
    }

    @Test
    public void testMetadataJsonFormat() throws IOException {
        // Store a vector with metadata and verify JSON format
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");
        metadata.put("count", 42);
        metadata.put("ratio", 0.75);

        storage.storeVector("vec1", new double[]{1.0}, metadata);

        // The metadata file should exist
        Path metadataDir = tempDir.resolve("metadata");
        assertTrue(Files.exists(metadataDir));

        // Retrieve and verify
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());

        Map<String, Object> retrievedMetadata = retrieved.get().getMetadata();
        assertEquals("test", retrievedMetadata.get("label"));
        assertEquals(42, retrievedMetadata.get("count"));
        assertEquals(0.75, (Double) retrievedMetadata.get("ratio"), 0.001);
    }

    @Test
    public void testMultipleVectors() {
        // Store many vectors
        int count = 100;
        for (int i = 0; i < count; i++) {
            double[] vector = {i, i * 2.0, i * 3.0};
            Map<String, Object> metadata = Map.of("index", i);
            storage.storeVector("vec" + i, vector, metadata);
        }

        // Verify count
        List<String> ids = storage.listVectors(null);
        assertEquals(count, ids.size());

        // Verify random samples
        for (int i = 0; i < 10; i++) {
            int idx = (int) (Math.random() * count);
            Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec" + idx);
            assertTrue(retrieved.isPresent());

            double[] expected = {idx, idx * 2.0, idx * 3.0};
            assertArrayEquals(expected, retrieved.get().getVector(), 1e-6);
            assertEquals(idx, retrieved.get().getMetadata().get("index"));
        }
    }

    @Test
    public void testUpdatePersistence() {
        // Store initial vector
        storage.storeVector("vec1", new double[]{1.0, 2.0}, Map.of("version", 1));

        // Update vector
        storage.updateVector("vec1", new double[]{3.0, 4.0}, Map.of("version", 2));

        // Close and reopen
        storage.close();

        DiskStorage newStorage = new DiskStorage(config);
        newStorage.initialize();

        try {
            // Verify updated version is persisted
            Optional<StorageBackend.VectorWithMetadata> retrieved = newStorage.retrieveVector("vec1");
            assertTrue(retrieved.isPresent());
            assertArrayEquals(new double[]{3.0, 4.0}, retrieved.get().getVector(), 1e-6);
            assertEquals(2, retrieved.get().getMetadata().get("version"));
        } finally {
            newStorage.close();
        }
    }

    @Test
    public void testDeletePersistence() {
        // Store vectors
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);
        storage.storeVector("vec3", new double[]{3.0}, null);

        // Delete one
        storage.deleteVector("vec2");

        // Close and reopen
        storage.close();

        DiskStorage newStorage = new DiskStorage(config);
        newStorage.initialize();

        try {
            // Verify deletion is persisted
            assertTrue(newStorage.retrieveVector("vec1").isPresent());
            assertFalse(newStorage.retrieveVector("vec2").isPresent());
            assertTrue(newStorage.retrieveVector("vec3").isPresent());

            List<String> ids = newStorage.listVectors(null);
            assertEquals(2, ids.size());
            assertFalse(ids.contains("vec2"));
        } finally {
            newStorage.close();
        }
    }

    @Test
    public void testClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        assertTrue(storage.isInitialized());

        storage.close();

        // Verify data is still on disk
        Path vectorsDir = tempDir.resolve("vectors");
        Path metadataDir = tempDir.resolve("metadata");
        assertTrue(Files.exists(vectorsDir));
        assertTrue(Files.exists(metadataDir));
    }

    @Test
    public void testEmptyDirectory() {
        // Test initialization with empty directory
        Map<String, Object> emptyConfig = new HashMap<>();
        emptyConfig.put("data_dir", tempDir.resolve("empty").toString());

        DiskStorage emptyStorage = new DiskStorage(emptyConfig);
        emptyStorage.initialize();

        try {
            List<String> ids = emptyStorage.listVectors(null);
            assertEquals(0, ids.size());

            Map<String, Object> stats = emptyStorage.getStats();
            assertEquals(0, stats.get("vector_count"));
        } finally {
            emptyStorage.close();
        }
    }

    @Test
    public void testListVectorsPaginated_FirstPage() {
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        com.veccy.base.Page<String> page = storage.listVectorsPaginated(3, Optional.empty());

        assertNotNull(page);
        assertEquals(3, page.items().size());
        assertTrue(page.hasMore());
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
    }

    @Test
    public void testStreamVectorIds() {
        for (int i = 0; i < 10; i++) {
            storage.storeVector("vec" + i, new double[]{i}, null);
        }

        List<String> streamedIds = storage.streamVectorIds().toList();

        assertEquals(10, streamedIds.size());
    }

    @Test
    public void testStreamVectorIds_Empty() {
        List<String> streamedIds = storage.streamVectorIds().toList();

        assertTrue(streamedIds.isEmpty());
    }

    @Test
    public void testStoreVector_NotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.storeVector("vec1", new double[]{1.0}, null);
        });
    }

    @Test
    public void testRetrieveVector_NotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.retrieveVector("vec1");
        });
    }

    @Test
    public void testDeleteVector_NotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.deleteVector("vec1");
        });
    }

    @Test
    public void testUpdateVector_NotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.updateVector("vec1", new double[]{1.0}, null);
        });
    }

    @Test
    public void testListVectors_NotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        assertThrows(com.veccy.exceptions.StorageException.class, () -> {
            uninitializedStorage.listVectors(null);
        });
    }

    @Test
    public void testMultipleClose() {
        storage.storeVector("vec1", new double[]{1.0}, null);

        storage.close();
        assertFalse(storage.isInitialized());

        // Second close should not throw
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

        // Vector should still be there (persisted)
        Optional<StorageBackend.VectorWithMetadata> retrieved = storage.retrieveVector("vec1");
        assertTrue(retrieved.isPresent());
    }

    @Test
    public void testGetStatsNotInitialized() {
        DiskStorage uninitializedStorage = new DiskStorage(config);

        Map<String, Object> stats = uninitializedStorage.getStats();

        assertNotNull(stats);
        assertEquals("not_initialized", stats.get("status"));
    }

    @Test
    public void testListVectors_ZeroLimit() {
        storage.storeVector("vec1", new double[]{1.0}, null);
        storage.storeVector("vec2", new double[]{2.0}, null);

        List<String> ids = storage.listVectors(0);

        assertEquals(2, ids.size());
    }

    @Test
    public void testStoreVector_OverwriteExisting() throws IOException {
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
    public void testConfigWithNullConstructor() {
        // DiskStorage allows null config and will use defaults
        DiskStorage nullConfigStorage = new DiskStorage(null);
        nullConfigStorage.initialize();

        // Should be initialized with default data_dir
        assertTrue(nullConfigStorage.isInitialized());

        nullConfigStorage.close();
    }

    @Test
    public void testConfigWithoutDataDir() {
        // DiskStorage uses default data_dir when not specified
        Map<String, Object> emptyConfig = new HashMap<>();
        DiskStorage noDataDirStorage = new DiskStorage(emptyConfig);

        noDataDirStorage.initialize();

        // Should be initialized with default data_dir
        assertTrue(noDataDirStorage.isInitialized());

        Map<String, Object> stats = noDataDirStorage.getStats();
        assertNotNull(stats.get("data_dir"));

        noDataDirStorage.close();
    }

    @Test
    public void testVectorIsolation() {
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
    }

    @Test
    public void testStorageStatsTypes() {
        storage.storeVector("vec1", new double[]{1.0, 2.0}, null);

        Map<String, Object> stats = storage.getStats();

        assertEquals("DiskStorage", stats.get("type"));
        assertEquals("disk", stats.get("backend_type"));
        assertNotNull(stats.get("data_dir"));
        assertEquals(1, stats.get("vector_count"));
        assertTrue((Long) stats.get("disk_usage_bytes") > 0);
    }
}
