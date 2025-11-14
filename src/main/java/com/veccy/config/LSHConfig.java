package com.veccy.config;

import com.veccy.exceptions.ConfigurationException;

import java.util.Optional;

/**
 * Type-safe configuration for LSH (Locality-Sensitive Hashing) index.
 * <p>
 * LSH uses random projection hash functions to map similar vectors
 * to the same buckets with high probability.
 * <p>
 * Parameters:
 * - metric: Distance metric to use (default: COSINE)
 * - numTables: Number of hash tables (default: 5, range: 1-50)
 * - numHashBits: Number of hash bits per table (default: 8, range: 1-32)
 * - bucketWidth: Width for p-stable hashing (default: 4.0, range: 0.1-100.0)
 * - randomSeed: Optional seed for deterministic hash function generation
 *
 * @param metric distance metric to use
 * @param numTables number of hash tables (1-50)
 * @param numHashBits number of hash bits per table (1-32)
 * @param bucketWidth width for p-stable hashing (0.1-100.0)
 * @param randomSeed optional seed for deterministic hash function generation
 */
public record LSHConfig(
        Metric metric,
        int numTables,
        int numHashBits,
        double bucketWidth,
        Optional<Long> randomSeed
) {
    /**
     * Default metric.
     */
    public static final Metric DEFAULT_METRIC = Metric.COSINE;

    /**
     * Default number of tables.
     */
    public static final int DEFAULT_NUM_TABLES = 5;

    /**
     * Default number of hash bits.
     */
    public static final int DEFAULT_NUM_HASH_BITS = 8;

    /**
     * Default bucket width.
     */
    public static final double DEFAULT_BUCKET_WIDTH = 4.0;

    /**
     * Compact constructor with validation.
     */
    public LSHConfig {
        if (metric == null) {
            throw new ConfigurationException("metric cannot be null");
        }
        if (numTables < 1 || numTables > 50) {
            throw new ConfigurationException("numTables must be between 1 and 50, got: " + numTables);
        }
        if (numHashBits < 1 || numHashBits > 32) {
            throw new ConfigurationException("numHashBits must be between 1 and 32, got: " + numHashBits);
        }
        if (bucketWidth < 0.1 || bucketWidth > 100.0) {
            throw new ConfigurationException("bucketWidth must be between 0.1 and 100.0, got: " + bucketWidth);
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
    public static LSHConfig defaults() {
        return new LSHConfig(DEFAULT_METRIC, DEFAULT_NUM_TABLES, DEFAULT_NUM_HASH_BITS,
                DEFAULT_BUCKET_WIDTH, Optional.empty());
    }

    /**
     * Builder for LSHConfig.
     */
    public static class Builder {
        private Metric metric = DEFAULT_METRIC;
        private int numTables = DEFAULT_NUM_TABLES;
        private int numHashBits = DEFAULT_NUM_HASH_BITS;
        private double bucketWidth = DEFAULT_BUCKET_WIDTH;
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
         * Set the number of hash tables.
         *
         * @param numTables number of tables (1-50)
         * @return this builder
         */
        public Builder numTables(int numTables) {
            this.numTables = numTables;
            return this;
        }

        /**
         * Set the number of hash bits per table.
         *
         * @param numHashBits number of bits (1-32)
         * @return this builder
         */
        public Builder numHashBits(int numHashBits) {
            this.numHashBits = numHashBits;
            return this;
        }

        /**
         * Set the bucket width for p-stable hashing.
         *
         * @param bucketWidth width (0.1-100.0)
         * @return this builder
         */
        public Builder bucketWidth(double bucketWidth) {
            this.bucketWidth = bucketWidth;
            return this;
        }

        /**
         * Set the random seed for deterministic hash function generation.
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
        public LSHConfig build() {
            return new LSHConfig(metric, numTables, numHashBits, bucketWidth,
                    Optional.ofNullable(randomSeed));
        }
    }
}
