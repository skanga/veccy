package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.DocumentProcessor;
import com.veccy.processing.chunking.FixedSizeChunkingStrategy;
import com.veccy.processing.embeddings.TfidfEmbeddingProcessor;
import com.veccy.processing.parsers.TextParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RAG Demo using TF-IDF embeddings (NO native libraries required).
 * <p>
 * This version works on ANY system without ONNX Runtime or Visual C++ dependencies.
 * Perfect for:
 * - Testing and development
 * - Systems where you can't install native libraries
 * - Quick prototyping
 * - Windows systems with ONNX issues
 * <p>
 * Note: TF-IDF provides keyword-based search, not semantic search like ONNX.
 * For production semantic search, use ONNX (once native lib issues are resolved)
 * or External API embeddings (OpenAI, Cohere).
 */
public class RAGDemoTFIDF {

    // Configuration
    private static final String DOCS_DIR = "./docs";
    private static final String DATA_DIR = "./data/rag-demo-tfidf";
    private static final int CHUNK_SIZE = 512;
    private static final int CHUNK_OVERLAP = 128;
    private static final int MAX_VOCAB_SIZE = 10000;

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Veccy RAG Demo - TF-IDF Version (No Native Dependencies)");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("ℹ️  This version uses TF-IDF embeddings instead of ONNX");
        System.out.println("   ✓ Works on any system without native library issues");
        System.out.println("   ✓ Instant setup - no model downloads needed");
        System.out.println("   ⚠ Uses keyword matching instead of semantic understanding");
        System.out.println();

        try {
            runDemo();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDemo() throws IOException {
        // Step 1: Initialize TF-IDF Embedding Processor
        System.out.println("Step 1: Initialize TF-IDF Embedding Processor");
        System.out.println("-".repeat(80));

        Map<String, Object> embeddingConfig = new HashMap<>();
        embeddingConfig.put("max_vocab_size", MAX_VOCAB_SIZE);

        TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
        embedder.initialize(embeddingConfig);
        System.out.println("  ✓ TF-IDF processor initialized");
        System.out.println("  ✓ Max vocabulary size: " + MAX_VOCAB_SIZE);
        System.out.println();

        try {
            // Step 2: Create Persistent Vector Database
            System.out.println("Step 2: Create Persistent Vector Database");
            System.out.println("-".repeat(80));

            Path dataPath = Paths.get(DATA_DIR);
            boolean dbExists = Files.exists(dataPath) &&
                             Files.list(dataPath).findAny().isPresent();

            VectorDB db = VectorDBFactory.createPersistent(DATA_DIR, true);
            System.out.println("  ✓ Persistent vector database created");
            System.out.println("  ✓ Storage location: " + DATA_DIR);
            System.out.println("  ✓ Index type: HNSW");
            System.out.println();

            try {
                // Step 3: Load and process documentation
                if (!dbExists || getVectorCount(db) == 0) {
                    // First, collect all documents for training
                    List<String> allDocuments = loadAllDocuments();

                    if (allDocuments.isEmpty()) {
                        System.err.println("  ✗ No documents found in: " + DOCS_DIR);
                        return;
                    }

                    // Train TF-IDF on the corpus
                    System.out.println("Step 3: Train TF-IDF Model");
                    System.out.println("-".repeat(80));
                    System.out.println("  Training on " + allDocuments.size() + " text chunks...");

                    long startTime = System.currentTimeMillis();
                    embedder.train(allDocuments);
                    long trainTime = System.currentTimeMillis() - startTime;

                    System.out.println("  ✓ Training complete");
                    System.out.println("  ✓ Vocabulary size: " + embedder.getDimensions());
                    System.out.println("  ✓ Training time: " + trainTime + "ms");
                    System.out.println();

                    // Now index the documents
                    indexDocumentation(db, embedder, allDocuments);
                } else {
                    System.out.println("Step 3: Load Existing Index");
                    System.out.println("-".repeat(80));
                    System.out.println("  ✓ Found existing vector database");
                    System.out.println("  ✓ Loaded " + getVectorCount(db) + " vectors");
                    System.out.println("  ✓ To rebuild index, delete directory: " + DATA_DIR);
                    System.out.println();

                    // Need to retrain for this session
                    List<String> allDocuments = loadAllDocuments();
                    if (!allDocuments.isEmpty()) {
                        System.out.println("  Retraining TF-IDF for this session...");
                        embedder.train(allDocuments);
                        System.out.println("  ✓ Ready for search");
                        System.out.println();
                    }
                }

                // Step 4: Question Answering
                performQuestionAnswering(db, embedder);

            } finally {
                db.close();
            }
        } finally {
            embedder.close();
        }
    }

    private static List<String> loadAllDocuments() throws IOException {
        Path docsPath = Paths.get(DOCS_DIR);
        if (!Files.exists(docsPath)) {
            return Collections.emptyList();
        }

        List<Path> markdownFiles;
        try (Stream<Path> paths = Files.walk(docsPath)) {
            markdownFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                    .collect(Collectors.toList());
        }

        List<String> allDocuments = new ArrayList<>();
        TextParser parser = new TextParser();

        for (Path file : markdownFiles) {
            try {
                String content = Files.readString(file);
                // Split into chunks
                List<String> chunks = chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);
                allDocuments.addAll(chunks);
            } catch (Exception e) {
                System.err.println("  ⚠ Failed to read " + file.getFileName() + ": " + e.getMessage());
            }
        }

        return allDocuments;
    }

