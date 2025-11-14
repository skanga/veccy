package com.veccy.rest.dto;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for importing vectors.
 */
public class ImportRequest {
    private List<VectorData> vectors;
    private String format; // "json" or "csv" (for future use)

    public List<VectorData> getVectors() {
        return vectors;
    }

    public void setVectors(List<VectorData> vectors) {
        this.vectors = vectors;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Represents a single vector with optional ID and metadata.
     */
    public static class VectorData {
        private String id;
        private double[] vector;
        private Map<String, Object> metadata;

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
