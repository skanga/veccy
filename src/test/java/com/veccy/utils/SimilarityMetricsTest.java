package com.veccy.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimilarityMetrics utility class.
 */
public class SimilarityMetricsTest {

    private static final double EPSILON = 1e-6;

    @Test
    public void testCosineSimilarity() {
        // Test identical vectors
        double[] v1 = {1.0, 0.0, 0.0};
        double[] v2 = {1.0, 0.0, 0.0};
        assertEquals(1.0, SimilarityMetrics.cosineSimilarity(v1, v2), EPSILON);

        // Test orthogonal vectors
        double[] v3 = {1.0, 0.0, 0.0};
        double[] v4 = {0.0, 1.0, 0.0};
        assertEquals(0.0, SimilarityMetrics.cosineSimilarity(v3, v4), EPSILON);

        // Test opposite vectors
        double[] v5 = {1.0, 0.0, 0.0};
        double[] v6 = {-1.0, 0.0, 0.0};
        assertEquals(-1.0, SimilarityMetrics.cosineSimilarity(v5, v6), EPSILON);

        // Test general case
        double[] v7 = {1.0, 2.0, 3.0};
        double[] v8 = {4.0, 5.0, 6.0};
        double expected = (1*4 + 2*5 + 3*6) / (Math.sqrt(14) * Math.sqrt(77));
        assertEquals(expected, SimilarityMetrics.cosineSimilarity(v7, v8), EPSILON);
    }

    @Test
    public void testCosineSimilarityZeroVector() {
        // Test with zero vector
        double[] v1 = {0.0, 0.0, 0.0};
        double[] v2 = {1.0, 2.0, 3.0};
        assertEquals(0.0, SimilarityMetrics.cosineSimilarity(v1, v2), EPSILON);
    }

    @Test
    public void testCosineDistance() {
        double[] v1 = {1.0, 0.0, 0.0};
        double[] v2 = {1.0, 0.0, 0.0};

        // Distance = 1 - similarity
        double similarity = SimilarityMetrics.cosineSimilarity(v1, v2);
        double distance = SimilarityMetrics.cosineDistance(v1, v2);
        assertEquals(1.0 - similarity, distance, EPSILON);
        assertEquals(0.0, distance, EPSILON);

        // Test orthogonal vectors (distance should be 1.0)
        double[] v3 = {1.0, 0.0, 0.0};
        double[] v4 = {0.0, 1.0, 0.0};
        assertEquals(1.0, SimilarityMetrics.cosineDistance(v3, v4), EPSILON);
    }

    @Test
    public void testEuclideanDistance() {
        // Test identical vectors
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {1.0, 2.0, 3.0};
        assertEquals(0.0, SimilarityMetrics.euclideanDistance(v1, v2), EPSILON);

        // Test simple case
        double[] v3 = {0.0, 0.0, 0.0};
        double[] v4 = {3.0, 4.0, 0.0};
        assertEquals(5.0, SimilarityMetrics.euclideanDistance(v3, v4), EPSILON);

        // Test general case
        double[] v5 = {1.0, 2.0, 3.0};
        double[] v6 = {4.0, 6.0, 8.0};
        double expected = Math.sqrt(9 + 16 + 25); // sqrt(50)
        assertEquals(expected, SimilarityMetrics.euclideanDistance(v5, v6), EPSILON);
    }

    @Test
    public void testEuclideanDistanceSquared() {
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {4.0, 6.0, 8.0};

        double distanceSquared = SimilarityMetrics.euclideanDistanceSquared(v1, v2);
        double distance = SimilarityMetrics.euclideanDistance(v1, v2);

        assertEquals(50.0, distanceSquared, EPSILON);
        assertEquals(distance * distance, distanceSquared, EPSILON);
    }

