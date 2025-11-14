package com.veccy.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Pre-configured index templates for different use cases.
 */
public class IndexConfig {

    /**
     * Flat index for small datasets (&lt;10K vectors).
     * Provides exact search with 100% accuracy.
     *
     * @return configuration map for flat index
     */
    public static Map<String, Object> forSmallDataset() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "flat");
        config.put("metric", "cosine");
        return config;
    }

    /**
     * HNSW index for medium datasets (10K-1M vectors).
     * Provides good balance between speed and accuracy.
     */
    public static Map<String, Object> forMediumDataset() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hnsw");
        config.put("metric", "cosine");
        config.put("m", 16);
        config.put("ef_construction", 200);
        config.put("ef_search", 50);
        return config;
    }

    /**
     * Fast HNSW configuration with lower accuracy.
     * Optimized for speed over recall.
     */
    public static Map<String, Object> forFastSearch() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hnsw");
        config.put("metric", "cosine");
        config.put("m", 8);
        config.put("ef_construction", 100);
        config.put("ef_search", 20);
        return config;
    }

    /**
     * High-accuracy HNSW configuration.
     * Better recall at the cost of speed.
     */
    public static Map<String, Object> forHighAccuracy() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hnsw");
        config.put("metric", "cosine");
        config.put("m", 32);
        config.put("ef_construction", 400);
        config.put("ef_search", 100);
        return config;
    }

    /**
     * Balanced configuration for general purpose use.
     */
    public static Map<String, Object> balanced() {
        Map<String, Object> config = new HashMap<>();
        config.put("type", "hnsw");
        config.put("metric", "cosine");
        config.put("m", 16);
        config.put("ef_construction", 200);
        config.put("ef_search", 50);
        return config;
    }

    /**
     * Custom index configuration with specified parameters.
     */
    public static Map<String, Object> custom(String type, String metric, Map<String, Object> params) {
        Map<String, Object> config = new HashMap<>();
        config.put("type", type);
        config.put("metric", metric);
        if (params != null) {
            config.putAll(params);
        }
        return config;
    }
}
