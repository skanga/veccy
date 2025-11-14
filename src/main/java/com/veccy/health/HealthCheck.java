package com.veccy.health;

/**
 * Interface for health checks.
 * Components implement this interface to report their health status.
 */
public interface HealthCheck {

    /**
     * Get the name of this health check.
     */
    String getName();

    /**
     * Perform the health check.
     *
     * @return the health check result
     */
    HealthCheckResult check();

    /**
     * Get the category of this health check.
     * Common categories: "database", "storage", "index", "memory", "disk"
     */
    default String getCategory() {
        return "general";
    }

    /**
     * Whether this check is critical for the system to be considered healthy.
     * Non-critical checks can fail without affecting overall health status.
     */
    default boolean isCritical() {
        return true;
    }
}
