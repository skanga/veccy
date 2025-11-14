package com.veccy.rest.dto;

/**
 * Request DTO for vector search.
 * Note: The distance metric is configured at database creation time,
 * not per-search request.
 */
public class SearchRequest {
    private double[] queryVector;
    private int k = 10;

    public double[] getQueryVector() {
        return queryVector;
    }

    public void setQueryVector(double[] queryVector) {
        this.queryVector = queryVector;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }
}
