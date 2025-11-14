package com.veccy.health.checks;

import com.veccy.health.HealthCheck;
import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;

import java.io.File;
import java.nio.file.Path;

/**
 * Health check for disk usage.
 */
public class DiskHealthCheck implements HealthCheck {

    private final Path directory;

    public DiskHealthCheck(Path directory) {
        this.directory = directory;
    }

    @Override
    public String getName() {
        return "disk";
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
            File dir = directory.toFile();

            if (!dir.exists()) {
                return HealthCheckResult.down("Directory does not exist: " + directory);
            }

            long totalSpace = dir.getTotalSpace();
            long freeSpace = dir.getFreeSpace();
            long usableSpace = dir.getUsableSpace();
            long usedSpace = totalSpace - freeSpace;

            // Calculate usage percentage
            double usagePercent = totalSpace > 0 ? (double) usedSpace / totalSpace * 100.0 : 0.0;

            // Determine status based on usage
            HealthStatus status;
            String message;
            if (usagePercent < 85.0) {
                status = HealthStatus.UP;
                message = String.format("Disk usage is healthy (%.1f%%)", usagePercent);
            } else if (usagePercent < 95.0) {
                status = HealthStatus.DEGRADED;
                message = String.format("Disk usage is elevated (%.1f%%)", usagePercent);
            } else {
                status = HealthStatus.DOWN;
                message = String.format("Disk usage is critical (%.1f%%)", usagePercent);
            }

            return HealthCheckResult.builder()
                    .status(status)
                    .message(message)
                    .detail("total_space", totalSpace)
                    .detail("free_space", freeSpace)
                    .detail("usable_space", usableSpace)
                    .detail("usage_percent", String.format("%.2f", usagePercent))
                    .detail("directory", directory.toString())
                    .build();

        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Disk health check failed")
                    .error(e)
                    .build();
        }
    }
}
