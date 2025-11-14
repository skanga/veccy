package com.veccy.quantization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProductQuantizer.
 */
class ProductQuantizerTest {

    private ProductQuantizer quantizer;

    @BeforeEach
    void setUp() {
        Map<String, Object> config = new HashMap<>();
        config.put("num_subspaces", 4);
        config.put("num_clusters", 16);  // Smaller for faster tests
        config.put("max_iterations", 50);

        quantizer = new ProductQuantizer(config);
        quantizer.initialize();
    }

    @Test
    void testInitialization() {
        assertTrue(quantizer.isInitialized());
        assertFalse(quantizer.isTrained());

        Map<String, Object> stats = quantizer.getStats();
        assertEquals("ProductQuantizer", stats.get("type"));
        assertEquals(4, stats.get("num_subspaces"));
        assertEquals(16, stats.get("num_clusters"));
    }

    @Test
    void testTraining() {
        // Generate training data (64-dimensional vectors)
        double[][] trainingData = generateRandomVectors(100, 64);

        quantizer.train(trainingData);

        assertTrue(quantizer.isTrained());

        Map<String, Object> stats = quantizer.getStats();
        assertEquals(64, stats.get("dimensions"));
        assertEquals(16, stats.get("subspace_dim"));  // 64/4
        assertEquals(4, stats.get("bytes_per_vector"));
    }

    @Test
    void testQuantizeAndDequantize() {
        // Train on random data
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        // Quantize vectors
        double[][] vectors = generateRandomVectors(10, 64);
        byte[][] quantized = quantizer.quantize(vectors);

        assertEquals(10, quantized.length);
        assertEquals(4, quantized[0].length);  // 4 subspaces = 4 bytes

        // Dequantize
        double[][] dequantized = quantizer.dequantize(quantized);

        assertEquals(10, dequantized.length);
        assertEquals(64, dequantized[0].length);
    }

    @Test
    void testQuantizationAccuracy() {
        // Train on structured data
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        // Quantize and dequantize
        double[][] vectors = generateRandomVectors(10, 64);
        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // Check that dequantized vectors are reasonably close to originals
        for (int i = 0; i < vectors.length; i++) {
            double dist = euclideanDistance(vectors[i], dequantized[i]);
            // Distance should be reasonable (not too large)
            assertTrue(dist < 50.0, "Quantization error too large: " + dist);
        }
    }

    @Test
    void testComputeDistance() {
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        double[] query = generateRandomVector(64);
        double[][] database = generateRandomVectors(5, 64);

        byte[][] quantizedDB = quantizer.quantize(database);

        // Compute distance using quantized vectors
        for (int i = 0; i < database.length; i++) {
            double quantizedDist = quantizer.computeDistance(query, quantizedDB[i]);

            // Distance should be positive
            assertTrue(quantizedDist >= 0);

            // Compute exact distance for comparison
            double exactDist = euclideanDistance(query, database[i]);

            // Quantized distance should be somewhat close to exact
            // (though not perfect due to quantization error)
            double error = Math.abs(quantizedDist - exactDist);

            // Handle edge case where exact distance is very small
            if (exactDist > 1.0) {
                double relativeError = error / exactDist;
                assertTrue(relativeError < 2.5,
                        "Quantized distance error too large: " + relativeError);
            } else {
                // For very small distances, check absolute error instead
                // Note: Product quantization with 16 clusters can have significant error
                assertTrue(error < 30.0,
                        "Quantized distance absolute error too large: " + error);
            }
        }
    }

    @Test
    void testCompressionRatio() {
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        Map<String, Object> stats = quantizer.getStats();

        // For 64-dim vectors with 4 subspaces:
        // Original: 64 * 8 bytes (double) = 512 bytes
        // Quantized: 4 bytes
        // Compression ratio: 512/4 = 128
        double compressionRatio = (Double) stats.get("compression_ratio");
        assertEquals(128.0, compressionRatio, 0.1);
    }

