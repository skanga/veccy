package com.veccy.indices;

import com.veccy.base.AbstractIndex;
import com.veccy.base.SearchResult;
import com.veccy.config.FlatConfig;
import com.veccy.exceptions.IndexException;
import com.veccy.storage.StorageBackend;
import com.veccy.utils.SimilarityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Flat index implementation using brute force search.
 * <p>
 * This index performs exhaustive search over all vectors, making it
 * accurate but potentially slow for large datasets. It's suitable for
 * small to medium-sized collections (<10,000 vectors).
 * <p>
 * Example usage:
 * <pre>{@code
 * FlatConfig config = FlatConfig.builder()
 *     .metric(Metric.COSINE)
 *     .build();
 * FlatIndex index = new FlatIndex(config);
 * }</pre>
 */
public class FlatIndex extends AbstractIndex {

    private static final Logger logger = LoggerFactory.getLogger(FlatIndex.class);

    private final FlatConfig config;
    private final String metricName;
    private final List<String> vectorIds;
    private final Map<String, double[]> vectorCache;

    /**
     * Create a new FlatIndex with type-safe configuration.
     *
     * @param config the flat index configuration
     */
    public FlatIndex(FlatConfig config) {
        super();
        this.config = config;
        this.metricName = config.metric().getValue();
        this.vectorIds = new CopyOnWriteArrayList<>();
        this.vectorCache = new ConcurrentHashMap<>();
    }

    /**
     * Create a builder for FlatConfig.
     *
     * @return a new builder instance
     */
    public static FlatConfig.Builder builder() {
        return FlatConfig.builder();
    }

    @Override
    protected void doInitialize() throws Exception {
        // Load existing vector IDs
        List<String> existingIds = storageBackend.listVectors(null);
        vectorIds.addAll(existingIds);

        logger.info("Flat index initialized with {} vectors", vectorIds.size());
    }

