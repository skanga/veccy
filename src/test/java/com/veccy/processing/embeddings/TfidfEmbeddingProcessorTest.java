package com.veccy.processing.embeddings;

import com.veccy.exceptions.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TfidfEmbeddingProcessorTest {

    private TfidfEmbeddingProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TfidfEmbeddingProcessor();
    }

    @Test
    void testInitialization() {
        Map<String, Object> config = new HashMap<>();
        config.put("max_vocab_size", 5000);

        processor.initialize(config);

        assertTrue(processor.isInitialized());
        assertEquals("TF-IDF Embedding Processor", processor.getName());
    }

    @Test
    void testInitializationWithDefaults() {
        processor.initialize(null);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testTraining() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "the quick brown fox jumps over the lazy dog",
                "the dog was really very lazy",
                "the fox was quick and clever"
        );

        processor.train(documents);

        Map<String, Object> stats = processor.getStats();
        assertTrue((Integer) stats.get("vocabulary_size") > 0);
        assertEquals(3, stats.get("document_count"));
        assertEquals(processor.getDimensions(), stats.get("vocabulary_size"));
    }

    @Test
    void testEmbedWithoutTraining() {
        processor.initialize(null);

        assertThrows(ProcessingException.class, () -> {
            processor.embed("test text");
        });
    }

    @Test
    void testEmbedWithoutInitialization() {
        assertThrows(ProcessingException.class, () -> {
            processor.embed("test text");
        });
    }

    @Test
    void testEmbedSingleText() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "machine learning is fascinating",
                "deep learning is a subset of machine learning",
                "neural networks power deep learning"
        );

        processor.train(documents);

        double[] embedding = processor.embed("machine learning");

        assertNotNull(embedding);
        assertEquals(processor.getDimensions(), embedding.length);

        // Check L2 normalization (vector norm should be close to 1)
        double norm = 0.0;
        for (double v : embedding) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        assertEquals(1.0, norm, 0.01, "Embedding should be L2 normalized");
    }

    @Test
    void testEmbedBatch() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "artificial intelligence and machine learning",
                "natural language processing is part of AI",
                "computer vision is another AI field"
        );

        processor.train(documents);

        List<String> texts = Arrays.asList(
                "machine learning",
                "computer vision",
                "natural language"
        );

        double[][] embeddings = processor.embedBatch(texts);

        assertNotNull(embeddings);
        assertEquals(3, embeddings.length);

        for (double[] embedding : embeddings) {
            assertEquals(processor.getDimensions(), embedding.length);

            // Check normalization
            double norm = 0.0;
            for (double v : embedding) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            assertEquals(1.0, norm, 0.01);
        }
    }

    @Test
    void testEmbedEmptyText() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "document one",
                "document two"
        );

        processor.train(documents);

        double[] embedding = processor.embed("");

        assertNotNull(embedding);
        assertEquals(processor.getDimensions(), embedding.length);

        // Empty text should produce zero vector
        boolean allZeros = true;
        for (double v : embedding) {
            if (v != 0.0) {
                allZeros = false;
                break;
            }
        }
        assertTrue(allZeros || Math.abs(Arrays.stream(embedding).sum()) < 0.01);
    }

    @Test
    void testEmbedUnknownWords() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "the quick brown fox",
                "the lazy dog"
        );

        processor.train(documents);

        // Embed text with words not in vocabulary
        double[] embedding = processor.embed("completely unknown words here");

        assertNotNull(embedding);
        assertEquals(processor.getDimensions(), embedding.length);
    }

    @Test
    void testVocabularyLimit() {
        Map<String, Object> config = new HashMap<>();
        config.put("max_vocab_size", 5);  // Very small vocabulary

        processor.initialize(config);

        List<String> documents = Arrays.asList(
                "one two three four five six seven eight nine ten"
        );

        processor.train(documents);

        // Vocabulary should be limited to 5 most frequent terms
        assertTrue(processor.getDimensions() <= 5);
    }

    @Test
    void testSimilarTextsShouldHaveSimilarEmbeddings() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "the cat sat on the mat",
                "the dog sat on the log",
                "cats and dogs are pets",
                "birds fly in the sky"
        );

        processor.train(documents);

        double[] embedding1 = processor.embed("the cat sat on the mat");
        double[] embedding2 = processor.embed("the cat sat on the rug");  // Similar text
        double[] embedding3 = processor.embed("birds fly high");  // Different text

        // Compute cosine similarity
        double sim12 = cosineSimilarity(embedding1, embedding2);
        double sim13 = cosineSimilarity(embedding1, embedding3);

        // Similar texts should have higher similarity than different texts
        assertTrue(sim12 > sim13, "Similar texts should have higher cosine similarity");
    }

    @Test
    void testGetStats() {
        processor.initialize(null);

        List<String> documents = Arrays.asList(
                "document one",
                "document two",
                "document three"
        );

        processor.train(documents);

        Map<String, Object> stats = processor.getStats();

        assertNotNull(stats);
        assertEquals("TfidfEmbeddingProcessor", stats.get("type"));
        assertTrue(stats.containsKey("vocabulary_size"));
        assertTrue(stats.containsKey("dimensions"));
        assertEquals(3, stats.get("document_count"));
    }

    @Test
    void testClose() {
        processor.initialize(null);

        List<String> documents = Arrays.asList("doc one", "doc two");
        processor.train(documents);

        processor.close();

        assertFalse(processor.isInitialized());
    }

    /**
     * Helper method to compute cosine similarity between two vectors.
     */
    private double cosineSimilarity(double[] a, double[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);

        if (normA < 1e-10 || normB < 1e-10) {
            return 0.0;
        }

        return dotProduct / (normA * normB);
    }
}
