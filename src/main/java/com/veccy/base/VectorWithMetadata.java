package com.veccy.base;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a vector with its associated metadata.
 */
public record VectorWithMetadata(String id, double[] vector, Map<String, Object> metadata) {

    /**
     * Compact constructor with validation.
     */
    public VectorWithMetadata {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(vector, "Vector cannot be null");
    }

    /**
     * Gets the vector ID.
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the vector.
     * @return the vector
     */
    public double[] getVector() {
        return vector;
    }

    /**
     * Gets the metadata.
     * @return the metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Custom equals that only compares id (not vector or metadata).
     * This is intentional - vectors are identified by their id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorWithMetadata that = (VectorWithMetadata) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Custom hashCode that only uses id (consistent with equals).
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Custom toString that shows vector dimension instead of full array.
     */
    @Override
    public String toString() {
        return "VectorWithMetadata{" +
                "id='" + id + '\'' +
                ", vectorDim=" + vector.length +
                ", metadata=" + metadata +
                '}';
    }
}
