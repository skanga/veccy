package com.veccy.processing.embeddings;

import com.veccy.exceptions.ProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ONNXEmbeddingProcessor.
 * Note: These tests focus on initialization and configuration validation,
 * as actual ONNX model testing requires model files.
 */
class ONNXEmbeddingProcessorTest {

    private ONNXEmbeddingProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ONNXEmbeddingProcessor();
    }

    @AfterEach
    void tearDown() {
        if (processor != null && processor.isInitialized()) {
            processor.close();
        }
    }

    @Test
    void testConstructor() {
        assertNotNull(processor);
        assertFalse(processor.isInitialized());
    }

    @Test
    void testGetName() {
        assertEquals("ONNX Embedding Processor", processor.getName());
    }

    @Test
    void testIsInitializedReturnsFalseBeforeInit() {
        assertFalse(processor.isInitialized());
    }

    @Test
    void testInitializeWithNullConfig() {
        // Should throw ProcessingException because model_path is required
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(null);
        });
    }

    @Test
    void testInitializeWithEmptyConfig() {
        Map<String, Object> config = new HashMap<>();

        // Should throw ProcessingException because model_path is required
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithEmptyModelPath() {
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "");

        // Should throw ProcessingException for empty model_path
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithInvalidModelPath() {
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "/nonexistent/model.onnx");

        // Should throw ProcessingException when model file doesn't exist
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testEmbedBeforeInitialization() {
        // Should throw ProcessingException when not initialized
        assertThrows(ProcessingException.class, () -> {
            processor.embed("test text");
        });
    }

    @Test
    void testEmbedBatchBeforeInitialization() {
        // Should throw ProcessingException when not initialized
        assertThrows(ProcessingException.class, () -> {
            processor.embedBatch(java.util.List.of("test1", "test2"));
        });
    }

    @Test
    void testGetStatsBeforeInitialization() {
        Map<String, Object> stats = processor.getStats();

        assertNotNull(stats);
        assertEquals("ONNXEmbeddingProcessor", stats.get("type"));
        assertEquals("ONNX Embedding Processor", stats.get("name"));
        assertEquals(false, stats.get("initialized"));
        assertNull(stats.get("model_path"));
        assertEquals(0, stats.get("dimensions"));
        assertEquals(0, stats.get("vocabulary_size"));
    }

    @Test
    void testGetDimensionsBeforeInitialization() {
        // Dimensions should be 0 before initialization
        assertEquals(0, processor.getDimensions());
    }

    @Test
    void testClose() {
        // Should not throw even if not initialized
        assertDoesNotThrow(() -> processor.close());
        assertFalse(processor.isInitialized());
    }

    @Test
    void testCloseIdempotent() {
        // Multiple closes should be safe
        processor.close();
        processor.close();
        processor.close();

        assertFalse(processor.isInitialized());
    }

    @Test
    void testInitializeWithMaxLength() {
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "/nonexistent/model.onnx");
        config.put("max_length", 256);

        // Will fail on model loading, but config should be read
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithDimensions() {
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "/nonexistent/model.onnx");
        config.put("dimensions", 512);

        // Will fail on model loading, but config should be read
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }

    @Test
    void testInitializeWithVocabPath() {
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "/nonexistent/model.onnx");
        config.put("vocab_path", "/nonexistent/vocab.txt");

        // Will fail on model loading, but config should be read
        assertThrows(ProcessingException.class, () -> {
            processor.initialize(config);
        });
    }
}
