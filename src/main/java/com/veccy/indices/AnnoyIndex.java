package com.veccy.indices;

import com.veccy.base.AbstractIndex;
import com.veccy.base.SearchResult;
import com.veccy.config.AnnoyConfig;
import com.veccy.exceptions.IndexException;
import com.veccy.utils.SimilarityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Annoy Index (Approximate Nearest Neighbors Oh Yeah) for efficient similarity search.
 * <p>
 * Annoy builds a forest of random projection trees where each tree recursively
 * splits the vector space using random hyperplanes. During search, the algorithm
 * traverses the trees to find candidate vectors in nearby leaf nodes.
 * <p>
 * Algorithm:
 * 1. Build: Create multiple random projection trees by recursively splitting
 *    the space with random hyperplanes until leaf size is reached
 * 2. Search: Traverse each tree to leaf nodes, collect candidates, compute
 *    exact distances, and return top-k results
 * <p>
 * Example usage:
 * <pre>{@code
 * AnnoyConfig config = AnnoyConfig.builder()
 *     .metric(Metric.COSINE)
 *     .numTrees(10)
 *     .maxLeafSize(10)
 *     .build();
 * AnnoyIndex index = new AnnoyIndex(config);
 * }</pre>
 */
public class AnnoyIndex extends AbstractIndex {

    private static final Logger logger = LoggerFactory.getLogger(AnnoyIndex.class);

    private final AnnoyConfig config;
    private final String metricName;
    private final int numTrees;
    private final int maxLeafSize;
    private int searchK;  // Computed during build if default (-1)
    private int dimensions;

    // Forest of trees
    private List<TreeNode> forest;

    // Vector ID to index mapping
    private Map<String, Integer> vectorIdToIndex;
    private List<String> indexToVectorId;

    // Cached vectors for tree building
    private List<double[]> vectors;

    private volatile boolean built;

    /**
     * Create a new AnnoyIndex with type-safe configuration.
     *
     * @param config the Annoy index configuration
     */
    public AnnoyIndex(AnnoyConfig config) {
        super();
        this.config = config;
        this.metricName = config.metric().getValue();
        this.numTrees = config.numTrees();
        this.maxLeafSize = config.maxLeafSize();
        this.searchK = config.searchK();
        this.vectorIdToIndex = new ConcurrentHashMap<>();
        this.indexToVectorId = new ArrayList<>();
        this.vectors = new ArrayList<>();
        this.built = false;
    }

    /**
     * Create a builder for AnnoyConfig.
     *
     * @return a new builder instance
     */
    public static AnnoyConfig.Builder builder() {
        return AnnoyConfig.builder();
    }

    @Override
    protected void doInitialize() throws Exception {
        logger.info("Annoy index initialized: trees={}, max_leaf_size={}, metric={}",
                numTrees, maxLeafSize, metricName);
    }

    @Override
    public List<String> insert(double[][] vectors, List<Map<String, Object>> metadata) {
        ensureInitialized();

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < vectors.length; i++) {
            // Store in storage backend
            String id = UUID.randomUUID().toString();
            Map<String, Object> meta = (metadata != null && i < metadata.size()) ? metadata.get(i) : null;

            if (storageBackend.storeVector(id, vectors[i], meta)) {
                // Add to internal structure
                int index = this.vectors.size();
                this.vectors.add(vectors[i].clone());
                this.vectorIdToIndex.put(id, index);
                this.indexToVectorId.add(id);
                ids.add(id);

                if (dimensions == 0) {
                    dimensions = vectors[i].length;
                }
            }
        }

        // Mark as not built since we added new vectors
        built = false;

