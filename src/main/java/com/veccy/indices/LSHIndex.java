package com.veccy.indices;

import com.veccy.base.AbstractIndex;
import com.veccy.base.SearchResult;
import com.veccy.config.LSHConfig;
import com.veccy.exceptions.IndexException;
import com.veccy.utils.SimilarityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Locality-Sensitive Hashing (LSH) Index for approximate nearest neighbor search.
 * <p>
 * LSH uses random projection hash functions to map similar vectors to the same
 * buckets with high probability. Multiple hash tables are used to improve recall.
 * <p>
 * Algorithm:
 * 1. Initialization: Generate random projection vectors
 * 2. Indexing: Hash each vector into buckets across multiple hash tables
 * 3. Search: Hash query vector and search corresponding buckets
 * <p>
 * Example usage:
 * <pre>{@code
 * LSHConfig config = LSHConfig.builder()
 *     .metric(Metric.COSINE)
 *     .numTables(5)
 *     .numHashBits(8)
 *     .build();
 * LSHIndex index = new LSHIndex(config);
 * }</pre>
 */
public class LSHIndex extends AbstractIndex {

    private static final Logger logger = LoggerFactory.getLogger(LSHIndex.class);

    private final LSHConfig config;
    private final String metricName;
    private final int numTables;
    private final int numHashBits;
    private final double bucketWidth;
    private int dimensions;

    // Hash tables: table -> hash code -> list of vector IDs
    private List<Map<Integer, List<String>>> hashTables;

    // Random projection vectors for each table and hash bit
    // projectionVectors[table][bit][dimension]
    private double[][][] projectionVectors;

    // Random offsets for p-stable hashing (used with euclidean)
    private double[][] randomOffsets;

    // Vector ID to hash codes mapping (for efficient deletion)
    private Map<String, int[]> vectorToHashes;

    /**
     * Create a new LSHIndex with type-safe configuration.
     *
     * @param config the LSH index configuration
     */
    public LSHIndex(LSHConfig config) {
        super();
        this.config = config;
        this.metricName = config.metric().getValue();
        this.numTables = config.numTables();
        this.numHashBits = config.numHashBits();
        this.bucketWidth = config.bucketWidth();
    }

    /**
     * Create a builder for LSHConfig.
     *
     * @return a new builder instance
     */
    public static LSHConfig.Builder builder() {
        return LSHConfig.builder();
    }

    @Override
    protected void doInitialize() throws Exception {
        this.hashTables = new ArrayList<>();
        for (int i = 0; i < numTables; i++) {
            hashTables.add(new ConcurrentHashMap<>());
        }

        this.vectorToHashes = new ConcurrentHashMap<>();

        logger.info("LSH index initialized: tables={}, hash_bits={}, metric={}",
                numTables, numHashBits, metricName);
    }

    @Override
    public List<String> insert(double[][] vectors, List<Map<String, Object>> metadata) {
        ensureInitialized();

        // Initialize projection vectors on first insert
        if (projectionVectors == null && vectors.length > 0) {
            dimensions = vectors[0].length;
            initializeProjectionVectors();
        }

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < vectors.length; i++) {
            // Store in storage backend
            String id = UUID.randomUUID().toString();
            Map<String, Object> meta = (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

            if (storageBackend.storeVector(id, vectors[i], meta)) {
                // Hash and insert into LSH tables
                int[] hashes = computeHashes(vectors[i]);
                vectorToHashes.put(id, hashes);

                for (int t = 0; t < numTables; t++) {
                    hashTables.get(t)
                            .computeIfAbsent(hashes[t], k -> Collections.synchronizedList(new ArrayList<>()))
                            .add(id);
                }

                ids.add(id);
            }
        }

        logger.debug("Inserted {} vectors into LSH index", ids.size());
        return ids;
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int k) {
        ensureInitialized();

        if (projectionVectors == null) {
            throw new IndexException("Index not trained. Insert vectors first.");
        }

        // Hash query vector
        int[] queryHashes = computeHashes(queryVector);

        // Collect candidate vector IDs from all matching buckets
        Set<String> candidates = new HashSet<>();
        for (int t = 0; t < numTables; t++) {
            List<String> bucket = hashTables.get(t).get(queryHashes[t]);
            if (bucket != null) {
                candidates.addAll(bucket);
            }
        }

        // Compute distances for all candidates
        List<SearchResult> results = new ArrayList<>();
        for (String id : candidates) {
            storageBackend.retrieveVector(id).ifPresent(vectorWithMetadata -> {
                double distance = computeDistance(queryVector, vectorWithMetadata.getVector());
                results.add(new SearchResult(id, distance, vectorWithMetadata.getMetadata()));
            });
        }

        // Sort by distance and return top k
        results.sort(Comparator.comparingDouble(SearchResult::getDistance));
        int resultSize = Math.min(k, results.size());

        logger.debug("LSH search: {} candidates, returning {} results", candidates.size(), resultSize);

        return results.subList(0, resultSize);
    }

