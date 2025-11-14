package com.veccy.health.checks;

import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;
import com.veccy.storage.StorageBackend;

import java.util.Map;

/**
 * Health check for storage backend.
 */
public class StorageHealthCheck implements HealthCheck {

    private final StorageBackend storage;

    public StorageHealthCheck(StorageBackend storage) {
        this.storage = storage;
    }

    @Override
    public String getName() {
        return "storage";
    }

    @Override
    public String getCategory() {
        return "database";
    }

    @Override
    public HealthCheckResult check() {
        try {
            // Get storage stats to verify it's accessible
            Map<String, Object> stats = storage.getStats();

            if (stats == null) {
                return HealthCheckResult.down("Storage stats unavailable");
            }

            // Check if storage is initialized
            Object vectorCount = stats.get("vector_count");
            if (vectorCount == null) {
                return HealthCheckResult.degraded("Storage missing vector count");
            }

            return HealthCheckResult.builder()
                    .status(HealthStatus.UP)
                    .message("Storage is operational")
                    .detail("type", stats.get("type"))
                    .detail("vector_count", vectorCount)
                    .build();

        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Storage health check failed")
                    .error(e)
                    .build();
        }
    }

    @Override
    public boolean isCritical() {
        return true;
    }
}
