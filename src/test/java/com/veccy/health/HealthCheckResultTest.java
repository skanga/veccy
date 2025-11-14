package com.veccy.health;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthCheckResult.
 */
class HealthCheckResultTest {

    @Test
    void testBuilderWithStatus() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .build();

        assertEquals(HealthStatus.UP, result.getStatus());
        assertNull(result.getMessage());
        assertTrue(result.getDetails().isEmpty());
        assertNotNull(result.getTimestamp());
        assertNull(result.getError());
    }

    @Test
    void testBuilderWithAllFields() {
        Instant timestamp = Instant.now();
        RuntimeException error = new RuntimeException("Test error");

        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.DOWN)
            .message("System is down")
            .detail("reason", "database unavailable")
            .detail("attempts", 3)
            .timestamp(timestamp)
            .error(error)
            .build();

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertEquals("System is down", result.getMessage());
        assertEquals(2, result.getDetails().size());
        assertEquals("database unavailable", result.getDetails().get("reason"));
        assertEquals(3, result.getDetails().get("attempts"));
        assertEquals(timestamp, result.getTimestamp());
        assertEquals(error, result.getError());
    }

    @Test
    void testBuilderWithNullStatusThrows() {
        assertThrows(NullPointerException.class, () -> {
            HealthCheckResult.builder().build();
        });
    }

    @Test
    void testUpFactoryMethod() {
        HealthCheckResult result = HealthCheckResult.up();

        assertEquals(HealthStatus.UP, result.getStatus());
        assertNull(result.getMessage());
        assertTrue(result.isHealthy());
    }

    @Test
    void testUpFactoryMethodWithMessage() {
        HealthCheckResult result = HealthCheckResult.up("All systems operational");

        assertEquals(HealthStatus.UP, result.getStatus());
        assertEquals("All systems operational", result.getMessage());
        assertTrue(result.isHealthy());
    }

    @Test
    void testDownFactoryMethod() {
        HealthCheckResult result = HealthCheckResult.down();

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertNull(result.getMessage());
        assertFalse(result.isHealthy());
    }

    @Test
    void testDownFactoryMethodWithMessage() {
        HealthCheckResult result = HealthCheckResult.down("Database connection failed");

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertEquals("Database connection failed", result.getMessage());
        assertFalse(result.isHealthy());
    }

    @Test
    void testDownFactoryMethodWithError() {
        RuntimeException error = new RuntimeException("Connection timeout");
        HealthCheckResult result = HealthCheckResult.down(error);

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertEquals("Connection timeout", result.getMessage()); // Extracted from error
        assertEquals(error, result.getError());
        assertFalse(result.isHealthy());
    }

    @Test
    void testDegradedFactoryMethod() {
        HealthCheckResult result = HealthCheckResult.degraded("High memory usage");

        assertEquals(HealthStatus.DEGRADED, result.getStatus());
        assertEquals("High memory usage", result.getMessage());
        assertFalse(result.isHealthy()); // isHealthy() only returns true for UP
    }

    @Test
    void testIsHealthyOnlyTrueForUp() {
        assertTrue(HealthCheckResult.up().isHealthy());
        assertFalse(HealthCheckResult.degraded("test").isHealthy());
        assertFalse(HealthCheckResult.down().isHealthy());
        assertFalse(HealthCheckResult.builder().status(HealthStatus.UNKNOWN).build().isHealthy());
    }

    @Test
    void testDetailsAreCopied() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .detail("key1", "value1")
            .build();

        Map<String, Object> details = result.getDetails();
        details.put("key2", "value2"); // Modify returned map

        // Original should be unchanged
        assertEquals(1, result.getDetails().size());
        assertFalse(result.getDetails().containsKey("key2"));
    }

    @Test
    void testBuilderDetailsMethod() {
        Map<String, Object> initialDetails = Map.of(
            "memory_used", "500MB",
            "memory_total", "1GB",
            "cpu_usage", "45%"
        );

        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .details(initialDetails)
            .build();

        assertEquals(3, result.getDetails().size());
        assertEquals("500MB", result.getDetails().get("memory_used"));
        assertEquals("1GB", result.getDetails().get("memory_total"));
        assertEquals("45%", result.getDetails().get("cpu_usage"));
    }

    @Test
    void testBuilderDetailAndDetailsMethods() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .detail("key1", "value1")
            .details(Map.of("key2", "value2", "key3", "value3"))
            .detail("key4", "value4")
            .build();

        assertEquals(4, result.getDetails().size());
    }

    @Test
    void testErrorWithNullMessage() {
        RuntimeException error = new RuntimeException(); // No message
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.DOWN)
            .error(error)
            .build();

        assertNull(result.getMessage());
        assertEquals(error, result.getError());
    }

    @Test
    void testErrorDoesNotOverrideExplicitMessage() {
        RuntimeException error = new RuntimeException("Error message");
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.DOWN)
            .message("Custom message")
            .error(error)
            .build();

        assertEquals("Custom message", result.getMessage());
        assertEquals(error, result.getError());
    }

    @Test
    void testTimestampDefaultsToNow() {
        Instant before = Instant.now();
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .build();
        Instant after = Instant.now();

        assertNotNull(result.getTimestamp());
        assertFalse(result.getTimestamp().isBefore(before));
        assertFalse(result.getTimestamp().isAfter(after));
    }

    @Test
    void testCustomTimestamp() {
        Instant customTime = Instant.parse("2024-01-01T00:00:00Z");
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .timestamp(customTime)
            .build();

        assertEquals(customTime, result.getTimestamp());
    }

    @Test
    void testToStringWithMinimalData() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .build();

        String str = result.toString();

        assertTrue(str.contains("HealthCheckResult"));
        assertTrue(str.contains("status=UP"));
        assertTrue(str.contains("timestamp="));
        assertFalse(str.contains("message="));
    }

    @Test
    void testToStringWithAllData() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.DOWN)
            .message("Test message")
            .detail("key", "value")
            .build();

        String str = result.toString();

        assertTrue(str.contains("status=DOWN"));
        assertTrue(str.contains("message='Test message'"));
        assertTrue(str.contains("details="));
        assertTrue(str.contains("timestamp="));
    }

    @Test
    void testWithDifferentStatuses() {
        HealthCheckResult up = HealthCheckResult.builder().status(HealthStatus.UP).build();
        HealthCheckResult degraded = HealthCheckResult.builder().status(HealthStatus.DEGRADED).build();
        HealthCheckResult down = HealthCheckResult.builder().status(HealthStatus.DOWN).build();
        HealthCheckResult unknown = HealthCheckResult.builder().status(HealthStatus.UNKNOWN).build();

        assertEquals(HealthStatus.UP, up.getStatus());
        assertEquals(HealthStatus.DEGRADED, degraded.getStatus());
        assertEquals(HealthStatus.DOWN, down.getStatus());
        assertEquals(HealthStatus.UNKNOWN, unknown.getStatus());
    }

    @Test
    void testEmptyDetailsMap() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .details(Map.of())
            .build();

        assertTrue(result.getDetails().isEmpty());
    }

    @Test
    void testNullValuesInDetails() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .detail("nullable_key", null)
            .build();

        assertTrue(result.getDetails().containsKey("nullable_key"));
        assertNull(result.getDetails().get("nullable_key"));
    }

    @Test
    void testComplexDetailsValues() {
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .detail("list", java.util.List.of(1, 2, 3))
            .detail("map", Map.of("nested", "value"))
            .detail("number", 42)
            .detail("boolean", true)
            .detail("double", 3.14)
            .build();

        assertEquals(5, result.getDetails().size());
        assertInstanceOf(java.util.List.class, result.getDetails().get("list"));
        assertInstanceOf(Map.class, result.getDetails().get("map"));
        assertInstanceOf(Integer.class, result.getDetails().get("number"));
        assertInstanceOf(Boolean.class, result.getDetails().get("boolean"));
        assertInstanceOf(Double.class, result.getDetails().get("double"));
    }

    @Test
    void testBuilderReusability() {
        HealthCheckResult.Builder builder = HealthCheckResult.builder();

        HealthCheckResult result1 = builder.status(HealthStatus.UP).build();
        assertEquals(HealthStatus.UP, result1.getStatus());

        // Reuse builder (though typically not recommended)
        HealthCheckResult result2 = builder.status(HealthStatus.DOWN).build();
        assertEquals(HealthStatus.DOWN, result2.getStatus());
    }

    @Test
    void testLongMessage() {
        String longMessage = "a".repeat(10000);
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.UP)
            .message(longMessage)
            .build();

        assertEquals(10000, result.getMessage().length());
    }

    @Test
    void testSpecialCharactersInMessage() {
        String message = "Error: \"quoted\" & <special> chars\n\ttab";
        HealthCheckResult result = HealthCheckResult.builder()
            .status(HealthStatus.DOWN)
            .message(message)
            .build();

        assertEquals(message, result.getMessage());
    }
}
