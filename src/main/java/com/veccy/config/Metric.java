package com.veccy.config;

/**
 * Supported distance metrics for vector similarity.
 */
public enum Metric {
    /**
     * Cosine similarity (normalized dot product).
     * Range: 0 (identical) to 2 (opposite).
     * Best for: Text embeddings, normalized vectors.
     */
    COSINE("cosine"),

    /**
     * Euclidean distance (L2 norm).
     * Range: 0 (identical) to infinity.
     * Best for: General-purpose, physical distances.
     */
    EUCLIDEAN("euclidean"),

    /**
     * Dot product (inner product, negated for distance).
     * Range: -infinity to infinity.
     * Best for: Normalized vectors, recommendation systems.
     */
    DOT_PRODUCT("dot_product"),

    /**
     * Manhattan distance (L1 norm).
     * Range: 0 (identical) to infinity.
     * Best for: Grid-based distances, sparse vectors.
     */
    MANHATTAN("manhattan");

    private final String value;

    Metric(String value) {
        this.value = value;
    }

    /**
     * Get the string representation of this metric.
     */
    public String getValue() {
        return value;
    }

    /**
     * Parse a metric from string value.
     *
     * @param value the string value
     * @return the corresponding Metric enum
     * @throws IllegalArgumentException if value is not recognized
     */
    public static Metric fromString(String value) {
        for (Metric metric : values()) {
            if (metric.value.equalsIgnoreCase(value)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unknown metric: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
