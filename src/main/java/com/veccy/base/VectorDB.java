package com.veccy.base;

import java.util.List;
import java.util.Map;

/**
 * Abstract base interface for vector database implementations.
 * <p>
 * This interface defines the core operations that all vector database
 * implementations must support, including insertion, search, deletion,
 * and updates of vector data.
 */
public interface VectorDB extends AutoCloseable {

    /**
     * Initialize the database and any required resources.
     * This method must be called before using the database.
     */
    void initialize();

    /**
     * Insert vectors into the database.
     *
     * @param vectors array of vectors to insert, each vector is a double array
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
     * Get database statistics.
     *
     * @return map of statistics including vector count, dimensions, etc.
     */
    Map<String, Object> getStats();

    /**
     * Check if the database has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Close the database and clean up resources.
     * After calling this method, the database should not be used.
     */
    @Override
    void close();
}