    private static void indexDocumentation(VectorDB db, TfidfEmbeddingProcessor embedder,
                                          List<String> allDocuments) throws IOException {
        System.out.println("Step 4: Index Documentation");
        System.out.println("-".repeat(80));

        // Find all markdown files
        Path docsPath = Paths.get(DOCS_DIR);
        List<Path> markdownFiles;
        try (Stream<Path> paths = Files.walk(docsPath)) {
            markdownFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                    .collect(Collectors.toList());
        }

        System.out.println("  Found " + markdownFiles.size() + " markdown files");
        System.out.println();

        long startTime = System.currentTimeMillis();
        int totalChunks = 0;

        for (Path file : markdownFiles) {
            System.out.println("  Processing: " + file.getFileName() + "...");
            try {
                String content = Files.readString(file);
                List<String> chunks = chunkText(content, CHUNK_SIZE, CHUNK_OVERLAP);

                // Generate embeddings for chunks
                double[][] embeddings = embedder.embedBatch(chunks);

                // Prepare metadata
                List<Map<String, Object>> metadataList = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("text", chunks.get(i));
                    meta.put("source_file", file.getFileName().toString());
                    meta.put("chunk_index", i);
                    metadataList.add(meta);
                }

                // Insert into database
                db.insert(embeddings, metadataList);
                totalChunks += chunks.size();

                System.out.println("    ✓ Created " + chunks.size() + " chunks");
            } catch (Exception e) {
                System.err.println("    ✗ Failed: " + e.getMessage());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;

        System.out.println();
        System.out.println("  Indexing Complete:");
        System.out.println("    ✓ Processed " + markdownFiles.size() + " documents");
        System.out.println("    ✓ Created " + totalChunks + " searchable chunks");
        System.out.println("    ✓ Time: " + processingTime + "ms");
        System.out.println();
    }

    private static void performQuestionAnswering(VectorDB db, TfidfEmbeddingProcessor embedder) {
        System.out.println("Step 5: Question Answering");
        System.out.println("-".repeat(80));
        System.out.println();

        String[] questions = {
                "How do I use the REST API?",
                "What are the available index types?",
                "How do I perform batch operations?",
                "Tell me about health checks",
                "How do I import and export data?"
        };

        for (String question : questions) {
            answerQuestion(question, db, embedder);
        }

        // Show statistics
        System.out.println("=".repeat(80));
        System.out.println("Database Statistics");
        System.out.println("=".repeat(80));

        Map<String, Object> stats = db.getStats();
        System.out.println("  Vectors: " + getVectorCount(db));
        System.out.println("  Dimensions: " + embedder.getDimensions());
        System.out.println("  Vocabulary: TF-IDF with " + MAX_VOCAB_SIZE + " max terms");
        System.out.println();

        System.out.println("=".repeat(80));
        System.out.println("✅ TF-IDF RAG Demo Complete!");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  • TF-IDF uses keyword matching, not semantic understanding");
        System.out.println("  • For better results, use ONNX or External API embeddings");
        System.out.println("  • See docs/EMBEDDING_OPTIONS.md for comparison");
        System.out.println();
    }

    private static void answerQuestion(String question, VectorDB db, TfidfEmbeddingProcessor embedder) {
        System.out.println("🔍 Question: \"" + question + "\"");
        System.out.println();

        long startTime = System.currentTimeMillis();
        double[] queryEmbedding = embedder.embed(question);
        List<SearchResult> results = db.search(queryEmbedding, 3);
        long searchTime = System.currentTimeMillis() - startTime;

        if (results.isEmpty()) {
            System.out.println("   No relevant context found.");
            System.out.println();
            return;
        }

        System.out.println("   📚 Retrieved Context (in " + searchTime + "ms):");
        System.out.println();

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            Map<String, Object> metadata = result.getMetadata();

            double similarity = 1.0 - result.getDistance();
            String text = (String) metadata.get("text");
            String source = (String) metadata.get("source_file");
            Integer chunkIndex = (Integer) metadata.get("chunk_index");

            System.out.printf("   %d. [%.1f%% match] %s (chunk %d)%n",
                    i + 1, similarity * 100, source != null ? source : "unknown",
                    chunkIndex != null ? chunkIndex : 0);

            String preview = text != null ? text : "";
            if (preview.length() > 200) {
                preview = preview.substring(0, 200) + "...";
            }
            preview = preview.replaceAll("\\s+", " ").trim();
            System.out.println("      " + preview);
            System.out.println();
        }

        System.out.println("-".repeat(80));
        System.out.println();
    }

    private static List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            chunks.add(text.substring(pos, end));
            pos += chunkSize - overlap;

            if (pos >= text.length()) break;
        }

        return chunks;
    }

    private static int getVectorCount(VectorDB db) {
        try {
            Map<String, Object> stats = db.getStats();
            if (stats.get("vector_count") instanceof Number) {
                return ((Number) stats.get("vector_count")).intValue();
            }
            if (stats.get("index") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
                if (indexStats.get("vector_count") instanceof Number) {
                    return ((Number) indexStats.get("vector_count")).intValue();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
}
