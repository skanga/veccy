package com.veccy.storage;

import com.veccy.base.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Abstract base interface for storage backends.
 *
 * Storage backends are responsible for persisting and retrieving
 * vector data and associated metadata.
 */
public interface StorageBackend extends AutoCloseable {

    /**
     * Initialize the storage backend.
     */
    void initialize();

    /**
     * Store a vector with its ID and optional metadata.
     *
     * @param id unique identifier for the vector
     * @param vector the vector data to store
     * @param metadata optional metadata dictionary (can be null)
     * @return true if successful
     */
    boolean storeVector(String id, double[] vector, Map<String, Object> metadata);

    /**
     * Retrieve a vector by its ID.
     *
     * @param id the vector identifier
     * @return optional containing the vector and metadata, or empty if not found
     */
    Optional<VectorWithMetadata> retrieveVector(String id);

    /**
     * Delete a vector by its ID.
     *
     * @param id the vector identifier
     * @return true if successful
     */
    boolean deleteVector(String id);

    /**
     * Update a vector by its ID.
     *
     * @param id the vector identifier
     * @param vector the new vector data
     * @param metadata optional new metadata (can be null)
     * @return true if successful
     */
    boolean updateVector(String id, double[] vector, Map<String, Object> metadata);

    /**
     * List all vector IDs.
     *
     * @param limit optional limit on number of IDs to return (null for no limit)
     * @return list of vector IDs
     */
    List<String> listVectors(Integer limit);

    /**
     * List vector IDs with cursor-based pagination.
     * <p>
     * Cursor-based pagination is more efficient than offset-based pagination for large datasets
     * and provides consistent results even when data is being modified.
     *
     * @param pageSize maximum number of IDs to return per page
     * @param cursor optional cursor from previous page (empty for first page)
     * @return page containing IDs and cursor for next page
     */
    default Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
        // Default implementation: load all and simulate pagination
        // Subclasses should override with more efficient implementations
        List<String> allIds = listVectors(null);

        int startIndex = 0;
        if (cursor.isPresent()) {
            String cursorId = cursor.get();
            int cursorIndex = allIds.indexOf(cursorId);
            if (cursorIndex >= 0) {
                startIndex = cursorIndex + 1;
            }
        }

        if (startIndex >= allIds.size()) {
            return Page.empty();
        }

        int endIndex = Math.min(startIndex + pageSize, allIds.size());
        List<String> pageItems = allIds.subList(startIndex, endIndex);

        if (endIndex < allIds.size()) {
            String nextCursor = allIds.get(endIndex - 1);
            return Page.of(pageItems, nextCursor);
        } else {
            return Page.last(pageItems);
        }
    }

    /**
     * Stream all vector IDs.
     * <p>
     * This provides a memory-efficient way to iterate over all vector IDs
     * without loading them all into memory at once.
     * <p>
     * Note: The returned stream should be closed after use to release resources.
     *
     * @return stream of vector IDs
     */
    default Stream<String> streamVectorIds() {
        // Default implementation: stream from list
        // Subclasses can override with more efficient streaming implementations
        return listVectors(null).stream();
    }

    /**
     * Get storage statistics.
     *
     * @return map of statistics
     */
    Map<String, Object> getStats();

    /**
     * Check if the storage backend has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Close the storage backend and clean up resources.
     */
    @Override
    void close();

    /**
     * Represents a vector with its associated metadata.
     */
    record VectorWithMetadata(double[] vector, Map<String, Object> metadata) {

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
    }
}
