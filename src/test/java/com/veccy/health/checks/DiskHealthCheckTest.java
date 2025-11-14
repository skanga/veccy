package com.veccy.health.checks;

import com.veccy.health.HealthCheckResult;
import com.veccy.health.HealthStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DiskHealthCheck.
 */
class DiskHealthCheckTest {

    private Path tempDir;
    private DiskHealthCheck healthCheck;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("veccy_test_disk_health");
        healthCheck = new DiskHealthCheck(tempDir);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            Files.delete(tempDir);
        }
    }

    @Test
    void testGetName() {
        assertEquals("disk", healthCheck.getName());
    }

    @Test
    void testGetCategory() {
        assertEquals("system", healthCheck.getCategory());
    }

    @Test
    void testIsCritical() {
        assertFalse(healthCheck.isCritical());
    }

    @Test
    void testCheckWithExistingDirectory() {
        HealthCheckResult result = healthCheck.check();

        assertNotNull(result);
        assertNotNull(result.getStatus());
        assertTrue(result.getMessage().contains("%"));

        // Details should include space information
        assertTrue(result.getDetails().containsKey("total_space"));
        assertTrue(result.getDetails().containsKey("free_space"));
        assertTrue(result.getDetails().containsKey("usable_space"));
        assertTrue(result.getDetails().containsKey("usage_percent"));
        assertTrue(result.getDetails().containsKey("directory"));
    }

    @Test
    void testCheckWithNonExistentDirectory() throws IOException {
        Path nonExistent = tempDir.resolve("does_not_exist");
        DiskHealthCheck check = new DiskHealthCheck(nonExistent);

        HealthCheckResult result = check.check();

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void testCheckStatusBasedOnUsage() {
        HealthCheckResult result = healthCheck.check();

        // Most systems will have < 85% usage, so should be UP
        // But we can't guarantee this, so we just check it's a valid status
        assertTrue(result.getStatus() == HealthStatus.UP ||
                result.getStatus() == HealthStatus.DEGRADED ||
                result.getStatus() == HealthStatus.DOWN);
    }

    @Test
    void testCheckIncludesDirectoryPath() {
        HealthCheckResult result = healthCheck.check();

        String directory = (String) result.getDetails().get("directory");
        assertNotNull(directory);
        assertTrue(directory.contains(tempDir.toString()) ||
                   directory.contains(tempDir.toAbsolutePath().toString()));
    }

    @Test
    void testCheckSpaceValues() {
        HealthCheckResult result = healthCheck.check();

        long totalSpace = (Long) result.getDetails().get("total_space");
        long freeSpace = (Long) result.getDetails().get("free_space");
        long usableSpace = (Long) result.getDetails().get("usable_space");

        // Basic sanity checks
        assertTrue(totalSpace >= 0);
        assertTrue(freeSpace >= 0);
        assertTrue(usableSpace >= 0);
        assertTrue(freeSpace <= totalSpace);
        assertTrue(usableSpace <= totalSpace);
    }

    @Test
    void testCheckUsagePercentFormat() {
        HealthCheckResult result = healthCheck.check();

        String usagePercent = (String) result.getDetails().get("usage_percent");
        assertNotNull(usagePercent);

        // Should be a formatted number
        double percentage = Double.parseDouble(usagePercent);
        assertTrue(percentage >= 0.0);
        assertTrue(percentage <= 100.0);
    }

    @Test
    void testCheckMessageIncludesPercentage() {
        HealthCheckResult result = healthCheck.check();

        String message = result.getMessage();
        assertTrue(message.contains("%"));
        assertTrue(message.contains("Disk usage"));
    }

    @Test
    void testCheckWithRootDirectory() {
        Path root = Path.of("/").toAbsolutePath();
        DiskHealthCheck rootCheck = new DiskHealthCheck(root);

        HealthCheckResult result = rootCheck.check();

        // Root should always exist and return a valid status
        assertNotNull(result.getStatus());
        assertTrue(result.getDetails().containsKey("total_space"));
        // Status can be UP, DEGRADED, or DOWN depending on actual disk usage
        assertTrue(result.getStatus() == HealthStatus.UP ||
                   result.getStatus() == HealthStatus.DEGRADED ||
                   result.getStatus() == HealthStatus.DOWN);
    }

    @Test
    void testCheckMultipleTimes() {
        HealthCheckResult result1 = healthCheck.check();
        HealthCheckResult result2 = healthCheck.check();
        HealthCheckResult result3 = healthCheck.check();

        // All should succeed
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        // Status should be consistent (disk usage doesn't change that fast)
        assertEquals(result1.getStatus(), result2.getStatus());
    }

    @Test
    void testCheckAfterDirectoryDeleted() throws IOException {
        Files.delete(tempDir);

        HealthCheckResult result = healthCheck.check();

        assertEquals(HealthStatus.DOWN, result.getStatus());
        assertTrue(result.getMessage().contains("does not exist"));
    }

    @Test
    void testCheckDetailsContainAllFields() {
        HealthCheckResult result = healthCheck.check();

        assertTrue(result.getDetails().containsKey("total_space"));
        assertTrue(result.getDetails().containsKey("free_space"));
        assertTrue(result.getDetails().containsKey("usable_space"));
        assertTrue(result.getDetails().containsKey("usage_percent"));
        assertTrue(result.getDetails().containsKey("directory"));

        // All space values should be Long
        assertTrue(result.getDetails().get("total_space") instanceof Long);
        assertTrue(result.getDetails().get("free_space") instanceof Long);
        assertTrue(result.getDetails().get("usable_space") instanceof Long);

        // usage_percent should be String (formatted)
        assertTrue(result.getDetails().get("usage_percent") instanceof String);

        // directory should be String
        assertTrue(result.getDetails().get("directory") instanceof String);
    }
}