    @Test
    public void testManhattanDistance() {
        // Test identical vectors
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {1.0, 2.0, 3.0};
        assertEquals(0.0, SimilarityMetrics.manhattanDistance(v1, v2), EPSILON);

        // Test simple case
        double[] v3 = {0.0, 0.0, 0.0};
        double[] v4 = {1.0, 2.0, 3.0};
        assertEquals(6.0, SimilarityMetrics.manhattanDistance(v3, v4), EPSILON);

        // Test with negative differences
        double[] v5 = {5.0, 3.0, 1.0};
        double[] v6 = {2.0, 7.0, 6.0};
        assertEquals(3.0 + 4.0 + 5.0, SimilarityMetrics.manhattanDistance(v5, v6), EPSILON);
    }

    @Test
    public void testChebyshevDistance() {
        // Test identical vectors
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {1.0, 2.0, 3.0};
        assertEquals(0.0, SimilarityMetrics.chebyshevDistance(v1, v2), EPSILON);

        // Test simple case (max difference is in the third dimension)
        double[] v3 = {0.0, 0.0, 0.0};
        double[] v4 = {1.0, 2.0, 5.0};
        assertEquals(5.0, SimilarityMetrics.chebyshevDistance(v3, v4), EPSILON);

        // Test with negative differences
        double[] v5 = {10.0, 5.0, 3.0};
        double[] v6 = {2.0, 7.0, 6.0};
        assertEquals(8.0, SimilarityMetrics.chebyshevDistance(v5, v6), EPSILON);
    }

    @Test
    public void testDotProduct() {
        // Test orthogonal vectors
        double[] v1 = {1.0, 0.0, 0.0};
        double[] v2 = {0.0, 1.0, 0.0};
        assertEquals(0.0, SimilarityMetrics.dotProduct(v1, v2), EPSILON);

        // Test simple case
        double[] v3 = {1.0, 2.0, 3.0};
        double[] v4 = {4.0, 5.0, 6.0};
        assertEquals(32.0, SimilarityMetrics.dotProduct(v3, v4), EPSILON);

        // Test with negative values
        double[] v5 = {1.0, -2.0, 3.0};
        double[] v6 = {-1.0, 2.0, -3.0};
        assertEquals(-14.0, SimilarityMetrics.dotProduct(v5, v6), EPSILON);
    }

    @Test
    public void testHammingDistance() {
        // Test identical vectors
        double[] v1 = {1.0, 0.0, 1.0, 0.0};
        double[] v2 = {1.0, 0.0, 1.0, 0.0};
        assertEquals(0, SimilarityMetrics.hammingDistance(v1, v2));

        // Test completely different
        double[] v3 = {1.0, 1.0, 1.0, 1.0};
        double[] v4 = {0.0, 0.0, 0.0, 0.0};
        assertEquals(4, SimilarityMetrics.hammingDistance(v3, v4));

        // Test partial differences
        double[] v5 = {1.0, 0.0, 1.0, 0.0};
        double[] v6 = {1.0, 1.0, 0.0, 0.0};
        assertEquals(2, SimilarityMetrics.hammingDistance(v5, v6));
    }

    @Test
    public void testJaccardSimilarity() {
        // Test identical sets
        double[] v1 = {1.0, 0.0, 1.0, 0.0};
        double[] v2 = {1.0, 0.0, 1.0, 0.0};
        assertEquals(1.0, SimilarityMetrics.jaccardSimilarity(v1, v2), EPSILON);

        // Test disjoint sets
        double[] v3 = {1.0, 1.0, 0.0, 0.0};
        double[] v4 = {0.0, 0.0, 1.0, 1.0};
        assertEquals(0.0, SimilarityMetrics.jaccardSimilarity(v3, v4), EPSILON);

        // Test partial overlap
        double[] v5 = {1.0, 1.0, 1.0, 0.0};
        double[] v6 = {1.0, 0.0, 1.0, 1.0};
        // intersection = 2 (positions 0 and 2), union = 4 (all positions)
        assertEquals(0.5, SimilarityMetrics.jaccardSimilarity(v5, v6), EPSILON);
    }

