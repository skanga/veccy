package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for UpdateVectorRequest DTO.
 */
class UpdateVectorRequestTest {

    @Test
    void testDefaultConstructor() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        assertNull(request.getVector());
        assertNull(request.getMetadata());
    }

    @Test
    void testSetAndGetVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector = {1.0, 2.0, 3.0};
        request.setVector(vector);

        assertArrayEquals(vector, request.getVector());
    }

    @Test
    void testSetAndGetMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "updated");
        metadata.put("version", 2);

        request.setMetadata(metadata);

        assertEquals(metadata, request.getMetadata());
        assertEquals("updated", request.getMetadata().get("label"));
        assertEquals(2, request.getMetadata().get("version"));
    }

    @Test
    void testWithNullVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();
        request.setVector(null);

        assertNull(request.getVector());
    }

    @Test
    void testWithNullMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();
        request.setMetadata(null);

        assertNull(request.getMetadata());
    }

    @Test
    void testWithEmptyVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();
        request.setVector(new double[]{});

        assertEquals(0, request.getVector().length);
    }

    @Test
    void testWithEmptyMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();
        request.setMetadata(new HashMap<>());

        assertTrue(request.getMetadata().isEmpty());
    }

    @Test
    void testWithSingleDimensionVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector = {42.0};
        request.setVector(vector);

        assertEquals(1, request.getVector().length);
        assertEquals(42.0, request.getVector()[0]);
    }

    @Test
    void testWithHighDimensionalVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector = new double[1536];
        Arrays.fill(vector, 0.5);

        request.setVector(vector);

        assertEquals(1536, request.getVector().length);
        assertEquals(0.5, request.getVector()[0]);
        assertEquals(0.5, request.getVector()[1535]);
    }

    @Test
    void testWithComplexMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("string", "value");
        metadata.put("integer", 42);
        metadata.put("double", 3.14);
        metadata.put("boolean", true);
        metadata.put("list", Arrays.asList("a", "b", "c"));
        metadata.put("nested", Map.of("key", "value"));

        request.setMetadata(metadata);

        assertEquals(6, request.getMetadata().size());
        assertEquals("value", request.getMetadata().get("string"));
        assertEquals(42, request.getMetadata().get("integer"));
        assertEquals(true, request.getMetadata().get("boolean"));
    }

    @Test
    void testUpdateOnlyVector() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        request.setVector(new double[]{1.0, 2.0, 3.0});

        assertNotNull(request.getVector());
        assertNull(request.getMetadata());
    }

    @Test
    void testUpdateOnlyMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        Map<String, Object> metadata = Map.of("label", "test");
        request.setMetadata(metadata);

        assertNull(request.getVector());
        assertNotNull(request.getMetadata());
    }

    @Test
    void testUpdateBothVectorAndMetadata() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector = {1.0, 2.0, 3.0};
        Map<String, Object> metadata = Map.of("label", "test");

        request.setVector(vector);
        request.setMetadata(metadata);

        assertNotNull(request.getVector());
        assertNotNull(request.getMetadata());
    }

    @Test
    void testMultipleSetCalls() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector1 = {1.0, 2.0};
        double[] vector2 = {3.0, 4.0};

        request.setVector(vector1);
        request.setVector(vector2);

        assertArrayEquals(vector2, request.getVector());
    }

    @Test
    void testVectorWithSpecialValues() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        double[] vector = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                0.0,
                -0.0
        };

        request.setVector(vector);

        assertEquals(7, request.getVector().length);
        assertTrue(Double.isNaN(request.getVector()[0]));
        assertEquals(Double.POSITIVE_INFINITY, request.getVector()[1]);
        assertEquals(Double.NEGATIVE_INFINITY, request.getVector()[2]);
    }

    @Test
    void testMetadataWithNullValues() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", null);

        request.setMetadata(metadata);

        assertEquals(2, request.getMetadata().size());
        assertEquals("value1", request.getMetadata().get("key1"));
        assertNull(request.getMetadata().get("key2"));
    }

    @Test
    void testSettersReturnVoid() {
        UpdateVectorRequest request = new UpdateVectorRequest();

        // Verify setters work without returning anything
        request.setVector(new double[]{1.0});
        request.setMetadata(new HashMap<>());

        assertNotNull(request.getVector());
        assertNotNull(request.getMetadata());
    }
}
