package com.veccy.quantization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScalarQuantizer.
 */
public class ScalarQuantizerTest {

    private ScalarQuantizer quantizer;

    @BeforeEach
    public void setUp() {
        Map<String, Object> config = new HashMap<>();
        config.put("bits", 8);
        quantizer = new ScalarQuantizer(config);
        quantizer.initialize();
    }

    @AfterEach
    public void tearDown() {
        if (quantizer != null) {
            quantizer.close();
        }
    }

    @Test
    public void testInitialization() {
        Map<String, Object> config = Map.of("bits", 8);
        ScalarQuantizer newQuantizer = new ScalarQuantizer(config);

        assertFalse(newQuantizer.isInitialized());
        assertFalse(newQuantizer.isTrained());

        newQuantizer.initialize();
        assertTrue(newQuantizer.isInitialized());
        assertFalse(newQuantizer.isTrained());

        newQuantizer.close();
    }

    @Test
    public void testTrain_Simple() {
        double[][] vectors = {
            {0.0, 0.0, 0.0},
            {1.0, 1.0, 1.0},
            {0.5, 0.5, 0.5}
        };

        quantizer.train(vectors);
        assertTrue(quantizer.isTrained());
    }

    @Test
    public void testTrain_RandomVectors() {
        Random random = new Random(42);
        double[][] vectors = new double[100][64];

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 64; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        quantizer.train(vectors);
        assertTrue(quantizer.isTrained());
    }

    @Test
    public void testQuantize_SingleVector() {
        // Train with simple range
        double[][] trainVectors = {
            {0.0, 0.0, 0.0},
            {10.0, 10.0, 10.0}
        };
        quantizer.train(trainVectors);

        // Quantize a vector
        double[][] vectors = {{5.0, 5.0, 5.0}};
        byte[][] quantized = quantizer.quantize(vectors);

        assertNotNull(quantized);
        assertEquals(1, quantized.length);
        assertEquals(3, quantized[0].length);
    }

