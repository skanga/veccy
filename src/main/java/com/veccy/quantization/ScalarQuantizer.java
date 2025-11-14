package com.veccy.quantization;

import com.veccy.exceptions.QuantizationException;
import com.veccy.utils.SimilarityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scalar quantization implementation for vector compression.
 * <p>
 * Scalar quantization quantizes each dimension of a vector independently
 * to a fixed number of bits (default 8 bits). This provides a simple
 * compression method with configurable precision/memory tradeoff.
 * <p>
 * The quantization process:
 * 1. Training: Compute min/max values for each dimension
 * 2. Compute scale and zero-point for linear quantization
 * 3. Quantize: q = round((x - min) / scale) + zero_point
 * 4. Dequantize: x = (q - zero_point) * scale + min
 * <p>
 * Compression ratio: bits/64 (e.g., 8-bit = 8x compression for double)
 */
public class ScalarQuantizer implements Quantizer {

    private static final Logger logger = LoggerFactory.getLogger(ScalarQuantizer.class);

    private final Map<String, Object> config;
    private final int bits;
    private final int maxVal;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private double[] minValues;
    private double[] maxValues;
    private double[] scale;
    private int[] zeroPoint;
    private int dimensions;
    private boolean initialized;
    private boolean trained;

    public ScalarQuantizer(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.bits = ((Number) this.config.getOrDefault("bits", 8)).intValue();
        this.maxVal = (1 << bits) - 1;
        this.initialized = false;
        this.trained = false;
    }

    @Override
    public void initialize() {
        this.minValues = null;
        this.maxValues = null;
        this.scale = null;
        this.zeroPoint = null;
        this.dimensions = 0;
        this.initialized = true;
        logger.info("Scalar quantizer initialized with {} bits", bits);
    }

