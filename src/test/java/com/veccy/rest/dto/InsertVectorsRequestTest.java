package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for InsertVectorsRequest DTO.
 */
class InsertVectorsRequestTest {

    @Test
    void testDefaultConstructor() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        assertNull(request.getVectors());
        assertNull(request.getMetadata());
    }

    @Test
    void testSetAndGetVectors() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(
                new double[]{1.0, 2.0, 3.0},
                new double[]{4.0, 5.0, 6.0}
        );

        request.setVectors(vectors);

        assertEquals(vectors, request.getVectors());
        assertEquals(2, request.getVectors().size());
    }

    @Test
    void testSetAndGetMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("label", "A");

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("label", "B");

        List<Map<String, Object>> metadata = Arrays.asList(metadata1, metadata2);

        request.setMetadata(metadata);

        assertEquals(metadata, request.getMetadata());
        assertEquals(2, request.getMetadata().size());
    }

    @Test
    void testWithNullVectors() {
        InsertVectorsRequest request = new InsertVectorsRequest();
        request.setVectors(null);

        assertNull(request.getVectors());
    }

    @Test
    void testWithNullMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();
        request.setMetadata(null);

        assertNull(request.getMetadata());
    }

    @Test
    void testWithEmptyVectors() {
        InsertVectorsRequest request = new InsertVectorsRequest();
        request.setVectors(new ArrayList<>());

        assertTrue(request.getVectors().isEmpty());
    }

    @Test
    void testWithEmptyMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();
        request.setMetadata(new ArrayList<>());

        assertTrue(request.getMetadata().isEmpty());
    }

    @Test
    void testWithSingleVector() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(new double[]{1.0, 2.0});
        request.setVectors(vectors);

        assertEquals(1, request.getVectors().size());
    }

    @Test
    void testWithMultipleVectors() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0},
                new double[]{5.0, 6.0}
        );

        request.setVectors(vectors);

        assertEquals(3, request.getVectors().size());
    }

    @Test
    void testWithMatchingVectorsAndMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0}
        );

        List<Map<String, Object>> metadata = Arrays.asList(
                Map.of("label", "A"),
                Map.of("label", "B")
        );

        request.setVectors(vectors);
        request.setMetadata(metadata);

        assertEquals(vectors.size(), request.getMetadata().size());
    }

    @Test
    void testWithMismatchedVectorsAndMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0},
                new double[]{5.0, 6.0}
        );

        List<Map<String, Object>> metadata = Arrays.asList(
                Map.of("label", "A")
        );

        request.setVectors(vectors);
        request.setMetadata(metadata);

        assertNotEquals(vectors.size(), request.getMetadata().size());
    }

    @Test
    void testWithComplexMetadata() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        Map<String, Object> complexMetadata = new HashMap<>();
        complexMetadata.put("string", "value");
        complexMetadata.put("number", 42);
        complexMetadata.put("double", 3.14);
        complexMetadata.put("boolean", true);
        complexMetadata.put("list", Arrays.asList("a", "b", "c"));

        request.setMetadata(Arrays.asList(complexMetadata));

        assertEquals(1, request.getMetadata().size());
        assertEquals(5, request.getMetadata().get(0).size());
    }

    @Test
    void testWithHighDimensionalVectors() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        double[] highDimVector = new double[1536]; // Common embedding size
        Arrays.fill(highDimVector, 1.0);

        request.setVectors(Arrays.asList(highDimVector));

        assertEquals(1, request.getVectors().size());
        assertEquals(1536, request.getVectors().get(0).length);
    }

    @Test
    void testWithEmptyVectorArray() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors = Arrays.asList(new double[]{});
        request.setVectors(vectors);

        assertEquals(1, request.getVectors().size());
        assertEquals(0, request.getVectors().get(0).length);
    }

    @Test
    void testSettersReturnVoid() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        // Just verify setters work without returning anything
        request.setVectors(new ArrayList<>());
        request.setMetadata(new ArrayList<>());

        assertNotNull(request.getVectors());
        assertNotNull(request.getMetadata());
    }

    @Test
    void testMultipleSetCalls() {
        InsertVectorsRequest request = new InsertVectorsRequest();

        List<double[]> vectors1 = Arrays.asList(new double[]{1.0});
        List<double[]> vectors2 = Arrays.asList(new double[]{2.0});

        request.setVectors(vectors1);
        request.setVectors(vectors2);

        assertEquals(vectors2, request.getVectors());
    }
}
