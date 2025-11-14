package com.veccy.rest.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for InputValidator.
 */
class InputValidatorTest {

    // ===== ValidationResult Tests =====

    @Test
    void testValidationResultSuccess() {
        InputValidator.ValidationResult result = InputValidator.ValidationResult.success();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals("", result.getErrorMessage());
    }

    @Test
    void testValidationResultFailureWithSingleError() {
        InputValidator.ValidationResult result = InputValidator.ValidationResult.failure("Error message");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("Error message", result.getErrorMessage());
    }

    @Test
    void testValidationResultFailureWithMultipleErrors() {
        List<String> errors = new ArrayList<>();
        errors.add("Error 1");
        errors.add("Error 2");
        errors.add("Error 3");

        InputValidator.ValidationResult result = InputValidator.ValidationResult.failure(errors);

        assertFalse(result.isValid());
        assertEquals(3, result.getErrors().size());
        assertEquals("Error 1; Error 2; Error 3", result.getErrorMessage());
    }

    // ===== Vector Validation Tests =====

    @Test
    void testValidateVectorNull() {
        InputValidator.ValidationResult result = InputValidator.validateVector(null, 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
    }

    @Test
    void testValidateVectorEmpty() {
        InputValidator.ValidationResult result = InputValidator.validateVector(new float[]{}, 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be empty"));
    }

    @Test
    void testValidateVectorValid() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 3);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateVectorDimensionMismatch() {
        float[] vector = {1.0f, 2.0f, 3.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 5);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("dimension mismatch"));
        assertTrue(result.getErrorMessage().contains("Expected: 5"));
    }

    @Test
    void testValidateVectorExceedsMaxDimension() {
        float[] vector = new float[10001];
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum"));
    }

