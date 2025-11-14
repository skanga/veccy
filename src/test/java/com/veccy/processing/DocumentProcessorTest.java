package com.veccy.processing;

import com.veccy.base.VectorDB;
import com.veccy.client.VectorDBClient;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.exceptions.ProcessingException;
import com.veccy.indices.FlatIndex;
import com.veccy.processing.chunking.ChunkingStrategy;
import com.veccy.processing.chunking.FixedSizeChunkingStrategy;
import com.veccy.processing.embeddings.EmbeddingProcessor;
import com.veccy.processing.parsers.DocumentParser;
import com.veccy.processing.parsers.ParsedDocument;
import com.veccy.storage.MemoryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Comprehensive tests for DocumentProcessor.
 */
class DocumentProcessorTest {

    private DocumentProcessor processor;
    private VectorDB vectorDB;
    private Logger logger;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        processor = new DocumentProcessor(logger);

        // Create a real vector database for testing
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        FlatIndex index = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        vectorDB = new VectorDBClient(storage, index);
        vectorDB.initialize();
    }

    @Test
    void testConstructor() {
        assertNotNull(processor);
        assertFalse(processor.isInitialized());
    }

    @Test
    void testInitialize() {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());

        processor.initialize(embeddingProcessor);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testInitializeWithUninitializedEmbeddingProcessor() {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        // Not initialized

        assertThrows(ProcessingException.class, () -> {
            processor.initialize(embeddingProcessor);
        });
    }

    @Test
    void testRegisterParser() {
        TestDocumentParser parser = new TestDocumentParser();

        processor.registerParser(parser);

        assertTrue(processor.getSupportedExtensions().contains("txt"));
        assertTrue(processor.getSupportedExtensions().contains("test"));
    }

    @Test
    void testSupportsFileType() {
        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Path txtFile = tempDir.resolve("test.txt");
        Path testFile = tempDir.resolve("test.test");
        Path unknownFile = tempDir.resolve("test.unknown");

        assertTrue(processor.supports(txtFile));
        assertTrue(processor.supports(testFile));
        assertFalse(processor.supports(unknownFile));
    }

    @Test
    void testGetSupportedExtensions() {
        TestDocumentParser parser1 = new TestDocumentParser();
        processor.registerParser(parser1);

        Set<String> extensions = processor.getSupportedExtensions();

        assertTrue(extensions.contains("txt"));
        assertTrue(extensions.contains("test"));
        assertEquals(2, extensions.size());
    }

    @Test
    void testSetChunkingStrategy() {
        ChunkingStrategy strategy = new FixedSizeChunkingStrategy();
        Map<String, Object> config = new HashMap<>();
        config.put("chunk_size", 200);

        processor.setChunkingStrategy(strategy, config);

        // Verify through stats
        Map<String, Object> stats = processor.getStats();
        assertEquals("Fixed-Size Chunking", stats.get("chunking_strategy"));
    }

    @Test
    void testSetChunkingStrategyWithNullConfig() {
        ChunkingStrategy strategy = new FixedSizeChunkingStrategy();

        processor.setChunkingStrategy(strategy, null);

        Map<String, Object> stats = processor.getStats();
        assertEquals("Fixed-Size Chunking", stats.get("chunking_strategy"));
    }

    @Test
    void testProcessDocumentNotInitialized() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");

        assertThrows(ProcessingException.class, () -> {
            processor.processDocument(testFile, vectorDB);
        });
    }

    @Test
    void testProcessDocumentFileNotFound() {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        Path nonexistentFile = tempDir.resolve("nonexistent.txt");

        assertThrows(ProcessingException.class, () -> {
            processor.processDocument(nonexistentFile, vectorDB);
        });
    }

    @Test
    void testProcessDocumentNoParserRegistered() throws IOException {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        Path testFile = tempDir.resolve("test.unknown");
        Files.writeString(testFile, "Test content");

        assertThrows(ProcessingException.class, () -> {
            processor.processDocument(testFile, vectorDB);
        });
    }

    @Test
    void testProcessDocumentSuccess() throws IOException {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "This is test content for processing.");

        List<String> chunkIds = processor.processDocument(testFile, vectorDB);

        assertFalse(chunkIds.isEmpty());
        assertTrue(chunkIds.size() >= 1);
    }

    @Test
    void testProcessDocumentsMultipleFiles() throws IOException {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Path file1 = tempDir.resolve("test1.txt");
        Path file2 = tempDir.resolve("test2.txt");
        Files.writeString(file1, "Content of file 1");
        Files.writeString(file2, "Content of file 2");

        List<Path> files = Arrays.asList(file1, file2);
        Map<Path, List<String>> results = processor.processDocuments(files, vectorDB);

        assertEquals(2, results.size());
        assertTrue(results.containsKey(file1));
        assertTrue(results.containsKey(file2));
        assertFalse(results.get(file1).isEmpty());
        assertFalse(results.get(file2).isEmpty());
    }

    @Test
    void testProcessDocumentsWithFailure() throws IOException {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Path validFile = tempDir.resolve("valid.txt");
        Path invalidFile = tempDir.resolve("invalid.unknown");
        Files.writeString(validFile, "Valid content");
        Files.writeString(invalidFile, "Invalid content");

        List<Path> files = Arrays.asList(validFile, invalidFile);
        Map<Path, List<String>> results = processor.processDocuments(files, vectorDB);

        assertEquals(2, results.size());
        assertFalse(results.get(validFile).isEmpty());
        assertTrue(results.get(invalidFile).isEmpty()); // Failed processing
    }

    @Test
    void testGetStatsNotInitialized() {
        Map<String, Object> stats = processor.getStats();

        assertEquals(false, stats.get("initialized"));
        assertEquals(0, stats.get("registered_parsers"));
        assertEquals("none", stats.get("embedding_processor"));
    }

    @Test
    void testGetStatsInitialized() {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Map<String, Object> stats = processor.getStats();

        assertEquals(true, stats.get("initialized"));
        assertEquals(2, stats.get("registered_parsers")); // txt and test extensions
        assertEquals("Test Embedding Processor", stats.get("embedding_processor"));
        assertNotNull(stats.get("supported_extensions"));
    }

    @Test
    void testIsInitializedFalse() {
        assertFalse(processor.isInitialized());
    }

    @Test
    void testIsInitializedTrue() {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        assertTrue(processor.isInitialized());
    }

    @Test
    void testProcessDocumentWithCustomChunkingStrategy() throws IOException {
        TestEmbeddingProcessor embeddingProcessor = new TestEmbeddingProcessor();
        embeddingProcessor.initialize(new HashMap<>());
        processor.initialize(embeddingProcessor);

        TestDocumentParser parser = new TestDocumentParser();
        processor.registerParser(parser);

        Map<String, Object> chunkConfig = new HashMap<>();
        chunkConfig.put("chunk_size", 100);
        chunkConfig.put("overlap", 10);
        processor.setChunkingStrategy(new FixedSizeChunkingStrategy(), chunkConfig);

        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "This is a long document that should be chunked into multiple pieces based on the chunking strategy configuration.");

        List<String> chunkIds = processor.processDocument(testFile, vectorDB);

        assertFalse(chunkIds.isEmpty());
    }

    /**
     * Test document parser implementation.
     */
    private static class TestDocumentParser implements DocumentParser {

        @Override
        public ParsedDocument parse(InputStream input) throws IOException {
            String content = new String(input.readAllBytes());
            return new ParsedDocument(content);
        }

        @Override
        public ParsedDocument parse(Path filePath) throws IOException {
            String content = Files.readString(filePath);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("filename", filePath.getFileName().toString());
            return new ParsedDocument(content, metadata);
        }

        @Override
        public boolean supports(String fileExtension) {
            return fileExtension.equals("txt") || fileExtension.equals("test");
        }

        @Override
        public String getName() {
            return "Test Parser";
        }

        @Override
        public String[] getSupportedExtensions() {
            return new String[]{"txt", "test"};
        }
    }

    /**
     * Test embedding processor implementation.
     */
    private static class TestEmbeddingProcessor implements EmbeddingProcessor {
        private boolean initialized = false;
        private static final int DIMENSIONS = 128;

        @Override
        public void initialize(Map<String, Object> config) {
            initialized = true;
        }

        @Override
        public double[] embed(String text) {
            if (!initialized) {
                throw new ProcessingException("Not initialized");
            }
            double[] embedding = new double[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                embedding[i] = Math.random();
            }
            return embedding;
        }

        @Override
        public double[][] embedBatch(List<String> texts) {
            if (!initialized) {
                throw new ProcessingException("Not initialized");
            }
            double[][] embeddings = new double[texts.size()][];
            for (int i = 0; i < texts.size(); i++) {
                embeddings[i] = embed(texts.get(i));
            }
            return embeddings;
        }

        @Override
        public int getDimensions() {
            return DIMENSIONS;
        }

        @Override
        public String getName() {
            return "Test Embedding Processor";
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public Map<String, Object> getStats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", getName());
            stats.put("dimensions", DIMENSIONS);
            stats.put("initialized", initialized);
            return stats;
        }

        @Override
        public void close() {
            initialized = false;
        }
    }
}
