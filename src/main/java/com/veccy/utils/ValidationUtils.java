package com.veccy.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veccy.exceptions.ValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validation utilities for vector database operations.
 * Provides methods to validate vectors, configurations, metrics, and other inputs.
 */
public final class ValidationUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Set<String> VALID_METRICS = Set.of(
            "cosine", "euclidean", "dot_product", "manhattan",
            "chebyshev", "hamming", "jaccard"
    );

    private ValidationUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Validate a single vector.
     *
     * @param vector the vector to validate
     * @param minDim minimum dimension (null for no minimum)
     * @param maxDim maximum dimension (null for no maximum)
     * @param allowZero whether to allow zero vectors
     * @return true if valid
     * @throws ValidationException if vector is invalid
     */
    public static boolean validateVector(double[] vector, Integer minDim, Integer maxDim, boolean allowZero) {
        if (vector == null) {
            throw new ValidationException("Vector cannot be null");
        }

        if (vector.length == 0) {
            throw new ValidationException("Vector cannot be empty");
        }

        if (minDim != null && vector.length < minDim) {
            throw new ValidationException(
                    "Vector dimension " + vector.length + " is less than minimum " + minDim);
        }

        if (maxDim != null && vector.length > maxDim) {
            throw new ValidationException(
                    "Vector dimension " + vector.length + " is greater than maximum " + maxDim);
        }

        if (!allowZero) {
            boolean allZero = true;
            for (double v : vector) {
                if (v != 0.0) {
                    allZero = false;
                    break;
                }
            }
            if (allZero) {
                throw new ValidationException("Zero vector not allowed");
            }
        }

        // Check for NaN or infinite values
        for (int i = 0; i < vector.length; i++) {
            if (Double.isNaN(vector[i])) {
                throw new ValidationException("Vector contains NaN at index " + i);
            }
            if (Double.isInfinite(vector[i])) {
                throw new ValidationException("Vector contains infinite value at index " + i);
            }
        }

        return true;
    }

    /**
     * Validate a single vector with default parameters.
     *
     * @param vector the vector to validate
     * @return true if valid
     */
    public static boolean validateVector(double[] vector) {
        return validateVector(vector, null, null, true);
    }

    /**
     * Validate a batch of vectors.
     *
     * @param vectors the batch of vectors to validate
     * @param minDim minimum dimension (null for no minimum)
     * @param maxDim maximum dimension (null for no maximum)
     * @param allowZero whether to allow zero vectors
     * @param consistentDims whether all vectors must have same dimension
     * @return true if valid
     * @throws ValidationException if vectors are invalid
     */
    public static boolean validateVectors(double[][] vectors, Integer minDim, Integer maxDim,
                                          boolean allowZero, boolean consistentDims) {
        if (vectors == null) {
            throw new ValidationException("Vectors cannot be null");
        }

        if (vectors.length == 0) {
            throw new ValidationException("Vector batch cannot be empty");
        }

        int expectedDim = vectors[0].length;

        for (int i = 0; i < vectors.length; i++) {
            if (vectors[i] == null) {
                throw new ValidationException("Vector at index " + i + " is null");
            }

            if (consistentDims && vectors[i].length != expectedDim) {
                throw new ValidationException(
                        "Inconsistent vector dimensions: expected " + expectedDim +
                                " but got " + vectors[i].length + " at index " + i);
            }

            try {
                validateVector(vectors[i], minDim, maxDim, allowZero);
            } catch (ValidationException e) {
                throw new ValidationException("Invalid vector at index " + i + ": " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Validate a batch of vectors with default parameters.
     *
     * @param vectors the batch of vectors to validate
     * @return true if valid
     */
    public static boolean validateVectors(double[][] vectors) {
        return validateVectors(vectors, null, null, true, true);
    }

    /**
     * Validate configuration dictionary.
     *
     * @param config the configuration to validate
     * @param requiredKeys list of required keys (null for none)
     * @param allowedKeys list of allowed keys (null for all allowed)
     * @return true if valid
     * @throws ValidationException if config is invalid
     */
    public static boolean validateConfig(Map<String, Object> config,
                                         List<String> requiredKeys,
                                         List<String> allowedKeys) {
        if (config == null) {
            throw new ValidationException("Config cannot be null");
        }

        // Check required keys
        if (requiredKeys != null) {
            for (String key : requiredKeys) {
                if (!config.containsKey(key)) {
                    throw new ValidationException("Missing required key: " + key);
                }
            }
        }

        // Check allowed keys
        if (allowedKeys != null) {
            for (String key : config.keySet()) {
                if (!allowedKeys.contains(key)) {
                    throw new ValidationException("Invalid key: " + key);
                }
            }
        }

        return true;
    }

    /**
     * Validate configuration with expected types and ranges.
     *
     * @param config the configuration to validate
     * @param valueTypes map of keys to expected types (null for no type checking)
     * @param valueRanges map of keys to [min, max] ranges (null for no range checking)
     * @return true if valid
     * @throws ValidationException if config is invalid
     */
    public static boolean validateConfigValues(Map<String, Object> config,
                                               Map<String, Class<?>> valueTypes,
                                               Map<String, double[]> valueRanges) {
        if (config == null) {
            throw new ValidationException("Config cannot be null");
        }

        // Check value types
        if (valueTypes != null) {
            for (Map.Entry<String, Class<?>> entry : valueTypes.entrySet()) {
                String key = entry.getKey();
                Class<?> expectedType = entry.getValue();

                if (config.containsKey(key)) {
                    Object value = config.get(key);
                    if (value != null && !expectedType.isInstance(value)) {
                        throw new ValidationException(
                                "Key '" + key + "' must be of type " + expectedType.getSimpleName() +
                                        ", got " + value.getClass().getSimpleName());
                    }
                }
            }
        }

        // Check value ranges
        if (valueRanges != null) {
            for (Map.Entry<String, double[]> entry : valueRanges.entrySet()) {
                String key = entry.getKey();
                double[] range = entry.getValue();

                if (config.containsKey(key)) {
                    Object value = config.get(key);
                    if (value instanceof Number) {
                        double numValue = ((Number) value).doubleValue();
                        if (numValue < range[0] || numValue > range[1]) {
                            throw new ValidationException(
                                    "Key '" + key + "' value " + numValue +
                                            " is outside range [" + range[0] + ", " + range[1] + "]");
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validate similarity metric name.
     *
     * @param metric the metric name to validate
     * @return true if valid
     * @throws ValidationException if metric is invalid
     */
    public static boolean validateMetric(String metric) {
        if (metric == null) {
            throw new ValidationException("Metric cannot be null");
        }

        if (!VALID_METRICS.contains(metric)) {
            throw new ValidationException(
                    "Invalid metric '" + metric + "'. Valid metrics: " + VALID_METRICS);
        }

        return true;
    }

    /**
     * Validate k parameter for search operations.
     *
     * @param k the k value to validate
     * @param maxK maximum allowed k value (null for no maximum)
     * @return true if valid
     * @throws ValidationException if k is invalid
     */
    public static boolean validateK(int k, Integer maxK) {
        if (k <= 0) {
            throw new ValidationException("k must be positive, got " + k);
        }

        if (maxK != null && k > maxK) {
            throw new ValidationException("k (" + k + ") cannot be greater than max_k (" + maxK + ")");
        }

        return true;
    }

    /**
     * Validate k parameter for search operations (no maximum).
     *
     * @param k the k value to validate
     * @return true if valid
     */
    public static boolean validateK(int k) {
        return validateK(k, null);
    }

    /**
     * Validate vector ID.
     *
     * @param id the ID to validate
     * @return true if valid
     * @throws ValidationException if ID is invalid
     */
    public static boolean validateId(String id) {
        if (id == null) {
            throw new ValidationException("ID cannot be null");
        }

        if (id.trim().isEmpty()) {
            throw new ValidationException("ID cannot be empty");
        }

        return true;
    }

    /**
     * Validate list of vector IDs.
     *
     * @param ids the list of IDs to validate
     * @return true if valid
     * @throws ValidationException if IDs are invalid
     */
    public static boolean validateIds(List<String> ids) {
        if (ids == null) {
            throw new ValidationException("IDs list cannot be null");
        }

        if (ids.isEmpty()) {
            throw new ValidationException("IDs list cannot be empty");
        }

        Set<String> uniqueIds = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            try {
                validateId(id);
            } catch (ValidationException e) {
                throw new ValidationException("Invalid ID at index " + i + ": " + e.getMessage());
            }

            if (!uniqueIds.add(id)) {
                throw new ValidationException("Duplicate ID at index " + i + ": " + id);
            }
        }

        return true;
    }

    /**
     * Validate metadata dictionary.
     *
     * @param metadata the metadata to validate (can be null)
     * @return true if valid
     * @throws ValidationException if metadata is invalid
     */
    public static boolean validateMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return true;
        }

        // Check for string keys
        for (Object key : metadata.keySet()) {
            if (!(key instanceof String)) {
                throw new ValidationException("Metadata keys must be strings");
            }
        }

        // Check if values are JSON serializable
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            try {
                OBJECT_MAPPER.writeValueAsString(entry.getValue());
            } catch (Exception e) {
                throw new ValidationException(
                        "Metadata value for key '" + entry.getKey() + "' is not JSON serializable");
            }
        }

        return true;
    }

    /**
     * Validate a list of metadata dictionaries.
     *
     * @param metadataList the list of metadata to validate (can be null)
     * @return true if valid
     * @throws ValidationException if any metadata is invalid
     */
    public static boolean validateMetadataList(List<Map<String, Object>> metadataList) {
        if (metadataList == null) {
            return true;
        }

        for (int i = 0; i < metadataList.size(); i++) {
            try {
                validateMetadata(metadataList.get(i));
            } catch (ValidationException e) {
                throw new ValidationException("Invalid metadata at index " + i + ": " + e.getMessage());
            }
        }

        return true;
    }
}
