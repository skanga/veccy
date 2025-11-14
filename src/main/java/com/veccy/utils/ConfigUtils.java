package com.veccy.utils;

import java.util.Map;

/**
 * Utility class for safely extracting typed values from configuration maps.
 * <p>
 * This class provides type-safe methods to extract configuration values with
 * default fallbacks, reducing code duplication across index implementations.
 */
public class ConfigUtils {

    private ConfigUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get an integer value from configuration map.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or invalid
     * @return the integer value or default
     */
    public static int getInt(Map<String, Object> config, String key, int defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a double value from configuration map.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or invalid
     * @return the double value or default
     */
    public static double getDouble(Map<String, Object> config, String key, double defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a long value from configuration map.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or invalid
     * @return the long value or default
     */
    public static long getLong(Map<String, Object> config, String key, long defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        return defaultValue;
    }

    /**
     * Get a string value from configuration map.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or null
     * @return the string value or default
     */
    public static String getString(Map<String, Object> config, String key, String defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Get a boolean value from configuration map.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or invalid
     * @return the boolean value or default
     */
    public static boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        return defaultValue;
    }

    /**
     * Check if a key exists in the configuration map.
     *
     * @param config the configuration map
     * @param key    the key to check
     * @return true if the key exists, false otherwise
     */
    public static boolean hasKey(Map<String, Object> config, String key) {
        return config != null && config.containsKey(key);
    }

    /**
     * Get a value from configuration map with type casting.
     *
     * @param config       the configuration map
     * @param key          the key to look up
     * @param defaultValue the default value if key is missing or invalid
     * @param <T>          the expected type
     * @return the typed value or default
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Map<String, Object> config, String key, T defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        try {
            Object value = config.get(key);
            return value != null ? (T) value : defaultValue;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }
}
