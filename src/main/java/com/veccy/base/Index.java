package com.veccy.base;

import com.veccy.storage.StorageBackend;

import java.util.List;
import java.util.Map;

/**
 * Abstract base interface for vector indices.
 * <p>
 * Indices are responsible for efficient similarity search over vector data.
 * They work with storage backends to persist and retrieve vectors.
 */
public interface Index extends AutoCloseable {

    /**
     * Initialize the index with a storage backend.
     *
     * @param storageBackend the storage backend to use for persistence
     */
    void initialize(StorageBackend storageBackend);

    /**
     * Insert vectors into the index.
     *
     * @param vectors array of vectors to insert
     * @param metadata optional metadata for each vector (can be null)
     * @return list of IDs for the inserted vectors
     */
    List<String> insert(double[][] vectors, List<Map<String, Object>> metadata);

    /**
     * Search for similar vectors.
     *
     * @param queryVector the query vector
     * @param k the number of results to return
     * @return list of search results with distances and metadata
     */
    List<SearchResult> search(double[] queryVector, int k);

    /**
     * Delete vectors by their IDs.
     *
     * @param ids list of vector IDs to delete
     * @return true if all deletions were successful
     */
    boolean delete(List<String> ids);

    /**
     * Update a vector by its ID.
     *
     * @param id the vector ID to update
     * @param vector the new vector data
     * @param metadata optional new metadata (can be null)
     * @return true if the update was successful
     */
    boolean update(String id, double[] vector, Map<String, Object> metadata);

    /**
     * Batch update vectors by their IDs.
     * <p>
     * This is more efficient than calling update() multiple times as it can
     * optimize locking and storage operations.
     *
     * @param ids list of vector IDs to update
     * @param vectors list of new vector data (can contain nulls to only update metadata)
     * @param metadata list of optional new metadata (can be null or contain nulls)
     * @return list of booleans indicating success for each update
     */
    default List<Boolean> batchUpdate(List<String> ids, List<double[]> vectors,
                                      List<Map<String, Object>> metadata) {
        List<Boolean> results = new java.util.ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            double[] vector = (vectors != null && i < vectors.size()) ? vectors.get(i) : null;
            Map<String, Object> meta = (metadata != null && i < metadata.size()) ? metadata.get(i) : null;
            results.add(update(ids.get(i), vector, meta));
        }
        return results;
    }

    /**
     * Batch search for similar vectors.
     * <p>
     * This is more efficient than calling search() multiple times as it can
     * share computation and optimize locking.
     *
     * @param queryVectors array of query vectors
     * @param k the number of results to return per query
     * @return list of search results for each query vector
     */
    default List<List<SearchResult>> batchSearch(double[][] queryVectors, int k) {
        List<List<SearchResult>> results = new java.util.ArrayList<>();
        for (double[] queryVector : queryVectors) {
            results.add(search(queryVector, k));
        }
        return results;
    }

    /**
     * Get index statistics.
     *
     * @return map of statistics
     */
    Map<String, Object> getStats();

    /**
     * Check if the index has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Close the index and clean up resources.
     */
    @Override
    void close();
}