    @Test
    public void testQuantize_MultipleVectors() {
        // Train quantizer
        Random random = new Random(42);
        double[][] trainVectors = new double[50][32];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 32; j++) {
                trainVectors[i][j] = random.nextGaussian();
            }
        }
        quantizer.train(trainVectors);

        // Quantize vectors
        double[][] vectors = new double[10][32];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 32; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        byte[][] quantized = quantizer.quantize(vectors);

        assertEquals(10, quantized.length);
        for (byte[] q : quantized) {
            assertEquals(32, q.length);
        }
    }

    @Test
    public void testDequantize_RoundTrip() {
        // Train quantizer
        Random random = new Random(42);
        double[][] trainVectors = new double[50][32];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 32; j++) {
                trainVectors[i][j] = random.nextGaussian();
            }
        }
        quantizer.train(trainVectors);

        // Use a subset of training vectors to test round-trip
        // This ensures values are within the trained [min, max] range
        double[][] vectors = new double[5][32];
        for (int i = 0; i < 5; i++) {
            System.arraycopy(trainVectors[i], 0, vectors[i], 0, 32);
        }

        // Quantize and dequantize
        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // Check dimensions
        assertEquals(vectors.length, dequantized.length);
        assertEquals(vectors[0].length, dequantized[0].length);

        // Values should be approximately equal (with quantization error)
        for (int i = 0; i < vectors.length; i++) {
            for (int j = 0; j < vectors[0].length; j++) {
                double original = vectors[i][j];
                double recovered = dequantized[i][j];

                // Compute relative error
                double error = Math.abs(original - recovered);
                double range = getValueRange(trainVectors, j);
                double relativeError = error / (range + 1e-10);

                // With 8-bit quantization (256 levels), each dimension gets split into ~255 bins
                // Max quantization error is approximately range/510 (half a bin)
                // Expect relative error < 0.5% for typical values
                assertTrue(relativeError < 0.005,
                    String.format("High error at [%d][%d]: original=%.4f, recovered=%.4f, relativeError=%.4f, range=%.4f",
                        i, j, original, recovered, relativeError, range));
            }
        }
    }

    private double getValueRange(double[][] vectors, int dimension) {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (double[] vector : vectors) {
            min = Math.min(min, vector[dimension]);
            max = Math.max(max, vector[dimension]);
        }

        return max - min;
    }

    @Test
    public void testQuantizationAccuracy() {
        // Test that quantization preserves relative distances
        double[][] trainVectors = {
            {0.0, 0.0, 0.0},
            {10.0, 10.0, 10.0}
        };
        quantizer.train(trainVectors);

        double[][] vectors = {
            {1.0, 1.0, 1.0},
            {2.0, 2.0, 2.0},
            {9.0, 9.0, 9.0}
        };

        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // Check that relative ordering is preserved
        double dist01_original = euclideanDistance(vectors[0], vectors[1]);
        double dist02_original = euclideanDistance(vectors[0], vectors[2]);

        double dist01_quantized = euclideanDistance(dequantized[0], dequantized[1]);
        double dist02_quantized = euclideanDistance(dequantized[0], dequantized[2]);

        // Ordering should be preserved
        assertTrue(dist01_original < dist02_original);
        assertTrue(dist01_quantized < dist02_quantized);
    }

    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    @Test
    public void testComputeDistance() {
        // Train quantizer
        double[][] trainVectors = {
            {0.0, 0.0, 0.0},
            {10.0, 10.0, 10.0}
        };
        quantizer.train(trainVectors);

        // Quantize a vector
        double[][] vectors = {{5.0, 5.0, 5.0}};
        byte[][] quantized = quantizer.quantize(vectors);

        // Compute distance from query to quantized vector
        double[] query = {6.0, 6.0, 6.0};
        double distance = quantizer.computeDistance(query, quantized[0]);

        // Distance should be positive and reasonable
        assertTrue(distance >= 0.0);
        assertTrue(distance < 10.0);  // Should be less than max possible distance
    }

    @Test
    public void testGetStats() {
        // Train quantizer
        double[][] vectors = new double[50][32];
        Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 32; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }
        quantizer.train(vectors);

        Map<String, Object> stats = quantizer.getStats();

        assertNotNull(stats);
        assertEquals("ScalarQuantizer", stats.get("type"));
        assertEquals(8, stats.get("bits"));
        assertEquals(true, stats.get("trained"));
        assertEquals(32, stats.get("dimensions"));
    }

    @Test
    public void testGetStats_Untrained() {
        Map<String, Object> stats = quantizer.getStats();

        assertNotNull(stats);
        assertEquals("ScalarQuantizer", stats.get("type"));
        assertEquals(8, stats.get("bits"));
        assertEquals(false, stats.get("trained"));
    }

    @Test
    public void test16BitQuantization() {
        // Test with 16-bit quantization
        Map<String, Object> config = Map.of("bits", 16);
        ScalarQuantizer quantizer16 = new ScalarQuantizer(config);
        quantizer16.initialize();

        try {
            Random random = new Random(42);
            double[][] trainVectors = new double[50][32];
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 32; j++) {
                    trainVectors[i][j] = random.nextGaussian();
                }
            }
            quantizer16.train(trainVectors);

            double[][] vectors = new double[5][32];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 32; j++) {
                    vectors[i][j] = random.nextGaussian();
                }
            }

            byte[][] quantized = quantizer16.quantize(vectors);
            double[][] dequantized = quantizer16.dequantize(quantized);

            // 16-bit should have better accuracy than 8-bit
            assertEquals(vectors.length, dequantized.length);
            assertEquals(vectors[0].length, dequantized[0].length);

            Map<String, Object> stats = quantizer16.getStats();
            assertEquals(16, stats.get("bits"));
        } finally {
            quantizer16.close();
        }
    }

    @Test
    public void testCompressionRatio() {
        // Test that quantization actually reduces memory
        double[][] vectors = new double[100][128];
        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 128; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        quantizer.train(vectors);
        byte[][] quantized = quantizer.quantize(vectors);

        // Original size: 100 * 128 * 8 bytes (double) = 102,400 bytes
        // Quantized size: 100 * 128 * 1 byte = 12,800 bytes
        // Compression ratio: 8x

        int originalBytes = 100 * 128 * 8;
        int quantizedBytes = 100 * 128 * 1;

        Map<String, Object> stats = quantizer.getStats();
        assertEquals(8.0, stats.get("compression_ratio"));
    }

    @Test
    public void testUniformVectors() {
        // Test with uniform values
        double[][] vectors = {
            {5.0, 5.0, 5.0},
            {5.0, 5.0, 5.0},
            {5.0, 5.0, 5.0}
        };

        quantizer.train(vectors);

        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // All should be close to 5.0
        for (int i = 0; i < dequantized.length; i++) {
            for (int j = 0; j < dequantized[0].length; j++) {
                assertTrue(Math.abs(dequantized[i][j] - 5.0) < 1.0);
            }
        }
    }

    @Test
    public void testExtremeValues() {
        // Test with extreme value ranges
        double[][] trainVectors = {
            {-1000.0, -1000.0, -1000.0},
            {1000.0, 1000.0, 1000.0}
        };
        quantizer.train(trainVectors);

        double[][] vectors = {
            {0.0, 0.0, 0.0},
            {500.0, 500.0, 500.0},
            {-500.0, -500.0, -500.0}
        };

        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // Check that values are in reasonable range
        for (int i = 0; i < dequantized.length; i++) {
            for (int j = 0; j < dequantized[0].length; j++) {
                assertTrue(dequantized[i][j] >= -1100.0 && dequantized[i][j] <= 1100.0);
            }
        }
    }

    @Test
    public void testMixedDimensionRanges() {
        // Test vectors where different dimensions have different ranges
        Random random = new Random(42);
        double[][] vectors = new double[100][10];

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                // Each dimension has different scale
                vectors[i][j] = random.nextGaussian() * (j + 1);
            }
        }

        quantizer.train(vectors);

        byte[][] quantized = quantizer.quantize(vectors);
        double[][] dequantized = quantizer.dequantize(quantized);

        // Verify dimensions are preserved
        assertEquals(vectors.length, dequantized.length);
        assertEquals(vectors[0].length, dequantized[0].length);

        // Per-dimension quantization should handle different scales
        // With correct quantization, error should be < range/510 ≈ scale * 8 / 510 ≈ scale * 0.016
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                double error = Math.abs(vectors[i][j] - dequantized[i][j]);
                double scale = j + 1;
                // Max error is approximately scale * 8 / 255 ≈ 0.03 * scale
                // (8 std devs for Gaussian range, divided by 255 quantization levels)
                double maxError = scale * 0.04;
                assertTrue(error < maxError,
                    String.format("Error too high at [%d][%d]: error=%.4f, scale=%.1f, maxError=%.4f",
                        i, j, error, scale, maxError));
            }
        }
    }

    @Test
    public void testClose() {
        quantizer.train(new double[][]{{1.0, 2.0, 3.0}});
        assertTrue(quantizer.isTrained());

        quantizer.close();

        // Stats should still be accessible
        Map<String, Object> stats = quantizer.getStats();
        assertNotNull(stats);
    }
}
