package com.veccy.client;

import com.veccy.base.Index;
import com.veccy.base.Page;
import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.exceptions.VeccyException;
import com.veccy.persistence.PersistenceManager;
import com.veccy.quantization.Quantizer;
import com.veccy.storage.StorageBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * High-level client for interacting with the Veccy vector database.
 * <p>
 * This client provides a convenient interface for common vector database
 * operations while abstracting away the complexity of underlying components.
 */
public class VectorDBClient implements VectorDB {

    private static final Logger logger = LoggerFactory.getLogger(VectorDBClient.class);

    private final StorageBackend storageBackend;
    private final Index index;
    private final Quantizer quantizer;
    private final PersistenceManager persistenceManager;
    private final Map<String, Object> config;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private boolean initialized;

    /**
     * Create a new VectorDBClient with required components.
     *
     * @param storageBackend the storage backend implementation
     * @param index the index implementation for similarity search
     */
    public VectorDBClient(StorageBackend storageBackend, Index index) {
        this(storageBackend, index, null, null, null);
    }

    /**
     * Create a new VectorDBClient with all components.
     *
     * @param storageBackend the storage backend implementation
     * @param index the index implementation for similarity search
     * @param quantizer optional quantizer for vector compression
     * @param persistenceManager optional persistence manager
     * @param config configuration dictionary
     */
    public VectorDBClient(StorageBackend storageBackend, Index index,
                          Quantizer quantizer, PersistenceManager persistenceManager,
                          Map<String, Object> config) {
        this.storageBackend = storageBackend;
        this.index = index;
        this.quantizer = quantizer;
        this.persistenceManager = persistenceManager;
        this.config = config != null ? config : new HashMap<>();
        this.initialized = false;
    }

    @Override
    public void initialize() {
        try {
            // Initialize storage backend
            if (!storageBackend.isInitialized()) {
                storageBackend.initialize();
            }

            // Initialize index with storage backend
            if (!index.isInitialized()) {
                index.initialize(storageBackend);
            }

            // Initialize optional components
            if (quantizer != null && !quantizer.isInitialized()) {
                quantizer.initialize();
            }

            if (persistenceManager != null && !persistenceManager.isInitialized()) {
                persistenceManager.initialize();
            }

            initialized = true;
            logger.info("VectorDBClient initialized successfully");
        } catch (Exception e) {
            throw new VeccyException("Failed to initialize client: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> insert(double[][] vectors, List<Map<String, Object>> metadata) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.insert(vectors, metadata);
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int k) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.search(queryVector, k);
    }

    @Override
    public boolean delete(List<String> ids) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.delete(ids);
    }

    @Override
    public boolean update(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.update(id, vector, metadata);
    }

    /**
     * Batch update multiple vectors by their IDs.
     * <p>
     * This is more efficient than calling update() multiple times as it can
     * optimize locking and storage operations.
     *
     * @param ids list of vector IDs to update
     * @param vectors list of new vector data (can contain nulls to only update metadata)
     * @param metadata list of optional new metadata (can be null or contain nulls)
     * @return list of booleans indicating success for each update
     */
    public List<Boolean> batchUpdate(List<String> ids, List<double[]> vectors,
                                     List<Map<String, Object>> metadata) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.batchUpdate(ids, vectors, metadata);
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
    public List<List<SearchResult>> batchSearch(double[][] queryVectors, int k) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return index.batchSearch(queryVectors, k);
    }

    /**
     * List all vector IDs with optional limit.
     *
     * @param limit optional maximum number of IDs to return (null for no limit)
     * @return list of vector IDs
     */
    public List<String> listVectorIds(Integer limit) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return storageBackend.listVectors(limit);
    }

    /**
     * List vector IDs with cursor-based pagination.
     * <p>
     * This method provides efficient pagination over large result sets without
     * loading all IDs into memory at once.
     *
     * @param pageSize maximum number of IDs per page
     * @param cursor optional cursor from previous page (empty for first page)
     * @return page containing IDs and next cursor
     */
    public Page<String> listVectorIdsPaginated(int pageSize, Optional<String> cursor) {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return storageBackend.listVectorsPaginated(pageSize, cursor);
    }

    /**
     * Stream all vector IDs efficiently.
     * <p>
     * This method provides a memory-efficient way to iterate over all vector IDs
     * without loading them all into memory at once.
     * <p>
     * Note: The returned stream should be closed after use.
     *
     * @return stream of vector IDs
     */
    public Stream<String> streamVectorIds() {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        return storageBackend.streamVectorIds();
    }

    @Override
    public Map<String, Object> getStats() {
        if (!initialized) {
            throw new VeccyException("Client not initialized. Call initialize() first.");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("storage", storageBackend.getStats());
        stats.put("index", index.getStats());

        if (quantizer != null) {
            stats.put("quantization", quantizer.getStats());
        }

        if (persistenceManager != null) {
            stats.put("persistence", persistenceManager.getStats());
        }

        return stats;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    /**
     * Close the client and release all resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe: Multiple concurrent calls to close()
     * are safe.
     */
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (persistenceManager != null) {
                    try {
                        persistenceManager.close();
                    } catch (Exception e) {
                        logger.error("Error closing persistence manager: {}", e.getMessage(), e);
                    }
                }

                if (quantizer != null) {
                    try {
                        quantizer.close();
                    } catch (Exception e) {
                        logger.error("Error closing quantizer: {}", e.getMessage(), e);
                    }
                }

                try {
                    index.close();
                } catch (Exception e) {
                    logger.error("Error closing index: {}", e.getMessage(), e);
                }

                try {
                    storageBackend.close();
                } catch (Exception e) {
                    logger.error("Error closing storage backend: {}", e.getMessage(), e);
                }

                initialized = false;
                logger.info("VectorDBClient closed successfully");
            } catch (Exception e) {
                logger.error("Unexpected error during close: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("VectorDBClient already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Get the storage backend used by this client.
     */
    public StorageBackend getStorageBackend() {
        return storageBackend;
    }

    /**
     * Get the index used by this client.
     */
    public Index getIndex() {
        return index;
    }

    /**
     * Get the quantizer used by this client (may be null).
     */
    public Quantizer getQuantizer() {
        return quantizer;
    }

    /**
     * Get the persistence manager used by this client (may be null).
     */
    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    /**
     * Get the configuration map.
     */
    public Map<String, Object> getConfig() {
        return new HashMap<>(config);
    }
}
