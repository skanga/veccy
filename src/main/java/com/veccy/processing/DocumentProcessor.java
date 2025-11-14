package com.veccy.processing;

import com.veccy.base.VectorDB;
import com.veccy.exceptions.ProcessingException;
import com.veccy.processing.chunking.ChunkingStrategy;
import com.veccy.processing.chunking.FixedSizeChunkingStrategy;
import com.veccy.processing.embeddings.EmbeddingProcessor;
import com.veccy.processing.parsers.DocumentParser;
import com.veccy.processing.parsers.ParsedDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates the full document processing pipeline.
 * <p>
 * Pipeline stages:
 * 1. Parse: Extract text and metadata from document
 * 2. Chunk: Split document into manageable pieces
 * 3. Embed: Generate vector embeddings for each chunk
 * 4. Store: Insert chunks and embeddings into vector store
 * <p>
 * Features:
 * - Automatic parser selection based on file extension
 * - Configurable chunking strategies
 * - Batch embedding for efficiency
 * - Metadata preservation throughout pipeline
 * - Returns chunk IDs for later retrieval
 */
public class DocumentProcessor {

    private final Logger logger;
    private Map<String, DocumentParser> parserRegistry;
    private EmbeddingProcessor embeddingProcessor;
    private ChunkingStrategy chunkingStrategy;
    private Map<String, Object> chunkingConfig;
    private boolean initialized;

    public DocumentProcessor() {
        this.parserRegistry = new HashMap<>();
        this.chunkingStrategy = new FixedSizeChunkingStrategy();  // Default strategy
        this.chunkingConfig = new HashMap<>();
        this.initialized = false;
        this.logger = LoggerFactory.getLogger(DocumentProcessor.class);
    }

    public DocumentProcessor(Logger logger) {
        this.parserRegistry = new HashMap<>();
        this.chunkingStrategy = new FixedSizeChunkingStrategy();  // Default strategy
        this.chunkingConfig = new HashMap<>();
        this.initialized = false;
        this.logger = logger;
    }

    /**
     * Initializes the document processor.
     *
     * @param embeddingProcessor The embedding processor to use
     */
    public void initialize(EmbeddingProcessor embeddingProcessor) {
        if (!embeddingProcessor.isInitialized()) {
            throw new ProcessingException("Embedding processor must be initialized before use");
        }
        this.embeddingProcessor = embeddingProcessor;
        this.initialized = true;
        logger.info("DocumentProcessor initialized with embedding processor: {}",
                embeddingProcessor.getName());
    }

    /**
     * Registers a document parser for specific file types.
     *
     * @param parser The parser to register
     */
    public void registerParser(DocumentParser parser) {
        for (String extension : parser.getSupportedExtensions()) {
            parserRegistry.put(extension.toLowerCase(), parser);
        }
        logger.debug("Registered parser '{}' for extensions: {}",
                parser.getName(), Arrays.toString(parser.getSupportedExtensions()));
    }

    /**
     * Sets the chunking strategy to use.
     *
     * @param strategy The chunking strategy
     * @param config   Configuration for the strategy
     */
    public void setChunkingStrategy(ChunkingStrategy strategy, Map<String, Object> config) {
        this.chunkingStrategy = strategy;
        this.chunkingConfig = config != null ? new HashMap<>(config) : new HashMap<>();
        logger.info("Chunking strategy set to: {}", strategy.getName());
    }

    /**
     * Processes a document through the full pipeline and inserts into vector database.
     *
     * @param filePath  Path to the document file
     * @param vectorDB  The vector database to insert into
     * @return List of inserted chunk IDs
     * @throws IOException If file reading or parsing fails
     */
    public List<String> processDocument(Path filePath, VectorDB vectorDB) throws IOException {
        if (!initialized) {
            throw new ProcessingException("DocumentProcessor not initialized. Call initialize() first.");
        }

        if (!Files.exists(filePath)) {
            throw new ProcessingException("File not found: " + filePath);
        }

        logger.info("Processing document: {}", filePath);

        // Stage 1: Parse document
        ParsedDocument parsedDoc = parseDocument(filePath);
        logger.debug("Parsed document: {} characters extracted", parsedDoc.getTextLength());

        // Stage 2: Chunk document
        List<ProcessedChunk> chunks = chunkDocument(parsedDoc, filePath);
        logger.debug("Created {} chunks from document", chunks.size());

        if (chunks.isEmpty()) {
            logger.warn("No chunks created from document: {}", filePath);
            return new ArrayList<>();
        }

        // Stage 3: Embed chunks (batch processing)
        embedChunks(chunks);
        logger.debug("Generated embeddings for {} chunks", chunks.size());

        // Stage 4: Insert into vector database
        List<String> chunkIds = insertChunks(chunks, vectorDB);
        logger.info("Successfully processed document '{}': {} chunks inserted",
                filePath.getFileName(), chunkIds.size());

        return chunkIds;
    }

