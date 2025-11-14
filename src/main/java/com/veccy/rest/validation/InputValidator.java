package com.veccy.rest.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive input validation for REST API requests.
 */
public class InputValidator {

    // Configuration limits
    private static final int MAX_DIMENSION = 10000;
    private static final int MAX_VECTORS_PER_BATCH = 1000;
    private static final int MAX_METADATA_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_METADATA_KEY_LENGTH = 256;
    private static final int MAX_METADATA_VALUE_LENGTH = 10000;
    private static final int MAX_METADATA_ENTRIES = 100;
    private static final int MAX_SEARCH_K = 1000;

    /**
     * Validation result containing errors if validation fails.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validate a single vector.
     */
    public static ValidationResult validateVector(float[] vector, int expectedDimensions) {
        List<String> errors = new ArrayList<>();

        if (vector == null) {
            errors.add("Vector cannot be null");
            return ValidationResult.failure(errors);
        }

        if (vector.length == 0) {
            errors.add("Vector cannot be empty");
        }

        if (vector.length > MAX_DIMENSION) {
            errors.add("Vector dimension exceeds maximum allowed: " + MAX_DIMENSION);
        }

        if (expectedDimensions > 0 && vector.length != expectedDimensions) {
            errors.add("Vector dimension mismatch. Expected: " + expectedDimensions + ", got: " + vector.length);
        }

        // Check for NaN and Infinity values
        for (int i = 0; i < vector.length; i++) {
            if (Float.isNaN(vector[i])) {
                errors.add("Vector contains NaN at index " + i);
                break; // Don't report all NaN values
            }
            if (Float.isInfinite(vector[i])) {
                errors.add("Vector contains Infinity at index " + i);
                break;
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate metadata object.
     */
    public static ValidationResult validateMetadata(Map<String, Object> metadata) {
        List<String> errors = new ArrayList<>();

        if (metadata == null) {
            return ValidationResult.success(); // Metadata is optional
        }

        // Check number of entries
        if (metadata.size() > MAX_METADATA_ENTRIES) {
            errors.add("Metadata exceeds maximum entries: " + MAX_METADATA_ENTRIES);
        }

        // Estimate total size
        int estimatedSize = 0;

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Validate key
            if (key == null || key.trim().isEmpty()) {
                errors.add("Metadata keys cannot be null or empty");
                continue;
            }

            if (key.length() > MAX_METADATA_KEY_LENGTH) {
                errors.add("Metadata key too long: '" + key.substring(0, 50) + "...' (max: " + MAX_METADATA_KEY_LENGTH + ")");
            }

            // Validate value
            if (value == null) {
                errors.add("Metadata value for key '" + key + "' is null");
                continue;
            }

            String valueStr = value.toString();
            if (valueStr.length() > MAX_METADATA_VALUE_LENGTH) {
                errors.add("Metadata value for key '" + key + "' exceeds maximum length: " + MAX_METADATA_VALUE_LENGTH);
            }

            // Estimate size
            estimatedSize += key.length() + valueStr.length();
        }

        // Check total size
        if (estimatedSize > MAX_METADATA_SIZE) {
            errors.add("Total metadata size exceeds maximum: " + MAX_METADATA_SIZE + " bytes");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate batch of vectors.
     */
    public static ValidationResult validateVectorBatch(List<float[]> vectors, int expectedDimensions) {
        List<String> errors = new ArrayList<>();

        if (vectors == null) {
            errors.add("Vector batch cannot be null");
            return ValidationResult.failure(errors);
        }

        if (vectors.isEmpty()) {
            errors.add("Vector batch cannot be empty");
            return ValidationResult.failure(errors);
        }

        if (vectors.size() > MAX_VECTORS_PER_BATCH) {
            errors.add("Batch size exceeds maximum: " + MAX_VECTORS_PER_BATCH);
        }

        // Validate each vector (limit error reporting)
        int validationErrors = 0;
        for (int i = 0; i < vectors.size() && validationErrors < 5; i++) {
            ValidationResult result = validateVector(vectors.get(i), expectedDimensions);
            if (!result.isValid()) {
                errors.add("Vector at index " + i + ": " + result.getErrorMessage());
                validationErrors++;
            }
        }

        if (validationErrors >= 5) {
            errors.add("... and possibly more vector validation errors (showing first 5)");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate search parameters.
     */
    public static ValidationResult validateSearchParams(float[] queryVector, int k, int expectedDimensions) {
        List<String> errors = new ArrayList<>();

        // Validate query vector
        ValidationResult vectorResult = validateVector(queryVector, expectedDimensions);
        if (!vectorResult.isValid()) {
            errors.add("Query vector invalid: " + vectorResult.getErrorMessage());
        }

        // Validate k
        if (k <= 0) {
            errors.add("k must be positive");
        }

        if (k > MAX_SEARCH_K) {
            errors.add("k exceeds maximum allowed: " + MAX_SEARCH_K);
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate dimension parameter.
     */
    public static ValidationResult validateDimension(int dimension) {
        List<String> errors = new ArrayList<>();

        if (dimension <= 0) {
            errors.add("Dimension must be positive");
        }

        if (dimension > MAX_DIMENSION) {
            errors.add("Dimension exceeds maximum allowed: " + MAX_DIMENSION);
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate vector ID.
     */
    public static ValidationResult validateVectorId(String id) {
        List<String> errors = new ArrayList<>();

        if (id == null || id.trim().isEmpty()) {
            errors.add("Vector ID cannot be null or empty");
        } else if (id.length() > 256) {
            errors.add("Vector ID exceeds maximum length: 256");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    // Getters for configuration limits (useful for documentation)
    public static int getMaxDimension() {
        return MAX_DIMENSION;
    }

    public static int getMaxVectorsPerBatch() {
        return MAX_VECTORS_PER_BATCH;
    }

    public static int getMaxMetadataSize() {
        return MAX_METADATA_SIZE;
    }

    public static int getMaxSearchK() {
        return MAX_SEARCH_K;
    }
}
