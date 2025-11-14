package com.veccy.rest.dto;

import java.util.Map;

/**
 * Request DTO for creating a new database.
 */
public class CreateDatabaseRequest {
    private String name;
    private int dimensions;
    private Map<String, Object> indexConfig;
    private Map<String, Object> storageConfig;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public Map<String, Object> getIndexConfig() {
        return indexConfig;
    }

    public void setIndexConfig(Map<String, Object> indexConfig) {
        this.indexConfig = indexConfig;
    }

    public Map<String, Object> getStorageConfig() {
        return storageConfig;
    }

    public void setStorageConfig(Map<String, Object> storageConfig) {
        this.storageConfig = storageConfig;
    }
}