    /**
     * Processes multiple documents in batch.
     *
     * @param filePaths List of document file paths
     * @param vectorDB  The vector database to insert into
     * @return Map of file path to list of chunk IDs
     */
    public Map<Path, List<String>> processDocuments(List<Path> filePaths, VectorDB vectorDB) {
        Map<Path, List<String>> results = new HashMap<>();

        for (Path filePath : filePaths) {
            try {
                List<String> chunkIds = processDocument(filePath, vectorDB);
                results.put(filePath, chunkIds);
            } catch (Exception e) {
                logger.error("Failed to process document: {}", filePath, e);
                results.put(filePath, new ArrayList<>());
            }
        }

        logger.info("Batch processing complete: {}/{} documents processed successfully",
                results.values().stream().filter(list -> !list.isEmpty()).count(),
                filePaths.size());

        return results;
    }

    /**
     * Parses a document using the appropriate parser.
     */
    private ParsedDocument parseDocument(Path filePath) throws IOException {
        String extension = getFileExtension(filePath);
        DocumentParser parser = parserRegistry.get(extension.toLowerCase());

        if (parser == null) {
            throw new ProcessingException("No parser registered for file extension: " + extension);
        }

        return parser.parse(filePath);
    }

    /**
     * Chunks a parsed document into ProcessedChunk objects.
     */
    private List<ProcessedChunk> chunkDocument(ParsedDocument parsedDoc, Path filePath) {
        // Use chunking strategy to split text
        List<ChunkingStrategy.TextChunk> textChunks = chunkingStrategy.chunk(parsedDoc, chunkingConfig);

        // Convert to ProcessedChunk objects
        List<ProcessedChunk> processedChunks = new ArrayList<>();
        String documentId = generateDocumentId(filePath);

        for (int i = 0; i < textChunks.size(); i++) {
            ChunkingStrategy.TextChunk textChunk = textChunks.get(i);

            // Merge document metadata with chunk metadata
            Map<String, Object> chunkMetadata = new HashMap<>(parsedDoc.getMetadata());
            chunkMetadata.putAll(textChunk.getMetadata());
            chunkMetadata.put("source_file", filePath.toString());
            chunkMetadata.put("start_offset", textChunk.getStartOffset());
            chunkMetadata.put("end_offset", textChunk.getEndOffset());

            ProcessedChunk processedChunk = new ProcessedChunk(
                    textChunk.getText(),
                    i,
                    documentId,
                    chunkMetadata
            );

            processedChunks.add(processedChunk);
        }

        return processedChunks;
    }

    /**
     * Generates embeddings for all chunks using batch processing.
     */
    private void embedChunks(List<ProcessedChunk> chunks) {
        // Extract text from all chunks
        List<String> texts = new ArrayList<>();
        for (ProcessedChunk chunk : chunks) {
            texts.add(chunk.getText());
        }

        // Generate embeddings in batch
        double[][] embeddings = embeddingProcessor.embedBatch(texts);

        // Assign embeddings to chunks
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbedding(embeddings[i]);
        }
    }

    /**
     * Inserts chunks into the vector database.
     */
    private List<String> insertChunks(List<ProcessedChunk> chunks, VectorDB vectorDB) {
        // Filter out chunks without embeddings
        List<ProcessedChunk> validChunks = new ArrayList<>();
        for (ProcessedChunk chunk : chunks) {
            if (!chunk.hasEmbedding()) {
                logger.warn("Chunk {} has no embedding, skipping insertion", chunk.getChunkId());
            } else {
                validChunks.add(chunk);
            }
        }

        if (validChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // Prepare vectors and metadata for batch insertion
        double[][] vectors = new double[validChunks.size()][];
        List<Map<String, Object>> metadataList = new ArrayList<>();

        for (int i = 0; i < validChunks.size(); i++) {
            ProcessedChunk chunk = validChunks.get(i);
            vectors[i] = chunk.getEmbedding();

            // Add chunk ID to metadata
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("chunk_id", chunk.getChunkId());
            metadata.put("document_id", chunk.getDocumentId());
            metadata.put("chunk_index", chunk.getChunkIndex());
            metadataList.add(metadata);
        }

        // Batch insert into vector database
        List<String> chunkIds = vectorDB.insert(vectors, metadataList);

        return chunkIds;
    }

    /**
     * Generates a unique document ID from file path.
     */
    private String generateDocumentId(Path filePath) {
        // Use filename without extension as base
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

        // Add timestamp for uniqueness
        long timestamp = System.currentTimeMillis();
        return baseName + "_" + timestamp;
    }

    /**
     * Extracts file extension from path.
     */
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * Checks if a file type is supported.
     *
     * @param filePath Path to check
     * @return true if a parser is registered for this file type
     */
    public boolean supports(Path filePath) {
        String extension = getFileExtension(filePath);
        return parserRegistry.containsKey(extension.toLowerCase());
    }

    /**
     * Gets the list of supported file extensions.
     *
     * @return Set of supported extensions
     */
    public Set<String> getSupportedExtensions() {
        return new HashSet<>(parserRegistry.keySet());
    }

    /**
     * Gets statistics about the processor.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("initialized", initialized);
        stats.put("registered_parsers", parserRegistry.size());
        stats.put("supported_extensions", getSupportedExtensions());
        stats.put("chunking_strategy", chunkingStrategy != null ? chunkingStrategy.getName() : "none");
        stats.put("embedding_processor", embeddingProcessor != null ? embeddingProcessor.getName() : "none");
        return stats;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
