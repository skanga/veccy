package com.veccy.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the configuration template classes IndexConfig and StorageConfig.
 */
public class ConfigTemplatesTest {

    @Test
    void testIndexConfig_forSmallDataset() {
        Map<String, Object> config = IndexConfig.forSmallDataset();
        assertEquals("flat", config.get("type"));
        assertEquals("cosine", config.get("metric"));
    }

    @Test
    void testIndexConfig_forMediumDataset() {
        Map<String, Object> config = IndexConfig.forMediumDataset();
        assertEquals("hnsw", config.get("type"));
        assertEquals("cosine", config.get("metric"));
        assertEquals(16, config.get("m"));
        assertEquals(200, config.get("ef_construction"));
        assertEquals(50, config.get("ef_search"));
    }

    @Test
    void testIndexConfig_forFastSearch() {
        Map<String, Object> config = IndexConfig.forFastSearch();
        assertEquals("hnsw", config.get("type"));
        assertEquals("cosine", config.get("metric"));
        assertEquals(8, config.get("m"));
        assertEquals(100, config.get("ef_construction"));
        assertEquals(20, config.get("ef_search"));
    }

    @Test
    void testIndexConfig_forHighAccuracy() {
        Map<String, Object> config = IndexConfig.forHighAccuracy();
        assertEquals("hnsw", config.get("type"));
        assertEquals("cosine", config.get("metric"));
        assertEquals(32, config.get("m"));
        assertEquals(400, config.get("ef_construction"));
        assertEquals(100, config.get("ef_search"));
    }

    @Test
    void testIndexConfig_balanced() {
        Map<String, Object> config = IndexConfig.balanced();
        assertEquals("hnsw", config.get("type"));
        assertEquals("cosine", config.get("metric"));
        assertEquals(16, config.get("m"));
        assertEquals(200, config.get("ef_construction"));
        assertEquals(50, config.get("ef_search"));
    }

    @Test
    void testIndexConfig_custom() {
        Map<String, Object> params = Map.of("m", 64, "ef_construction", 500);
        Map<String, Object> config = IndexConfig.custom("hnsw", "euclidean", params);
        assertEquals("hnsw", config.get("type"));
        assertEquals("euclidean", config.get("metric"));
        assertEquals(64, config.get("m"));
        assertEquals(500, config.get("ef_construction"));
    }

    @Test
    void testStorageConfig_memory() {
        Map<String, Object> config = StorageConfig.memory();
        assertEquals("memory", config.get("type"));
    }

    @Test
    void testStorageConfig_diskWithArg() {
        Map<String, Object> config = StorageConfig.disk("/test/path");
        assertEquals("disk", config.get("type"));
        assertEquals("/test/path", config.get("data_dir"));
    }

    @Test
    void testStorageConfig_diskWithoutArg() {
        Map<String, Object> config = StorageConfig.disk();
        assertEquals("disk", config.get("type"));
        assertEquals("./veccy_data", config.get("data_dir"));
    }

    @Test
    void testStorageConfig_hybrid() {
        Map<String, Object> config = StorageConfig.hybrid("/test/path", 2048);
        assertEquals("hybrid", config.get("type"));
        assertEquals("/test/path", config.get("data_dir"));
        assertEquals(2048, config.get("cache_size"));
    }

    @Test
    void testStorageConfig_custom() {
        Map<String, Object> params = Map.of("data_dir", "/custom/path", "cache_size", 4096);
        Map<String, Object> config = StorageConfig.custom("hybrid", params);
        assertEquals("hybrid", config.get("type"));
        assertEquals("/custom/path", config.get("data_dir"));
        assertEquals(4096, config.get("cache_size"));
    }
}
