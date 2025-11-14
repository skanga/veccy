package com.veccy.processing.embeddings;

import java.util.List;
import java.util.Map;

/**
 * Interface for embedding processors that convert text into vector representations.
 * <p>
 * Implementations can use various techniques:
 * - Statistical methods (TF-IDF, BM25)
 * - Neural embeddings (Sentence Transformers via ONNX)
 * - External APIs (OpenAI, Cohere, etc.)
 */
public interface EmbeddingProcessor extends AutoCloseable {

    /**
     * Initializes the embedding processor with configuration.
     *
     * @param config Configuration parameters
     */
    void initialize(Map<String, Object> config);

    /**
     * Generates embeddings for a single text.
     *
     * @param text The input text
     * @return Vector embedding as double array
     */
    double[] embed(String text);

    /**
     * Generates embeddings for multiple texts (batch processing).
     * <p>
     * Batch processing is often more efficient than processing texts individually.
     *
     * @param texts List of input texts
     * @return Array of vector embeddings
     */
    double[][] embedBatch(List<String> texts);

    /**
     * Gets the dimensionality of the embeddings produced.
     *
     * @return Number of dimensions in output vectors
     */
    int getDimensions();

    /**
     * Gets the name of this embedding processor.
     *
     * @return Processor name (e.g., "TF-IDF", "ONNX Sentence Transformer")
     */
    String getName();

    /**
     * Checks if the processor is initialized and ready to use.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Gets processor statistics and metadata.
     *
     * @return Map of stats (model name, dimensions, etc.)
     */
    Map<String, Object> getStats();

    /**
     * Closes the processor and releases resources.
     */
    @Override
    void close();
}
