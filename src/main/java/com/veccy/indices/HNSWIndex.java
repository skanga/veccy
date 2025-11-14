package com.veccy.indices;

import com.veccy.base.AbstractIndex;
import com.veccy.base.SearchResult;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.exceptions.IndexException;
import com.veccy.storage.StorageBackend;
import com.veccy.utils.SimilarityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * HNSW (Hierarchical Navigable Small World) index implementation.
 * <p>
 * HNSW is a graph-based algorithm that provides fast approximate nearest neighbor search
 * with high recall. It builds a multi-layer graph structure where each layer is a proximity
 * graph with increasingly sparse connections at higher layers.
 * <p>
 * Note: This is a simplified implementation suitable for moderate-scale datasets.
 * For production large-scale use, consider specialized libraries.
 * <p>
 * Parameters:
 * - m: Number of bidirectional links created for each node (default: 16)
 * - ef_construction: Size of dynamic candidate list during construction (default: 200)
 * - ef_search: Size of dynamic candidate list during search (default: 50)
 * - metric: Distance metric to use (cosine, euclidean)
 */
public class HNSWIndex extends AbstractIndex {

    private static final Logger logger = LoggerFactory.getLogger(HNSWIndex.class);
    private static final int MAX_LEVELS = 10;

    private final HNSWConfig config;
    private final int m;              // Number of bidirectional links
    private final int efConstruction; // Search width during construction
    private final int efSearch;       // Search width during query
    private final String metric;

    // Graph structure: levels[0] contains all nodes, higher levels are sparser
    private final List<Map<String, List<String>>> levels;
    private final Map<String, double[]> vectors;
    private int maxLevel;

    // Random number generator (supports seeding for deterministic tests)
    private volatile Random random;

    // Read-write lock to ensure thread-safety during updates
    private final ReadWriteLock updateLock = new ReentrantReadWriteLock();

    /**
     * Create HNSW index with type-safe configuration.
     *
     * @param hnswConfig type-safe configuration
     */
    public HNSWIndex(HNSWConfig hnswConfig) {
        super();
        this.config = hnswConfig;
        this.m = hnswConfig.m();
        this.efConstruction = hnswConfig.efConstruction();
        this.efSearch = hnswConfig.efSearch();
        this.metric = hnswConfig.metric().getValue();

        this.levels = new ArrayList<>();
        this.vectors = new ConcurrentHashMap<>();
        this.maxLevel = 0;
    }

    /**
     * Create a new builder for type-safe configuration.
     *
     * @return a new HNSWConfig builder
     */
    public static HNSWConfig.Builder builder() {
        return HNSWConfig.builder();
    }

    @Override
    protected void doInitialize() throws Exception {
        // Initialize random number generator (use seed if provided for deterministic behavior)
        if (config.randomSeed().isPresent()) {
            this.random = new Random(config.randomSeed().get());
        } else {
            this.random = new Random(ThreadLocalRandom.current().nextLong());
        }

        // Initialize level 0
        levels.add(new ConcurrentHashMap<>());

        // Load existing vectors if any
        List<String> existingIds = storageBackend.listVectors(null);
        for (String vectorId : existingIds) {
            Optional<StorageBackend.VectorWithMetadata> result = storageBackend.retrieveVector(vectorId);
            if (result.isPresent()) {
                insertVectorInternal(vectorId, result.get().getVector());
            }
        }

        logger.info("HNSW index initialized with {} vectors across {} levels",
                vectors.size(), levels.size());
    }

