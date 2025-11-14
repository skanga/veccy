package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;

import java.util.Optional;

/**
 * Type-safe configuration for Annoy (Approximate Nearest Neighbors Oh Yeah) index.
 * <p>
 * Annoy builds a forest of random projection trees for efficient
 * approximate nearest neighbor search.
 * <p>
 * Parameters:
 * - metric: Distance metric to use (default: COSINE)
 * - numTrees: Number of trees in the forest (default: 10, range: 1-1000)
 * - maxLeafSize: Maximum vectors per leaf node (default: 10, range: 1-1000)
 * - searchK: Number of nodes to search (default: -1 = auto, range: -1 or >= 1)
 * - randomSeed: Optional seed for deterministic tree construction
 */
public record AnnoyConfig(
        Metric metric,
        int numTrees,
        int maxLeafSize,
        int searchK,
        Optional<Long> randomSeed
) {
    /**
     * Default metric.
     */
    public static final Metric DEFAULT_METRIC = Metric.COSINE;

    /**
     * Default number of trees.
     */
    public static final int DEFAULT_NUM_TREES = 10;

    /**
     * Default maximum leaf size.
     */
    public static final int DEFAULT_MAX_LEAF_SIZE = 10;

    /**
     * Default search K (-1 means auto-compute).
     */
    public static final int DEFAULT_SEARCH_K = -1;

    /**
     * Compact constructor with validation.
     */
    public AnnoyConfig {
        if (metric == null) {
            throw new ConfigurationException("metric cannot be null");
        }
        if (numTrees < 1 || numTrees > 1000) {
            throw new ConfigurationException("numTrees must be between 1 and 1000, got: " + numTrees);
        }
        if (maxLeafSize < 1 || maxLeafSize > 1000) {
            throw new ConfigurationException("maxLeafSize must be between 1 and 1000, got: " + maxLeafSize);
        }
        if (searchK != -1 && searchK < 1) {
            throw new ConfigurationException("searchK must be -1 (auto) or >= 1, got: " + searchK);
        }
        if (randomSeed == null) {
            randomSeed = Optional.empty();
        }
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create default configuration.
     */
    public static AnnoyConfig defaults() {
        return new AnnoyConfig(DEFAULT_METRIC, DEFAULT_NUM_TREES, DEFAULT_MAX_LEAF_SIZE,
                DEFAULT_SEARCH_K, Optional.empty());
    }

    /**
     * Builder for AnnoyConfig.
     */
    public static class Builder {
        private Metric metric = DEFAULT_METRIC;
        private int numTrees = DEFAULT_NUM_TREES;
        private int maxLeafSize = DEFAULT_MAX_LEAF_SIZE;
        private int searchK = DEFAULT_SEARCH_K;
        private Long randomSeed = null;

        private Builder() {
        }

        /**
         * Set the distance metric.
         *
         * @param metric the metric to use
         * @return this builder
         */
        public Builder metric(Metric metric) {
            this.metric = metric;
            return this;
        }

        /**
         * Set the number of trees in the forest.
         *
         * @param numTrees number of trees (1-1000)
         * @return this builder
         */
        public Builder numTrees(int numTrees) {
            this.numTrees = numTrees;
            return this;
        }

        /**
         * Set the maximum vectors per leaf node.
         *
         * @param maxLeafSize maximum leaf size (1-1000)
         * @return this builder
         */
        public Builder maxLeafSize(int maxLeafSize) {
            this.maxLeafSize = maxLeafSize;
            return this;
        }

        /**
         * Set the number of nodes to search.
         *
         * @param searchK number of nodes (-1 for auto, or >= 1)
         * @return this builder
         */
        public Builder searchK(int searchK) {
            this.searchK = searchK;
            return this;
        }

        /**
         * Set the random seed for deterministic tree construction.
         *
         * @param randomSeed the seed value
         * @return this builder
         */
        public Builder randomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
            return this;
        }

        /**
         * Build the configuration.
         *
         * @return validated configuration
         * @throws ConfigurationException if validation fails
         */
        public AnnoyConfig build() {
            return new AnnoyConfig(metric, numTrees, maxLeafSize, searchK,
                    Optional.ofNullable(randomSeed));
        }
    }
}