    @Test
    void testValidateVectorContainsNaN() {
        float[] vector = {1.0f, Float.NaN, 3.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 3);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("NaN"));
    }

    @Test
    void testValidateVectorContainsInfinity() {
        float[] vector = {1.0f, Float.POSITIVE_INFINITY, 3.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 3);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Infinity"));
    }

    @Test
    void testValidateVectorWithZeroExpectedDimensions() {
        float[] vector = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 0);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateVectorWithNegativeExpectedDimensions() {
        float[] vector = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, -1);

        assertTrue(result.isValid());
    }

    // ===== Metadata Validation Tests =====

    @Test
    void testValidateMetadataNull() {
        InputValidator.ValidationResult result = InputValidator.validateMetadata(null);

        assertTrue(result.isValid()); // Metadata is optional
    }

    @Test
    void testValidateMetadataEmpty() {
        InputValidator.ValidationResult result = InputValidator.validateMetadata(new HashMap<>());

        assertTrue(result.isValid());
    }

    @Test
    void testValidateMetadataValid() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");
        metadata.put("value", 42);

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateMetadataTooManyEntries() {
        Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < 101; i++) {
            metadata.put("key" + i, "value");
        }

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum entries"));
    }

    @Test
    void testValidateMetadataKeyNull() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(null, "value");

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateMetadataKeyEmpty() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("", "value");

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateMetadataKeyWhitespace() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("   ", "value");

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateMetadataKeyTooLong() {
        Map<String, Object> metadata = new HashMap<>();
        String longKey = "a".repeat(257);
        metadata.put(longKey, "value");

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("key too long"));
    }

    @Test
    void testValidateMetadataValueNull() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", null);

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("is null"));
    }

    @Test
    void testValidateMetadataValueTooLong() {
        Map<String, Object> metadata = new HashMap<>();
        String longValue = "a".repeat(10001);
        metadata.put("key", longValue);

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum length"));
    }

    @Test
    void testValidateMetadataTotalSizeExceeded() {
        Map<String, Object> metadata = new HashMap<>();
        // Create metadata that exceeds 1MB
        for (int i = 0; i < 100; i++) {
            metadata.put("key" + i, "a".repeat(15000));
        }

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Total metadata size exceeds"));
    }

    // ===== Vector Batch Validation Tests =====

    @Test
    void testValidateVectorBatchNull() {
        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(null, 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null"));
    }

    @Test
    void testValidateVectorBatchEmpty() {
        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(new ArrayList<>(), 0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be empty"));
    }

    @Test
    void testValidateVectorBatchValid() {
        List<float[]> vectors = new ArrayList<>();
        vectors.add(new float[]{1.0f, 2.0f});
        vectors.add(new float[]{3.0f, 4.0f});

        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(vectors, 2);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateVectorBatchExceedsMaxSize() {
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            vectors.add(new float[]{1.0f});
        }

        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(vectors, 1);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum"));
    }

    @Test
    void testValidateVectorBatchWithInvalidVectors() {
        List<float[]> vectors = new ArrayList<>();
        vectors.add(new float[]{1.0f, 2.0f});
        vectors.add(null); // Invalid
        vectors.add(new float[]{3.0f, 4.0f});

        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(vectors, 2);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("index 1"));
    }

    @Test
    void testValidateVectorBatchMultipleErrors() {
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            vectors.add(null); // All invalid
        }

        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(vectors, 2);

        assertFalse(result.isValid());
        // Should limit to 5 errors
        assertTrue(result.getErrorMessage().contains("showing first 5"));
    }

    // ===== Search Parameters Validation Tests =====

    @Test
    void testValidateSearchParamsValid() {
        float[] query = {1.0f, 2.0f, 3.0f};
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(query, 10, 3);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateSearchParamsInvalidVector() {
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(null, 10, 3);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Query vector invalid"));
    }

    @Test
    void testValidateSearchParamsKZero() {
        float[] query = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(query, 0, 2);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("must be positive"));
    }

    @Test
    void testValidateSearchParamsKNegative() {
        float[] query = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(query, -1, 2);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("must be positive"));
    }

    @Test
    void testValidateSearchParamsKExceedsMax() {
        float[] query = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(query, 1001, 2);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum"));
    }

    // ===== Dimension Validation Tests =====

    @Test
    void testValidateDimensionValid() {
        InputValidator.ValidationResult result = InputValidator.validateDimension(128);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateDimensionZero() {
        InputValidator.ValidationResult result = InputValidator.validateDimension(0);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("must be positive"));
    }

    @Test
    void testValidateDimensionNegative() {
        InputValidator.ValidationResult result = InputValidator.validateDimension(-10);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("must be positive"));
    }

    @Test
    void testValidateDimensionExceedsMax() {
        InputValidator.ValidationResult result = InputValidator.validateDimension(10001);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum"));
    }

    // ===== Vector ID Validation Tests =====

    @Test
    void testValidateVectorIdValid() {
        InputValidator.ValidationResult result = InputValidator.validateVectorId("vec123");

        assertTrue(result.isValid());
    }

    @Test
    void testValidateVectorIdNull() {
        InputValidator.ValidationResult result = InputValidator.validateVectorId(null);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateVectorIdEmpty() {
        InputValidator.ValidationResult result = InputValidator.validateVectorId("");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateVectorIdWhitespace() {
        InputValidator.ValidationResult result = InputValidator.validateVectorId("   ");

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("cannot be null or empty"));
    }

    @Test
    void testValidateVectorIdTooLong() {
        String longId = "a".repeat(257);
        InputValidator.ValidationResult result = InputValidator.validateVectorId(longId);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("exceeds maximum length"));
    }

    // ===== Configuration Limits Tests =====

    @Test
    void testGetMaxDimension() {
        assertEquals(10000, InputValidator.getMaxDimension());
    }

    @Test
    void testGetMaxVectorsPerBatch() {
        assertEquals(1000, InputValidator.getMaxVectorsPerBatch());
    }

    @Test
    void testGetMaxMetadataSize() {
        assertEquals(1024 * 1024, InputValidator.getMaxMetadataSize());
    }

    @Test
    void testGetMaxSearchK() {
        assertEquals(1000, InputValidator.getMaxSearchK());
    }

    // ===== Edge Case Tests =====

    @Test
    void testValidateVectorAtMaxDimension() {
        float[] vector = new float[10000];
        InputValidator.ValidationResult result = InputValidator.validateVector(vector, 10000);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateVectorBatchAtMaxSize() {
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            vectors.add(new float[]{1.0f});
        }

        InputValidator.ValidationResult result = InputValidator.validateVectorBatch(vectors, 1);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateMetadataAtMaxEntries() {
        Map<String, Object> metadata = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            metadata.put("key" + i, "value");
        }

        InputValidator.ValidationResult result = InputValidator.validateMetadata(metadata);

        assertTrue(result.isValid());
    }

    @Test
    void testValidateSearchParamsAtMaxK() {
        float[] query = {1.0f, 2.0f};
        InputValidator.ValidationResult result = InputValidator.validateSearchParams(query, 1000, 2);

        assertTrue(result.isValid());
    }
}
