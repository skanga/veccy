package com.veccy.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthStatus enum.
 */
class HealthStatusTest {

    @Test
    void testEnumValues() {
        HealthStatus[] values = HealthStatus.values();

        assertEquals(4, values.length);
        assertEquals(HealthStatus.UP, values[0]);
        assertEquals(HealthStatus.DEGRADED, values[1]);
        assertEquals(HealthStatus.DOWN, values[2]);
        assertEquals(HealthStatus.UNKNOWN, values[3]);
    }

    @Test
    void testValueOf() {
        assertEquals(HealthStatus.UP, HealthStatus.valueOf("UP"));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.valueOf("DEGRADED"));
        assertEquals(HealthStatus.DOWN, HealthStatus.valueOf("DOWN"));
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.valueOf("UNKNOWN"));
    }

    @Test
    void testIsHealthyForUp() {
        assertTrue(HealthStatus.UP.isHealthy());
    }

    @Test
    void testIsHealthyForDegraded() {
        assertTrue(HealthStatus.DEGRADED.isHealthy());
    }

    @Test
    void testIsHealthyForDown() {
        assertFalse(HealthStatus.DOWN.isHealthy());
    }

    @Test
    void testIsHealthyForUnknown() {
        assertFalse(HealthStatus.UNKNOWN.isHealthy());
    }

    @Test
    void testWorstWithTwoDown() {
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.DOWN));
    }

    @Test
    void testWorstWithOneDown() {
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.UP));
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.UP, HealthStatus.DOWN));
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.DEGRADED));
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.UNKNOWN));
    }

    @Test
    void testWorstWithDegraded() {
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.UP));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.UP, HealthStatus.DEGRADED));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.DEGRADED));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.UNKNOWN));
    }

    @Test
    void testWorstWithUnknown() {
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.worst(HealthStatus.UNKNOWN, HealthStatus.UP));
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.worst(HealthStatus.UP, HealthStatus.UNKNOWN));
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.worst(HealthStatus.UNKNOWN, HealthStatus.UNKNOWN));
    }

    @Test
    void testWorstWithBothUp() {
        assertEquals(HealthStatus.UP, HealthStatus.worst(HealthStatus.UP, HealthStatus.UP));
    }

    @Test
    void testWorstPriority() {
        // DOWN is worst
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.DEGRADED));
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.UNKNOWN));
        assertEquals(HealthStatus.DOWN, HealthStatus.worst(HealthStatus.DOWN, HealthStatus.UP));

        // DEGRADED is second worst
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.UNKNOWN));
        assertEquals(HealthStatus.DEGRADED, HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.UP));

        // UNKNOWN is third worst
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.worst(HealthStatus.UNKNOWN, HealthStatus.UP));

        // UP is best
        assertEquals(HealthStatus.UP, HealthStatus.worst(HealthStatus.UP, HealthStatus.UP));
    }

    @Test
    void testWorstIsCommutative() {
        // Order shouldn't matter
        assertEquals(
            HealthStatus.worst(HealthStatus.UP, HealthStatus.DOWN),
            HealthStatus.worst(HealthStatus.DOWN, HealthStatus.UP)
        );

        assertEquals(
            HealthStatus.worst(HealthStatus.DEGRADED, HealthStatus.UNKNOWN),
            HealthStatus.worst(HealthStatus.UNKNOWN, HealthStatus.DEGRADED)
        );
    }

    @Test
    void testEnumToString() {
        assertEquals("UP", HealthStatus.UP.toString());
        assertEquals("DEGRADED", HealthStatus.DEGRADED.toString());
        assertEquals("DOWN", HealthStatus.DOWN.toString());
        assertEquals("UNKNOWN", HealthStatus.UNKNOWN.toString());
    }

    @Test
    void testEnumEquality() {
        HealthStatus status1 = HealthStatus.UP;
        HealthStatus status2 = HealthStatus.UP;
        HealthStatus status3 = HealthStatus.DOWN;

        assertEquals(status1, status2);
        assertNotEquals(status1, status3);
        assertSame(status1, status2); // Enums are singletons
    }

    @Test
    void testEnumInSwitch() {
        String result = switch (HealthStatus.UP) {
            case UP -> "healthy";
            case DEGRADED -> "degraded";
            case DOWN -> "down";
            case UNKNOWN -> "unknown";
        };

        assertEquals("healthy", result);
    }

    @Test
    void testHealthyStatusCombinations() {
        // Both healthy
        assertTrue(HealthStatus.UP.isHealthy());
        assertTrue(HealthStatus.DEGRADED.isHealthy());

        // Both unhealthy
        assertFalse(HealthStatus.DOWN.isHealthy());
        assertFalse(HealthStatus.UNKNOWN.isHealthy());
    }

    @Test
    void testWorstWithAllCombinations() {
        HealthStatus[] statuses = HealthStatus.values();

        // Test all combinations
        for (HealthStatus a : statuses) {
            for (HealthStatus b : statuses) {
                HealthStatus result = HealthStatus.worst(a, b);
                assertNotNull(result);

                // Verify result is one of the valid statuses
                assertTrue(result == HealthStatus.UP ||
                          result == HealthStatus.DEGRADED ||
                          result == HealthStatus.DOWN ||
                          result == HealthStatus.UNKNOWN);
            }
        }
    }

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            HealthStatus.valueOf("INVALID");
        });
    }

    @Test
    void testCaseSensitiveValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            HealthStatus.valueOf("up");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            HealthStatus.valueOf("Up");
        });
    }
}
