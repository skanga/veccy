package com.veccy.examples;

import com.veccy.client.VectorDBClient;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.health.*;
import com.veccy.health.HealthCheckRegistry.AggregatedHealthCheckResult;
import com.veccy.indices.FlatIndex;
import com.veccy.storage.MemoryStorage;

import java.util.HashMap;

/**
 * Example demonstrating health check functionality.
 */
public class HealthCheckExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Veccy Health Check Examples ===\n");

        // Example 1: Basic health check
        example1_BasicHealthCheck();

        // Example 2: Health endpoint
        example2_HealthEndpoint();

        // Example 3: Custom health checks
        example3_CustomHealthChecks();

        // Example 4: Production configuration
        example4_ProductionConfig();
    }

    /**
     * Example 1: Basic health check setup.
     */
    private static void example1_BasicHealthCheck() {
        System.out.println("=== Example 1: Basic Health Check ===");

        // Create a database client
        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        try (VectorDBClient client = new VectorDBClient(
                new MemoryStorage(new HashMap<>()),
                new FlatIndex(config))) {

            client.initialize();

            // Insert some vectors
            double[][] vectors = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
            client.insert(vectors, null);

            // Create health manager
            VeccyHealthManager healthManager = VeccyHealthManager.forClient(client);

            // Check health
            boolean healthy = healthManager.isHealthy();
            HealthStatus status = healthManager.getHealthStatus();

            System.out.println("Healthy: " + healthy);
            System.out.println("Status: " + status);

            // Get detailed results
            AggregatedHealthCheckResult result = healthManager.checkHealth();
            System.out.println("Overall Status: " + result.getOverallStatus());
            System.out.println("Duration: " + result.getDurationMs() + "ms");
            System.out.println("Individual Checks:");
            result.getResults().forEach((name, checkResult) -> {
                System.out.println("  " + name + ": " + checkResult.getStatus());
                if (checkResult.getMessage() != null) {
                    System.out.println("    Message: " + checkResult.getMessage());
                }
                if (!checkResult.getDetails().isEmpty()) {
                    System.out.println("    Details: " + checkResult.getDetails());
                }
            });

            healthManager.close();
        }

        System.out.println();
    }

    /**
     * Example 2: Health endpoint with HTTP server.
     */
    private static void example2_HealthEndpoint() throws InterruptedException {
        System.out.println("=== Example 2: Health Endpoint ===");

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        try (VectorDBClient client = new VectorDBClient(
                new MemoryStorage(new HashMap<>()),
                new FlatIndex(config))) {

            client.initialize();

            // Create health manager with HTTP endpoint
            VeccyHealthManager healthManager = VeccyHealthManager.builder()
                    .client(client)
                    .enableEndpoint(true)
                    .endpointPort(8080)
                    .enableMemoryCheck(true)
                    .build();

            System.out.println("Health endpoint started on http://localhost:8080");
            System.out.println();
            System.out.println("Available endpoints:");
            System.out.println("  GET http://localhost:8080/health       - Full health check");
            System.out.println("  GET http://localhost:8080/health/live  - Liveness probe");
            System.out.println("  GET http://localhost:8080/health/ready - Readiness probe");
            System.out.println("  GET http://localhost:8080/metrics      - Prometheus metrics");
            System.out.println();
            System.out.println("Try: curl http://localhost:8080/health");
            System.out.println();
            System.out.println("Endpoint will run for 30 seconds...");

            // Keep running for demonstration
            Thread.sleep(30000);

            healthManager.close();
            System.out.println("Health endpoint stopped");
        }

        System.out.println();
    }

    /**
     * Example 3: Custom health checks.
     */
    private static void example3_CustomHealthChecks() {
        System.out.println("=== Example 3: Custom Health Checks ===");

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        try (VectorDBClient client = new VectorDBClient(
                new MemoryStorage(new HashMap<>()),
                new FlatIndex(config))) {

            client.initialize();

            VeccyHealthManager healthManager = VeccyHealthManager.builder()
                    .client(client)
                    .build();

            // Register custom health check
            healthManager.getRegistry().register(new HealthCheck() {
                @Override
                public String getName() {
                    return "custom_business_logic";
                }

                @Override
                public String getCategory() {
                    return "application";
                }

                @Override
                public HealthCheckResult check() {
                    // Perform custom check
                    boolean businessRulesSatisfied = checkBusinessRules();

                    if (businessRulesSatisfied) {
                        return HealthCheckResult.builder()
                                .status(HealthStatus.UP)
                                .message("Business rules satisfied")
                                .detail("rule_count", 5)
                                .detail("last_check", System.currentTimeMillis())
                                .build();
                    } else {
                        return HealthCheckResult.down("Business rules not satisfied");
                    }
                }

                @Override
                public boolean isCritical() {
                    return false;  // Non-critical
                }

                private boolean checkBusinessRules() {
                    // Custom logic here
                    return true;
                }
            });

            // Check health including custom check
            AggregatedHealthCheckResult result = healthManager.checkHealth();
            System.out.println("Health with custom check:");
            result.getResults().forEach((name, checkResult) -> {
                System.out.println("  " + name + ": " + checkResult.getStatus());
            });

            healthManager.close();
        }

        System.out.println();
    }

    /**
     * Example 4: Production configuration.
     */
    private static void example4_ProductionConfig() {
        System.out.println("=== Example 4: Production Configuration ===");

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();

        try (VectorDBClient client = new VectorDBClient(
                new MemoryStorage(new HashMap<>()),
                new FlatIndex(config))) {

            client.initialize();

            // Production-ready configuration
            VeccyHealthManager healthManager = VeccyHealthManager.builder()
                    .client(client)
                    .enableEndpoint(true)
                    .endpointPort(9090)
                    .enableMemoryCheck(true)
                    .enableDiskCheck(true)
                    .diskCheckPath(System.getProperty("java.io.tmpdir"))
                    .healthCheckTimeout(5000)   // 5 second timeout
                    .cacheTtl(30000)            // 30 second cache
                    .build();

            System.out.println("Production health manager configured:");
            System.out.println("  Endpoint Port: 9090");
            System.out.println("  Memory Check: Enabled");
            System.out.println("  Disk Check: Enabled");
            System.out.println("  Timeout: 5000ms");
            System.out.println("  Cache TTL: 30000ms");
            System.out.println();

            // Perform health check
            AggregatedHealthCheckResult result = healthManager.checkHealth();
            System.out.println("Health Status: " + result.getOverallStatus());
            System.out.println("Check Duration: " + result.getDurationMs() + "ms");

            // Convert to map for logging/monitoring
            System.out.println("\nHealth Check Map:");
            System.out.println(result.toMap());

            healthManager.close();
        }

        System.out.println();
    }
}
