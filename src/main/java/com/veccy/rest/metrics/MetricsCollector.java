package com.veccy.rest.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and tracks various metrics for the REST API server.
 * Provides insights into request patterns, performance, and errors.
 */
public class MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollector.class);

    // Request counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);

    // Status code tracking
    private final Map<Integer, AtomicLong> statusCodeCounts = new ConcurrentHashMap<>();

    // Endpoint tracking
    private final Map<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();

    // Performance metrics
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);
    private final AtomicLong minResponseTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTimeMs = new AtomicLong(0);

    // Rate limiting metrics
    private final AtomicLong rateLimitHits = new AtomicLong(0);

    // Authentication metrics
    private final AtomicLong authFailures = new AtomicLong(0);

    // Database operation metrics
    private final Map<String, AtomicLong> dbOperationCounts = new ConcurrentHashMap<>();

    /**
     * Record a request with its status code and response time.
     */
    public void recordRequest(String method, String path, int statusCode, long responseTimeMs) {
        totalRequests.incrementAndGet();

        if (statusCode >= 200 && statusCode < 400) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }

        // Track status code
        statusCodeCounts.computeIfAbsent(statusCode, k -> new AtomicLong(0)).incrementAndGet();

        // Track endpoint
        String endpoint = normalizeEndpoint(method, path);
        endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics())
            .recordRequest(statusCode, responseTimeMs);

        // Track response time
        totalResponseTimeMs.addAndGet(responseTimeMs);
        updateMinMax(responseTimeMs);
    }

    /**
     * Record a rate limit hit.
     */
    public void recordRateLimitHit() {
        rateLimitHits.incrementAndGet();
    }

    /**
     * Record an authentication failure.
     */
    public void recordAuthFailure() {
        authFailures.incrementAndGet();
    }

    /**
     * Record a database operation.
     */
    public void recordDatabaseOperation(String operation) {
        dbOperationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Get all metrics as a map.
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Request metrics
        long total = totalRequests.get();
        metrics.put("total_requests", total);
        metrics.put("successful_requests", successfulRequests.get());
        metrics.put("failed_requests", failedRequests.get());
        metrics.put("success_rate", total > 0 ? (double) successfulRequests.get() / total : 0.0);

        // Response time metrics
        metrics.put("avg_response_time_ms", total > 0 ? (double) totalResponseTimeMs.get() / total : 0.0);
        metrics.put("min_response_time_ms", minResponseTimeMs.get() == Long.MAX_VALUE ? 0 : minResponseTimeMs.get());
        metrics.put("max_response_time_ms", maxResponseTimeMs.get());

        // Status code distribution
        Map<String, Long> statusCodes = new HashMap<>();
        statusCodeCounts.forEach((code, count) -> statusCodes.put(String.valueOf(code), count.get()));
        metrics.put("status_codes", statusCodes);

        // Endpoint metrics
        Map<String, Map<String, Object>> endpoints = new HashMap<>();
        endpointMetrics.forEach((endpoint, endpointMetric) ->
            endpoints.put(endpoint, endpointMetric.toMap()));
        metrics.put("endpoints", endpoints);

        // Security metrics
        metrics.put("rate_limit_hits", rateLimitHits.get());
        metrics.put("auth_failures", authFailures.get());

        // Database operation metrics
        Map<String, Long> dbOps = new HashMap<>();
        dbOperationCounts.forEach((op, count) -> dbOps.put(op, count.get()));
        metrics.put("database_operations", dbOps);

        return metrics;
    }

    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalResponseTimeMs.set(0);
        minResponseTimeMs.set(Long.MAX_VALUE);
        maxResponseTimeMs.set(0);
        rateLimitHits.set(0);
        authFailures.set(0);
        statusCodeCounts.clear();
        endpointMetrics.clear();
        dbOperationCounts.clear();
    }

    /**
     * Update min/max response times.
     */
    private void updateMinMax(long responseTimeMs) {
        // Update minimum
        long currentMin;
        do {
            currentMin = minResponseTimeMs.get();
            if (responseTimeMs >= currentMin) break;
        } while (!minResponseTimeMs.compareAndSet(currentMin, responseTimeMs));

        // Update maximum
        long currentMax;
        do {
            currentMax = maxResponseTimeMs.get();
            if (responseTimeMs <= currentMax) break;
        } while (!maxResponseTimeMs.compareAndSet(currentMax, responseTimeMs));
    }

    /**
     * Normalize endpoint path by removing IDs and parameters.
     */
    private String normalizeEndpoint(String method, String path) {
        // Replace UUID-like patterns with :id
        String normalized = path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/:id");

        // Replace numeric IDs with :id
        normalized = normalized.replaceAll("/\\d+", "/:id");

        // Remove query parameters
        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart);
        }

        return method + " " + normalized;
    }

    /**
     * Metrics for a specific endpoint.
     */
    private static class EndpointMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxResponseTime = new AtomicLong(0);

        void recordRequest(int statusCode, long responseTimeMs) {
            requestCount.incrementAndGet();

            if (statusCode >= 200 && statusCode < 400) {
                successCount.incrementAndGet();
            }

            totalResponseTime.addAndGet(responseTimeMs);

            // Update min
            long currentMin;
            do {
                currentMin = minResponseTime.get();
                if (responseTimeMs >= currentMin) break;
            } while (!minResponseTime.compareAndSet(currentMin, responseTimeMs));

            // Update max
            long currentMax;
            do {
                currentMax = maxResponseTime.get();
                if (responseTimeMs <= currentMax) break;
            } while (!maxResponseTime.compareAndSet(currentMax, responseTimeMs));
        }

        Map<String, Object> toMap() {
            long count = requestCount.get();
            Map<String, Object> map = new HashMap<>();
            map.put("request_count", count);
            map.put("success_count", successCount.get());
            map.put("success_rate", count > 0 ? (double) successCount.get() / count : 0.0);
            map.put("avg_response_time_ms", count > 0 ? (double) totalResponseTime.get() / count : 0.0);
            map.put("min_response_time_ms", minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get());
            map.put("max_response_time_ms", maxResponseTime.get());
            return map;
        }
    }
}
