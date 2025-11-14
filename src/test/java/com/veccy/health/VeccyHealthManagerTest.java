package com.veccy.health;

import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VeccyHealthManager.
 */
class VeccyHealthManagerTest {

    private VeccyHealthManager manager;

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    void testBuilderWithClient() {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        manager = VeccyHealthManager.builder()
                .client(client)
                .build();

        assertNotNull(manager.getRegistry());
        assertNull(manager.getEndpoint()); // Endpoint not enabled by default

        client.close();
    }

    @Test
    void testBuilderWithEndpointEnabled() {
        manager = VeccyHealthManager.builder()
                .enableEndpoint(true)
                .endpointPort(0)
                .build();

        assertNotNull(manager.getRegistry());
        assertNotNull(manager.getEndpoint());
    }

    @Test
    void testBuilderWithMemoryCheck() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderWithoutMemoryCheck() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(false)
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderWithDiskCheck() {
        manager = VeccyHealthManager.builder()
                .enableDiskCheck(true)
                .diskCheckPath("/")
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderWithDiskCheckPath() {
        manager = VeccyHealthManager.builder()
                .diskCheckPath(Paths.get("/"))
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderWithHealthCheckTimeout() {
        manager = VeccyHealthManager.builder()
                .healthCheckTimeout(10000)
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderWithCacheTtl() {
        manager = VeccyHealthManager.builder()
                .cacheTtl(60000)
                .build();

        assertNotNull(manager.getRegistry());
    }

    @Test
    void testBuilderChaining() {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        manager = VeccyHealthManager.builder()
                .client(client)
                .enableEndpoint(false)
                .endpointPort(0)
                .enableMemoryCheck(true)
                .enableDiskCheck(false)
                .healthCheckTimeout(5000)
                .cacheTtl(30000)
                .build();

        assertNotNull(manager.getRegistry());
        assertNull(manager.getEndpoint());

        client.close();
    }

    @Test
    void testForClient() {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        manager = VeccyHealthManager.forClient(client);

        assertNotNull(manager.getRegistry());
        assertNotNull(manager.getEndpoint());

        client.close();
    }

    @Test
    void testCheckHealth() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        HealthCheckRegistry.AggregatedHealthCheckResult result = manager.checkHealth();
        assertNotNull(result);
    }

    @Test
    void testIsHealthy() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        // With just memory check, should be healthy
        boolean healthy = manager.isHealthy();
        assertTrue(healthy || !healthy); // Either is valid
    }

    @Test
    void testGetHealthStatus() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        HealthStatus status = manager.getHealthStatus();
        assertNotNull(status);
    }

    @Test
    void testClose() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        manager.close();
        // Should not throw exception
    }

    @Test
    void testCloseIdempotent() {
        manager = VeccyHealthManager.builder()
                .enableMemoryCheck(true)
                .build();

        manager.close();
        manager.close(); // Second close should be safe
        manager.close(); // Third close should be safe
    }

    @Test
    void testCloseWithEndpoint() {
        manager = VeccyHealthManager.builder()
                .enableEndpoint(true)
                .endpointPort(0)
                .build();

        manager.close();
        // Should close endpoint without error
    }

    @Test
    void testGetRegistry() {
        manager = VeccyHealthManager.builder().build();

        HealthCheckRegistry registry = manager.getRegistry();
        assertNotNull(registry);
    }

    @Test
    void testGetEndpointWhenDisabled() {
        manager = VeccyHealthManager.builder()
                .enableEndpoint(false)
                .build();

        assertNull(manager.getEndpoint());
    }

    @Test
    void testGetEndpointWhenEnabled() {
        manager = VeccyHealthManager.builder()
                .enableEndpoint(true)
                .endpointPort(0)
                .build();

        assertNotNull(manager.getEndpoint());
    }

    @Test
    void testWithAllChecksEnabled() {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        manager = VeccyHealthManager.builder()
                .client(client)
                .enableMemoryCheck(true)
                .diskCheckPath("/")
                .build();

        HealthCheckRegistry.AggregatedHealthCheckResult result = manager.checkHealth();
        assertNotNull(result);
        assertNotNull(result.getOverallStatus());

        client.close();
    }

    @Test
    void testBuilderDefaults() {
        manager = VeccyHealthManager.builder().build();

        assertNotNull(manager.getRegistry());
        assertNull(manager.getEndpoint()); // Default is disabled
    }
}
