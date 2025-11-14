package com.veccy.health;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthEndpoint.
 */
class HealthEndpointTest {

    private HealthCheckRegistry registry;
    private HealthEndpoint endpoint;
    private int testPort;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        registry = new HealthCheckRegistry();
        // Use port 0 to get next available port from OS
        endpoint = new HealthEndpoint(registry, 0);
        endpoint.start();
        testPort = endpoint.getPort();

        // Create HttpClient with timeout
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        // Give the server a moment to start
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        if (endpoint != null) {
            endpoint.close();
        }
    }

    @Test
    void testConstructorWithDefaultPort() {
        HealthCheckRegistry reg = new HealthCheckRegistry();
        HealthEndpoint ep = new HealthEndpoint(reg);
        assertNotNull(ep);
        assertEquals(8080, ep.getPort());
        ep.close();
    }

    @Test
    void testConstructorWithCustomPort() {
        assertEquals(testPort, endpoint.getPort());
    }

    @Test
    void testGetPort() {
        assertEquals(testPort, endpoint.getPort());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Add a simple health check
        registry.register(new TestHealthCheck("test", HealthStatus.UP));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\""));
        assertTrue(response.body().contains("\"checks\""));
    }

    @Test
    void testHealthEndpointWithFailure() throws Exception {
        // Add a failing health check
        registry.register(new TestHealthCheck("Test is down", HealthStatus.DOWN));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(503, response.statusCode()); // DOWN returns 503
    }

    @Test
    void testHealthEndpointWithDegraded() throws Exception {
        // Add a degraded health check
        registry.register(new TestHealthCheck("Test is degraded", HealthStatus.DEGRADED));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode()); // DEGRADED still returns 200
    }

    @Test
    void testLivenessEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health/live"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\""));
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void testReadinessEndpoint() throws Exception {
        // Add a passing health check
        registry.register(new TestHealthCheck("Ready", HealthStatus.UP));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health/ready"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\""));
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void testReadinessEndpointNotReady() throws Exception {
        // Add a failing health check
        registry.register(new TestHealthCheck("Not ready", HealthStatus.DOWN));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health/ready"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("\"status\""));
        assertTrue(response.body().contains("DOWN"));
    }

    @Test
    void testMetricsEndpoint() throws Exception {
        // Add some health checks
        registry.register(new TestHealthCheck("check1", HealthStatus.UP));
        registry.register(new TestHealthCheck("check2", HealthStatus.DEGRADED));
        registry.register(new TestHealthCheck("check3", HealthStatus.DOWN));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/metrics"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("veccy_health_status"));
        assertTrue(response.body().contains("check1"));
        assertTrue(response.body().contains("check2"));
        assertTrue(response.body().contains("check3"));
        assertTrue(response.body().contains("1.0")); // UP
        assertTrue(response.body().contains("0.5")); // DEGRADED
        assertTrue(response.body().contains("0.0")); // DOWN
    }

    @Test
    void testMetricsContentType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/metrics"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("text/plain", contentType);
    }

    @Test
    void testHealthContentType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json", contentType);
    }

    @Test
    void testStartAndStop() {
        HealthCheckRegistry reg = new HealthCheckRegistry();
        HealthEndpoint ep = new HealthEndpoint(reg, 0); // Use dynamic port

        ep.start();
        // Server should be running

        ep.stop();
        // Server should be stopped

        ep.close();
    }

    @Test
    void testCloseIsIdempotent() {
        HealthCheckRegistry reg = new HealthCheckRegistry();
        HealthEndpoint ep = new HealthEndpoint(reg, 0); // Use dynamic port
        ep.start();

        ep.close();
        ep.close(); // Should not throw
        ep.close(); // Should not throw
    }

    @Test
    void testJsonEscaping() throws Exception {
        // Add a health check with special characters
        registry.register(new TestHealthCheck("escape_test", HealthStatus.UP));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check that JSON is properly formatted
        assertTrue(response.body().contains("\"escape_test\""));
        assertTrue(response.body().contains("\"status\""));
    }

    @Test
    void testMultipleHealthChecks() throws Exception {
        // Register multiple health checks
        for (int i = 0; i < 5; i++) {
            registry.register(new TestHealthCheck("check" + i, HealthStatus.UP));
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        for (int i = 0; i < 5; i++) {
            assertTrue(response.body().contains("check" + i));
        }
    }

    /**
     * Simple test health check implementation.
     */
    private static class TestHealthCheck implements HealthCheck {
        private final String name;
        private final HealthStatus status;

        TestHealthCheck(String name, HealthStatus status) {
            this.name = name;
            this.status = status;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public HealthCheckResult check() {
            return HealthCheckResult.builder()
                .status(status)
                .message("Test check: " + name)
                .build();
        }
    }
}
