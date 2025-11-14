package com.veccy.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SearchRequest.
 */
class SearchRequestTest {

    @Test
    void testDefaultConstructor() {
        SearchRequest request = new SearchRequest();

        assertNotNull(request);
        assertNull(request.getQueryVector());
        assertEquals(10, request.getK()); // Default k value
    }

    @Test
    void testSetAndGetQueryVector() {
        SearchRequest request = new SearchRequest();
        double[] vector = {1.0, 2.0, 3.0};

        request.setQueryVector(vector);

        assertArrayEquals(vector, request.getQueryVector());
    }

    @Test
    void testSetAndGetK() {
        SearchRequest request = new SearchRequest();

        request.setK(5);
        assertEquals(5, request.getK());

        request.setK(100);
        assertEquals(100, request.getK());
    }

    @Test
    void testWithNullVector() {
        SearchRequest request = new SearchRequest();
        request.setQueryVector(null);

        assertNull(request.getQueryVector());
    }

    @Test
    void testWithEmptyVector() {
        SearchRequest request = new SearchRequest();
        double[] emptyVector = {};

        request.setQueryVector(emptyVector);

        assertArrayEquals(emptyVector, request.getQueryVector());
        assertEquals(0, request.getQueryVector().length);
    }

    @Test
    void testWithLargeVector() {
        SearchRequest request = new SearchRequest();
        double[] largeVector = new double[768]; // Typical embedding size

        for (int i = 0; i < largeVector.length; i++) {
            largeVector[i] = Math.random();
        }

        request.setQueryVector(largeVector);

        assertEquals(768, request.getQueryVector().length);
    }

    @Test
    void testWithNegativeK() {
        SearchRequest request = new SearchRequest();
        request.setK(-5);

        assertEquals(-5, request.getK()); // No validation in DTO
    }

    @Test
    void testWithZeroK() {
        SearchRequest request = new SearchRequest();
        request.setK(0);

        assertEquals(0, request.getK());
    }

    @Test
    void testWithLargeK() {
        SearchRequest request = new SearchRequest();
        request.setK(10000);

        assertEquals(10000, request.getK());
    }

    @Test
    void testVectorWithSpecialValues() {
        SearchRequest request = new SearchRequest();
        double[] vector = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            0.0,
            -0.0,
            Double.MIN_VALUE,
            Double.MAX_VALUE
        };

        request.setQueryVector(vector);

        assertArrayEquals(vector, request.getQueryVector());
    }

    @Test
    void testVectorImmutability() {
        SearchRequest request = new SearchRequest();
        double[] original = {1.0, 2.0, 3.0};
        double[] copy = original.clone();

        request.setQueryVector(original);
        original[0] = 999.0; // Modify original

        // Vector in request is affected (no defensive copy)
        assertEquals(999.0, request.getQueryVector()[0]);
    }

    @Test
    void testMultipleUpdates() {
        SearchRequest request = new SearchRequest();

        request.setQueryVector(new double[]{1.0, 2.0});
        request.setK(5);

        assertEquals(2, request.getQueryVector().length);
        assertEquals(5, request.getK());

        request.setQueryVector(new double[]{3.0, 4.0, 5.0});
        request.setK(10);

        assertEquals(3, request.getQueryVector().length);
        assertEquals(10, request.getK());
    }

    @Test
    void testTypicalUsageScenario() {
        SearchRequest request = new SearchRequest();

        // Set up a typical search request
        double[] embedding = new double[384]; // Common embedding dimension
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = Math.sin(i) * 0.1;
        }

        request.setQueryVector(embedding);
        request.setK(20);

        assertNotNull(request.getQueryVector());
        assertEquals(384, request.getQueryVector().length);
        assertEquals(20, request.getK());
    }

    @Test
    void testDefaultKValue() {
        SearchRequest request1 = new SearchRequest();
        assertEquals(10, request1.getK());

        SearchRequest request2 = new SearchRequest();
        assertEquals(10, request2.getK());

        // Default is consistent across instances
        assertEquals(request1.getK(), request2.getK());
    }

    @Test
    void testVectorWithAllZeros() {
        SearchRequest request = new SearchRequest();
        double[] zeros = new double[100];

        request.setQueryVector(zeros);

        assertEquals(100, request.getQueryVector().length);
        for (double value : request.getQueryVector()) {
            assertEquals(0.0, value);
        }
    }

    @Test
    void testVectorWithAllOnes() {
        SearchRequest request = new SearchRequest();
        double[] ones = new double[50];
        for (int i = 0; i < ones.length; i++) {
            ones[i] = 1.0;
        }

        request.setQueryVector(ones);

        assertEquals(50, request.getQueryVector().length);
        for (double value : request.getQueryVector()) {
            assertEquals(1.0, value);
        }
    }

    @Test
    void testVectorWithMixedValues() {
        SearchRequest request = new SearchRequest();
        double[] mixed = {-1.5, 0.0, 1.5, 2.5, -2.5};

        request.setQueryVector(mixed);

        assertArrayEquals(mixed, request.getQueryVector());
    }
}
