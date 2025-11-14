package com.veccy.rest.dto;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for exporting vectors.
 */
public class ExportResponse {
    private String database;
    private int count;
    private List<VectorData> vectors;
    private Map<String, Object> stats;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<VectorData> getVectors() {
        return vectors;
    }

    public void setVectors(List<VectorData> vectors) {
        this.vectors = vectors;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }

    /**
     * Represents a single exported vector with ID and metadata.
     */
    public static class VectorData {
        private String id;
        private double[] vector;
        private Map<String, Object> metadata;

        public VectorData() {
        }

        public VectorData(String id, double[] vector, Map<String, Object> metadata) {
            this.id = id;
            this.vector = vector;
            this.metadata = metadata;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public double[] getVector() {
            return vector;
        }

        public void setVector(double[] vector) {
            this.vector = vector;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
