package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;

import java.util.Optional;

/**
 * Type-safe configuration for IVF (Inverted File) index.
 * <p>
 * IVF partitions the vector space into clusters using K-means,
 * creating an inverted index for efficient approximate search.
 * <p>
 * Parameters:
 * - metric: Distance metric to use (default: COSINE)
 * - numClusters: Number of K-means clusters (default: 100, range: 1-10000)
 * - numProbes: Number of clusters to probe during search (default: 10, range: 1-numClusters)
 * - maxIterations: Maximum K-means iterations (default: 100, range: 1-1000)
 * - convergenceThreshold: K-means convergence threshold (default: 0.001, range: 0.0-1.0)
 * - randomSeed: Optional seed for deterministic K-means initialization
 *
 * @param metric distance metric to use
 * @param numClusters number of K-means clusters (1-10000)
 * @param numProbes number of clusters to probe during search (1-numClusters)
 * @param maxIterations maximum K-means iterations (1-1000)
 * @param convergenceThreshold K-means convergence threshold (0.0-1.0)
 * @param randomSeed optional seed for deterministic K-means initialization
 */
public record IVFConfig(
        Metric metric,
        int numClusters,
        int numProbes,
        int maxIterations,
        double convergenceThreshold,
        Optional<Long> randomSeed
) {
    /**
     * Default metric.
     */
    public static final Metric DEFAULT_METRIC = Metric.COSINE;

    /**
     * Default number of clusters.
     */
    public static final int DEFAULT_NUM_CLUSTERS = 100;

    /**
     * Default number of probes.
     */
    public static final int DEFAULT_NUM_PROBES = 10;

    /**
     * Default maximum iterations.
     */
    public static final int DEFAULT_MAX_ITERATIONS = 100;

    /**
     * Default convergence threshold.
     */
    public static final double DEFAULT_CONVERGENCE_THRESHOLD = 0.001;

    /**
     * Compact constructor with validation.
     */
    public IVFConfig {
        if (metric == null) {
            throw new ConfigurationException("metric cannot be null");
        }
        if (numClusters < 1 || numClusters > 10000) {
            throw new ConfigurationException("numClusters must be between 1 and 10000, got: " + numClusters);
        }
        if (numProbes < 1 || numProbes > numClusters) {
            throw new ConfigurationException("numProbes must be between 1 and numClusters (" + numClusters + "), got: " + numProbes);
        }
        if (maxIterations < 1 || maxIterations > 1000) {
            throw new ConfigurationException("maxIterations must be between 1 and 1000, got: " + maxIterations);
        }
        if (convergenceThreshold < 0.0 || convergenceThreshold > 1.0) {
            throw new ConfigurationException("convergenceThreshold must be between 0.0 and 1.0, got: " + convergenceThreshold);
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
    public static IVFConfig defaults() {
        return new IVFConfig(DEFAULT_METRIC, DEFAULT_NUM_CLUSTERS, DEFAULT_NUM_PROBES,
                DEFAULT_MAX_ITERATIONS, DEFAULT_CONVERGENCE_THRESHOLD, Optional.empty());
    }

    /**
     * Builder for IVFConfig.
     */
    public static class Builder {
        private Metric metric = DEFAULT_METRIC;
        private int numClusters = DEFAULT_NUM_CLUSTERS;
        private int numProbes = DEFAULT_NUM_PROBES;
        private int maxIterations = DEFAULT_MAX_ITERATIONS;
        private double convergenceThreshold = DEFAULT_CONVERGENCE_THRESHOLD;
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
         * Set the number of K-means clusters.
         *
         * @param numClusters number of clusters (1-10000)
         * @return this builder
         */
        public Builder numClusters(int numClusters) {
            this.numClusters = numClusters;
            return this;
        }

        /**
         * Set the number of clusters to probe during search.
         *
         * @param numProbes number of probes (1-numClusters)
         * @return this builder
         */
        public Builder numProbes(int numProbes) {
            this.numProbes = numProbes;
            return this;
        }

        /**
         * Set the maximum K-means iterations.
         *
         * @param maxIterations maximum iterations (1-1000)
         * @return this builder
         */
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        /**
         * Set the K-means convergence threshold.
         *
         * @param convergenceThreshold threshold (0.0-1.0)
         * @return this builder
         */
        public Builder convergenceThreshold(double convergenceThreshold) {
            this.convergenceThreshold = convergenceThreshold;
            return this;
        }

        /**
         * Set the random seed for deterministic K-means initialization.
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
        public IVFConfig build() {
            return new IVFConfig(metric, numClusters, numProbes, maxIterations,
                    convergenceThreshold, Optional.ofNullable(randomSeed));
        }
    }
}
