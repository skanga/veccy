package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;

/**
 * Type-safe configuration for Flat index.
 * <p>
 * Flat index performs brute-force exhaustive search over all vectors.
 * It's simple, accurate, but can be slow for large datasets.
 * <p>
 * Parameters:
 * - metric: Distance metric to use (default: COSINE)
 *
 * @param metric distance metric to use
 */
public record FlatConfig(
        Metric metric
) {
    /**
     * Default metric.
     */
    public static final Metric DEFAULT_METRIC = Metric.COSINE;

    /**
     * Compact constructor with validation.
     */
    public FlatConfig {
        if (metric == null) {
            throw new ConfigurationException("metric cannot be null");
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
    public static FlatConfig defaults() {
        return new FlatConfig(DEFAULT_METRIC);
    }

    /**
     * Builder for FlatConfig.
     */
    public static class Builder {
        private Metric metric = DEFAULT_METRIC;

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
         * Build the configuration.
         *
         * @return validated configuration
         * @throws ConfigurationException if validation fails
         */
        public FlatConfig build() {
            return new FlatConfig(metric);
        }
    }
}
