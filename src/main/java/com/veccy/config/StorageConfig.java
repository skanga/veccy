package com.veccy.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-configured storage backend templates.
 */
public class StorageConfig {

    /**
     * In-memory storage for fast access.
     * No persistence across restarts.
     */
    public static Map<String, Object> memory() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "memory");
        return config;
    }

    /**
     * Disk-based storage for persistence.
     */
    public static Map<String, Object> disk(String dataDir) {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "disk");
        config.put("data_dir", dataDir);
        return config;
    }

    /**
     * Disk storage with default directory.
     */
    public static Map<String, Object> disk() {
        return disk("./veccy_data");
    }

    /**
     * Hybrid storage with memory cache and disk persistence.
     * (Note: Implementation pending)
     */
    public static Map<String, Object> hybrid(String dataDir, int cacheSize) {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hybrid");
        config.put("data_dir", dataDir);
        config.put("cache_size", cacheSize);
        config.put("memory_limit", cacheSize * 1024 * 1024); // Convert to bytes
        return config;
    }

    /**
     * Custom storage configuration.
     */
    public static Map<String, Object> custom(String type, Map<String, Object> params) {
        Map<String, Object> config = new HashMap<>();
        config.put("type", type);
        if (params != null) {
            config.putAll(params);
        }
        return config;
    }
}
