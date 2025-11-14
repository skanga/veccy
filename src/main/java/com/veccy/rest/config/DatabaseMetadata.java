package com.veccy.rest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Metadata about a vector database including configuration and creation info.
 */
public class DatabaseMetadata {
    private final String name;
    private final int dimensions;
    private final String indexType;
    private final String storageType;
    private final String distanceMetric;
    private final long createdAt;
    private final Map<String, Object> indexConfig;
    private final Map<String, Object> storageConfig;

    private DatabaseMetadata(Builder builder) {
        this.name = builder.name;
        this.dimensions = builder.dimensions;
        this.indexType = builder.indexType;
        this.storageType = builder.storageType;
        this.distanceMetric = builder.distanceMetric;
        this.createdAt = builder.createdAt;
        this.indexConfig = new HashMap<>(builder.indexConfig);
        this.storageConfig = new HashMap<>(builder.storageConfig);
    }

    public String getName() {
        return name;
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getIndexType() {
        return indexType;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getDistanceMetric() {
        return distanceMetric;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Map<String, Object> getIndexConfig() {
        return new HashMap<>(indexConfig);
    }

    public Map<String, Object> getStorageConfig() {
        return new HashMap<>(storageConfig);
    }

    /**
     * Convert metadata to a map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("dimensions", dimensions);
        map.put("index_type", indexType);
        map.put("storage_type", storageType);
        map.put("distance_metric", distanceMetric);
        map.put("created_at", createdAt);
        map.put("index_config", indexConfig);
        map.put("storage_config", storageConfig);
        return map;
    }

    /**
     * Builder for DatabaseMetadata.
     */
    public static class Builder {
        private String name;
        private int dimensions;
        private String indexType = "hnsw";
        private String storageType = "memory";
        private String distanceMetric = "cosine";
        private long createdAt = System.currentTimeMillis();
        private Map<String, Object> indexConfig = new HashMap<>();
        private Map<String, Object> storageConfig = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public Builder indexType(String indexType) {
            this.indexType = indexType;
            return this;
        }

        public Builder storageType(String storageType) {
            this.storageType = storageType;
            return this;
        }

        public Builder distanceMetric(String distanceMetric) {
            this.distanceMetric = distanceMetric;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder indexConfig(Map<String, Object> indexConfig) {
            this.indexConfig = indexConfig != null ? indexConfig : new HashMap<>();
            return this;
        }

        public Builder storageConfig(Map<String, Object> storageConfig) {
            this.storageConfig = storageConfig != null ? storageConfig : new HashMap<>();
            return this;
        }

        public DatabaseMetadata build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Database name is required");
            }
            if (dimensions <= 0) {
                throw new IllegalArgumentException("Dimensions must be positive");
            }
            return new DatabaseMetadata(this);
        }
    }
}
