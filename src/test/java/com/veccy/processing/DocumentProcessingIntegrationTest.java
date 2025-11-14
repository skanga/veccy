package com.veccy.processing;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.chunking.FixedSizeChunkingStrategy;
import com.veccy.processing.chunking.SentenceChunkingStrategy;
import com.veccy.processing.embeddings.TfidfEmbeddingProcessor;
import com.veccy.processing.parsers.TextParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating the complete document processing pipeline:
 * Document → Parse → Chunk → Embed → Store → Search
 */
class DocumentProcessingIntegrationTest {

    private DocumentProcessor documentProcessor;
    private VectorDB vectorDB;
    private TfidfEmbeddingProcessor embeddingProcessor;

    @BeforeEach
    void setUp() {
        // Create vector database
        vectorDB = VectorDBFactory.createSimple();

        // Create and train embedding processor
        embeddingProcessor = new TfidfEmbeddingProcessor();
        Map<String, Object> embeddingConfig = new HashMap<>();
        embeddingConfig.put("max_vocab_size", 1000);
        embeddingProcessor.initialize(embeddingConfig);

        // Train on sample corpus
        List<String> trainingCorpus = Arrays.asList(
                "Machine learning is a subset of artificial intelligence",
                "Deep learning uses neural networks with multiple layers",
                "Natural language processing enables computers to understand human language",
                "Computer vision allows machines to interpret visual information",
                "Reinforcement learning trains agents through rewards and penalties"
        );
        embeddingProcessor.train(trainingCorpus);

        // Create document processor
        documentProcessor = new DocumentProcessor();
        documentProcessor.initialize(embeddingProcessor);
        documentProcessor.registerParser(new TextParser());
    }

    @Test
    void testCompleteDocumentProcessingPipeline(@TempDir Path tempDir) throws IOException {
        // Stage 1: Create a sample document
        String documentContent = "Artificial intelligence and machine learning are transforming technology. " +
                "Deep learning, a subset of machine learning, uses neural networks. " +
                "Natural language processing is another important AI field.";

        Path documentPath = tempDir.resolve("ai_document.txt");
        Files.writeString(documentPath, documentContent);

        // Stage 2: Process document through complete pipeline
        List<String> chunkIds = documentProcessor.processDocument(documentPath, vectorDB);

        // Verify chunks were created and inserted
        assertNotNull(chunkIds);
        assertFalse(chunkIds.isEmpty(), "Should create at least one chunk");

        Map<String, Object> stats = vectorDB.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
        assertTrue((Integer) indexStats.get("vector_count") > 0, "Vectors should be inserted");

        // Stage 3: Search for relevant content
        String queryText = "deep learning neural networks";
        double[] queryEmbedding = embeddingProcessor.embed(queryText);

        List<SearchResult> results = vectorDB.search(queryEmbedding, 3);

        // Verify search results
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search should return results");

        // Check that results have metadata
        SearchResult topResult = results.get(0);
        assertNotNull(topResult.getMetadata());
        assertTrue(topResult.getMetadata().containsKey("source_file"));
    }

    @Test
    void testMultipleDocumentsProcessing(@TempDir Path tempDir) throws IOException {
        // Create multiple documents
        Path doc1 = tempDir.resolve("ml_basics.txt");
        Files.writeString(doc1, "Machine learning is about training models on data. " +
                "Supervised learning uses labeled datasets.");

        Path doc2 = tempDir.resolve("dl_advanced.txt");
        Files.writeString(doc2, "Deep learning uses deep neural networks. " +
                "Convolutional networks are used for image processing.");

        Path doc3 = tempDir.resolve("nlp_intro.txt");
        Files.writeString(doc3, "Natural language processing handles text and speech. " +
                "Transformers are state-of-the-art NLP models.");

        // Process all documents
        List<Path> documents = Arrays.asList(doc1, doc2, doc3);
        Map<Path, List<String>> results = documentProcessor.processDocuments(documents, vectorDB);

        // Verify all documents were processed
        assertEquals(3, results.size());
        assertTrue(results.get(doc1).size() > 0);
        assertTrue(results.get(doc2).size() > 0);
        assertTrue(results.get(doc3).size() > 0);

        // Verify total chunks in database
        Map<String, Object> stats = vectorDB.getStats();
        @SuppressWarnings("unchecked")
        Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
        int totalChunks = results.values().stream()
                .mapToInt(List::size)
                .sum();
        assertEquals(totalChunks, indexStats.get("vector_count"));
    }

