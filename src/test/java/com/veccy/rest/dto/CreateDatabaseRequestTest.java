package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CreateDatabaseRequest.
 */
class CreateDatabaseRequestTest {

    @Test
    void testDefaultConstructor() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        assertNotNull(request);
        assertNull(request.getName());
        assertEquals(0, request.getDimensions()); // Default int value
        assertNull(request.getIndexConfig());
        assertNull(request.getStorageConfig());
    }

    @Test
    void testSetAndGetName() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setName("test_db");
        assertEquals("test_db", request.getName());

        request.setName("my_vectors");
        assertEquals("my_vectors", request.getName());
    }

    @Test
    void testSetAndGetDimensions() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setDimensions(768);
        assertEquals(768, request.getDimensions());

        request.setDimensions(384);
        assertEquals(384, request.getDimensions());
    }

    @Test
    void testSetAndGetIndexConfig() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "hnsw");
        indexConfig.put("M", 16);
        indexConfig.put("efConstruction", 200);

        request.setIndexConfig(indexConfig);

        assertEquals(indexConfig, request.getIndexConfig());
        assertEquals("hnsw", request.getIndexConfig().get("type"));
        assertEquals(16, request.getIndexConfig().get("M"));
    }

    @Test
    void testSetAndGetStorageConfig() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");
        storageConfig.put("cache_size", 512);

        request.setStorageConfig(storageConfig);

        assertEquals(storageConfig, request.getStorageConfig());
        assertEquals("memory", request.getStorageConfig().get("type"));
        assertEquals(512, request.getStorageConfig().get("cache_size"));
    }

    @Test
    void testWithNullConfigs() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setIndexConfig(null);
        request.setStorageConfig(null);

        assertNull(request.getIndexConfig());
        assertNull(request.getStorageConfig());
    }

    @Test
    void testWithEmptyConfigs() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setIndexConfig(new HashMap<>());
        request.setStorageConfig(new HashMap<>());

        assertNotNull(request.getIndexConfig());
        assertNotNull(request.getStorageConfig());
        assertTrue(request.getIndexConfig().isEmpty());
        assertTrue(request.getStorageConfig().isEmpty());
    }

    @Test
    void testCompleteRequest() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setName("embeddings_db");
        request.setDimensions(768);

        Map<String, Object> indexConfig = Map.of(
            "type", "hnsw",
            "M", 16,
            "efConstruction", 200,
            "metric", "cosine"
        );
        request.setIndexConfig(indexConfig);

        Map<String, Object> storageConfig = Map.of(
            "type", "hybrid",
            "path", "/data/vectors",
            "cache_size_mb", 512
        );
        request.setStorageConfig(storageConfig);

        assertEquals("embeddings_db", request.getName());
        assertEquals(768, request.getDimensions());
        assertEquals(4, request.getIndexConfig().size());
        assertEquals(3, request.getStorageConfig().size());
    }

    @Test
    void testDifferentIndexTypes() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        // HNSW
        request.setIndexConfig(Map.of("type", "hnsw"));
        assertEquals("hnsw", request.getIndexConfig().get("type"));

        // Flat
        request.setIndexConfig(Map.of("type", "flat"));
        assertEquals("flat", request.getIndexConfig().get("type"));

        // IVF
        request.setIndexConfig(Map.of("type", "ivf"));
        assertEquals("ivf", request.getIndexConfig().get("type"));

        // LSH
        request.setIndexConfig(Map.of("type", "lsh"));
        assertEquals("lsh", request.getIndexConfig().get("type"));

        // Annoy
        request.setIndexConfig(Map.of("type", "annoy"));
        assertEquals("annoy", request.getIndexConfig().get("type"));
    }

    @Test
    void testDifferentStorageTypes() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        // Memory
        request.setStorageConfig(Map.of("type", "memory"));
        assertEquals("memory", request.getStorageConfig().get("type"));

        // Disk
        request.setStorageConfig(Map.of("type", "disk", "path", "/data"));
        assertEquals("disk", request.getStorageConfig().get("type"));

        // Hybrid
        request.setStorageConfig(Map.of("type", "hybrid", "path", "/data"));
        assertEquals("hybrid", request.getStorageConfig().get("type"));
    }

    @Test
    void testWithVariousDimensions() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        // Small dimension
        request.setDimensions(128);
        assertEquals(128, request.getDimensions());

        // Medium dimension
        request.setDimensions(384);
        assertEquals(384, request.getDimensions());

        // Large dimension
        request.setDimensions(768);
        assertEquals(768, request.getDimensions());

        // Very large dimension
        request.setDimensions(1536);
        assertEquals(1536, request.getDimensions());
    }

    @Test
    void testWithNegativeDimensions() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        request.setDimensions(-100);

        assertEquals(-100, request.getDimensions()); // No validation in DTO
    }

    @Test
    void testWithZeroDimensions() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        request.setDimensions(0);

        assertEquals(0, request.getDimensions());
    }

    @Test
    void testNameWithSpecialCharacters() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setName("test_db-123");
        assertEquals("test_db-123", request.getName());

        request.setName("my.vectors.db");
        assertEquals("my.vectors.db", request.getName());
    }

    @Test
    void testEmptyName() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        request.setName("");

        assertEquals("", request.getName());
    }

    @Test
    void testNullName() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        request.setName(null);

        assertNull(request.getName());
    }

    @Test
    void testLongName() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        String longName = "a".repeat(1000);

        request.setName(longName);

        assertEquals(1000, request.getName().length());
    }

    @Test
    void testIndexConfigModification() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hnsw");

        request.setIndexConfig(config);

        // Modify original map
        config.put("M", 32);

        // Changes are reflected (no defensive copy)
        assertEquals(32, request.getIndexConfig().get("M"));
    }

    @Test
    void testStorageConfigModification() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();
        Map<String, Object> config = new HashMap<>();
        config.put("type", "disk");

        request.setStorageConfig(config);

        // Modify original map
        config.put("path", "/new/path");

        // Changes are reflected (no defensive copy)
        assertEquals("/new/path", request.getStorageConfig().get("path"));
    }

    @Test
    void testMultipleUpdates() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        request.setName("db1");
        request.setDimensions(128);
        assertEquals("db1", request.getName());

        request.setName("db2");
        request.setDimensions(256);
        assertEquals("db2", request.getName());
        assertEquals(256, request.getDimensions());
    }

    @Test
    void testTypicalUsageScenarios() {
        // Development setup
        CreateDatabaseRequest devRequest = new CreateDatabaseRequest();
        devRequest.setName("dev_db");
        devRequest.setDimensions(384);
        devRequest.setIndexConfig(Map.of("type", "flat"));
        devRequest.setStorageConfig(Map.of("type", "memory"));

        assertEquals("dev_db", devRequest.getName());
        assertEquals("flat", devRequest.getIndexConfig().get("type"));

        // Production setup
        CreateDatabaseRequest prodRequest = new CreateDatabaseRequest();
        prodRequest.setName("prod_embeddings");
        prodRequest.setDimensions(768);
        prodRequest.setIndexConfig(Map.of(
            "type", "hnsw",
            "M", 16,
            "efConstruction", 200
        ));
        prodRequest.setStorageConfig(Map.of(
            "type", "hybrid",
            "path", "/var/data/vectors"
        ));

        assertEquals("prod_embeddings", prodRequest.getName());
        assertEquals("hnsw", prodRequest.getIndexConfig().get("type"));
        assertEquals("hybrid", prodRequest.getStorageConfig().get("type"));
    }

    @Test
    void testNestedConfigValues() {
        CreateDatabaseRequest request = new CreateDatabaseRequest();

        Map<String, Object> nestedConfig = new HashMap<>();
        nestedConfig.put("type", "hnsw");
        nestedConfig.put("advanced", Map.of(
            "parallel_build", true,
            "max_threads", 8
        ));

        request.setIndexConfig(nestedConfig);

        @SuppressWarnings("unchecked")
        Map<String, Object> advanced = (Map<String, Object>) request.getIndexConfig().get("advanced");
        assertTrue((Boolean) advanced.get("parallel_build"));
        assertEquals(8, advanced.get("max_threads"));
    }
}
