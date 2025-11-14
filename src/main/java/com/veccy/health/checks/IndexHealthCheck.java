package com.veccy.health.checks;

import com.veccy.base.Index;
import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;

import java.util.Map;

/**
 * Health check for index.
 */
public class IndexHealthCheck implements HealthCheck {

    private final Index index;

    public IndexHealthCheck(Index index) {
        this.index = index;
    }

    @Override
    public String getName() {
        return "index";
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
            // Check if index is initialized
            if (!index.isInitialized()) {
                return HealthCheckResult.down("Index not initialized");
            }

            // Get index stats to verify it's accessible
            Map<String, Object> stats = index.getStats();

            if (stats == null) {
                return HealthCheckResult.degraded("Index stats unavailable");
            }

            // Check if index has vector count
            Object vectorCount = stats.get("vector_count");
            if (vectorCount == null) {
                return HealthCheckResult.degraded("Index missing vector count");
            }

            return HealthCheckResult.builder()
                    .status(HealthStatus.UP)
                    .message("Index is operational")
                    .detail("vector_count", vectorCount)
                    .detail("initialized", true)
                    .build();

        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Index health check failed")
                    .error(e)
                    .build();
        }
    }
}
