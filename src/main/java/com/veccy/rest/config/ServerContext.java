package com.veccy.rest.config;

import com.veccy.base.VectorDB;
import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckRegistry;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-wide context for managing database instances and shared resources.
 */
public class ServerContext {
    private static final Logger logger = LoggerFactory.getLogger(ServerContext.class);

    private final Map<String, VectorDB> databases;
    private final Map<String, DatabaseMetadata> databaseMetadata;
    private final HealthCheckRegistry healthCheckRegistry;
    private final RestConfig config;

    public ServerContext(RestConfig config) {
        this.config = config;
        this.databases = new ConcurrentHashMap<>();
        this.databaseMetadata = new ConcurrentHashMap<>();
        this.healthCheckRegistry = new HealthCheckRegistry();

        // Register default health checks
        registerDefaultHealthChecks();
    }

    /**
     * Register a database instance with a name and metadata.
     */
    public void registerDatabase(String name, VectorDB database, DatabaseMetadata metadata) {
        databases.put(name, database);
        databaseMetadata.put(name, metadata);
        logger.info("Registered database '{}' with {} dimensions", name, metadata.getDimensions());
    }

    /**
     * Register a database instance with a name (backward compatibility).
     * @deprecated Use registerDatabase(String, VectorDB, DatabaseMetadata) instead
     */
    @Deprecated
    public void registerDatabase(String name, VectorDB database) {
        databases.put(name, database);
        logger.warn("Database '{}' registered without metadata - dimension validation will not be available", name);
    }

    /**
     * Get a database by name.
     */
    public VectorDB getDatabase(String name) {
        return databases.get(name);
    }

    /**
     * Get all registered database names.
     */
    public Iterable<String> getDatabaseNames() {
        return databases.keySet();
    }

    /**
     * Check if a database exists.
     */
    public boolean hasDatabase(String name) {
        return databases.containsKey(name);
    }

    /**
     * Get database metadata by name.
     */
    public DatabaseMetadata getMetadata(String name) {
        return databaseMetadata.get(name);
    }

    /**
     * Remove a database.
     */
    public VectorDB removeDatabase(String name) {
        databaseMetadata.remove(name);
        return databases.remove(name);
    }

    /**
     * Get the health check registry.
     */
    public HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }

    /**
     * Get the REST configuration.
     */
    public RestConfig getConfig() {
        return config;
    }

    /**
     * Register default health checks for the server.
     */
    private void registerDefaultHealthChecks() {
        // 1. Memory health check
        healthCheckRegistry.register(new HealthCheck() {
            @Override
            public String getName() {
                return "memory";
            }

            @Override
            public String getCategory() {
                return "system";
            }

            @Override
            public HealthCheckResult check() {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;

                double usagePercent = (double) usedMemory / maxMemory * 100;

                HealthStatus status;
                if (usagePercent > 90) {
                    status = HealthStatus.DOWN;
                } else if (usagePercent > 75) {
                    status = HealthStatus.DEGRADED;
                } else {
                    status = HealthStatus.UP;
                }

                return HealthCheckResult.builder()
                    .status(status)
                    .message(String.format("Memory usage: %.1f%%", usagePercent))
                    .detail("used_mb", usedMemory / 1024 / 1024)
                    .detail("max_mb", maxMemory / 1024 / 1024)
                    .detail("free_mb", freeMemory / 1024 / 1024)
                    .detail("usage_percent", usagePercent)
                    .build();
            }
        });

        // 2. Database health check
        healthCheckRegistry.register(new HealthCheck() {
            @Override
            public String getName() {
                return "databases";
            }

            @Override
            public String getCategory() {
                return "application";
            }

            @Override
            public HealthCheckResult check() {
                try {
                    int totalDbs = 0;
                    int initializedDbs = 0;

                    for (Map.Entry<String, VectorDB> entry : databases.entrySet()) {
                        totalDbs++;
                        if (entry.getValue().isInitialized()) {
                            initializedDbs++;
                        }
                    }

                    HealthStatus status = (totalDbs == 0)
                        ? HealthStatus.UP  // No databases is OK
                        : (initializedDbs == totalDbs)
                            ? HealthStatus.UP
                            : HealthStatus.DEGRADED;

                    return HealthCheckResult.builder()
                        .status(status)
                        .message(String.format("%d/%d databases healthy", initializedDbs, totalDbs))
                        .detail("total_databases", totalDbs)
                        .detail("initialized_databases", initializedDbs)
                        .detail("database_names", new ArrayList<>(databases.keySet()))
                        .build();

                } catch (Exception e) {
                    return HealthCheckResult.builder()
                        .status(HealthStatus.DOWN)
                        .message("Database check failed")
                        .error(e)
                        .build();
                }
            }
        });

        // 3. Disk space health check
        healthCheckRegistry.register(new HealthCheck() {
            @Override
            public String getName() {
                return "disk_space";
            }

            @Override
            public String getCategory() {
                return "system";
            }

            @Override
            public HealthCheckResult check() {
                try {
                    File dataDir = new File("./veccy_data");
                    if (!dataDir.exists()) {
                        return HealthCheckResult.builder()
                            .status(HealthStatus.UP)
                            .message("Data directory not in use (memory-only mode)")
                            .build();
                    }

                    long freeSpace = dataDir.getFreeSpace();
                    long totalSpace = dataDir.getTotalSpace();
                    double freePercent = (double) freeSpace / totalSpace * 100;

                    HealthStatus status;
                    if (freePercent < 10) {
                        status = HealthStatus.DOWN;
                    } else if (freePercent < 20) {
                        status = HealthStatus.DEGRADED;
                    } else {
                        status = HealthStatus.UP;
                    }

                    return HealthCheckResult.builder()
                        .status(status)
                        .message(String.format("Disk space: %.1f%% free", freePercent))
                        .detail("free_mb", freeSpace / 1024 / 1024)
                        .detail("total_mb", totalSpace / 1024 / 1024)
                        .detail("free_percent", freePercent)
                        .build();

                } catch (Exception e) {
                    return HealthCheckResult.builder()
                        .status(HealthStatus.UNKNOWN)
                        .message("Unable to check disk space")
                        .error(e)
                        .build();
                }
            }
        });

        logger.info("Registered {} default health checks", healthCheckRegistry.getHealthCheckNames().size());
    }

    /**
     * Close all databases and clean up resources.
     */
    public void shutdown() {
        logger.info("Shutting down server context, closing {} databases", databases.size());
        databases.values().forEach(db -> {
            try {
                db.close();
            } catch (Exception e) {
                logger.error("Error closing database: {}", e.getMessage(), e);
            }
        });
        databases.clear();
        databaseMetadata.clear();
    }
}
