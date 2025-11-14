package com.veccy.utils;

import com.veccy.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils.
 */
public class ValidationUtilsTest {

    @Test
    public void testValidateVector_Valid() {
        double[] vector = {1.0, 2.0, 3.0};
        assertTrue(ValidationUtils.validateVector(vector));
        assertTrue(ValidationUtils.validateVector(vector, null, null, true));
        assertTrue(ValidationUtils.validateVector(vector, 2, 5, true));
    }

    @Test
    public void testValidateVector_Null() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(null));
    }

    @Test
    public void testValidateVector_Empty() {
        double[] vector = {};
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector));
    }

    @Test
    public void testValidateVector_MinDimension() {
        double[] vector = {1.0, 2.0};
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector, 3, null, true));
    }

    @Test
    public void testValidateVector_MaxDimension() {
        double[] vector = {1.0, 2.0, 3.0, 4.0, 5.0};
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector, null, 3, true));
    }

    @Test
    public void testValidateVector_ZeroVector() {
        double[] vector = {0.0, 0.0, 0.0};

        // Should be valid when allowZero=true
        assertTrue(ValidationUtils.validateVector(vector, null, null, true));

        // Should throw when allowZero=false
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector, null, null, false));
    }

    @Test
    public void testValidateVector_NaN() {
        double[] vector = {1.0, Double.NaN, 3.0};
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector));
    }

    @Test
    public void testValidateVector_Infinite() {
        double[] vector1 = {1.0, Double.POSITIVE_INFINITY, 3.0};
        double[] vector2 = {1.0, Double.NEGATIVE_INFINITY, 3.0};

        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector1));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVector(vector2));
    }

    @Test
    public void testValidateVectors_Valid() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0},
            {7.0, 8.0, 9.0}
        };
        assertTrue(ValidationUtils.validateVectors(vectors));
        assertTrue(ValidationUtils.validateVectors(vectors, null, null, true, true));
    }

    @Test
    public void testValidateVectors_Null() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVectors(null));
    }

    @Test
    public void testValidateVectors_Empty() {
        double[][] vectors = {};
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVectors(vectors));
    }

    @Test
    public void testValidateVectors_NullElement() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            null,
            {7.0, 8.0, 9.0}
        };
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVectors(vectors));
    }

    @Test
    public void testValidateVectors_InconsistentDimensions() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0},
            {7.0, 8.0, 9.0}
        };

        // Should throw when consistentDims=true
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVectors(vectors, null, null, true, true));

        // Should be valid when consistentDims=false
        assertTrue(ValidationUtils.validateVectors(vectors, null, null, true, false));
    }

    @Test
    public void testValidateVectors_IndividualValidation() {
        double[][] vectors = {
            {1.0, 2.0, 3.0},
            {4.0, Double.NaN, 6.0},
            {7.0, 8.0, 9.0}
        };
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateVectors(vectors));
    }

    @Test
    public void testValidateConfig_Valid() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", 123);

        assertTrue(ValidationUtils.validateConfig(config, null, null));
    }

    @Test
    public void testValidateConfig_Null() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateConfig(null, null, null));
    }

    @Test
    public void testValidateConfig_RequiredKeys() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", 123);

        List<String> requiredKeys = List.of("key1", "key2");
        assertTrue(ValidationUtils.validateConfig(config, requiredKeys, null));

        List<String> missingKeys = List.of("key1", "key2", "key3");
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateConfig(config, missingKeys, null));
    }

    @Test
    public void testValidateConfig_AllowedKeys() {
        Map<String, Object> config = new HashMap<>();
        config.put("key1", "value1");
        config.put("key2", 123);

        List<String> allowedKeys = List.of("key1", "key2", "key3");
        assertTrue(ValidationUtils.validateConfig(config, null, allowedKeys));

        List<String> restrictedKeys = List.of("key1");
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateConfig(config, null, restrictedKeys));
    }

    @Test
    public void testValidateConfigValues_Valid() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "test");
        config.put("count", 10);
        config.put("ratio", 0.5);

        Map<String, Class<?>> valueTypes = new HashMap<>();
        valueTypes.put("name", String.class);
        valueTypes.put("count", Integer.class);

        assertTrue(ValidationUtils.validateConfigValues(config, valueTypes, null));
    }

    @Test
    public void testValidateConfigValues_InvalidType() {
        Map<String, Object> config = new HashMap<>();
        config.put("name", "test");
        config.put("count", "not a number");

        Map<String, Class<?>> valueTypes = new HashMap<>();
        valueTypes.put("count", Integer.class);

        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateConfigValues(config, valueTypes, null));
    }

    @Test
    public void testValidateConfigValues_Range() {
        Map<String, Object> config = new HashMap<>();
        config.put("ratio", 0.5);
        config.put("count", 10);

        Map<String, double[]> valueRanges = new HashMap<>();
        valueRanges.put("ratio", new double[]{0.0, 1.0});
        valueRanges.put("count", new double[]{0.0, 100.0});

        assertTrue(ValidationUtils.validateConfigValues(config, null, valueRanges));

        // Test out of range
        config.put("ratio", 1.5);
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateConfigValues(config, null, valueRanges));
    }

    @Test
    public void testValidateMetric_Valid() {
        assertTrue(ValidationUtils.validateMetric("cosine"));
        assertTrue(ValidationUtils.validateMetric("euclidean"));
        assertTrue(ValidationUtils.validateMetric("dot_product"));
        assertTrue(ValidationUtils.validateMetric("manhattan"));
        assertTrue(ValidationUtils.validateMetric("chebyshev"));
        assertTrue(ValidationUtils.validateMetric("hamming"));
        assertTrue(ValidationUtils.validateMetric("jaccard"));
    }

    @Test
    public void testValidateMetric_Invalid() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateMetric("invalid_metric"));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateMetric(null));
    }

    @Test
    public void testValidateK_Valid() {
        assertTrue(ValidationUtils.validateK(1));
        assertTrue(ValidationUtils.validateK(10));
        assertTrue(ValidationUtils.validateK(5, 10));
    }

    @Test
    public void testValidateK_Invalid() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateK(0));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateK(-5));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateK(15, 10));
    }

    @Test
    public void testValidateId_Valid() {
        assertTrue(ValidationUtils.validateId("id123"));
        assertTrue(ValidationUtils.validateId("vector-001"));
        assertTrue(ValidationUtils.validateId("uuid-abc-def"));
    }

    @Test
    public void testValidateId_Invalid() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateId(null));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateId(""));
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateId("   "));
    }

    @Test
    public void testValidateIds_Valid() {
        List<String> ids = List.of("id1", "id2", "id3");
        assertTrue(ValidationUtils.validateIds(ids));
    }

    @Test
    public void testValidateIds_Null() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateIds(null));
    }

    @Test
    public void testValidateIds_Empty() {
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateIds(List.of()));
    }

    @Test
    public void testValidateIds_InvalidElement() {
        List<String> ids = List.of("id1", "", "id3");
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateIds(ids));
    }

    @Test
    public void testValidateIds_Duplicates() {
        List<String> ids = List.of("id1", "id2", "id1");
        assertThrows(ValidationException.class, () ->
            ValidationUtils.validateIds(ids));
    }

    @Test
    public void testValidateMetadata_Valid() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");
        metadata.put("category", 123);
        metadata.put("tags", List.of("tag1", "tag2"));
        metadata.put("nested", Map.of("key", "value"));

        assertTrue(ValidationUtils.validateMetadata(metadata));
    }

    @Test
    public void testValidateMetadata_Null() {
        assertTrue(ValidationUtils.validateMetadata(null));
    }

    @Test
    public void testValidateMetadata_EmptyIsValid() {
        Map<String, Object> metadata = new HashMap<>();
        assertTrue(ValidationUtils.validateMetadata(metadata));
    }

    @Test
    public void testValidateMetadata_JSONSerializable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", "test");
        metadata.put("count", 123);
        metadata.put("ratio", 0.5);
        metadata.put("flag", true);
        metadata.put("list", Arrays.asList(1, 2, 3));
        metadata.put("nested", Collections.singletonMap("key", "value"));

        assertTrue(ValidationUtils.validateMetadata(metadata));
    }

    @Test
    public void testValidateMetadataList_Valid() {
        List<Map<String, Object>> metadataList = Arrays.asList(
            Map.of("label", "test1"),
            Map.of("label", "test2"),
            Map.of("label", "test3")
        );

        assertTrue(ValidationUtils.validateMetadataList(metadataList));
    }

    @Test
    public void testValidateMetadataList_Null() {
        assertTrue(ValidationUtils.validateMetadataList(null));
    }

    @Test
    public void testValidateMetadataList_NullElement() {
        List<Map<String, Object>> metadataList = Arrays.asList(
            Map.of("label", "test1"),
            null,
            Map.of("label", "test3")
        );

        // null elements in the list are valid (they're treated as null metadata)
        assertTrue(ValidationUtils.validateMetadataList(metadataList));
    }

    @Test
    public void testValidateMetadataList_Empty() {
        List<Map<String, Object>> metadataList = new ArrayList<>();
        assertTrue(ValidationUtils.validateMetadataList(metadataList));
    }

    @Test
    public void testErrorMessages() {
        // Test that error messages are descriptive
        try {
            ValidationUtils.validateVector(null);
            fail("Should have thrown exception");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("null"));
        }

        try {
            double[] vector = {1.0, 2.0};
            ValidationUtils.validateVector(vector, 5, null, true);
            fail("Should have thrown exception");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("dimension"));
            assertTrue(e.getMessage().contains("minimum"));
        }

        try {
            ValidationUtils.validateMetric("invalid");
            fail("Should have thrown exception");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Invalid metric"));
        }

        try {
            ValidationUtils.validateK(0);
            fail("Should have thrown exception");
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("positive"));
        }
    }
}