    @Override
    public List<String> insert(double[][] vectors, List<Map<String, Object>> metadata) {
        ensureInitialized();

        if (vectors == null || vectors.length == 0) {
            throw new IndexException("Vectors cannot be null or empty");
        }

        // Acquire write lock to ensure atomic insertion
        updateLock.writeLock().lock();
        try {
            List<String> insertedIds = new ArrayList<>();

            for (int i = 0; i < vectors.length; i++) {
                String vectorId = UUID.randomUUID().toString();
                Map<String, Object> vectorMetadata =
                        (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

                // Store in backend
                storageBackend.storeVector(vectorId, vectors[i], vectorMetadata);

                // Insert into HNSW graph
                insertVectorInternal(vectorId, vectors[i]);

                insertedIds.add(vectorId);
            }

            logger.debug("Inserted {} vectors into HNSW index", insertedIds.size());
            return insertedIds;
        } catch (Exception e) {
            throw new IndexException("Failed to insert vectors: " + e.getMessage(), e);
        } finally {
            updateLock.writeLock().unlock();
        }
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int k) {
        ensureInitialized();

        if (queryVector == null) {
            throw new IndexException("Query vector cannot be null");
        }

        // Acquire read lock to allow concurrent searches but block during updates
        updateLock.readLock().lock();
        try {
            if (vectors.isEmpty()) {
                return Collections.emptyList();
            }

            k = Math.min(k, vectors.size());

            // Find entry point at highest level
            int currentLevel = Math.min(maxLevel, levels.size() - 1);
            List<NodeDistance> candidates = new ArrayList<>();

            if (currentLevel >= 0 && !levels.get(currentLevel).isEmpty()) {
                // Start from first node at top level
                String entryPoint = levels.get(currentLevel).keySet().iterator().next();
                double distance = computeDistance(queryVector, vectors.get(entryPoint));
                candidates.add(new NodeDistance(entryPoint, distance));
            }

            // Traverse down levels
            while (currentLevel > 0) {
                Set<String> visited = new HashSet<>();
                PriorityQueue<NodeDistance> newCandidates = new PriorityQueue<>(
                        Comparator.comparingDouble(NodeDistance::getDistance));

                for (NodeDistance candidate : candidates) {
                    String nodeId = candidate.getId();
                    if (levels.get(currentLevel).containsKey(nodeId)) {
                        for (String neighborId : levels.get(currentLevel).get(nodeId)) {
                            if (!visited.contains(neighborId) && vectors.containsKey(neighborId)) {
                                visited.add(neighborId);
                                double dist = computeDistance(queryVector, vectors.get(neighborId));
                                newCandidates.offer(new NodeDistance(neighborId, dist));
                            }
                        }
                    }
                }

                // Keep best ef_search candidates, but don't clear if no new candidates found
                if (!newCandidates.isEmpty()) {
                    candidates.clear();
                    for (int i = 0; i < efSearch && !newCandidates.isEmpty(); i++) {
                        candidates.add(newCandidates.poll());
                    }
                }

                currentLevel--;
            }

            // Final search at level 0
            if (currentLevel == 0 && !candidates.isEmpty()) {
                Set<String> visited = new HashSet<>();
                PriorityQueue<NodeDistance> finalCandidates = new PriorityQueue<>(
                        Comparator.comparingDouble(NodeDistance::getDistance));

                // Add initial candidates
                for (NodeDistance candidate : candidates) {
                    finalCandidates.offer(candidate);
                    visited.add(candidate.getId());
                }

                // Explore neighbors at level 0
                for (NodeDistance candidate : candidates) {
                    String nodeId = candidate.getId();
                    if (levels.get(0).containsKey(nodeId)) {
                        for (String neighborId : levels.get(0).get(nodeId)) {
                            if (!visited.contains(neighborId) && vectors.containsKey(neighborId)) {
                                visited.add(neighborId);
                                double dist = computeDistance(queryVector, vectors.get(neighborId));
                                finalCandidates.offer(new NodeDistance(neighborId, dist));
                            }
                        }
                    }
                }

                // Get top k results
                List<SearchResult> results = new ArrayList<>();
                for (int i = 0; i < k && !finalCandidates.isEmpty(); i++) {
                    NodeDistance nd = finalCandidates.poll();
                    Optional<StorageBackend.VectorWithMetadata> result =
                            storageBackend.retrieveVector(nd.getId());
                    Map<String, Object> meta = result.map(StorageBackend.VectorWithMetadata::getMetadata)
                            .orElse(null);
                    results.add(new SearchResult(nd.getId(), nd.getDistance(), meta));
                }

                return results;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            throw new IndexException("Search failed: " + e.getMessage(), e);
        } finally {
            updateLock.readLock().unlock();
        }
    }

    @Override
    public boolean delete(List<String> ids) {
        ensureInitialized();

        // Acquire write lock to ensure atomic deletion
        updateLock.writeLock().lock();
        try {
            boolean success = true;

            for (String vectorId : ids) {
                if (!storageBackend.deleteVector(vectorId)) {
                    success = false;
                }
                removeVectorInternal(vectorId);
            }

            logger.debug("Deleted {} vectors from HNSW index", ids.size());
            return success;
        } catch (Exception e) {
            throw new IndexException("Failed to delete vectors: " + e.getMessage(), e);
        } finally {
            updateLock.writeLock().unlock();
        }
    }

    @Override
    public boolean update(String id, double[] vector, Map<String, Object> metadata) {
        ensureInitialized();

        // Acquire write lock to ensure atomic update
        updateLock.writeLock().lock();
        try {
            if (!storageBackend.updateVector(id, vector, metadata)) {
                return false;
            }

            // For HNSW, updating a vector requires rebuilding graph connections
            // Remove old vector from graph and re-insert with new value
            if (vector != null && vectors.containsKey(id)) {
                // Determine the level this vector was at before removal
                int originalLevel = 0;
                for (int l = 0; l < levels.size(); l++) {
                    if (levels.get(l).containsKey(id)) {
                        originalLevel = l;
                    }
                }

                // Remove from graph structure
                removeVectorInternal(id);

                // Re-insert with new vector value at the same level to maintain graph structure
                vectors.put(id, vector.clone());
                insertVectorInternalAtLevel(id, vector, originalLevel);
            }

            logger.debug("Updated vector with ID: {}", id);
            return true;
        } catch (Exception e) {
            throw new IndexException("Failed to update vector " + id + ": " + e.getMessage(), e);
        } finally {
            updateLock.writeLock().unlock();
        }
    }

    @Override
    public List<Boolean> batchUpdate(List<String> ids, List<double[]> vectors,
                                     List<Map<String, Object>> metadata) {
        ensureInitialized();

        // Acquire write lock once for entire batch
        updateLock.writeLock().lock();
        try {
            List<Boolean> results = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                double[] vector = (vectors != null && i < vectors.size()) ? vectors.get(i) : null;
                Map<String, Object> meta = (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

                try {
                    if (!storageBackend.updateVector(id, vector, meta)) {
                        results.add(false);
                        continue;
                    }

                    if (vector != null && this.vectors.containsKey(id)) {
                        int originalLevel = 0;
                        for (int l = 0; l < levels.size(); l++) {
                            if (levels.get(l).containsKey(id)) {
                                originalLevel = l;
                            }
                        }

                        removeVectorInternal(id);
                        this.vectors.put(id, vector.clone());
                        insertVectorInternalAtLevel(id, vector, originalLevel);
                    }

                    results.add(true);
                } catch (Exception e) {
                    logger.warn("Failed to update vector {}: {}", id, e.getMessage());
                    results.add(false);
                }
            }

            logger.debug("Batch updated {} vectors", ids.size());
            return results;
        } finally {
            updateLock.writeLock().unlock();
        }
    }

    @Override
    public List<List<SearchResult>> batchSearch(double[][] queryVectors, int k) {
        ensureInitialized();

        if (queryVectors == null || queryVectors.length == 0) {
            throw new IndexException("Query vectors cannot be null or empty");
        }

        // Acquire read lock once for entire batch
        updateLock.readLock().lock();
        try {
            List<List<SearchResult>> results = new ArrayList<>();
            for (double[] queryVector : queryVectors) {
                if (queryVector == null) {
                    throw new IndexException("Query vector cannot be null");
                }

                if (this.vectors.isEmpty()) {
                    results.add(Collections.emptyList());
                    continue;
                }

                int actualK = Math.min(k, this.vectors.size());

                // Find entry point at highest level
                int currentLevel = Math.min(maxLevel, levels.size() - 1);
                List<NodeDistance> candidates = new ArrayList<>();

                if (currentLevel >= 0 && !levels.get(currentLevel).isEmpty()) {
                    String entryPoint = levels.get(currentLevel).keySet().iterator().next();
                    double distance = computeDistance(queryVector, this.vectors.get(entryPoint));
                    candidates.add(new NodeDistance(entryPoint, distance));
                }

                // Traverse down levels
                while (currentLevel > 0) {
                    Set<String> visited = new HashSet<>();
                    PriorityQueue<NodeDistance> newCandidates = new PriorityQueue<>(
                            Comparator.comparingDouble(NodeDistance::getDistance));

                    for (NodeDistance candidate : candidates) {
                        String nodeId = candidate.getId();
                        if (levels.get(currentLevel).containsKey(nodeId)) {
                            for (String neighborId : levels.get(currentLevel).get(nodeId)) {
                                if (!visited.contains(neighborId) && this.vectors.containsKey(neighborId)) {
                                    visited.add(neighborId);
                                    double dist = computeDistance(queryVector, this.vectors.get(neighborId));
                                    newCandidates.offer(new NodeDistance(neighborId, dist));
                                }
                            }
                        }
                    }

                    if (!newCandidates.isEmpty()) {
                        candidates.clear();
                        for (int i = 0; i < efSearch && !newCandidates.isEmpty(); i++) {
                            candidates.add(newCandidates.poll());
                        }
                    }

                    currentLevel--;
                }

                // Final search at level 0
                if (currentLevel == 0 && !candidates.isEmpty()) {
                    Set<String> visited = new HashSet<>();
                    PriorityQueue<NodeDistance> finalCandidates = new PriorityQueue<>(
                            Comparator.comparingDouble(NodeDistance::getDistance));

                    for (NodeDistance candidate : candidates) {
                        finalCandidates.offer(candidate);
                        visited.add(candidate.getId());
                    }

                    for (NodeDistance candidate : candidates) {
                        String nodeId = candidate.getId();
                        if (levels.get(0).containsKey(nodeId)) {
                            for (String neighborId : levels.get(0).get(nodeId)) {
                                if (!visited.contains(neighborId) && this.vectors.containsKey(neighborId)) {
                                    visited.add(neighborId);
                                    double dist = computeDistance(queryVector, this.vectors.get(neighborId));
                                    finalCandidates.offer(new NodeDistance(neighborId, dist));
                                }
                            }
                        }
                    }

                    List<SearchResult> queryResults = new ArrayList<>();
                    for (int i = 0; i < actualK && !finalCandidates.isEmpty(); i++) {
                        NodeDistance nd = finalCandidates.poll();
                        Optional<StorageBackend.VectorWithMetadata> result =
                                storageBackend.retrieveVector(nd.getId());
                        Map<String, Object> meta = result.map(StorageBackend.VectorWithMetadata::getMetadata)
                                .orElse(null);
                        queryResults.add(new SearchResult(nd.getId(), nd.getDistance(), meta));
                    }

                    results.add(queryResults);
                } else {
                    results.add(Collections.emptyList());
                }
            }

            logger.debug("Batch searched {} query vectors", queryVectors.length);
            return results;
        } catch (Exception e) {
            throw new IndexException("Batch search failed: " + e.getMessage(), e);
        } finally {
            updateLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        // Acquire read lock to ensure consistent snapshot
        updateLock.readLock().lock();
        try {
            stats.put("type", "HNSWIndex");
            stats.put("index_type", "hnsw");
            stats.put("metric", metric);
            stats.put("m", m);
            stats.put("ef_construction", efConstruction);
            stats.put("ef_search", efSearch);
            stats.put("max_level", maxLevel);
            stats.put("vector_count", vectors.size());
            stats.put("levels", levels.size());
            stats.put("initialized", initialized);

            return stats;
        } finally {
            updateLock.readLock().unlock();
        }
    }

    @Override
    protected void doClose() {
        vectors.clear();
        levels.clear();
        maxLevel = 0;
        logger.info("HNSW index closed");
    }

    /**
     * Internal method to insert a vector into the HNSW graph.
     */
    private void insertVectorInternal(String vectorId, double[] vector) {
        vectors.put(vectorId, vector.clone());

        // Assign random level using exponential decay
        int level = randomLevel();

        insertVectorInternalAtLevel(vectorId, vector, level);
    }

    /**
     * Internal method to insert a vector into the HNSW graph at a specific level.
     */
    private void insertVectorInternalAtLevel(String vectorId, double[] vector, int level) {
        // Ensure we have enough levels
        while (levels.size() <= level) {
            levels.add(new ConcurrentHashMap<>());
        }

        // Add node to all levels from 0 to assigned level
        for (int l = 0; l <= level; l++) {
            levels.get(l).putIfAbsent(vectorId, new ArrayList<>());
        }

        maxLevel = Math.max(maxLevel, level);

        // Connect to neighbors at each level
        for (int l = 0; l <= level && l < levels.size(); l++) {
            connectToNeighbors(vectorId, l);
        }
    }

    /**
     * Connect a node to its neighbors at a specific level.
     */
    private void connectToNeighbors(String vectorId, int level) {
        Map<String, List<String>> levelGraph = levels.get(level);
        List<String> existingNodes = new ArrayList<>(levelGraph.keySet());

        if (existingNodes.isEmpty()) {
            return;
        }

        // Get the vector to connect
        double[] vector = vectors.get(vectorId);

        // Find nearest neighbors by distance
        List<NodeDistance> distances = new ArrayList<>();
        for (String nodeId : existingNodes) {
            if (!nodeId.equals(vectorId) && vectors.containsKey(nodeId)) {
                double dist = computeDistance(vector, vectors.get(nodeId));
                distances.add(new NodeDistance(nodeId, dist));
            }
        }

        // Sort by distance and connect to nearest m neighbors
        distances.sort(Comparator.comparingDouble(NodeDistance::getDistance));
        int numConnections = Math.min(m, distances.size());

        for (int i = 0; i < numConnections; i++) {
            String neighborId = distances.get(i).getId();

            // Bidirectional connection
            List<String> neighborList = levelGraph.get(neighborId);
            if (!neighborList.contains(vectorId)) {
                neighborList.add(vectorId);
            }

            List<String> myList = levelGraph.get(vectorId);
            if (!myList.contains(neighborId)) {
                myList.add(neighborId);
            }
        }
    }

    /**
     * Remove a vector from the HNSW graph.
     */
    private void removeVectorInternal(String vectorId) {
        vectors.remove(vectorId);

        // Remove from all levels
        for (Map<String, List<String>> level : levels) {
            if (level.containsKey(vectorId)) {
                // Remove bidirectional connections
                for (String neighborId : level.get(vectorId)) {
                    if (level.containsKey(neighborId)) {
                        level.get(neighborId).remove(vectorId);
                    }
                }
                level.remove(vectorId);
            }
        }
    }

    /**
     * Assign a random level to a new node using exponential decay.
     */
    private int randomLevel() {
        int level = 0;
        while (random.nextDouble() < 0.5 && level < MAX_LEVELS - 1) {
            level++;
        }
        return level;
    }

    /**
     * Compute distance between two vectors based on the configured metric.
     */
    private double computeDistance(double[] query, double[] vector) {
        switch (metric) {
            case "cosine":
                return SimilarityMetrics.cosineDistance(query, vector);
            case "euclidean":
                return SimilarityMetrics.euclideanDistance(query, vector);
            default:
                throw new IndexException("Unsupported metric: " + metric);
        }
    }

    /**
     * Helper class to hold node ID and distance.
     */
    private static class NodeDistance {
        private final String id;
        private final double distance;

        NodeDistance(String id, double distance) {
            this.id = id;
            this.distance = distance;
        }

        String getId() {
            return id;
        }

        double getDistance() {
            return distance;
        }
    }
}
