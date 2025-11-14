package com.veccy.rest.dto;

import java.util.List;

/**
 * Request DTO for batch search operations.
 */
public class BatchSearchRequest {
    private List<double[]> queryVectors;
    private int k = 10;

    public List<double[]> getQueryVectors() {
        return queryVectors;
    }

    public void setQueryVectors(List<double[]> queryVectors) {
        this.queryVectors = queryVectors;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }
}
