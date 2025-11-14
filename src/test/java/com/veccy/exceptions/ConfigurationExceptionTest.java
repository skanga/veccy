package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigurationException.
 */
class ConfigurationExceptionTest {

    @Test
    void testDefaultConstructor() {
        ConfigurationException exception = new ConfigurationException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Invalid configuration";
        ConfigurationException exception = new ConfigurationException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Failed to parse configuration";
        IllegalArgumentException cause = new IllegalArgumentException("Invalid value");
        ConfigurationException exception = new ConfigurationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        NumberFormatException cause = new NumberFormatException("For input string: \"abc\"");
        ConfigurationException exception = new ConfigurationException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        ConfigurationException exception = new ConfigurationException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(ConfigurationException.class, () -> {
            throw new ConfigurationException("Configuration error");
        });
    }

    @Test
    void testTypicalConfigurationErrors() {
        // Missing required field
        ConfigurationException missingField = new ConfigurationException("Missing required field: 'dimensions'");
        assertTrue(missingField.getMessage().contains("Missing"));

        // Invalid value
        ConfigurationException invalidValue = new ConfigurationException("Invalid value for 'M': must be positive");
        assertTrue(invalidValue.getMessage().contains("Invalid"));

        // Out of range
        ConfigurationException outOfRange = new ConfigurationException("Value out of range: efConstruction must be >= 1");
        assertTrue(outOfRange.getMessage().contains("out of range"));

        // Incompatible settings
        ConfigurationException incompatible = new ConfigurationException("Incompatible configuration: cannot use disk storage with in-memory index");
        assertTrue(incompatible.getMessage().contains("Incompatible"));

        // Invalid type
        ConfigurationException invalidType = new ConfigurationException("Invalid index type: 'unknown'");
        assertTrue(invalidType.getMessage().contains("Invalid"));
    }

    @Test
    void testIndexConfigurationErrors() {
        // HNSW config
        ConfigurationException hnswError = new ConfigurationException("HNSW: M must be between 2 and 100");
        assertTrue(hnswError.getMessage().contains("HNSW"));

        // IVF config
        ConfigurationException ivfError = new ConfigurationException("IVF: numClusters must be positive");
        assertTrue(ivfError.getMessage().contains("IVF"));

        // LSH config
        ConfigurationException lshError = new ConfigurationException("LSH: numTables must be at least 1");
        assertTrue(lshError.getMessage().contains("LSH"));

        // Annoy config
        ConfigurationException annoyError = new ConfigurationException("Annoy: numTrees must be positive");
        assertTrue(annoyError.getMessage().contains("Annoy"));
    }

    @Test
    void testStorageConfigurationErrors() {
        // Missing path
        ConfigurationException missingPath = new ConfigurationException("Disk storage requires 'path' to be set");
        assertTrue(missingPath.getMessage().contains("path"));

        // Invalid cache size
        ConfigurationException cacheSize = new ConfigurationException("Cache size must be positive");
        assertTrue(cacheSize.getMessage().contains("Cache"));

        // Invalid storage type
        ConfigurationException storageType = new ConfigurationException("Unknown storage type: 'redis'");
        assertTrue(storageType.getMessage().contains("storage type"));
    }

    @Test
    void testMetricConfigurationErrors() {
        ConfigurationException metricError = new ConfigurationException("Unknown metric: 'invalid_metric'");
        assertTrue(metricError.getMessage().contains("metric"));
    }

    @Test
    void testConfigurationWithMultipleFields() {
        String message = "Configuration validation failed: [dimensions=0, M=-5, efConstruction=0]";
        ConfigurationException exception = new ConfigurationException(message);

        assertTrue(exception.getMessage().contains("dimensions"));
        assertTrue(exception.getMessage().contains("M"));
        assertTrue(exception.getMessage().contains("efConstruction"));
    }
}
