package com.veccy.rest.dto;

import java.util.Map;

/**
 * Request DTO for updating a vector.
 */
public class UpdateVectorRequest {
    private double[] vector;
    private Map<String, Object> metadata;

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
