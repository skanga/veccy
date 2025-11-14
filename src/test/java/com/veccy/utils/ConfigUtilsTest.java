package com.veccy.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigUtils utility class.
 */
class ConfigUtilsTest {

    @Test
    void testGetInt_WithNumber() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", 42);

        assertEquals(42, ConfigUtils.getInt(config, "key", 0));
    }

    @Test
    void testGetInt_WithString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "42");

        assertEquals(42, ConfigUtils.getInt(config, "key", 0));
    }

    @Test
    void testGetInt_WithMissingKey() {
        Map<String, Object> config = new HashMap<>();

        assertEquals(10, ConfigUtils.getInt(config, "missing", 10));
    }

    @Test
    void testGetInt_WithNullConfig() {
        assertEquals(10, ConfigUtils.getInt(null, "key", 10));
    }

    @Test
    void testGetInt_WithInvalidString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "not a number");

        assertEquals(10, ConfigUtils.getInt(config, "key", 10));
    }

    @Test
    void testGetDouble_WithNumber() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", 3.14);

        assertEquals(3.14, ConfigUtils.getDouble(config, "key", 0.0), 0.001);
    }

    @Test
    void testGetDouble_WithString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "3.14");

        assertEquals(3.14, ConfigUtils.getDouble(config, "key", 0.0), 0.001);
    }

    @Test
    void testGetDouble_WithMissingKey() {
        Map<String, Object> config = new HashMap<>();

        assertEquals(1.5, ConfigUtils.getDouble(config, "missing", 1.5), 0.001);
    }

    @Test
    void testGetDouble_WithNullConfig() {
        assertEquals(1.5, ConfigUtils.getDouble(null, "key", 1.5), 0.001);
    }

    @Test
    void testGetLong_WithNumber() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", 123456789L);

        assertEquals(123456789L, ConfigUtils.getLong(config, "key", 0L));
    }

    @Test
    void testGetLong_WithString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "123456789");

        assertEquals(123456789L, ConfigUtils.getLong(config, "key", 0L));
    }

    @Test
    void testGetString_WithString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "value");

        assertEquals("value", ConfigUtils.getString(config, "key", "default"));
    }

    @Test
    void testGetString_WithNumber() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", 42);

        assertEquals("42", ConfigUtils.getString(config, "key", "default"));
    }

    @Test
    void testGetString_WithMissingKey() {
        Map<String, Object> config = new HashMap<>();

        assertEquals("default", ConfigUtils.getString(config, "missing", "default"));
    }

    @Test
    void testGetString_WithNull() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", null);

        assertEquals("default", ConfigUtils.getString(config, "key", "default"));
    }

    @Test
    void testGetBoolean_WithBoolean() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", true);

        assertTrue(ConfigUtils.getBoolean(config, "key", false));
    }

    @Test
    void testGetBoolean_WithString() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "true");
        config.put("key2", "false");

        assertTrue(ConfigUtils.getBoolean(config, "key1", false));
        assertFalse(ConfigUtils.getBoolean(config, "key2", true));
    }

    @Test
    void testGetBoolean_WithMissingKey() {
        Map<String, Object> config = new HashMap<>();

        assertTrue(ConfigUtils.getBoolean(config, "missing", true));
    }

    @Test
    void testHasKey_Exists() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "value");

        assertTrue(ConfigUtils.hasKey(config, "key"));
    }

    @Test
    void testHasKey_Missing() {
        Map<String, Object> config = new HashMap<>();

        assertFalse(ConfigUtils.hasKey(config, "missing"));
    }

    @Test
    void testHasKey_NullConfig() {
        assertFalse(ConfigUtils.hasKey(null, "key"));
    }

    @Test
    void testGet_WithCorrectType() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", "value");

        assertEquals("value", ConfigUtils.get(config, "key", "default"));
    }

    @Test
    void testGet_WithMissingKey() {
        Map<String, Object> config = new HashMap<>();

        assertEquals("default", ConfigUtils.get(config, "missing", "default"));
    }

    @Test
    void testGet_WithWrongType() {
        Map<String, Object> config = new HashMap<>();
        config.put("key", 42);

        // Generic get will throw ClassCastException if types don't match
        // This is expected behavior - use specific type methods instead
        assertThrows(ClassCastException.class, () -> {
            String result = ConfigUtils.get(config, "key", "default");
        });
    }

    @Test
    void testNumberConversions() {
        Map<String, Object> config = new HashMap<>();
        config.put("intValue", 42);
        config.put("doubleValue", 3.14);
        config.put("longValue", 123456789L);

        // Integer to double should work
        assertEquals(42.0, ConfigUtils.getDouble(config, "intValue", 0.0), 0.001);

        // Double to int should truncate
        assertEquals(3, ConfigUtils.getInt(config, "doubleValue", 0));

        // Long to int should work (within int range)
        assertEquals(123456789, ConfigUtils.getInt(config, "longValue", 0));
    }
}