    @Override
    public boolean delete(List<String> ids) {
        ensureInitialized();

        boolean allSuccess = true;
        for (String id : ids) {
            // Remove from hash tables
            int[] hashes = vectorToHashes.remove(id);
            if (hashes != null) {
                for (int t = 0; t < numTables; t++) {
                    List<String> bucket = hashTables.get(t).get(hashes[t]);
                    if (bucket != null) {
                        bucket.remove(id);
                    }
                }
            }

            // Delete from storage
            if (!storageBackend.deleteVector(id)) {
                allSuccess = false;
            }
        }

        logger.debug("Deleted {} vectors from LSH index", ids.size());
        return allSuccess;
    }

    @Override
    public boolean update(String id, double[] vector, Map<String, Object> metadata) {
        ensureInitialized();

        if (projectionVectors == null) {
            throw new IndexException("Index not initialized with vectors");
        }

        // Remove from old buckets
        int[] oldHashes = vectorToHashes.get(id);
        if (oldHashes != null) {
            for (int t = 0; t < numTables; t++) {
                List<String> bucket = hashTables.get(t).get(oldHashes[t]);
                if (bucket != null) {
                    bucket.remove(id);
                }
            }
        }

        // Update in storage
        if (!storageBackend.updateVector(id, vector, metadata)) {
            return false;
        }

        // Recompute hashes and insert into new buckets
        if (vector != null) {
            int[] newHashes = computeHashes(vector);
            vectorToHashes.put(id, newHashes);

            for (int t = 0; t < numTables; t++) {
                hashTables.get(t)
                        .computeIfAbsent(newHashes[t], k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(id);
            }
        }

        logger.debug("Updated vector with ID: {}", id);
        return true;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!isInitialized()) {
            stats.put("status", "not_initialized");
            return stats;
        }

        // Note: Stats may have minor inconsistencies due to concurrent modifications
        // This is acceptable for monitoring purposes
        stats.put("type", "LSHIndex");
        stats.put("index_type", "lsh");
        stats.put("metric", metricName);
        stats.put("num_tables", numTables);
        stats.put("num_hash_bits", numHashBits);
        stats.put("vector_count", vectorToHashes.size());
        stats.put("initialized", isInitialized());

        if (projectionVectors != null) {
            stats.put("dimensions", dimensions);

            // Bucket distribution statistics
            Map<String, Object> bucketStats = new HashMap<>();
            int totalBuckets = 0;
            int nonEmptyBuckets = 0;
            int minBucketSize = Integer.MAX_VALUE;
            int maxBucketSize = 0;
            int totalVectors = 0;

            for (Map<Integer, List<String>> table : hashTables) {
                totalBuckets += (1 << numHashBits);  // 2^numHashBits possible buckets
                nonEmptyBuckets += table.size();

                for (List<String> bucket : table.values()) {
                    int size = bucket.size();
                    minBucketSize = Math.min(minBucketSize, size);
                    maxBucketSize = Math.max(maxBucketSize, size);
                    totalVectors += size;
                }
            }

            bucketStats.put("total_buckets", totalBuckets);
            bucketStats.put("non_empty_buckets", nonEmptyBuckets);
            bucketStats.put("min_bucket_size", minBucketSize == Integer.MAX_VALUE ? 0 : minBucketSize);
            bucketStats.put("max_bucket_size", maxBucketSize);
            bucketStats.put("avg_bucket_size", nonEmptyBuckets > 0 ? totalVectors / (double) nonEmptyBuckets : 0.0);

            stats.put("bucket_stats", bucketStats);
        }

        return stats;
    }

