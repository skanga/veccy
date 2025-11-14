package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BatchSearchRequest DTO.
 */
class BatchSearchRequestTest {

    @Test
    void testDefaultConstructor() {
        BatchSearchRequest request = new BatchSearchRequest();

        assertNull(request.getQueryVectors());
        assertEquals(10, request.getK()); // Default value
    }

    @Test
    void testSetAndGetQueryVectors() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(
                new double[]{1.0, 2.0, 3.0},
                new double[]{4.0, 5.0, 6.0}
        );

        request.setQueryVectors(queryVectors);

        assertEquals(queryVectors, request.getQueryVectors());
        assertEquals(2, request.getQueryVectors().size());
    }

    @Test
    void testSetAndGetK() {
        BatchSearchRequest request = new BatchSearchRequest();

        request.setK(5);

        assertEquals(5, request.getK());
    }

    @Test
    void testWithNullQueryVectors() {
        BatchSearchRequest request = new BatchSearchRequest();
        request.setQueryVectors(null);

        assertNull(request.getQueryVectors());
    }

    @Test
    void testWithEmptyQueryVectors() {
        BatchSearchRequest request = new BatchSearchRequest();
        request.setQueryVectors(new ArrayList<>());

        assertTrue(request.getQueryVectors().isEmpty());
    }

    @Test
    void testWithSingleQueryVector() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(new double[]{1.0, 2.0, 3.0});
        request.setQueryVectors(queryVectors);

        assertEquals(1, request.getQueryVectors().size());
    }

    @Test
    void testWithMultipleQueryVectors() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0},
                new double[]{5.0, 6.0},
                new double[]{7.0, 8.0}
        );

        request.setQueryVectors(queryVectors);

        assertEquals(4, request.getQueryVectors().size());
    }

    @Test
    void testDefaultKValue() {
        BatchSearchRequest request = new BatchSearchRequest();

        assertEquals(10, request.getK());
    }

    @Test
    void testSetKToZero() {
        BatchSearchRequest request = new BatchSearchRequest();
        request.setK(0);

        assertEquals(0, request.getK());
    }

    @Test
    void testSetKToNegative() {
        BatchSearchRequest request = new BatchSearchRequest();
        request.setK(-1);

        assertEquals(-1, request.getK());
    }

    @Test
    void testSetKToLargeValue() {
        BatchSearchRequest request = new BatchSearchRequest();
        request.setK(1000);

        assertEquals(1000, request.getK());
    }

    @Test
    void testWithHighDimensionalVectors() {
        BatchSearchRequest request = new BatchSearchRequest();

        double[] highDimVector1 = new double[1536];
        double[] highDimVector2 = new double[1536];
        Arrays.fill(highDimVector1, 0.5);
        Arrays.fill(highDimVector2, 0.7);

        request.setQueryVectors(Arrays.asList(highDimVector1, highDimVector2));

        assertEquals(2, request.getQueryVectors().size());
        assertEquals(1536, request.getQueryVectors().get(0).length);
        assertEquals(1536, request.getQueryVectors().get(1).length);
    }

    @Test
    void testWithEmptyVector() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(new double[]{});
        request.setQueryVectors(queryVectors);

        assertEquals(1, request.getQueryVectors().size());
        assertEquals(0, request.getQueryVectors().get(0).length);
    }

    @Test
    void testWithMixedDimensions() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(
                new double[]{1.0, 2.0},
                new double[]{3.0, 4.0, 5.0},
                new double[]{6.0}
        );

        request.setQueryVectors(queryVectors);

        assertEquals(3, request.getQueryVectors().size());
        assertEquals(2, request.getQueryVectors().get(0).length);
        assertEquals(3, request.getQueryVectors().get(1).length);
        assertEquals(1, request.getQueryVectors().get(2).length);
    }

    @Test
    void testMultipleSetCalls() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> vectors1 = Arrays.asList(new double[]{1.0});
        List<double[]> vectors2 = Arrays.asList(new double[]{2.0});

        request.setQueryVectors(vectors1);
        request.setQueryVectors(vectors2);

        assertEquals(vectors2, request.getQueryVectors());
    }

    @Test
    void testSetKMultipleTimes() {
        BatchSearchRequest request = new BatchSearchRequest();

        request.setK(5);
        request.setK(20);
        request.setK(15);

        assertEquals(15, request.getK());
    }

    @Test
    void testFullRequestConfiguration() {
        BatchSearchRequest request = new BatchSearchRequest();

        List<double[]> queryVectors = Arrays.asList(
                new double[]{1.0, 2.0, 3.0},
                new double[]{4.0, 5.0, 6.0}
        );

        request.setQueryVectors(queryVectors);
        request.setK(5);

        assertEquals(2, request.getQueryVectors().size());
        assertEquals(5, request.getK());
    }

    @Test
    void testWithSpecialDoubleValues() {
        BatchSearchRequest request = new BatchSearchRequest();

        double[] specialVector = {
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                0.0,
                -0.0
        };

        request.setQueryVectors(Arrays.asList(specialVector));

        assertEquals(1, request.getQueryVectors().size());
        assertEquals(7, request.getQueryVectors().get(0).length);
        assertTrue(Double.isNaN(request.getQueryVectors().get(0)[0]));
    }

    @Test
    void testSettersReturnVoid() {
        BatchSearchRequest request = new BatchSearchRequest();

        // Verify setters work without returning anything
        request.setQueryVectors(new ArrayList<>());
        request.setK(5);

        assertNotNull(request.getQueryVectors());
        assertEquals(5, request.getK());
    }
}
