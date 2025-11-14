package com.veccy.health;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthCheckRegistry.
 */
class HealthCheckRegistryTest {

    private HealthCheckRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new HealthCheckRegistry();
    }

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.shutdown();
        }
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(registry);
    }

    @Test
    void testParameterizedConstructor() {
        HealthCheckRegistry customRegistry = new HealthCheckRegistry(10000, 60000);
        assertNotNull(customRegistry);
        customRegistry.shutdown();
    }

    @Test
    void testRegisterHealthCheck() {
        TestHealthCheck check = new TestHealthCheck("test", HealthStatus.UP);

        registry.register(check);

        assertTrue(registry.getHealthCheckNames().contains("test"));
    }

    @Test
    void testRegisterNullHealthCheckThrows() {
        assertThrows(NullPointerException.class, () -> {
            registry.register(null);
        });
    }

    @Test
    void testUnregisterHealthCheck() {
        TestHealthCheck check = new TestHealthCheck("test", HealthStatus.UP);
        registry.register(check);

        assertTrue(registry.getHealthCheckNames().contains("test"));

        registry.unregister("test");

        assertFalse(registry.getHealthCheckNames().contains("test"));
    }

    @Test
    void testUnregisterNonExistentCheck() {
        // Should not throw
        assertDoesNotThrow(() -> registry.unregister("nonexistent"));
    }

    @Test
    void testGetHealthCheckNames() {
        registry.register(new TestHealthCheck("check1", HealthStatus.UP));
        registry.register(new TestHealthCheck("check2", HealthStatus.UP));
        registry.register(new TestHealthCheck("check3", HealthStatus.UP));

        Set<String> names = registry.getHealthCheckNames();

        assertEquals(3, names.size());
        assertTrue(names.contains("check1"));
        assertTrue(names.contains("check2"));
        assertTrue(names.contains("check3"));
    }

    @Test
    void testRunHealthChecksWithNoChecks() {
        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertNotNull(result);
        assertEquals(HealthStatus.UNKNOWN, result.getOverallStatus());
        assertTrue(result.getResults().isEmpty());
        assertFalse(result.isHealthy());
    }

    @Test
    void testRunHealthChecksWithSingleCheck() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertNotNull(result);
        assertEquals(1, result.getResults().size());
        assertEquals(HealthStatus.UP, result.getOverallStatus());
        assertTrue(result.isHealthy());
    }

    @Test
    void testRunHealthChecksWithMultipleChecks() {
        registry.register(new TestHealthCheck("check1", HealthStatus.UP));
        registry.register(new TestHealthCheck("check2", HealthStatus.UP));
        registry.register(new TestHealthCheck("check3", HealthStatus.UP));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertEquals(3, result.getResults().size());
        assertEquals(HealthStatus.UP, result.getOverallStatus());
        assertTrue(result.isHealthy());
    }

    @Test
    void testRunHealthChecksAggregatesStatus() {
        registry.register(new TestHealthCheck("up", HealthStatus.UP));
        registry.register(new TestHealthCheck("degraded", HealthStatus.DEGRADED));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertEquals(HealthStatus.DEGRADED, result.getOverallStatus());
    }

    @Test
    void testRunHealthChecksWithFailure() {
        registry.register(new TestHealthCheck("up", HealthStatus.UP));
        registry.register(new TestHealthCheck("down", HealthStatus.DOWN));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertEquals(HealthStatus.DOWN, result.getOverallStatus());
        assertFalse(result.isHealthy());
    }

    @Test
    void testRunSingleHealthCheck() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HealthCheckResult result = registry.runHealthCheck("test");

        assertNotNull(result);
        assertEquals(HealthStatus.UP, result.getStatus());
    }

    @Test
    void testRunNonExistentHealthCheck() {
        HealthCheckResult result = registry.runHealthCheck("nonexistent");

        assertNotNull(result);
        assertEquals(HealthStatus.UNKNOWN, result.getStatus());
        assertTrue(result.getMessage().contains("not found"));
    }

    @Test
    void testCachingBehavior() {
        TestHealthCheck check = new TestHealthCheck("test", HealthStatus.UP);
        registry.register(check);

        // First call
        HealthCheckResult result1 = registry.runHealthCheck("test");
        assertEquals(1, check.getCallCount());

        // Second call (should be cached)
        HealthCheckResult result2 = registry.runHealthCheck("test");
        assertEquals(1, check.getCallCount()); // Still 1, used cache

        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    void testClearCache() {
        TestHealthCheck check = new TestHealthCheck("test", HealthStatus.UP);
        registry.register(check);

        registry.runHealthCheck("test");
        assertEquals(1, check.getCallCount());

        registry.clearCache();

        registry.runHealthCheck("test");
        assertEquals(2, check.getCallCount()); // Cache cleared, called again
    }

    @Test
    void testGetHealthChecksByCategory() {
        registry.register(new TestHealthCheck("db1", HealthStatus.UP, "database"));
        registry.register(new TestHealthCheck("db2", HealthStatus.UP, "database"));
        registry.register(new TestHealthCheck("mem1", HealthStatus.UP, "memory"));

        Map<String, List<HealthCheck>> byCategory = registry.getHealthChecksByCategory();

        assertEquals(2, byCategory.size());
        assertTrue(byCategory.containsKey("database"));
        assertTrue(byCategory.containsKey("memory"));
        assertEquals(2, byCategory.get("database").size());
        assertEquals(1, byCategory.get("memory").size());
    }

    @Test
    void testAggregatedResultGetDurationMs() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertTrue(result.getDurationMs() >= 0);
        assertTrue(result.getDurationMs() < 5000); // Should complete quickly
    }

    @Test
    void testAggregatedResultToMap() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();
        Map<String, Object> map = result.toMap();

        assertNotNull(map);
        assertTrue(map.containsKey("status"));
        assertTrue(map.containsKey("duration_ms"));
        assertTrue(map.containsKey("timestamp"));
        assertTrue(map.containsKey("checks"));

        assertEquals("UP", map.get("status"));
    }

    @Test
    void testAggregatedResultToMapWithMultipleChecks() {
        registry.register(new TestHealthCheck("check1", HealthStatus.UP));
        registry.register(new TestHealthCheck("check2", HealthStatus.DEGRADED));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();
        Map<String, Object> map = result.toMap();

        @SuppressWarnings("unchecked")
        Map<String, Object> checks = (Map<String, Object>) map.get("checks");

        assertEquals(2, checks.size());
        assertTrue(checks.containsKey("check1"));
        assertTrue(checks.containsKey("check2"));
    }

    @Test
    void testShutdown() {
        assertDoesNotThrow(() -> registry.shutdown());
    }

    @Test
    void testMultipleShutdowns() {
        registry.shutdown();
        // Second shutdown should not throw
        assertDoesNotThrow(() -> registry.shutdown());
    }

    @Test
    void testRunHealthChecksWithTimeout() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks(10000);

        assertNotNull(result);
        assertTrue(result.getDurationMs() < 10000);
    }

    @Test
    void testHealthCheckException() {
        registry.register(new FailingHealthCheck("failing"));

        HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

        assertNotNull(result);
        assertEquals(HealthStatus.DOWN, result.getOverallStatus());
    }

    @Test
    void testMultipleRegistrationsOverwrite() {
        registry.register(new TestHealthCheck("test", HealthStatus.UP));
        registry.register(new TestHealthCheck("test", HealthStatus.DOWN));

        HealthCheckResult result = registry.runHealthCheck("test");

        assertEquals(HealthStatus.DOWN, result.getStatus());
    }

    @Test
    void testEmptyNamesAfterUnregisterAll() {
        registry.register(new TestHealthCheck("check1", HealthStatus.UP));
        registry.register(new TestHealthCheck("check2", HealthStatus.UP));

        registry.unregister("check1");
        registry.unregister("check2");

        assertTrue(registry.getHealthCheckNames().isEmpty());
    }

    // Helper test health check
    private static class TestHealthCheck implements HealthCheck {
        private final String name;
        private final HealthStatus status;
        private final String category;
        private int callCount = 0;

        TestHealthCheck(String name, HealthStatus status) {
            this(name, status, "general");
        }

        TestHealthCheck(String name, HealthStatus status, String category) {
            this.name = name;
            this.status = status;
            this.category = category;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public HealthCheckResult check() {
            callCount++;
            return HealthCheckResult.builder()
                .status(status)
                .message("Test check result")
                .build();
        }

        int getCallCount() {
            return callCount;
        }
    }

    // Helper failing health check
    private static class FailingHealthCheck implements HealthCheck {
        private final String name;

        FailingHealthCheck(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public HealthCheckResult check() {
            throw new RuntimeException("Health check failed");
        }
    }
}
