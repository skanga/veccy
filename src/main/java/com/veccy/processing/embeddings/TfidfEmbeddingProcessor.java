package com.veccy.processing.embeddings;

import com.veccy.exceptions.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TF-IDF (Term Frequency-Inverse Document Frequency) embedding processor.
 * <p>
 * Implements a simple bag-of-words approach using TF-IDF weighting.
 * This is a statistical method that doesn't require pre-trained models.
 * <p>
 * Features:
 * - Vocabulary building from training corpus
 * - Configurable vocabulary size (top N most frequent terms)
 * - L2 normalization of output vectors
 * - Fast, deterministic, no external dependencies
 */
public class TfidfEmbeddingProcessor implements EmbeddingProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TfidfEmbeddingProcessor.class);

    private Map<String, Object> config;
    private Map<String, Integer> vocabulary;  // term -> index
    private Map<String, Double> idfScores;    // term -> IDF score
    private int maxVocabSize;
    private int dimensions;
    private boolean initialized;
    private int documentCount;

    public TfidfEmbeddingProcessor() {
        this.initialized = false;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.maxVocabSize = ((Number) this.config.getOrDefault("max_vocab_size", 10000)).intValue();
        this.vocabulary = new ConcurrentHashMap<>();
        this.idfScores = new ConcurrentHashMap<>();
        this.documentCount = 0;
        this.initialized = true;

        logger.info("TF-IDF processor initialized with max vocabulary size: {}", maxVocabSize);
    }

    /**
     * Trains the TF-IDF model on a corpus of documents.
     * <p>
     * This method must be called before embedding to build the vocabulary
     * and compute IDF scores.
     *
     * @param documents Training corpus
     */
    public void train(List<String> documents) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        logger.info("Training TF-IDF on {} documents", documents.size());

        // Count term frequencies across all documents
        Map<String, Integer> termDocFreq = new HashMap<>();
        Map<String, Integer> termTotalFreq = new HashMap<>();

        for (String doc : documents) {
            Set<String> uniqueTerms = new HashSet<>();
            String[] tokens = tokenize(doc);

            for (String token : tokens) {
                if (!token.isEmpty()) {
                    termTotalFreq.put(token, termTotalFreq.getOrDefault(token, 0) + 1);
                    uniqueTerms.add(token);
                }
            }

            // Count document frequency (how many docs contain this term)
            for (String term : uniqueTerms) {
                termDocFreq.put(term, termDocFreq.getOrDefault(term, 0) + 1);
            }
        }

        documentCount = documents.size();

        // Select top N most frequent terms for vocabulary
        List<Map.Entry<String, Integer>> sortedTerms = termTotalFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxVocabSize)
                .collect(Collectors.toList());

        // Build vocabulary and compute IDF scores
        vocabulary.clear();
        idfScores.clear();
        int index = 0;

        for (Map.Entry<String, Integer> entry : sortedTerms) {
            String term = entry.getKey();
            vocabulary.put(term, index++);

            // IDF = log(N / df_t)
            int docFreq = termDocFreq.get(term);
            double idf = Math.log((double) documentCount / docFreq);
            idfScores.put(term, idf);
        }

        dimensions = vocabulary.size();

        logger.info("TF-IDF training complete: {} terms in vocabulary", dimensions);
    }

    @Override
    public double[] embed(String text) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        if (vocabulary.isEmpty()) {
            throw new ProcessingException("Vocabulary not trained. Call train() first.");
        }

        // Initialize vector
        double[] vector = new double[dimensions];

        // Tokenize and count term frequencies
        String[] tokens = tokenize(text);
        Map<String, Integer> termFreq = new HashMap<>();

        for (String token : tokens) {
            if (vocabulary.containsKey(token)) {
                termFreq.put(token, termFreq.getOrDefault(token, 0) + 1);
            }
        }

        // Compute TF-IDF for each term
        int docLength = tokens.length;
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            int freq = entry.getValue();

            if (vocabulary.containsKey(term)) {
                int idx = vocabulary.get(term);
                double tf = (double) freq / docLength;  // Normalized term frequency
                double idf = idfScores.get(term);
                vector[idx] = tf * idf;
            }
        }

        // L2 normalization
        return normalize(vector);
    }

    @Override
    public double[][] embedBatch(List<String> texts) {
        if (!initialized) {
            throw new ProcessingException("Processor not initialized");
        }

        double[][] embeddings = new double[texts.size()][];
        for (int i = 0; i < texts.size(); i++) {
            embeddings[i] = embed(texts.get(i));
        }

        logger.debug("Generated TF-IDF embeddings for {} texts", texts.size());
        return embeddings;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    @Override
    public String getName() {
        return "TF-IDF Embedding Processor";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "TfidfEmbeddingProcessor");
        stats.put("name", getName());
        stats.put("initialized", initialized);
        stats.put("vocabulary_size", vocabulary.size());
        stats.put("dimensions", dimensions);
        stats.put("max_vocab_size", maxVocabSize);
        stats.put("document_count", documentCount);
        return stats;
    }

    @Override
    public void close() {
        if (vocabulary != null) {
            vocabulary.clear();
        }
        if (idfScores != null) {
            idfScores.clear();
        }
        initialized = false;
        logger.info("TF-IDF processor closed");
    }

    /**
     * Tokenizes text into terms (simple whitespace + lowercase).
     */
    private String[] tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new String[0];
        }

        // Convert to lowercase, split on non-alphanumeric, remove short tokens
        return text.toLowerCase()
                .split("[\\W_]+")
                .clone();
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
            return vector;  // Avoid division by zero
        }

        double[] normalized = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }
}
