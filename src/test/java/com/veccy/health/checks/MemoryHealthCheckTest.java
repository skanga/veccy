package com.veccy.health.checks;

import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MemoryHealthCheck.
 */
class MemoryHealthCheckTest {

    private MemoryHealthCheck healthCheck;

    @BeforeEach
    void setUp() {
        healthCheck = new MemoryHealthCheck();
    }

    @Test
    void testGetName() {
        assertEquals("memory", healthCheck.getName());
    }

    @Test
    void testGetCategory() {
        assertEquals("system", healthCheck.getCategory());
    }

    @Test
    void testIsCritical() {
        assertFalse(healthCheck.isCritical());
    }

    @Test
    void testCheckReturnsResult() {
        HealthCheckResult result = healthCheck.check();

        assertNotNull(result);
        assertNotNull(result.getStatus());
        assertNotNull(result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testCheckIncludesMemoryDetails() {
        HealthCheckResult result = healthCheck.check();

        assertTrue(result.getDetails().containsKey("total_memory"));
        assertTrue(result.getDetails().containsKey("free_memory"));
        assertTrue(result.getDetails().containsKey("used_memory"));
        assertTrue(result.getDetails().containsKey("max_memory"));
        assertTrue(result.getDetails().containsKey("usage_percent"));
    }

    @Test
    void testCheckMemoryValuesAreValid() {
        HealthCheckResult result = healthCheck.check();

        long totalMemory = (Long) result.getDetails().get("total_memory");
        long freeMemory = (Long) result.getDetails().get("free_memory");
        long usedMemory = (Long) result.getDetails().get("used_memory");
        long maxMemory = (Long) result.getDetails().get("max_memory");

        assertTrue(totalMemory > 0);
        assertTrue(freeMemory >= 0);
        assertTrue(usedMemory >= 0);
        assertTrue(maxMemory > 0);
        assertTrue(totalMemory <= maxMemory);
        assertEquals(totalMemory - freeMemory, usedMemory);
    }

    @Test
    void testCheckUsagePercentIsValid() {
        HealthCheckResult result = healthCheck.check();

        String usagePercentStr = (String) result.getDetails().get("usage_percent");
        double usagePercent = Double.parseDouble(usagePercentStr);

        assertTrue(usagePercent >= 0.0);
        assertTrue(usagePercent <= 100.0);
    }

    @Test
    void testCheckStatusIsOneOfExpected() {
        HealthCheckResult result = healthCheck.check();
        HealthStatus status = result.getStatus();

        assertTrue(
            status == HealthStatus.UP ||
            status == HealthStatus.DEGRADED ||
            status == HealthStatus.DOWN
        );
    }

    @Test
    void testCheckMessageContainsPercentage() {
        HealthCheckResult result = healthCheck.check();

        assertTrue(result.getMessage().contains("%"));
        assertTrue(result.getMessage().contains("Memory usage"));
    }

    @Test
    void testCheckMultipleTimes() {
        HealthCheckResult result1 = healthCheck.check();
        HealthCheckResult result2 = healthCheck.check();
        HealthCheckResult result3 = healthCheck.check();

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        // All should return valid results
        assertTrue(result1.getStatus() == HealthStatus.UP ||
                   result1.getStatus() == HealthStatus.DEGRADED ||
                   result1.getStatus() == HealthStatus.DOWN);
    }

    @Test
    void testCheckAlwaysSucceeds() {
        // Memory check should not throw exceptions
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                healthCheck.check();
            }
        });
    }

    @Test
    void testCheckResultTimestamp() {
        long before = System.currentTimeMillis();
        HealthCheckResult result = healthCheck.check();
        long after = System.currentTimeMillis();

        long timestamp = result.getTimestamp().toEpochMilli();
        assertTrue(timestamp >= before);
        assertTrue(timestamp <= after);
    }

    @Test
    void testCheckStatusReflectsUsage() {
        HealthCheckResult result = healthCheck.check();
        String usagePercentStr = (String) result.getDetails().get("usage_percent");
        double usagePercent = Double.parseDouble(usagePercentStr);
        HealthStatus status = result.getStatus();

        if (usagePercent < 80.0) {
            assertEquals(HealthStatus.UP, status);
            assertTrue(result.getMessage().contains("healthy"));
        } else if (usagePercent < 95.0) {
            assertEquals(HealthStatus.DEGRADED, status);
            assertTrue(result.getMessage().contains("elevated"));
        } else {
            assertEquals(HealthStatus.DOWN, status);
            assertTrue(result.getMessage().contains("critical"));
        }
    }

    @Test
    void testCheckDetailsTypesAreCorrect() {
        HealthCheckResult result = healthCheck.check();

        assertInstanceOf(Long.class, result.getDetails().get("total_memory"));
        assertInstanceOf(Long.class, result.getDetails().get("free_memory"));
        assertInstanceOf(Long.class, result.getDetails().get("used_memory"));
        assertInstanceOf(Long.class, result.getDetails().get("max_memory"));
        assertInstanceOf(String.class, result.getDetails().get("usage_percent"));
    }

    @Test
    void testCheckDetailsCount() {
        HealthCheckResult result = healthCheck.check();

        assertEquals(5, result.getDetails().size());
    }

    @Test
    void testMultipleHealthCheckInstances() {
        MemoryHealthCheck check1 = new MemoryHealthCheck();
        MemoryHealthCheck check2 = new MemoryHealthCheck();

        HealthCheckResult result1 = check1.check();
        HealthCheckResult result2 = check2.check();

        assertNotNull(result1);
        assertNotNull(result2);

        // Both should have similar results (memory usage should be similar)
        long usage1 = (Long) result1.getDetails().get("used_memory");
        long usage2 = (Long) result2.getDetails().get("used_memory");

        // Memory usage should be within reasonable range
        assertTrue(Math.abs(usage1 - usage2) < 100 * 1024 * 1024); // Within 100MB
    }

    @Test
    void testCheckMessageFormat() {
        HealthCheckResult result = healthCheck.check();
        String message = result.getMessage();

        // Should match pattern: "Memory usage is ... (XX.X%)"
        assertTrue(message.matches("Memory usage is \\w+ \\(\\d+\\.\\d%\\)"));
    }

    @Test
    void testUsagePercentFormat() {
        HealthCheckResult result = healthCheck.check();
        String usagePercent = (String) result.getDetails().get("usage_percent");

        // Should have exactly 2 decimal places
        assertTrue(usagePercent.matches("\\d+\\.\\d{2}"));
    }
}
