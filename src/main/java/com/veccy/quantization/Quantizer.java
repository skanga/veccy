package com.veccy.quantization;

import java.util.Map;

/**
 * Abstract base interface for vector quantizers.
 *
 * Quantizers compress vectors to reduce memory usage and improve
 * search performance while maintaining reasonable accuracy.
 */
public interface Quantizer extends AutoCloseable {

    /**
     * Initialize the quantizer.
     */
    void initialize();

    /**
     * Train the quantizer on a set of vectors.
     * Some quantizers (like product quantization) require training.
     *
     * @param vectors the training vectors
     */
    void train(double[][] vectors);

    /**
     * Quantize vectors.
     *
     * @param vectors the vectors to quantize
     * @return quantized vectors (format depends on implementation)
     */
    byte[][] quantize(double[][] vectors);

    /**
     * Dequantize vectors.
     *
     * @param quantizedVectors the quantized vectors to dequantize
     * @return dequantized vectors
     */
    double[][] dequantize(byte[][] quantizedVectors);

    /**
     * Compute distance between query and quantized vector efficiently.
     * This method allows for optimized distance computation in the quantized space.
     *
     * @param query the query vector (unquantized)
     * @param quantizedVector the quantized vector
     * @return distance value
     */
    double computeDistance(double[] query, byte[] quantizedVector);

    /**
     * Get quantizer statistics.
     *
     * @return map of statistics including compression ratio, training status, etc.
     */
    Map<String, Object> getStats();

    /**
     * Check if the quantizer has been initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Check if the quantizer has been trained.
     *
     * @return true if trained
     */
    boolean isTrained();

    /**
     * Close the quantizer and clean up resources.
     */
    @Override
    void close();
}