    @Test
    void testInvalidDimension() {
        // Try to train with dimension not divisible by num_subspaces
        double[][] invalidData = generateRandomVectors(50, 63);  // 63 not divisible by 4

        assertThrows(Exception.class, () -> {
            quantizer.train(invalidData);
        });
    }

    @Test
    void testQuantizeBeforeTraining() {
        double[][] vectors = generateRandomVectors(10, 64);

        assertThrows(Exception.class, () -> {
            quantizer.quantize(vectors);
        });
    }

    @Test
    void testDequantizeBeforeTraining() {
        byte[][] quantized = new byte[10][4];

        assertThrows(Exception.class, () -> {
            quantizer.dequantize(quantized);
        });
    }

    @Test
    void testComputeDistanceBeforeTraining() {
        double[] query = generateRandomVector(64);
        byte[] quantized = new byte[4];

        assertThrows(Exception.class, () -> {
            quantizer.computeDistance(query, quantized);
        });
    }

    @Test
    void testDifferentSubspaceConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("num_subspaces", 8);
        config.put("num_clusters", 256);
        config.put("max_iterations", 30);

        ProductQuantizer pq = new ProductQuantizer(config);
        pq.initialize();

        double[][] trainingData = generateRandomVectors(100, 128);
        pq.train(trainingData);

        Map<String, Object> stats = pq.getStats();
        assertEquals(128, stats.get("dimensions"));
        assertEquals(16, stats.get("subspace_dim"));  // 128/8
        assertEquals(8, stats.get("bytes_per_vector"));

        pq.close();
    }

    @Test
    void testLargeDataset() {
        // Test with more realistic dataset size
        double[][] trainingData = generateRandomVectors(1000, 64);
        quantizer.train(trainingData);

        double[][] testVectors = generateRandomVectors(100, 64);
        byte[][] quantized = quantizer.quantize(testVectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        assertEquals(100, quantized.length);
        assertEquals(100, dequantized.length);
    }

    @Test
    void testConsistency() {
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        double[] vector = generateRandomVector(64);

        // Quantize twice - should get same result
        byte[] q1 = quantizer.quantize(new double[][]{vector})[0];
        byte[] q2 = quantizer.quantize(new double[][]{vector})[0];

        assertArrayEquals(q1, q2);

        // Dequantize twice - should get same result
        double[] d1 = quantizer.dequantize(new byte[][]{q1})[0];
        double[] d2 = quantizer.dequantize(new byte[][]{q2})[0];

        assertArrayEquals(d1, d2);
    }

    @Test
    void testClose() {
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        quantizer.close();

        assertFalse(quantizer.isInitialized());
        assertFalse(quantizer.isTrained());
    }

    @Test
    void testDistanceSymmetry() {
        double[][] trainingData = generateRandomVectors(100, 64);
        quantizer.train(trainingData);

        double[] vec1 = generateRandomVector(64);
        double[] vec2 = generateRandomVector(64);

        byte[] q1 = quantizer.quantize(new double[][]{vec1})[0];
        byte[] q2 = quantizer.quantize(new double[][]{vec2})[0];

        double d1 = quantizer.computeDistance(vec1, q2);
        double d2 = quantizer.computeDistance(vec2, q1);

        // Distances might not be exactly equal but should be similar
        assertTrue(Math.abs(d1 - d2) / Math.max(d1, d2) < 0.5);
    }

    // Helper methods

    private double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];
        java.util.Random random = new java.util.Random(42);  // Fixed seed for reproducibility

        for (int i = 0; i < count; i++) {
            for (int d = 0; d < dimensions; d++) {
                vectors[i][d] = random.nextDouble() * 10.0 - 5.0;  // Range [-5, 5]
            }
        }

        return vectors;
    }

    private double[] generateRandomVector(int dimensions) {
        return generateRandomVectors(1, dimensions)[0];
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