    @Override
    protected void doClose() {
        if (hashTables != null) {
            for (Map<Integer, List<String>> table : hashTables) {
                table.clear();
            }
            hashTables.clear();
        }
        if (vectorToHashes != null) {
            vectorToHashes.clear();
        }
        projectionVectors = null;
        randomOffsets = null;
        logger.info("LSH index closed");
    }

    /**
     * Initialize random projection vectors for LSH.
     */
    private void initializeProjectionVectors() {
        projectionVectors = new double[numTables][numHashBits][dimensions];
        Random random = ThreadLocalRandom.current();

        // Generate random projection vectors from standard normal distribution
        for (int t = 0; t < numTables; t++) {
            for (int h = 0; h < numHashBits; h++) {
                for (int d = 0; d < dimensions; d++) {
                    projectionVectors[t][h][d] = random.nextGaussian();
                }

                // Normalize for cosine hashing
                if (metricName.equalsIgnoreCase("cosine")) {
                    normalizeVector(projectionVectors[t][h]);
                }
            }
        }

        // Initialize random offsets for p-stable hashing (euclidean)
        if (metricName.equalsIgnoreCase("euclidean")) {
            randomOffsets = new double[numTables][numHashBits];
            for (int t = 0; t < numTables; t++) {
                for (int h = 0; h < numHashBits; h++) {
                    randomOffsets[t][h] = random.nextDouble() * bucketWidth;
                }
            }
        }

        logger.info("Initialized LSH projection vectors: dimensions={}", dimensions);
    }

    /**
     * Compute hash codes for a vector across all tables.
     */
    private int[] computeHashes(double[] vector) {
        int[] hashes = new int[numTables];

        for (int t = 0; t < numTables; t++) {
            int hash = 0;

            for (int h = 0; h < numHashBits; h++) {
                int bit = computeHashBit(vector, t, h);
                hash = (hash << 1) | bit;
            }

            hashes[t] = hash;
        }

        return hashes;
    }

    /**
     * Compute a single hash bit for a vector.
     */
    private int computeHashBit(double[] vector, int tableIdx, int bitIdx) {
        double[] projection = projectionVectors[tableIdx][bitIdx];

        if (metricName.equalsIgnoreCase("euclidean")) {
            // p-stable hashing for Euclidean distance
            double dotProduct = 0.0;
            for (int d = 0; d < dimensions; d++) {
                dotProduct += vector[d] * projection[d];
            }

            double offset = randomOffsets[tableIdx][bitIdx];
            double value = (dotProduct + offset) / bucketWidth;

            return (int) Math.floor(value) & 1;  // Use LSB for hash bit
        } else {
            // Sign-based random projection for cosine/dot product
            double dotProduct = 0.0;
            for (int d = 0; d < dimensions; d++) {
                dotProduct += vector[d] * projection[d];
            }

            return dotProduct >= 0 ? 1 : 0;
        }
    }

    /**
     * Normalize a vector to unit length.
     */
    private void normalizeVector(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    /**
     * Compute distance based on configured metric.
     */
    private double computeDistance(double[] a, double[] b) {
        switch (metricName.toLowerCase()) {
            case "cosine":
                return SimilarityMetrics.cosineDistance(a, b);
            case "euclidean":
                return SimilarityMetrics.euclideanDistance(a, b);
            case "dot":
                return -SimilarityMetrics.dotProduct(a, b);  // Negative for sorting
            default:
                return SimilarityMetrics.cosineDistance(a, b);
        }
    }
}
