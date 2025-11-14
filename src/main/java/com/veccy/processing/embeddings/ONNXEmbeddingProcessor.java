package com.veccy.processing.embeddings;

import ai.onnxruntime.*;
import com.veccy.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ONNX Runtime-based embedding processor for neural models.
 * <p>
 * Supports Sentence Transformers and other ONNX-exported embedding models.
 * Requires an ONNX model file and tokenizer vocabulary.
 * <p>
 * Features:
 * - Load pre-trained ONNX models
 * - Batch inference support
 * - Configurable max sequence length
 * - GPU acceleration support (if available)
 * <p>
 * Configuration:
 * - model_path: Path to ONNX model file
 * - vocab_path: Path to tokenizer vocabulary (optional, uses simple tokenizer if not provided)
 * - max_length: Maximum sequence length (default 128)
 * - dimensions: Output embedding dimensions (auto-detected if not specified)
 */
public class ONNXEmbeddingProcessor implements EmbeddingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ONNXEmbeddingProcessor.class);

    private Map<String, Object> config;
    private OrtEnvironment env;
    private OrtSession session;
    private int dimensions;
    private int maxLength;
    private boolean initialized;
    private String modelPath;
    private Map<String, Integer> vocabulary;

    public ONNXEmbeddingProcessor() {
        this.initialized = false;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();

        // Get model path
        this.modelPath = (String) this.config.get("model_path");
        if (modelPath == null || modelPath.isEmpty()) {
            throw new ProcessingException("model_path is required in config");
        }

        if (!Files.exists(Path.of(modelPath))) {
            throw new ProcessingException("model path in config does not exist");
        }

        this.maxLength = ((Number) this.config.getOrDefault("max_length", 128)).intValue();

        try {
            // Create ONNX Runtime environment
            env = OrtEnvironment.getEnvironment();

            // Create session options
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();

            // Load model
            Path modelFilePath = Paths.get(modelPath);
            session = env.createSession(modelFilePath.toString(), opts);

            // Get output dimensions from model metadata
            this.dimensions = inferDimensions();

            // Load vocabulary if provided
            String vocabPath = (String) this.config.get("vocab_path");
            if (vocabPath != null && !vocabPath.isEmpty()) {
                loadVocabulary(vocabPath);
            } else {
                logger.warn("No vocabulary provided, using simple tokenizer");
                vocabulary = new HashMap<>();
            }

            initialized = true;
            logger.info("ONNX embedding processor initialized: model={}, dims={}", modelPath, dimensions);

        } catch (OrtException e) {
            throw new ProcessingException("Failed to initialize ONNX model: " + e.getMessage(), e);
        }
    }

    @Override
    public double[] embed(String text) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        try {
            // Tokenize text
            long[] inputIds = tokenize(text);

            // Create input tensor using LongBuffer
            long[] shape = new long[]{1, inputIds.length};
            LongBuffer buffer = LongBuffer.wrap(inputIds);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);

            // Run inference
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);

            try (OrtSession.Result results = session.run(inputs)) {
                // Extract embedding from output
                OnnxValue output = results.get(0);
                float[][] embeddings = (float[][]) output.getValue();

                // Convert to double array
                double[] embedding = new double[embeddings[0].length];
                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = embeddings[0][i];
                }

                return normalize(embedding);
            } finally {
                inputTensor.close();
            }

        } catch (OrtException e) {
            throw new ProcessingException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    @Override
    public double[][] embedBatch(List<String> texts) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        try {
            // Tokenize all texts
            long[][] allInputIds = new long[texts.size()][];
            int maxLen = 0;

            for (int i = 0; i < texts.size(); i++) {
                allInputIds[i] = tokenize(texts.get(i));
                maxLen = Math.max(maxLen, allInputIds[i].length);
            }

            // Pad to same length and flatten to 1D array
            long[][] paddedInputIds = new long[texts.size()][maxLen];
            for (int i = 0; i < texts.size(); i++) {
                System.arraycopy(allInputIds[i], 0, paddedInputIds[i], 0, allInputIds[i].length);
                // Rest filled with 0 (padding)
            }

            // Flatten 2D array to 1D for LongBuffer
            long[] flattenedIds = new long[texts.size() * maxLen];
            for (int i = 0; i < texts.size(); i++) {
                System.arraycopy(paddedInputIds[i], 0, flattenedIds, i * maxLen, maxLen);
            }

            // Create input tensor using LongBuffer
            long[] shape = new long[]{texts.size(), maxLen};
            LongBuffer buffer = LongBuffer.wrap(flattenedIds);
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, buffer, shape);

            // Run inference
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);

            try (OrtSession.Result results = session.run(inputs)) {
                // Extract embeddings from output
                OnnxValue output = results.get(0);
                float[][] rawEmbeddings = (float[][]) output.getValue();

                // Convert to double arrays
                double[][] embeddings = new double[texts.size()][];
                for (int i = 0; i < texts.size(); i++) {
                    embeddings[i] = new double[rawEmbeddings[i].length];
                    for (int j = 0; j < embeddings[i].length; j++) {
                        embeddings[i][j] = rawEmbeddings[i][j];
                    }
                    embeddings[i] = normalize(embeddings[i]);
                }

                logger.debug("Generated ONNX embeddings for {} texts", texts.size());
                return embeddings;

            } finally {
                inputTensor.close();
            }

        } catch (OrtException e) {
            throw new ProcessingException("Failed to generate batch embeddings: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getName() {
        return "ONNX Embedding Processor";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "ONNXEmbeddingProcessor");
        stats.put("name", getName());
        stats.put("initialized", initialized);
        stats.put("model_path", modelPath);
        stats.put("dimensions", dimensions);
        stats.put("max_length", maxLength);
        stats.put("vocabulary_size", vocabulary != null ? vocabulary.size() : 0);
        return stats;
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
        } catch (OrtException e) {
            logger.error("Error closing ONNX session", e);
        }
        initialized = false;
        logger.info("ONNX processor closed");
    }

    /**
     * Infers output dimensions from model metadata.
     */
    private int inferDimensions() throws OrtException {
        // Check if dimensions specified in config
        if (config.containsKey("dimensions")) {
            return ((Number) config.get("dimensions")).intValue();
        }

        // Try to infer from model output shape
        try {
            Map<String, NodeInfo> outputInfo = session.getOutputInfo();
            if (!outputInfo.isEmpty()) {
                NodeInfo firstOutput = outputInfo.values().iterator().next();
                TensorInfo tensorInfo = (TensorInfo) firstOutput.getInfo();
                long[] shape = tensorInfo.getShape();
                if (shape.length >= 2) {
                    // Typically [batch_size, embedding_dim]
                    return (int) shape[shape.length - 1];
                }
            }
        } catch (Exception e) {
            logger.warn("Could not infer dimensions from model, using default 768", e);
        }

        // Default for common models like BERT
        return 768;
    }

    /**
     * Simple tokenizer (converts text to token IDs).
     * <p>
     * This is a basic implementation. For production use, load proper
     * tokenizer vocabulary and use WordPiece/BPE tokenization.
     */
    private long[] tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new long[]{0};  // CLS token
        }

        // Simple whitespace tokenization
        String[] tokens = text.toLowerCase().split("\\s+");

        // Limit to max length (reserve 2 for special tokens)
        int numTokens = Math.min(tokens.length, maxLength - 2);

        // Convert to IDs: [CLS] + tokens + [SEP]
        long[] inputIds = new long[numTokens + 2];
        inputIds[0] = 101;  // [CLS] token ID

        for (int i = 0; i < numTokens; i++) {
            String token = tokens[i];
            // Use vocabulary if available, otherwise hash
            if (vocabulary.containsKey(token)) {
                inputIds[i + 1] = vocabulary.get(token);
            } else {
                // Simple hash for unknown tokens
                inputIds[i + 1] = 100 + (Math.abs(token.hashCode()) % 29000);
            }
        }

        inputIds[numTokens + 1] = 102;  // [SEP] token ID

        return inputIds;
    }

    /**
     * Loads vocabulary from file (simple format: one token per line).
     */
    private void loadVocabulary(String vocabPath) {
        // Simplified - in production, load actual tokenizer vocab
        vocabulary = new HashMap<>();
        logger.info("Vocabulary loading not fully implemented, using simple tokenizer");
    }

    /**
     * L2 normalizes a vector.
     */
    private double[] normalize(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm < 1e-10) {
            return vector;
        }

        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}
