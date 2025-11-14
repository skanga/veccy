package com.veccy.base;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a search result containing vector ID, distance, and metadata.
 *
 * @param id the unique identifier of the vector
 * @param distance the distance/similarity score
 * @param metadata the associated metadata map
 */
public record SearchResult(String id, double distance, Map<String, Object> metadata) {

    /**
     * Compact constructor with validation.
     */
    public SearchResult {
        Objects.requireNonNull(id, "ID cannot be null");
    }

    /**
     * Gets the vector ID.
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the distance.
     * @return the distance
     */
    public double getDistance() {
        return distance;
    }

    /**
     * Gets the metadata.
     * @return the metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