        logger.debug("Inserted {} vectors into Annoy index (needs rebuild)", ids.size());
        return ids;
    }

    /**
     * Build the forest of random projection trees.
     * Must be called after inserting vectors and before searching.
     */
    public void build() {
        ensureInitialized();

        if (vectors.isEmpty()) {
            throw new IndexException("No vectors to build index with");
        }

        logger.info("Building Annoy index with {} trees and {} vectors", numTrees, vectors.size());

        forest = new ArrayList<>();

        for (int t = 0; t < numTrees; t++) {
            List<Integer> allIndices = new ArrayList<>();
            for (int i = 0; i < vectors.size(); i++) {
                allIndices.add(i);
            }

            TreeNode root = buildTree(allIndices, 0);
            forest.add(root);

            logger.debug("Built tree {}/{}", t + 1, numTrees);
        }

        // Auto-compute search_k if not specified
        if (searchK <= 0) {
            searchK = numTrees * maxLeafSize;
        }

        built = true;
        logger.info("Annoy index build complete");
    }

    @Override
    public List<SearchResult> search(double[] queryVector, int k) {
        ensureInitialized();

        if (!built) {
            // Auto-build if not built yet
            logger.info("Auto-building Annoy index before search");
            build();
        }

        // Collect candidates from all trees
        Set<Integer> candidateIndices = new HashSet<>();
        PriorityQueue<NodeDistance> nodesToSearch = new PriorityQueue<>(
                Comparator.comparingDouble(NodeDistance::getDistance));

        // Initialize with root nodes
        for (TreeNode root : forest) {
            double dist = computeDistanceToHyperplane(queryVector, root);
            nodesToSearch.offer(new NodeDistance(root, Math.abs(dist)));
        }

        // Priority search
        int nodesSearched = 0;
        while (!nodesToSearch.isEmpty() && nodesSearched < searchK) {
            NodeDistance current = nodesToSearch.poll();
            TreeNode node = current.getNode();
            nodesSearched++;

            if (node.isLeaf()) {
                // Add all vectors in leaf
                candidateIndices.addAll(node.getVectorIndices());
            } else {
                // Determine which child to prioritize
                double dist = computeDistanceToHyperplane(queryVector, node);

                if (dist >= 0) {
                    // Query is on right side, prioritize right
                    if (node.getRight() != null) {
                        nodesToSearch.offer(new NodeDistance(node.getRight(), 0.0));
                    }
                    if (node.getLeft() != null) {
                        nodesToSearch.offer(new NodeDistance(node.getLeft(), Math.abs(dist)));
                    }
                } else {
                    // Query is on left side, prioritize left
                    if (node.getLeft() != null) {
                        nodesToSearch.offer(new NodeDistance(node.getLeft(), 0.0));
                    }
                    if (node.getRight() != null) {
                        nodesToSearch.offer(new NodeDistance(node.getRight(), Math.abs(dist)));
                    }
                }
            }
        }

        // Compute exact distances for candidates
        List<SearchResult> results = new ArrayList<>();
        for (int index : candidateIndices) {
            String id = indexToVectorId.get(index);
            storageBackend.retrieveVector(id).ifPresent(vectorWithMetadata -> {
                double distance = computeDistance(queryVector, vectorWithMetadata.getVector());
                results.add(new SearchResult(id, distance, vectorWithMetadata.getMetadata()));
            });
        }

        // Sort and return top k
        results.sort(Comparator.comparingDouble(SearchResult::getDistance));
        int resultSize = Math.min(k, results.size());

        logger.debug("Annoy search: searched {} nodes, {} candidates, returning {} results",
                nodesSearched, candidateIndices.size(), resultSize);

        return results.subList(0, resultSize);
    }

    @Override
    public boolean delete(List<String> ids) {
        ensureInitialized();

        boolean allSuccess = true;

        for (String id : ids) {
            Integer index = vectorIdToIndex.remove(id);
            if (index != null) {
                // Mark as deleted (set to null to preserve indices)
                vectors.set(index, null);
            }

            if (!storageBackend.deleteVector(id)) {
                allSuccess = false;
            }
        }

        // Mark as not built since structure changed
        built = false;

        logger.debug("Deleted {} vectors from Annoy index (needs rebuild)", ids.size());
        return allSuccess;
    }

    @Override
    public boolean update(String id, double[] vector, Map<String, Object> metadata) {
        ensureInitialized();

        // Update in storage
        if (!storageBackend.updateVector(id, vector, metadata)) {
            return false;
        }

        // Update internal vector
        if (vector != null) {
            Integer index = vectorIdToIndex.get(id);
            if (index != null) {
                vectors.set(index, vector.clone());
            }
        }

        // Mark as not built since vector changed
        built = false;

        logger.debug("Updated vector with ID: {} (needs rebuild)", id);
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
        stats.put("type", "AnnoyIndex");
        stats.put("index_type", "annoy");
        stats.put("metric", metricName);
        stats.put("num_trees", numTrees);
        stats.put("max_leaf_size", maxLeafSize);
        stats.put("search_k", searchK);
        stats.put("built", built);
        stats.put("vector_count", vectorIdToIndex.size());
        stats.put("initialized", isInitialized());

        if (built && forest != null) {
            stats.put("dimensions", dimensions);

            // Tree statistics
            Map<String, Object> treeStats = new HashMap<>();
            int totalNodes = 0;
            int totalLeaves = 0;
            int maxDepth = 0;

            for (TreeNode root : forest) {
                TreeStatistics treeStat = computeTreeStatistics(root, 0);
                totalNodes += treeStat.nodeCount;
                totalLeaves += treeStat.leafCount;
                maxDepth = Math.max(maxDepth, treeStat.maxDepth);
            }

            treeStats.put("total_nodes", totalNodes);
            treeStats.put("total_leaves", totalLeaves);
            treeStats.put("avg_nodes_per_tree", totalNodes / (double) numTrees);
            treeStats.put("avg_leaves_per_tree", totalLeaves / (double) numTrees);
            treeStats.put("max_depth", maxDepth);

            stats.put("tree_stats", treeStats);
        }

        return stats;
    }

    public boolean isBuilt() {
        return built;
    }

    @Override
    protected void doClose() {
        if (forest != null) {
            forest.clear();
        }
        if (vectors != null) {
            vectors.clear();
        }
        if (vectorIdToIndex != null) {
            vectorIdToIndex.clear();
        }
        if (indexToVectorId != null) {
            indexToVectorId.clear();
        }
        built = false;
        logger.info("Annoy index closed");
    }

    /**
     * Recursively build a random projection tree.
     */
    private TreeNode buildTree(List<Integer> indices, int depth) {
        // Base case: create leaf node
        if (indices.size() <= maxLeafSize) {
            return new TreeNode(indices);
        }

        // Choose random hyperplane
        double[] hyperplane = generateRandomHyperplane();

        // Split vectors based on hyperplane
        List<Integer> leftIndices = new ArrayList<>();
        List<Integer> rightIndices = new ArrayList<>();

        for (int idx : indices) {
            if (vectors.get(idx) == null) {
                continue;  // Skip deleted vectors
            }

            double dotProduct = 0.0;
            for (int d = 0; d < dimensions; d++) {
                dotProduct += vectors.get(idx)[d] * hyperplane[d];
            }

            if (dotProduct >= 0) {
                rightIndices.add(idx);
            } else {
                leftIndices.add(idx);
            }
        }

        // Handle edge case where all vectors go to one side
        if (leftIndices.isEmpty() || rightIndices.isEmpty()) {
            return new TreeNode(indices);
        }

        // Create internal node and recurse
        TreeNode node = new TreeNode(hyperplane);
        node.setLeft(buildTree(leftIndices, depth + 1));
        node.setRight(buildTree(rightIndices, depth + 1));

        return node;
    }

    /**
     * Generate a random hyperplane (unit vector).
     */
    private double[] generateRandomHyperplane() {
        double[] hyperplane = new double[dimensions];
        Random random = ThreadLocalRandom.current();

        // Sample from normal distribution
        for (int d = 0; d < dimensions; d++) {
            hyperplane[d] = random.nextGaussian();
        }

        // Normalize
        double norm = 0.0;
        for (double v : hyperplane) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int d = 0; d < dimensions; d++) {
                hyperplane[d] /= norm;
            }
        }

        return hyperplane;
    }

    /**
     * Compute signed distance from query to hyperplane.
     */
    private double computeDistanceToHyperplane(double[] query, TreeNode node) {
        if (node.isLeaf()) {
            return 0.0;
        }

        double[] hyperplane = node.getHyperplane();
        double dotProduct = 0.0;

        for (int d = 0; d < dimensions; d++) {
            dotProduct += query[d] * hyperplane[d];
        }

        return dotProduct;
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

    /**
     * Compute statistics for a tree.
     */
    private TreeStatistics computeTreeStatistics(TreeNode node, int depth) {
        if (node.isLeaf()) {
            return new TreeStatistics(1, 1, depth);
        }

        TreeStatistics leftStats = computeTreeStatistics(node.getLeft(), depth + 1);
        TreeStatistics rightStats = computeTreeStatistics(node.getRight(), depth + 1);

        int nodeCount = 1 + leftStats.nodeCount + rightStats.nodeCount;
        int leafCount = leftStats.leafCount + rightStats.leafCount;
        int maxDepth = Math.max(leftStats.maxDepth, rightStats.maxDepth);

        return new TreeStatistics(nodeCount, leafCount, maxDepth);
    }

    /**
     * Tree node in the random projection forest.
     */
    private static class TreeNode {
        private double[] hyperplane;  // For internal nodes
        private List<Integer> vectorIndices;  // For leaf nodes
        private TreeNode left;
        private TreeNode right;

        // Leaf constructor
        TreeNode(List<Integer> vectorIndices) {
            this.vectorIndices = new ArrayList<>(vectorIndices);
        }

        // Internal node constructor
        TreeNode(double[] hyperplane) {
            this.hyperplane = hyperplane;
        }

        boolean isLeaf() {
            return vectorIndices != null;
        }

        double[] getHyperplane() {
            return hyperplane;
        }

        List<Integer> getVectorIndices() {
            return vectorIndices;
        }

        TreeNode getLeft() {
            return left;
        }

        void setLeft(TreeNode left) {
            this.left = left;
        }

        TreeNode getRight() {
            return right;
        }

        void setRight(TreeNode right) {
            this.right = right;
        }
    }

    /**
     * Helper class for priority queue during search.
     */
    private static class NodeDistance {
        private final TreeNode node;
        private final double distance;

        NodeDistance(TreeNode node, double distance) {
            this.node = node;
            this.distance = distance;
        }

        TreeNode getNode() {
            return node;
        }

        double getDistance() {
            return distance;
        }
    }

    /**
     * Helper class for tree statistics.
     */
    private static class TreeStatistics {
        final int nodeCount;
        final int leafCount;
        final int maxDepth;

        TreeStatistics(int nodeCount, int leafCount, int maxDepth) {
            this.nodeCount = nodeCount;
            this.leafCount = leafCount;
            this.maxDepth = maxDepth;
        }
    }
}
