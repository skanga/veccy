package com.veccy.health;

import com.veccy.client.VectorDBClient;
import com.veccy.health.checks.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized health management for Veccy.
 * Automatically registers appropriate health checks and manages the health endpoint.
 */
public class VeccyHealthManager implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(VeccyHealthManager.class);

    private final HealthCheckRegistry registry;
    private final HealthEndpoint endpoint;
    private final boolean endpointEnabled;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private VeccyHealthManager(Builder builder) {
        this.registry = new HealthCheckRegistry(
                builder.healthCheckTimeoutMs,
                builder.cacheTtlMs
        );
        this.endpointEnabled = builder.enableEndpoint;

        // Register health checks
        if (builder.client != null) {
            registry.register(new DatabaseHealthCheck(builder.client));

            if (builder.client.getStorageBackend() != null) {
                registry.register(new StorageHealthCheck(builder.client.getStorageBackend()));
            }

            if (builder.client.getIndex() != null) {
                registry.register(new IndexHealthCheck(builder.client.getIndex()));
            }
        }

        // Register system health checks
        if (builder.enableMemoryCheck) {
            registry.register(new MemoryHealthCheck());
        }

        if (builder.enableDiskCheck && builder.diskCheckPath != null) {
            registry.register(new DiskHealthCheck(builder.diskCheckPath));
        }

        // Start HTTP endpoint if enabled
        if (endpointEnabled) {
            this.endpoint = new HealthEndpoint(registry, builder.endpointPort);
            this.endpoint.start();
        } else {
            this.endpoint = null;
        }
    }

    /**
     * Get the health check registry.
     */
    public HealthCheckRegistry getRegistry() {
        return registry;
    }

    /**
     * Get the health endpoint (if enabled).
     */
    public HealthEndpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Run all health checks and get aggregated result.
     */
    public HealthCheckRegistry.AggregatedHealthCheckResult checkHealth() {
        return registry.runHealthChecks();
    }

    /**
     * Check if the system is healthy.
     */
    public boolean isHealthy() {
        return checkHealth().isHealthy();
    }

    /**
     * Get overall health status.
     */
    public HealthStatus getHealthStatus() {
        return checkHealth().getOverallStatus();
    }

    /**
     * Close the health manager and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                if (endpoint != null) {
                    try {
                        endpoint.close();
                    } catch (Exception e) {
                        logger.error("Error closing health endpoint: {}", e.getMessage(), e);
                    }
                }

                try {
                    registry.shutdown();
                } catch (Exception e) {
                    logger.error("Error shutting down health check registry: {}", e.getMessage(), e);
                }

                logger.info("Veccy health manager closed");
            } catch (Exception e) {
                logger.error("Error closing health manager: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Veccy health manager already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create default health manager for a VectorDBClient.
     */
    public static VeccyHealthManager forClient(VectorDBClient client) {
        return builder()
                .client(client)
                .enableMemoryCheck(true)
                .enableEndpoint(true)
                .endpointPort(0)
                .build();
    }

    /**
     * Builder for VeccyHealthManager.
     */
    public static class Builder {
        private VectorDBClient client;
        private boolean enableEndpoint = false;
        private int endpointPort = 8080;
        private boolean enableMemoryCheck = true;
        private boolean enableDiskCheck = false;
        private Path diskCheckPath;
        private long healthCheckTimeoutMs = 5000;
        private long cacheTtlMs = 30000;

        /**
         * Set the VectorDBClient to monitor.
         */
        public Builder client(VectorDBClient client) {
            this.client = client;
            return this;
        }

        /**
         * Enable HTTP health endpoint.
         */
        public Builder enableEndpoint(boolean enable) {
            this.enableEndpoint = enable;
            return this;
        }

        /**
         * Set health endpoint port.
         */
        public Builder endpointPort(int port) {
            this.endpointPort = port;
            return this;
        }

        /**
         * Enable memory health check.
         */
        public Builder enableMemoryCheck(boolean enable) {
            this.enableMemoryCheck = enable;
            return this;
        }

        /**
         * Enable disk health check.
         */
        public Builder enableDiskCheck(boolean enable) {
            this.enableDiskCheck = enable;
            return this;
        }

        /**
         * Set disk check path.
         */
        public Builder diskCheckPath(String path) {
            this.diskCheckPath = Paths.get(path);
            this.enableDiskCheck = true;
            return this;
        }

        /**
         * Set disk check path.
         */
        public Builder diskCheckPath(Path path) {
            this.diskCheckPath = path;
            this.enableDiskCheck = true;
            return this;
        }

        /**
         * Set health check timeout in milliseconds.
         */
        public Builder healthCheckTimeout(long timeoutMs) {
            this.healthCheckTimeoutMs = timeoutMs;
            return this;
        }

        /**
         * Set cache TTL in milliseconds.
         */
        public Builder cacheTtl(long ttlMs) {
            this.cacheTtlMs = ttlMs;
            return this;
        }

        /**
         * Build the VeccyHealthManager.
         */
        public VeccyHealthManager build() {
            return new VeccyHealthManager(this);
        }
    }
}
