package com.veccy.health;

/**
 * Health status levels.
 */
public enum HealthStatus {
    /**
     * System is fully operational.
     */
    UP,

    /**
     * System is operational but with degraded performance.
     */
    DEGRADED,

    /**
     * System is not operational.
     */
    DOWN,

    /**
     * Health status is unknown (check hasn't run yet or timed out).
     */
    UNKNOWN;

    /**
     * Check if this status indicates the system is healthy enough to serve requests.
     */
    public boolean isHealthy() {
        return this == UP || this == DEGRADED;
    }

    /**
     * Get the more severe of two statuses.
     */
    public static HealthStatus worst(HealthStatus a, HealthStatus b) {
        if (a == DOWN || b == DOWN) return DOWN;
        if (a == DEGRADED || b == DEGRADED) return DEGRADED;
        if (a == UNKNOWN || b == UNKNOWN) return UNKNOWN;
        return UP;
    }
}
