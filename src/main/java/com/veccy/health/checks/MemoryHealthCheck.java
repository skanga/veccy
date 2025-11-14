package com.veccy.health.checks;

import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;

/**
 * Health check for memory usage.
 */
public class MemoryHealthCheck implements HealthCheck {

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public String getCategory() {
        return "system";
    }

    @Override
    public boolean isCritical() {
        return false;
    }

    @Override
    public HealthCheckResult check() {
        try {
            Runtime runtime = Runtime.getRuntime();

            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            // Calculate usage percentage
            double usagePercent = (double) usedMemory / maxMemory * 100.0;

            // Determine status based on usage
            HealthStatus status;
            String message;
            if (usagePercent < 80.0) {
                status = HealthStatus.UP;
                message = String.format("Memory usage is healthy (%.1f%%)", usagePercent);
            } else if (usagePercent < 95.0) {
                status = HealthStatus.DEGRADED;
                message = String.format("Memory usage is elevated (%.1f%%)", usagePercent);
            } else {
                status = HealthStatus.DOWN;
                message = String.format("Memory usage is critical (%.1f%%)", usagePercent);
            }

            return HealthCheckResult.builder()
                    .status(status)
                    .message(message)
                    .detail("total_memory", totalMemory)
                    .detail("free_memory", freeMemory)
                    .detail("used_memory", usedMemory)
                    .detail("max_memory", maxMemory)
                    .detail("usage_percent", String.format("%.2f", usagePercent))
                    .build();

        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Memory health check failed")
                    .error(e)
                    .build();
        }
    }
}
