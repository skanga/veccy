package com.veccy.rest.dto;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for inserting vectors.
 */
public class InsertVectorsRequest {
    private List<double[]> vectors;
    private List<Map<String, Object>> metadata;

    public List<double[]> getVectors() {
        return vectors;
    }

    public void setVectors(List<double[]> vectors) {
        this.vectors = vectors;
    }

    public List<Map<String, Object>> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<Map<String, Object>> metadata) {
        this.metadata = metadata;
    }
}