    @Override
    public List<String> insert(double[][] vectors, List<Map<String, Object>> metadata) {
        ensureInitialized();

        if (vectors == null || vectors.length == 0) {
            throw new IndexException("Vectors cannot be null or empty");
        }

        try {
            List<String> insertedIds = new ArrayList<>();

            for (int i = 0; i < vectors.length; i++) {
                // Generate unique ID
                String vectorId = UUID.randomUUID().toString();

                // Get metadata for this vector
                Map<String, Object> vectorMetadata =
                        (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

                // Store in backend
                storageBackend.storeVector(vectorId, vectors[i], vectorMetadata);

                // Add to index
                vectorIds.add(vectorId);
                vectorCache.put(vectorId, vectors[i].clone());

                insertedIds.add(vectorId);
            }

            logger.debug("Inserted {} vectors", insertedIds.size());
            return insertedIds;
        } catch (Exception e) {
            throw new IndexException("Failed to insert vectors: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int k) {
        ensureInitialized();

        if (queryVector == null) {
            throw new IndexException("Query vector cannot be null");
        }

        try {
            if (vectorIds.isEmpty()) {
                return Collections.emptyList();
            }

            // Limit k to available vectors
            k = Math.min(k, vectorIds.size());

            // Load vectors and compute distances
            List<String> validIds = new ArrayList<>();
            List<Double> distances = new ArrayList<>();

            for (String vectorId : vectorIds) {
                // Get vector from cache or storage (atomic operation)
                double[] vector = vectorCache.get(vectorId);
                if (vector == null) {
                    Optional<StorageBackend.VectorWithMetadata> result =
                            storageBackend.retrieveVector(vectorId);
                    if (!result.isPresent()) {
                        continue;
                    }
                    vector = result.get().getVector();
                    vectorCache.put(vectorId, vector);
                }

                // Compute distance
                double distance = computeDistance(queryVector, vector);
                validIds.add(vectorId);
                distances.add(distance);
            }

            if (validIds.isEmpty()) {
                return Collections.emptyList();
            }

            // Get top k results using a max-heap priority queue
            // Max-heap keeps largest distances at top, so we can remove them when size > k
            PriorityQueue<ResultEntry> topK = new PriorityQueue<>(k,
                    Comparator.comparingDouble(ResultEntry::getDistance).reversed());

            for (int i = 0; i < validIds.size(); i++) {
                topK.offer(new ResultEntry(validIds.get(i), distances.get(i)));
                if (topK.size() > k) {
                    topK.poll(); // Remove the largest distance (farthest neighbor)
                }
            }

            // Build result list sorted by ascending distance
            List<SearchResult> results = new ArrayList<>(topK.size());
            while (!topK.isEmpty()) {
                results.add(0, topK.poll().toSearchResult(storageBackend));
            }

            logger.debug("Search returned {} results", results.size());
            return results;
        } catch (Exception e) {
            throw new IndexException("Search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(List<String> ids) {
        ensureInitialized();

        try {
            boolean success = true;

            for (String vectorId : ids) {
                // Remove from storage
                if (!storageBackend.deleteVector(vectorId)) {
                    success = false;
                }

                // Remove from index
                vectorIds.remove(vectorId);

                // Remove from cache
                vectorCache.remove(vectorId);
            }

            logger.debug("Deleted {} vectors", ids.size());
            return success;
        } catch (Exception e) {
            throw new IndexException("Failed to delete vectors: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(String id, double[] vector, Map<String, Object> metadata) {
        ensureInitialized();

        try {
            // Update in storage
            if (!storageBackend.updateVector(id, vector, metadata)) {
                return false;
            }

            // Update cache if vector was provided
            if (vector != null) {
                vectorCache.put(id, vector.clone());
            }

            logger.debug("Updated vector with ID: {}", id);
            return true;
        } catch (Exception e) {
            throw new IndexException("Failed to update vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Boolean> batchUpdate(List<String> ids, List<double[]> vectors,
                                     List<Map<String, Object>> metadata) {
        ensureInitialized();

        try {
            List<Boolean> results = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                double[] vector = (vectors != null && i < vectors.size()) ? vectors.get(i) : null;
                Map<String, Object> meta = (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

                if (!storageBackend.updateVector(id, vector, meta)) {
                    results.add(false);
                    continue;
                }

                if (vector != null) {
                    vectorCache.put(id, vector.clone());
                }

                results.add(true);
            }

            logger.debug("Batch updated {} vectors", ids.size());
            return results;
        } catch (Exception e) {
            throw new IndexException("Failed to batch update vectors: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<SearchResult>> batchSearch(double[][] queryVectors, int k) {
        ensureInitialized();

        if (queryVectors == null || queryVectors.length == 0) {
            throw new IndexException("Query vectors cannot be null or empty");
        }

        try {
            List<List<SearchResult>> results = new ArrayList<>();

            if (vectorIds.isEmpty()) {
                for (int q = 0; q < queryVectors.length; q++) {
                    results.add(Collections.emptyList());
                }
                return results;
            }

            // Pre-load all vectors into cache if not already cached
            for (String vectorId : vectorIds) {
                if (!vectorCache.containsKey(vectorId)) {
                    Optional<StorageBackend.VectorWithMetadata> result =
                            storageBackend.retrieveVector(vectorId);
                    if (result.isPresent()) {
                        vectorCache.put(vectorId, result.get().getVector());
                    }
                }
            }

            // Process each query vector
            for (double[] queryVector : queryVectors) {
                if (queryVector == null) {
                    throw new IndexException("Query vector cannot be null");
                }

                int actualK = Math.min(k, vectorIds.size());
                List<String> validIds = new ArrayList<>();
                List<Double> distances = new ArrayList<>();

                for (String vectorId : vectorIds) {
                    double[] vector = vectorCache.get(vectorId);
                    if (vector == null) {
                        continue;
                    }

                    double distance = computeDistance(queryVector, vector);
                    validIds.add(vectorId);
                    distances.add(distance);
                }

                if (validIds.isEmpty()) {
                    results.add(Collections.emptyList());
                    continue;
                }

                // Get top k results
                PriorityQueue<ResultEntry> topK = new PriorityQueue<>(actualK,
                        Comparator.comparingDouble(ResultEntry::getDistance).reversed());

                for (int i = 0; i < validIds.size(); i++) {
                    topK.offer(new ResultEntry(validIds.get(i), distances.get(i)));
                    if (topK.size() > actualK) {
                        topK.poll();
                    }
                }

                List<SearchResult> queryResults = new ArrayList<>(topK.size());
                while (!topK.isEmpty()) {
                    queryResults.add(0, topK.poll().toSearchResult(storageBackend));
                }

                results.add(queryResults);
            }

            logger.debug("Batch searched {} query vectors", queryVectors.length);
            return results;
        } catch (Exception e) {
            throw new IndexException("Batch search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        // Note: Stats may have minor inconsistencies due to concurrent modifications
        // This is acceptable for monitoring purposes
        stats.put("type", "FlatIndex");
        stats.put("index_type", "flat");
        stats.put("metric", config.metric().getValue());
        stats.put("vector_count", vectorIds.size());
        stats.put("cache_size", vectorCache.size());
        stats.put("initialized", initialized);

        return stats;
    }

    @Override
    protected void doClose() {
        vectorCache.clear();
        vectorIds.clear();
        logger.info("Flat index closed");
    }

    /**
     * Compute distance between two vectors based on the configured metric.
     */
    private double computeDistance(double[] query, double[] vector) {
        switch (metricName) {
            case "cosine":
                return SimilarityMetrics.cosineDistance(query, vector);
            case "euclidean":
                return SimilarityMetrics.euclideanDistance(query, vector);
            case "dot_product":
                return -SimilarityMetrics.dotProduct(query, vector); // Negate for distance
            case "manhattan":
                return SimilarityMetrics.manhattanDistance(query, vector);
            default:
                throw new IndexException("Unsupported metric: " + metricName);
        }
    }

    /**
     * Internal class to hold search results for sorting.
     */
    private static class ResultEntry {
        private final String id;
        private final double distance;

        ResultEntry(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }

        String getId() {
            return id;
        }

        double getDistance() {
            return distance;
        }

        SearchResult toSearchResult(StorageBackend storage) {
            Optional<StorageBackend.VectorWithMetadata> result = storage.retrieveVector(id);
            Map<String, Object> metadata = result.map(StorageBackend.VectorWithMetadata::getMetadata)
                    .orElse(null);
            return new SearchResult(id, distance, metadata);
        }
    }
}
