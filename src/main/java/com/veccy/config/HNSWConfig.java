package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;

import java.util.Optional;

/**
 * Type-safe configuration for HNSW index.
 * <p>
 * HNSW (Hierarchical Navigable Small World) parameters:
 * - m: Number of bidirectional links per node (default: 16, range: 2-100)
 * - efConstruction: Dynamic candidate list size during construction (default: 200, range: 10-1000)
 * - efSearch: Dynamic candidate list size during search (default: 50, range: 10-1000)
 * - metric: Distance metric to use
 * - randomSeed: Optional seed for deterministic behavior
 */
public record HNSWConfig(
        int m,
        int efConstruction,
        int efSearch,
        Metric metric,
        Optional<Long> randomSeed
) {
    /**
     * Default M value.
     */
    public static final int DEFAULT_M = 16;

    /**
     * Default ef_construction value.
     */
    public static final int DEFAULT_EF_CONSTRUCTION = 200;

    /**
     * Default ef_search value.
     */
    public static final int DEFAULT_EF_SEARCH = 50;

    /**
     * Default metric.
     */
    public static final Metric DEFAULT_METRIC = Metric.COSINE;

    /**
     * Compact constructor with validation.
     */
    public HNSWConfig {
        if (m < 2 || m > 100) {
            throw new ConfigurationException("m must be between 2 and 100, got: " + m);
        }
        if (efConstruction < 10 || efConstruction > 1000) {
            throw new ConfigurationException("efConstruction must be between 10 and 1000, got: " + efConstruction);
        }
        if (efSearch < 10 || efSearch > 1000) {
            throw new ConfigurationException("efSearch must be between 10 and 1000, got: " + efSearch);
        }
        if (efSearch > efConstruction) {
            throw new ConfigurationException("efSearch (" + efSearch +
                    ") should not exceed efConstruction (" + efConstruction + ")");
        }
        if (metric == null) {
            throw new ConfigurationException("metric cannot be null");
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
    public static HNSWConfig defaults() {
        return new HNSWConfig(DEFAULT_M, DEFAULT_EF_CONSTRUCTION, DEFAULT_EF_SEARCH,
                DEFAULT_METRIC, Optional.empty());
    }

    /**
     * Builder for HNSWConfig.
     */
    public static class Builder {
        private int m = DEFAULT_M;
        private int efConstruction = DEFAULT_EF_CONSTRUCTION;
        private int efSearch = DEFAULT_EF_SEARCH;
        private Metric metric = DEFAULT_METRIC;
        private Long randomSeed = null;

        private Builder() {
        }

        /**
         * Set the number of bidirectional links per node.
         *
         * @param m number of links (2-100)
         * @return this builder
         */
        public Builder m(int m) {
            this.m = m;
            return this;
        }

        /**
         * Set the dynamic candidate list size during construction.
         *
         * @param efConstruction candidate list size (10-1000)
         * @return this builder
         */
        public Builder efConstruction(int efConstruction) {
            this.efConstruction = efConstruction;
            return this;
        }

        /**
         * Set the dynamic candidate list size during search.
         *
         * @param efSearch candidate list size (10-1000)
         * @return this builder
         */
        public Builder efSearch(int efSearch) {
            this.efSearch = efSearch;
            return this;
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
         * Set the random seed for deterministic behavior.
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
        public HNSWConfig build() {
            return new HNSWConfig(m, efConstruction, efSearch, metric,
                    Optional.ofNullable(randomSeed));
        }
    }
}
