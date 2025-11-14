package com.veccy.utils;

import com.veccy.exceptions.ValidationException;

/**
 * Similarity and distance computation utilities for vector operations.
 * Provides various metrics including cosine, Euclidean, Manhattan, and more.
 */
public final class SimilarityMetrics {

    private SimilarityMetrics() {
        // Utility class, prevent instantiation
    }

    /**
     * Compute cosine similarity between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity value between -1 and 1
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        validateVectors(a, b);

        double dotProduct = dotProduct(a, b);
        double normA = norm(a);
        double normB = norm(b);

        // Handle zero vectors
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (normA * normB);

        // Clamp to valid range to handle numerical precision issues
        return Math.max(-1.0, Math.min(1.0, similarity));
    }

    /**
     * Compute cosine distance between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine distance value between 0 and 2
     */
    public static double cosineDistance(double[] a, double[] b) {
        return 1.0 - cosineSimilarity(a, b);
    }

    /**
     * Compute Euclidean distance between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return Euclidean distance value
     */
    public static double euclideanDistance(double[] a, double[] b) {
        validateVectors(a, b);
        return Math.sqrt(euclideanDistanceSquared(a, b));
    }

    /**
     * Compute squared Euclidean distance between two vectors.
     * This is more efficient when you don't need the actual distance.
     *
     * @param a first vector
     * @param b second vector
     * @return squared Euclidean distance value
     */
    public static double euclideanDistanceSquared(double[] a, double[] b) {
        validateVectors(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * Compute dot product between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return dot product value
     */
    public static double dotProduct(double[] a, double[] b) {
        validateVectors(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Compute Manhattan distance between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return Manhattan distance value
     */
    public static double manhattanDistance(double[] a, double[] b) {
        validateVectors(a, b);

        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum;
    }

    /**
     * Compute Chebyshev distance between two vectors.
     *
     * @param a first vector
     * @param b second vector
     * @return Chebyshev distance value
     */
    public static double chebyshevDistance(double[] a, double[] b) {
        validateVectors(a, b);

        double max = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = Math.abs(a[i] - b[i]);
            if (diff > max) {
                max = diff;
            }
        }
        return max;
    }

    /**
     * Compute Hamming distance between two binary vectors.
     *
     * @param a first binary vector
     * @param b second binary vector
     * @return Hamming distance value
     */
    public static int hammingDistance(double[] a, double[] b) {
        validateVectors(a, b);

        int distance = 0;
        for (int i = 0; i < a.length; i++) {
            boolean aBit = a[i] > 0;
            boolean bBit = b[i] > 0;
            if (aBit != bBit) {
                distance++;
            }
        }
        return distance;
    }

    /**
     * Compute Jaccard similarity between two binary vectors.
     *
     * @param a first binary vector
     * @param b second binary vector
     * @return Jaccard similarity value between 0 and 1
     */
    public static double jaccardSimilarity(double[] a, double[] b) {
        validateVectors(a, b);

        int intersection = 0;
        int union = 0;

        for (int i = 0; i < a.length; i++) {
            boolean aBit = a[i] > 0;
            boolean bBit = b[i] > 0;

            if (aBit && bBit) {
                intersection++;
            }
            if (aBit || bBit) {
                union++;
            }
        }

        // Handle case where both vectors are all zeros
        if (union == 0) {
            return intersection == 0 ? 1.0 : 0.0;
        }

        return (double) intersection / union;
    }

    /**
     * Compute Jaccard distance between two binary vectors.
     *
     * @param a first binary vector
     * @param b second binary vector
     * @return Jaccard distance value between 0 and 1
     */
    public static double jaccardDistance(double[] a, double[] b) {
        return 1.0 - jaccardSimilarity(a, b);
    }

    /**
     * Normalize a vector to unit length.
     *
     * @param vector vector to normalize
     * @param norm type of norm ('L1', 'L2', or 'MAX')
     * @return normalized vector (new array)
     */
    public static double[] normalizeVector(double[] vector, NormType norm) {
        if (vector == null) {
            throw new ValidationException("Vector cannot be null");
        }

        double normValue;
        switch (norm) {
            case L2:
                normValue = norm(vector);
                break;
            case L1:
                normValue = 0.0;
                for (double v : vector) {
                    normValue += Math.abs(v);
                }
                break;
            case MAX:
                normValue = 0.0;
                for (double v : vector) {
                    double abs = Math.abs(v);
                    if (abs > normValue) {
                        normValue = abs;
                    }
                }
                break;
            default:
                throw new ValidationException("Unsupported norm type: " + norm);
        }

        if (normValue == 0.0) {
            return vector.clone();
        }

        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / normValue;
        }
        return normalized;
    }

    /**
     * Normalize a vector to unit L2 length.
     *
     * @param vector vector to normalize
     * @return normalized vector (new array)
     */
    public static double[] normalizeVector(double[] vector) {
        return normalizeVector(vector, NormType.L2);
    }

    /**
     * Compute L2 norm (magnitude) of a vector.
     *
     * @param vector the vector
     * @return L2 norm value
     */
    public static double norm(double[] vector) {
        double sum = 0.0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /**
     * Compute cosine similarity between batches of vectors.
     *
     * @param vectorsA first batch of vectors (n_vectors_a x n_dim)
     * @param vectorsB second batch of vectors (n_vectors_b x n_dim)
     * @return similarity matrix (n_vectors_a x n_vectors_b)
     */
    public static double[][] batchCosineSimilarity(double[][] vectorsA, double[][] vectorsB) {
        if (vectorsA == null || vectorsB == null) {
            throw new ValidationException("Vector arrays cannot be null");
        }
        if (vectorsA.length == 0 || vectorsB.length == 0) {
            throw new ValidationException("Vector arrays cannot be empty");
        }
        if (vectorsA[0].length != vectorsB[0].length) {
            throw new ValidationException("Vectors must have the same dimension");
        }

        int nA = vectorsA.length;
        int nB = vectorsB.length;
        int dim = vectorsA[0].length;

        double[][] similarities = new double[nA][nB];

        // Compute norms for all vectors
        double[] normsA = new double[nA];
        double[] normsB = new double[nB];

        for (int i = 0; i < nA; i++) {
            normsA[i] = norm(vectorsA[i]);
            if (normsA[i] == 0.0) {
                normsA[i] = 1.0; // Avoid division by zero
            }
        }

        for (int j = 0; j < nB; j++) {
            normsB[j] = norm(vectorsB[j]);
            if (normsB[j] == 0.0) {
                normsB[j] = 1.0; // Avoid division by zero
            }
        }

        // Compute similarities
        for (int i = 0; i < nA; i++) {
            for (int j = 0; j < nB; j++) {
                double dotProd = dotProduct(vectorsA[i], vectorsB[j]);
                double similarity = dotProd / (normsA[i] * normsB[j]);
                similarities[i][j] = Math.max(-1.0, Math.min(1.0, similarity));
            }
        }

        return similarities;
    }

    /**
     * Validate that two vectors are compatible for distance/similarity computation.
     *
     * @param a first vector
     * @param b second vector
     * @throws ValidationException if vectors are invalid
     */
    private static void validateVectors(double[] a, double[] b) {
        if (a == null || b == null) {
            throw new ValidationException("Vectors cannot be null");
        }
        if (a.length != b.length) {
            throw new ValidationException(
                    "Vectors must have the same length (got " + a.length + " and " + b.length + ")");
        }
    }

    /**
     * Enumeration of norm types for vector normalization.
     */
    public enum NormType {
        L1,
        L2,
        MAX
    }
}