    @Test
    void testDifferentChunkingStrategies(@TempDir Path tempDir) throws IOException {
        String content = "First sentence about machine learning. " +
                "Second sentence about deep learning. " +
                "Third sentence about neural networks. " +
                "Fourth sentence about artificial intelligence.";

        Path docPath = tempDir.resolve("test.txt");
        Files.writeString(docPath, content);

        // Test with fixed-size chunking
        Map<String, Object> fixedConfig = new HashMap<>();
        fixedConfig.put("chunk_size", 50);
        fixedConfig.put("overlap", 10);
        documentProcessor.setChunkingStrategy(new FixedSizeChunkingStrategy(), fixedConfig);

        List<String> fixedChunks = documentProcessor.processDocument(docPath, vectorDB);
        assertFalse(fixedChunks.isEmpty());

        // Clear database
        vectorDB.close();
        vectorDB = VectorDBFactory.createSimple();

        // Test with sentence-based chunking
        Map<String, Object> sentenceConfig = new HashMap<>();
        sentenceConfig.put("max_chunk_size", 100);
        sentenceConfig.put("sentence_overlap", 0);
        documentProcessor.setChunkingStrategy(new SentenceChunkingStrategy(), sentenceConfig);

        List<String> sentenceChunks = documentProcessor.processDocument(docPath, vectorDB);
        assertFalse(sentenceChunks.isEmpty());

        // Strategies may produce different number of chunks
        // (This is expected based on content and configuration)
    }

    @Test
    void testSearchQuality(@TempDir Path tempDir) throws IOException {
        // Create documents with distinct topics
        Path aiDoc = tempDir.resolve("ai.txt");
        Files.writeString(aiDoc, "Artificial intelligence enables machines to think. " +
                "Machine learning is a core AI technology. " +
                "Deep learning powers modern AI systems.");

        Path dbDoc = tempDir.resolve("databases.txt");
        Files.writeString(dbDoc, "Databases store and organize data. " +
                "SQL is used to query relational databases. " +
                "NoSQL databases handle unstructured data.");

        // Process both documents
        documentProcessor.processDocument(aiDoc, vectorDB);
        documentProcessor.processDocument(dbDoc, vectorDB);

        // Search for AI-related content
        String aiQuery = "machine learning and artificial intelligence";
        double[] aiEmbedding = embeddingProcessor.embed(aiQuery);
        List<SearchResult> aiResults = vectorDB.search(aiEmbedding, 2);

        // Verify AI document chunks rank higher
        assertNotNull(aiResults);
        assertFalse(aiResults.isEmpty());

        // Top result should be from AI document
        SearchResult topResult = aiResults.get(0);
        String sourceFile = (String) topResult.getMetadata().get("source_file");
        assertTrue(sourceFile.contains("ai.txt"),
                "AI query should retrieve AI document chunks first");
    }

    @Test
    void testProcessorStats() {
        Map<String, Object> stats = documentProcessor.getStats();

        assertNotNull(stats);
        assertTrue((Boolean) stats.get("initialized"));
        assertTrue((Integer) stats.get("registered_parsers") > 0);
        assertNotNull(stats.get("supported_extensions"));
        assertNotNull(stats.get("chunking_strategy"));
        assertNotNull(stats.get("embedding_processor"));
    }

    @Test
    void testUnsupportedFileFormat(@TempDir Path tempDir) throws IOException {
        Path unsupportedFile = tempDir.resolve("test.unknown");
        Files.writeString(unsupportedFile, "Some content");

        assertFalse(documentProcessor.supports(unsupportedFile),
                "Should not support unknown file format");

        assertThrows(Exception.class, () -> {
            documentProcessor.processDocument(unsupportedFile, vectorDB);
        }, "Should throw exception for unsupported format");
    }

    @Test
    void testEmptyDocument(@TempDir Path tempDir) throws IOException {
        Path emptyDoc = tempDir.resolve("empty.txt");
        Files.writeString(emptyDoc, "");

        List<String> chunks = documentProcessor.processDocument(emptyDoc, vectorDB);

        // Empty document should produce no chunks
        assertTrue(chunks.isEmpty(), "Empty document should produce no chunks");
    }

    @Test
    void testChunkMetadataPreservation(@TempDir Path tempDir) throws IOException {
        String content = "This is a test document for metadata preservation. " +
                "It contains multiple sentences.";

        Path docPath = tempDir.resolve("metadata_test.txt");
        Files.writeString(docPath, content);

        List<String> chunkIds = documentProcessor.processDocument(docPath, vectorDB);
        assertFalse(chunkIds.isEmpty());

        // Search to retrieve chunk with metadata
        double[] queryEmbedding = embeddingProcessor.embed("test document");
        List<SearchResult> results = vectorDB.search(queryEmbedding, 1);

        assertFalse(results.isEmpty());
        Map<String, Object> metadata = results.get(0).getMetadata();

        // Verify important metadata fields
        assertTrue(metadata.containsKey("source_file"));
        assertTrue(metadata.containsKey("chunk_id"));
        assertTrue(metadata.containsKey("document_id"));
        assertTrue(metadata.containsKey("chunk_index"));
        assertTrue(metadata.containsKey("parser"));

        String sourceFile = (String) metadata.get("source_file");
        assertTrue(sourceFile.endsWith("metadata_test.txt"));
    }
}