    @Test
    public void testJaccardDistance() {
        double[] v1 = {1.0, 0.0, 1.0, 0.0};
        double[] v2 = {1.0, 0.0, 1.0, 0.0};

        // Distance = 1 - similarity
        double similarity = SimilarityMetrics.jaccardSimilarity(v1, v2);
        double distance = SimilarityMetrics.jaccardDistance(v1, v2);
        assertEquals(1.0 - similarity, distance, EPSILON);
        assertEquals(0.0, distance, EPSILON);
    }

    @Test
    public void testNorm() {
        // Test zero vector
        double[] v1 = {0.0, 0.0, 0.0};
        assertEquals(0.0, SimilarityMetrics.norm(v1), EPSILON);

        // Test unit vector
        double[] v2 = {1.0, 0.0, 0.0};
        assertEquals(1.0, SimilarityMetrics.norm(v2), EPSILON);

        // Test 3-4-5 triangle
        double[] v3 = {3.0, 4.0, 0.0};
        assertEquals(5.0, SimilarityMetrics.norm(v3), EPSILON);

        // Test general case
        double[] v4 = {1.0, 2.0, 3.0};
        assertEquals(Math.sqrt(14), SimilarityMetrics.norm(v4), EPSILON);
    }

    @Test
    public void testNormalizeL2() {
        // Test already normalized vector
        double[] v1 = {1.0, 0.0, 0.0};
        double[] normalized1 = SimilarityMetrics.normalizeVector(v1);
        assertArrayEquals(new double[]{1.0, 0.0, 0.0}, normalized1, EPSILON);

        // Test general vector
        double[] v2 = {3.0, 4.0, 0.0};
        double[] normalized2 = SimilarityMetrics.normalizeVector(v2);
        assertArrayEquals(new double[]{0.6, 0.8, 0.0}, normalized2, EPSILON);
        assertEquals(1.0, SimilarityMetrics.norm(normalized2), EPSILON);

        // Test zero vector (should return zero vector)
        double[] v3 = {0.0, 0.0, 0.0};
        double[] normalized3 = SimilarityMetrics.normalizeVector(v3);
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, normalized3, EPSILON);
    }


    @Test
    public void testBatchCosineSimilarity() {
        double[][] vectorsA = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0}
        };
        double[][] vectorsB = {
            {1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 1.0}
        };

        double[][] similarities = SimilarityMetrics.batchCosineSimilarity(vectorsA, vectorsB);

        // Check dimensions
        assertEquals(2, similarities.length);
        assertEquals(3, similarities[0].length);

        // Check values
        assertEquals(1.0, similarities[0][0], EPSILON); // [1,0,0] vs [1,0,0]
        assertEquals(0.0, similarities[0][1], EPSILON); // [1,0,0] vs [0,1,0]
        assertEquals(0.0, similarities[0][2], EPSILON); // [1,0,0] vs [0,0,1]
        assertEquals(0.0, similarities[1][0], EPSILON); // [0,1,0] vs [1,0,0]
        assertEquals(1.0, similarities[1][1], EPSILON); // [0,1,0] vs [0,1,0]
        assertEquals(0.0, similarities[1][2], EPSILON); // [0,1,0] vs [0,0,1]
    }

    @Test
    public void testInvalidDimensions() {
        double[] v1 = {1.0, 2.0, 3.0};
        double[] v2 = {1.0, 2.0};

        // These should throw ValidationException
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.cosineSimilarity(v1, v2));
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.euclideanDistance(v1, v2));
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.manhattanDistance(v1, v2));
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.dotProduct(v1, v2));
    }

    @Test
    public void testNullVectors() {
        double[] v1 = {1.0, 2.0, 3.0};

        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.cosineSimilarity(null, v1));
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.cosineSimilarity(v1, null));
        assertThrows(com.veccy.exceptions.ValidationException.class, () ->
            SimilarityMetrics.euclideanDistance(null, v1));
    }
}
