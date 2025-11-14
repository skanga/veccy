package com.veccy.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP endpoint for health checks.
 * Provides /health, /health/live, and /health/ready endpoints.
 */
public class HealthEndpoint implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(HealthEndpoint.class);

    private final HealthCheckRegistry registry;
    private final HttpServer server;
    private final int port;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public HealthEndpoint(HealthCheckRegistry registry) {
        this(registry, 8080);
    }

    public HealthEndpoint(HealthCheckRegistry registry, int port) {
        this.registry = registry;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.setExecutor(Executors.newFixedThreadPool(4));
            // Get actual bound port (important when port=0 for dynamic allocation)
            this.port = server.getAddress().getPort();
            setupEndpoints();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create health endpoint", e);
        }
    }

    private void setupEndpoints() {
        // Full health check endpoint
        server.createContext("/health", new HealthHandler());

        // Liveness probe - is the application running?
        server.createContext("/health/live", new LivenessHandler());

        // Readiness probe - is the application ready to serve traffic?
        server.createContext("/health/ready", new ReadinessHandler());

        // Metrics endpoint
        server.createContext("/metrics", new MetricsHandler());
    }

    /**
     * Start the health endpoint server.
     */
    public void start() {
        server.start();
    }

    /**
     * Stop the health endpoint server.
     */
    public void stop() {
        server.stop(0);
    }

    /**
     * Close the health endpoint and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                stop();
                logger.info("Health endpoint closed (port: {})", port);
            } catch (Exception e) {
                logger.error("Error closing health endpoint: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Health endpoint already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Get the port the server is running on.
     */
    public int getPort() {
        return port;
    }

    /**
     * Handler for /health endpoint - full health check.
     */
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();
            respondWithJson(exchange, result.toMap(), getHttpStatus(result.getOverallStatus()));
        }
    }

    /**
     * Handler for /health/live endpoint - liveness probe.
     * Returns 200 if the application is running, 503 if down.
     */
    private class LivenessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Simple liveness check - if we can respond, we're alive
            Map<String, Object> response = Map.of(
                    "status", "UP",
                    "timestamp", new java.util.Date()
            );
            respondWithJson(exchange, response, 200);
        }
    }

    /**
     * Handler for /health/ready endpoint - readiness probe.
     * Returns 200 if the application is ready to serve traffic, 503 if not.
     */
    private class ReadinessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Run critical health checks only
            HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

            // Check if all critical components are UP
            boolean ready = result.getOverallStatus().isHealthy();

            Map<String, Object> response = Map.of(
                    "status", ready ? "UP" : "DOWN",
                    "timestamp", new java.util.Date(),
                    "checks", result.toMap().get("checks")
            );

            respondWithJson(exchange, response, ready ? 200 : 503);
        }
    }

    /**
     * Handler for /metrics endpoint - Prometheus-compatible metrics.
     */
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            HealthCheckRegistry.AggregatedHealthCheckResult result = registry.runHealthChecks();

            StringBuilder metrics = new StringBuilder();
            metrics.append("# HELP veccy_health_status Health status (1=UP, 0.5=DEGRADED, 0=DOWN)\n");
            metrics.append("# TYPE veccy_health_status gauge\n");

            for (Map.Entry<String, HealthCheckResult> entry : result.getResults().entrySet()) {
                String name = entry.getKey();
                HealthStatus status = entry.getValue().getStatus();
                double value = switch (status) {
                    case UP -> 1.0;
                    case DEGRADED -> 0.5;
                    case DOWN, UNKNOWN -> 0.0;
                };
                metrics.append(String.format("veccy_health_status{check=\"%s\"} %s\n", name, value));
            }

            metrics.append("\n# HELP veccy_health_check_duration_ms Health check duration in milliseconds\n");
            metrics.append("# TYPE veccy_health_check_duration_ms gauge\n");
            metrics.append(String.format("veccy_health_check_duration_ms %d\n", result.getDurationMs()));

            respondWithText(exchange, metrics.toString(), 200);
        }
    }

    /**
     * Send JSON response.
     */
    private void respondWithJson(HttpExchange exchange, Map<String, Object> data, int statusCode) throws IOException {
        String json = toJson(data);
        byte[] response = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Send text response.
     */
    private void respondWithText(HttpExchange exchange, String text, int statusCode) throws IOException {
        byte[] response = text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Convert health status to HTTP status code.
     */
    private int getHttpStatus(HealthStatus status) {
        return switch (status) {
            case UP -> 200;
            case DEGRADED -> 200; // Still serving traffic
            case DOWN, UNKNOWN -> 503;
        };
    }

    /**
     * Simple JSON serialization.
     */
    @SuppressWarnings("unchecked")
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i++ > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escape((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(toJson((Map<String, Object>) value));
            } else if (value instanceof java.util.Date) {
                json.append("\"").append(value.toString()).append("\"");
            } else {
                json.append("\"").append(String.valueOf(value)).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Escape JSON string.
     */
    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
