package com.veccy.health.checks;

import com.veccy.client.VectorDBClient;
import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;

import java.util.Map;

/**
 * Health check for database client.
 */
public class DatabaseHealthCheck implements HealthCheck {

    private final VectorDBClient client;

    public DatabaseHealthCheck(VectorDBClient client) {
        this.client = client;
    }

    @Override
    public String getName() {
        return "database";
    }

    @Override
    public String getCategory() {
        return "database";
    }

    @Override
    public boolean isCritical() {
        return true;
    }

    @Override
    public HealthCheckResult check() {
        try {
            // Check if client is initialized
            if (!client.isInitialized()) {
                return HealthCheckResult.down("Database client not initialized");
            }

            // Get database stats to verify it's accessible
            Map<String, Object> stats = client.getStats();

            if (stats == null) {
                return HealthCheckResult.down("Database stats unavailable");
            }

            // Extract vector count from stats
            Object vectorCount = null;
            if (stats.containsKey("storage")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
                if (storageStats != null) {
                    vectorCount = storageStats.get("vector_count");
                }
            }

            HealthCheckResult.Builder resultBuilder = HealthCheckResult.builder()
                    .status(HealthStatus.UP)
                    .message("Database is operational")
                    .detail("initialized", true);

            if (vectorCount != null) {
                resultBuilder.detail("vector_count", vectorCount);
            }

            return resultBuilder.build();

        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Database health check failed")
                    .error(e)
                    .build();
        }
    }
}
