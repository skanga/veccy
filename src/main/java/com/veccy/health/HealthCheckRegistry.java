package com.veccy.health;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Registry for managing and executing health checks.
 * Provides centralized health check execution with timeout and caching support.
 */
public class HealthCheckRegistry {

    private final Map<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final long defaultTimeoutMs;
    private final Map<String, HealthCheckResult> cache = new ConcurrentHashMap<>();
    private final long cacheTtlMs;

    public HealthCheckRegistry() {
        this(5000, 30000); // 5s timeout, 30s cache
    }

    public HealthCheckRegistry(long defaultTimeoutMs, long cacheTtlMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
        this.cacheTtlMs = cacheTtlMs;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "health-check-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Register a health check.
     */
    public void register(HealthCheck healthCheck) {
        Objects.requireNonNull(healthCheck, "healthCheck cannot be null");
        healthChecks.put(healthCheck.getName(), healthCheck);
    }

    /**
     * Unregister a health check.
     */
    public void unregister(String name) {
        healthChecks.remove(name);
        cache.remove(name);
    }

    /**
     * Run all health checks and aggregate results.
     */
    public AggregatedHealthCheckResult runHealthChecks() {
        return runHealthChecks(defaultTimeoutMs);
    }

    /**
     * Run all health checks with specified timeout.
     */
    public AggregatedHealthCheckResult runHealthChecks(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        Map<String, HealthCheckResult> results = new LinkedHashMap<>();
        List<Future<Map.Entry<String, HealthCheckResult>>> futures = new ArrayList<>();

        // Submit all health checks
        for (Map.Entry<String, HealthCheck> entry : healthChecks.entrySet()) {
            String name = entry.getKey();
            HealthCheck check = entry.getValue();

            Future<Map.Entry<String, HealthCheckResult>> future = executor.submit(() -> {
                HealthCheckResult result = runSingleCheck(name, check);
                return new AbstractMap.SimpleEntry<>(name, result);
            });
            futures.add(future);
        }

        // Collect results with timeout
        for (Future<Map.Entry<String, HealthCheckResult>> future : futures) {
            try {
                long remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
                if (remainingTime <= 0) {
                    remainingTime = 100; // Give at least 100ms
                }

                Map.Entry<String, HealthCheckResult> result = future.get(remainingTime, TimeUnit.MILLISECONDS);
                results.put(result.getKey(), result.getValue());
            } catch (TimeoutException e) {
                future.cancel(true);
                results.put("unknown", HealthCheckResult.builder()
                        .status(HealthStatus.UNKNOWN)
                        .message("Health check timed out")
                        .build());
            } catch (Exception e) {
                results.put("unknown", HealthCheckResult.builder()
                        .status(HealthStatus.DOWN)
                        .message("Health check failed: " + e.getMessage())
                        .error(e)
                        .build());
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return new AggregatedHealthCheckResult(results, duration);
    }

    /**
     * Run a single health check with caching.
     */
    private HealthCheckResult runSingleCheck(String name, HealthCheck check) {
        // Check cache
        HealthCheckResult cached = cache.get(name);
        if (cached != null) {
            long age = System.currentTimeMillis() - cached.getTimestamp().toEpochMilli();
            if (age < cacheTtlMs) {
                return cached;
            }
        }

        // Run check
        HealthCheckResult result;
        try {
            result = check.check();
        } catch (Exception e) {
            result = HealthCheckResult.builder()
                    .status(HealthStatus.DOWN)
                    .message("Check threw exception: " + e.getMessage())
                    .error(e)
                    .build();
        }

        // Cache result
        cache.put(name, result);
        return result;
    }

    /**
     * Get a specific health check result.
     */
    public HealthCheckResult runHealthCheck(String name) {
        HealthCheck check = healthChecks.get(name);
        if (check == null) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.UNKNOWN)
                    .message("Health check not found: " + name)
                    .build();
        }
        return runSingleCheck(name, check);
    }

    /**
     * Get all registered health check names.
     */
    public Set<String> getHealthCheckNames() {
        return new HashSet<>(healthChecks.keySet());
    }

    /**
     * Get health checks by category.
     */
    public Map<String, List<HealthCheck>> getHealthChecksByCategory() {
        return healthChecks.values().stream()
                .collect(Collectors.groupingBy(HealthCheck::getCategory));
    }

    /**
     * Clear the result cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Shutdown the executor.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Aggregated result of multiple health checks.
     */
    public static class AggregatedHealthCheckResult {
        private final Map<String, HealthCheckResult> results;
        private final long durationMs;
        private final HealthStatus overallStatus;

        public AggregatedHealthCheckResult(Map<String, HealthCheckResult> results, long durationMs) {
            this.results = new LinkedHashMap<>(results);
            this.durationMs = durationMs;
            this.overallStatus = calculateOverallStatus();
        }

        private HealthStatus calculateOverallStatus() {
            if (results.isEmpty()) {
                return HealthStatus.UNKNOWN;
            }

            HealthStatus status = HealthStatus.UP;
            for (HealthCheckResult result : results.values()) {
                status = HealthStatus.worst(status, result.getStatus());
            }
            return status;
        }

        public Map<String, HealthCheckResult> getResults() {
            return new LinkedHashMap<>(results);
        }

        public long getDurationMs() {
            return durationMs;
        }

        public HealthStatus getOverallStatus() {
            return overallStatus;
        }

        public boolean isHealthy() {
            return overallStatus.isHealthy();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("status", overallStatus.toString());
            map.put("duration_ms", durationMs);
            map.put("timestamp", new java.util.Date());

            Map<String, Object> checks = new LinkedHashMap<>();
            for (Map.Entry<String, HealthCheckResult> entry : results.entrySet()) {
                Map<String, Object> checkMap = new LinkedHashMap<>();
                HealthCheckResult result = entry.getValue();
                checkMap.put("status", result.getStatus().toString());
                if (result.getMessage() != null) {
                    checkMap.put("message", result.getMessage());
                }
                if (!result.getDetails().isEmpty()) {
                    checkMap.put("details", result.getDetails());
                }
                checks.put(entry.getKey(), checkMap);
            }
            map.put("checks", checks);

            return map;
        }
    }
}