    @Override
    public void train(double[][] vectors) {
        if (!initialized) {
            throw new QuantizationException("Quantizer not initialized");
        }

        if (vectors == null || vectors.length == 0) {
            throw new QuantizationException("Training vectors cannot be null or empty");
        }

        try {
            int numVectors = vectors.length;
            dimensions = vectors[0].length;

            // Initialize arrays
            minValues = new double[dimensions];
            maxValues = new double[dimensions];
            scale = new double[dimensions];
            zeroPoint = new int[dimensions];

            // Compute min and max for each dimension
            for (int d = 0; d < dimensions; d++) {
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;

                for (int i = 0; i < numVectors; i++) {
                    double val = vectors[i][d];
                    if (val < min) min = val;
                    if (val > max) max = val;
                }

                minValues[d] = min;
                maxValues[d] = max;

                // Compute scale
                // For min-max quantization, we map [min, max] to [0, maxVal]
                // Formula: q = (value - min) / scale
                double range = max - min;
                if (range == 0) {
                    // Avoid division by zero for constant dimensions
                    scale[d] = 1.0;
                    zeroPoint[d] = 0;
                } else {
                    scale[d] = range / maxVal;
                    // No zero-point needed when using (value - min) offset
                    zeroPoint[d] = 0;
                }
            }

            trained = true;
            logger.info("Scalar quantizer trained on {} vectors with {} dimensions",
                    numVectors, dimensions);
        } catch (Exception e) {
            throw new QuantizationException("Failed to train scalar quantizer: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[][] quantize(double[][] vectors) {
        if (!initialized) {
            throw new QuantizationException("Quantizer not initialized");
        }

        if (!trained) {
            throw new QuantizationException("Quantizer not trained. Call train() first.");
        }

        if (vectors == null || vectors.length == 0) {
            throw new QuantizationException("Vectors to quantize cannot be null or empty");
        }

        try {
            int numVectors = vectors.length;
            byte[][] quantized = new byte[numVectors][];

            for (int v = 0; v < numVectors; v++) {
                quantized[v] = quantizeSingle(vectors[v]);
            }

            logger.debug("Quantized {} vectors", numVectors);
            return quantized;
        } catch (Exception e) {
            throw new QuantizationException("Failed to quantize vectors: " + e.getMessage(), e);
        }
    }

    @Override
    public double[][] dequantize(byte[][] quantizedVectors) {
        if (!initialized) {
            throw new QuantizationException("Quantizer not initialized");
        }

        if (!trained) {
            throw new QuantizationException("Quantizer not trained");
        }

        if (quantizedVectors == null || quantizedVectors.length == 0) {
            throw new QuantizationException("Quantized vectors cannot be null or empty");
        }

        try {
            int numVectors = quantizedVectors.length;
            double[][] dequantized = new double[numVectors][];

            for (int v = 0; v < numVectors; v++) {
                dequantized[v] = dequantizeSingle(quantizedVectors[v]);
            }

            logger.debug("Dequantized {} vectors", numVectors);
            return dequantized;
        } catch (Exception e) {
            throw new QuantizationException("Failed to dequantize vectors: " + e.getMessage(), e);
        }
    }

    @Override
    public double computeDistance(double[] query, byte[] quantizedVector) {
        if (!initialized) {
            throw new QuantizationException("Quantizer not initialized");
        }

        if (!trained) {
            throw new QuantizationException("Quantizer not trained");
        }

        try {
            if (query.length != dimensions) {
                throw new QuantizationException("Query vector dimension mismatch: expected " + dimensions + ", got " + query.length);
            }
            if (quantizedVector.length != dimensions) {
                throw new QuantizationException("Quantized vector dimension mismatch: expected " + dimensions + ", got " + quantizedVector.length);
            }

            // The original implementation used cosine distance. We will re-implement it here
            // efficiently without creating a full dequantized vector.
            double dotProduct = 0.0;
            double queryMagnitude = 0.0;
            double dequantizedMagnitude = 0.0;

            for (int d = 0; d < dimensions; d++) {
                // Dequantize the value for the current dimension on the fly
                int quantizedVal = quantizedVector[d] & 0xFF; // Unsigned byte
                double dequantizedVal = (quantizedVal - zeroPoint[d]) * scale[d] + minValues[d];

                dotProduct += query[d] * dequantizedVal;
                queryMagnitude += query[d] * query[d];
                dequantizedMagnitude += dequantizedVal * dequantizedVal;
            }

            queryMagnitude = Math.sqrt(queryMagnitude);
            dequantizedMagnitude = Math.sqrt(dequantizedMagnitude);

            if (queryMagnitude == 0 || dequantizedMagnitude == 0) {
                return 1.0; // Cosine distance is 1 if one vector is zero
            }

            double cosineSimilarity = dotProduct / (queryMagnitude * dequantizedMagnitude);

            // Cosine distance is 1 - similarity. Clamp to [0, 2] range.
            return Math.max(0, 1.0 - cosineSimilarity);
        } catch (Exception e) {
            throw new QuantizationException("Failed to compute distance: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        stats.put("type", "ScalarQuantizer");
        stats.put("quantizer_type", "scalar");
        stats.put("bits", bits);
        stats.put("max_val", maxVal);
        stats.put("trained", trained);
        stats.put("initialized", initialized);

        if (trained) {
            stats.put("dimensions", dimensions);
            stats.put("compression_ratio", 64.0 / (double) bits); // Double is 64 bits, ratio is original/compressed

            // Compute min/max ranges
            double minRange = Double.MAX_VALUE;
            double maxRange = Double.MIN_VALUE;
            for (int i = 0; i < dimensions; i++) {
                if (minValues[i] < minRange) minRange = minValues[i];
                if (maxValues[i] > maxRange) maxRange = maxValues[i];
            }
            stats.put("min_values_range", new double[]{minRange, maxRange});
            stats.put("max_values_range", new double[]{minRange, maxRange});
        }

        return stats;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isTrained() {
        return trained;
    }

    /**
     * Close the quantizer and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                minValues = null;
                maxValues = null;
                scale = null;
                zeroPoint = null;
                trained = false;
                initialized = false;
                dimensions = 0;
                logger.info("Scalar quantizer closed");
            } catch (Exception e) {
                logger.error("Error closing scalar quantizer: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Scalar quantizer already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Quantize a single vector.
     */
    private byte[] quantizeSingle(double[] vector) {
        if (vector.length != dimensions) {
            throw new QuantizationException(
                    "Vector dimension mismatch: expected " + dimensions + ", got " + vector.length);
        }

        byte[] quantized = new byte[dimensions];

        for (int d = 0; d < dimensions; d++) {
            double val = (vector[d] - minValues[d]) / scale[d] + zeroPoint[d];
            int quantizedVal = (int) Math.round(val);
            quantizedVal = Math.max(0, Math.min(maxVal, quantizedVal));
            quantized[d] = (byte) quantizedVal;
        }

        return quantized;
    }

    /**
     * Dequantize a single vector.
     */
    private double[] dequantizeSingle(byte[] quantizedVector) {
        if (quantizedVector.length != dimensions) {
            throw new QuantizationException(
                    "Quantized vector dimension mismatch: expected " + dimensions +
                            ", got " + quantizedVector.length);
        }

        double[] dequantized = new double[dimensions];

        for (int d = 0; d < dimensions; d++) {
            int quantizedVal = quantizedVector[d] & 0xFF; // Unsigned byte
            dequantized[d] = (quantizedVal - zeroPoint[d]) * scale[d] + minValues[d];
        }

        return dequantized;
    }
}
