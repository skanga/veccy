package com.veccy.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base interface for persistence managers.
 *
 * Persistence managers handle saving and loading of vector database
 * state, including indices, quantizers, and configuration data.
 */
public interface PersistenceManager extends AutoCloseable {

    /**
     * Initialize the persistence manager.
     */
    void initialize();

    /**
     * Save database state to a file.
     *
     * @param state the database state dictionary
     * @param path the file path to save to
     * @return true if successful
     */
    boolean saveState(Map<String, Object> state, String path);

    /**
     * Load database state from a file.
     *
     * @param path the file path to load from
     * @return optional containing the database state, or empty if failed
     */
    Optional<Map<String, Object>> loadState(String path);

    /**
     * Save vectors to a file.
     *
     * @param vectors the array of vectors
     * @param ids the list of vector IDs
     * @param path the file path to save to
     * @return true if successful
     */
    boolean saveVectors(double[][] vectors, List<String> ids, String path);

    /**
     * Load vectors from a file.
     *
     * @param path the file path to load from
     * @return optional containing the vectors and IDs, or empty if failed
     */
    Optional<VectorsWithIds> loadVectors(String path);

    /**
     * Save index data to a file.
     *
     * @param indexData the index data dictionary
     * @param path the file path to save to
     * @return true if successful
     */
    boolean saveIndex(Map<String, Object> indexData, String path);

    /**
     * Load index data from a file.
     *
     * @param path the file path to load from
     * @return optional containing the index data, or empty if failed
     */
    Optional<Map<String, Object>> loadIndex(String path);

    /**
     * Get persistence manager statistics.
     *
     * @return map of statistics
     */
    Map<String, Object> getStats();

    /**
     * Check if the persistence manager has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Close the persistence manager and clean up resources.
     */
    @Override
    void close();

    /**
     * Represents vectors with their associated IDs.
     */
    class VectorsWithIds {
        private final double[][] vectors;
        private final List<String> ids;

        public VectorsWithIds(double[][] vectors, List<String> ids) {
            this.vectors = vectors;
            this.ids = ids;
        }

        public double[][] getVectors() {
            return vectors;
        }

        public List<String> getIds() {
            return ids;
        }
    }
}
