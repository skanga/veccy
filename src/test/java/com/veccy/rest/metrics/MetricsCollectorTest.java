package com.veccy.rest.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MetricsCollector.
 */
class MetricsCollectorTest {

    private MetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MetricsCollector();
    }

    @Test
    void testInitialState() {
        Map<String, Object> metrics = collector.getMetrics();

        assertEquals(0L, metrics.get("total_requests"));
        assertEquals(0L, metrics.get("successful_requests"));
        assertEquals(0L, metrics.get("failed_requests"));
        assertEquals(0.0, metrics.get("success_rate"));
        assertEquals(0.0, metrics.get("avg_response_time_ms"));
        assertEquals(0L, metrics.get("min_response_time_ms"));
        assertEquals(0L, metrics.get("max_response_time_ms"));
        assertEquals(0L, metrics.get("rate_limit_hits"));
        assertEquals(0L, metrics.get("auth_failures"));
    }

    @Test
    void testRecordSuccessfulRequest() {
        collector.recordRequest("GET", "/api/v1/vectors", 200, 100);

        Map<String, Object> metrics = collector.getMetrics();

        assertEquals(1L, metrics.get("total_requests"));
        assertEquals(1L, metrics.get("successful_requests"));
        assertEquals(0L, metrics.get("failed_requests"));
        assertEquals(1.0, metrics.get("success_rate"));
        assertEquals(100.0, metrics.get("avg_response_time_ms"));
        assertEquals(100L, metrics.get("min_response_time_ms"));
        assertEquals(100L, metrics.get("max_response_time_ms"));
    }

    @Test
    void testRecordFailedRequest() {
        collector.recordRequest("POST", "/api/v1/vectors", 500, 50);

        Map<String, Object> metrics = collector.getMetrics();

        assertEquals(1L, metrics.get("total_requests"));
        assertEquals(0L, metrics.get("successful_requests"));
        assertEquals(1L, metrics.get("failed_requests"));
        assertEquals(0.0, metrics.get("success_rate"));
    }

    @Test
    void testMultipleRequests() {
        collector.recordRequest("GET", "/api/v1/vectors", 200, 100);
        collector.recordRequest("POST", "/api/v1/vectors", 201, 150);
        collector.recordRequest("GET", "/api/v1/vectors/123", 200, 80);
        collector.recordRequest("DELETE", "/api/v1/vectors/456", 500, 200);

        Map<String, Object> metrics = collector.getMetrics();

        assertEquals(4L, metrics.get("total_requests"));
        assertEquals(3L, metrics.get("successful_requests"));
        assertEquals(1L, metrics.get("failed_requests"));
        assertEquals(0.75, metrics.get("success_rate"));
        assertEquals(132.5, metrics.get("avg_response_time_ms")); // (100+150+80+200)/4
        assertEquals(80L, metrics.get("min_response_time_ms"));
        assertEquals(200L, metrics.get("max_response_time_ms"));
    }

    @Test
    void testStatusCodeTracking() {
        collector.recordRequest("GET", "/api/v1", 200, 10);
        collector.recordRequest("GET", "/api/v1", 200, 20);
        collector.recordRequest("POST", "/api/v1", 201, 30);
        collector.recordRequest("GET", "/api/v1", 404, 40);
        collector.recordRequest("POST", "/api/v1", 500, 50);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Long> statusCodes = (Map<String, Long>) metrics.get("status_codes");

        assertEquals(2L, statusCodes.get("200"));
        assertEquals(1L, statusCodes.get("201"));
        assertEquals(1L, statusCodes.get("404"));
        assertEquals(1L, statusCodes.get("500"));
    }

    @Test
    void testEndpointTracking() {
        collector.recordRequest("GET", "/api/v1/vectors", 200, 100);
        collector.recordRequest("GET", "/api/v1/vectors", 200, 150);
        collector.recordRequest("POST", "/api/v1/vectors", 201, 200);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        assertTrue(endpoints.containsKey("GET /api/v1/vectors"));
        assertTrue(endpoints.containsKey("POST /api/v1/vectors"));

        Map<String, Object> getMetrics = endpoints.get("GET /api/v1/vectors");
        assertEquals(2L, getMetrics.get("request_count"));
        assertEquals(2L, getMetrics.get("success_count"));
        assertEquals(1.0, getMetrics.get("success_rate"));
        assertEquals(125.0, getMetrics.get("avg_response_time_ms")); // (100+150)/2
    }

    @Test
    void testEndpointNormalization() {
        collector.recordRequest("GET", "/api/v1/vectors/123", 200, 10);
        collector.recordRequest("GET", "/api/v1/vectors/456", 200, 20);
        collector.recordRequest("GET", "/api/v1/vectors/789", 200, 30);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        // All should be normalized to the same endpoint
        assertTrue(endpoints.containsKey("GET /api/v1/vectors/:id"));
        Map<String, Object> endpointMetrics = endpoints.get("GET /api/v1/vectors/:id");
        assertEquals(3L, endpointMetrics.get("request_count"));
    }

    @Test
    void testEndpointNormalizationWithUUID() {
        collector.recordRequest("GET", "/api/v1/vectors/550e8400-e29b-41d4-a716-446655440000", 200, 10);
        collector.recordRequest("GET", "/api/v1/vectors/123e4567-e89b-12d3-a456-426614174000", 200, 20);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        assertTrue(endpoints.containsKey("GET /api/v1/vectors/:id"));
        Map<String, Object> endpointMetrics = endpoints.get("GET /api/v1/vectors/:id");
        assertEquals(2L, endpointMetrics.get("request_count"));
    }

    @Test
    void testEndpointNormalizationWithQueryParams() {
        collector.recordRequest("GET", "/api/v1/vectors?limit=10", 200, 10);
        collector.recordRequest("GET", "/api/v1/vectors?limit=20&offset=5", 200, 20);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        // Query params should be removed
        assertTrue(endpoints.containsKey("GET /api/v1/vectors"));
        Map<String, Object> endpointMetrics = endpoints.get("GET /api/v1/vectors");
        assertEquals(2L, endpointMetrics.get("request_count"));
    }

    @Test
    void testRecordRateLimitHit() {
        collector.recordRateLimitHit();
        collector.recordRateLimitHit();
        collector.recordRateLimitHit();

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(3L, metrics.get("rate_limit_hits"));
    }

    @Test
    void testRecordAuthFailure() {
        collector.recordAuthFailure();
        collector.recordAuthFailure();

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(2L, metrics.get("auth_failures"));
    }

    @Test
    void testRecordDatabaseOperation() {
        collector.recordDatabaseOperation("INSERT");
        collector.recordDatabaseOperation("INSERT");
        collector.recordDatabaseOperation("SELECT");
        collector.recordDatabaseOperation("UPDATE");

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Long> dbOps = (Map<String, Long>) metrics.get("database_operations");

        assertEquals(2L, dbOps.get("INSERT"));
        assertEquals(1L, dbOps.get("SELECT"));
        assertEquals(1L, dbOps.get("UPDATE"));
    }

    @Test
    void testReset() {
        collector.recordRequest("GET", "/api/v1", 200, 100);
        collector.recordRateLimitHit();
        collector.recordAuthFailure();
        collector.recordDatabaseOperation("SELECT");

        collector.reset();

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(0L, metrics.get("total_requests"));
        assertEquals(0L, metrics.get("successful_requests"));
        assertEquals(0L, metrics.get("failed_requests"));
        assertEquals(0L, metrics.get("rate_limit_hits"));
        assertEquals(0L, metrics.get("auth_failures"));
        @SuppressWarnings("unchecked")
        Map<String, Long> dbOps = (Map<String, Long>) metrics.get("database_operations");
        assertTrue(dbOps.isEmpty());
    }

    @Test
    void testSuccessRateCalculation() {
        // Test with no requests
        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(0.0, metrics.get("success_rate"));

        // Test with all successful
        collector.recordRequest("GET", "/api", 200, 10);
        collector.recordRequest("GET", "/api", 201, 10);
        metrics = collector.getMetrics();
        assertEquals(1.0, metrics.get("success_rate"));

        // Test with mixed
        collector.recordRequest("GET", "/api", 500, 10);
        metrics = collector.getMetrics();
        assertEquals(2.0 / 3.0, (Double) metrics.get("success_rate"), 0.001);
    }

    @Test
    void testResponseTimeTracking() {
        collector.recordRequest("GET", "/api", 200, 50);
        collector.recordRequest("GET", "/api", 200, 100);
        collector.recordRequest("GET", "/api", 200, 150);

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(100.0, metrics.get("avg_response_time_ms"));
        assertEquals(50L, metrics.get("min_response_time_ms"));
        assertEquals(150L, metrics.get("max_response_time_ms"));
    }

    @Test
    void testMinMaxResponseTimeUpdate() {
        collector.recordRequest("GET", "/api", 200, 100);
        collector.recordRequest("GET", "/api", 200, 50);  // New min
        collector.recordRequest("GET", "/api", 200, 200); // New max
        collector.recordRequest("GET", "/api", 200, 75);  // Between min and max

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(50L, metrics.get("min_response_time_ms"));
        assertEquals(200L, metrics.get("max_response_time_ms"));
    }

    @Test
    void testDifferentStatusCodeRanges() {
        // 2xx - success
        collector.recordRequest("GET", "/api", 200, 10);
        collector.recordRequest("GET", "/api", 201, 10);
        collector.recordRequest("GET", "/api", 204, 10);

        // 3xx - success (redirects)
        collector.recordRequest("GET", "/api", 301, 10);
        collector.recordRequest("GET", "/api", 304, 10);

        // 4xx - failure
        collector.recordRequest("GET", "/api", 400, 10);
        collector.recordRequest("GET", "/api", 404, 10);

        // 5xx - failure
        collector.recordRequest("GET", "/api", 500, 10);
        collector.recordRequest("GET", "/api", 503, 10);

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(9L, metrics.get("total_requests"));
        assertEquals(5L, metrics.get("successful_requests")); // 2xx and 3xx
        assertEquals(4L, metrics.get("failed_requests"));    // 4xx and 5xx
    }

    @Test
    void testEndpointMetricsIndependence() {
        collector.recordRequest("GET", "/api/v1/vectors", 200, 100);
        collector.recordRequest("POST", "/api/v1/vectors", 500, 50);
        collector.recordRequest("GET", "/api/v1/search", 200, 200);

        Map<String, Object> metrics = collector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        Map<String, Object> getVectors = endpoints.get("GET /api/v1/vectors");
        assertEquals(1L, getVectors.get("request_count"));
        assertEquals(1.0, getVectors.get("success_rate"));

        Map<String, Object> postVectors = endpoints.get("POST /api/v1/vectors");
        assertEquals(1L, postVectors.get("request_count"));
        assertEquals(0.0, postVectors.get("success_rate"));

        Map<String, Object> getSearch = endpoints.get("GET /api/v1/search");
        assertEquals(1L, getSearch.get("request_count"));
        assertEquals(1.0, getSearch.get("success_rate"));
    }

    @Test
    void testConcurrentRecording() throws InterruptedException {
        // Simple concurrency test
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                collector.recordRequest("GET", "/api", 200, 10);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                collector.recordRequest("POST", "/api", 201, 20);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(200L, metrics.get("total_requests"));
        assertEquals(200L, metrics.get("successful_requests"));
    }

    @Test
    void testZeroResponseTime() {
        collector.recordRequest("GET", "/api", 200, 0);

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(0.0, metrics.get("avg_response_time_ms"));
        assertEquals(0L, metrics.get("min_response_time_ms"));
        assertEquals(0L, metrics.get("max_response_time_ms"));
    }

    @Test
    void testVeryLargeResponseTime() {
        collector.recordRequest("GET", "/api", 200, 10000);

        Map<String, Object> metrics = collector.getMetrics();
        assertEquals(10000.0, metrics.get("avg_response_time_ms"));
        assertEquals(10000L, metrics.get("min_response_time_ms"));
        assertEquals(10000L, metrics.get("max_response_time_ms"));
    }
}
