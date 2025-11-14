package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.DocumentProcessor;
import com.veccy.processing.chunking.FixedSizeChunkingStrategy;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;
import com.veccy.processing.parsers.TextParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Demonstration of a Retrieval-Augmented Generation (RAG) system using Veccy.
 * <p>
 * This example shows how to:
 * 1. Build a persistent vector database from documentation
 * 2. Use ONNX Sentence Transformers for embeddings
 * 3. Process and chunk markdown documents
 * 4. Perform semantic search for question answering
 * 5. Retrieve relevant context for RAG applications
 * <p>
 * The system indexes all markdown files from the docs/ directory and allows
 * semantic search queries to find relevant documentation chunks.
 */
public class RAGDemo {

    // Configuration
    private static final String MODEL_PATH = "./models/all-MiniLM-L6-v2.onnx";
    private static final String DOCS_DIR = "./docs";
    private static final String DATA_DIR = "./data/rag-demo";
    private static final int DIMENSIONS = 384;  // all-MiniLM-L6-v2 dimensions
    private static final int CHUNK_SIZE = 512;  // Characters per chunk
    private static final int CHUNK_OVERLAP = 128;  // Overlap between chunks

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Veccy RAG Demo - Documentation Question Answering System");
        System.out.println("=".repeat(80));
        System.out.println();

        // Check if model exists
        if (!Files.exists(Paths.get(MODEL_PATH))) {
            printSetupInstructions();
            return;
        }

        try {
            runDemo();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDemo() throws IOException {
        // Step 1: Initialize ONNX Embedding Processor
        System.out.println("Step 1: Initialize Embedding Processor");
        System.out.println("-".repeat(80));

        Map<String, Object> embeddingConfig = new HashMap<>();
        embeddingConfig.put("model_path", MODEL_PATH);
        embeddingConfig.put("dimensions", DIMENSIONS);
        embeddingConfig.put("max_length", 128);

        ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
        try {
            embedder.initialize(embeddingConfig);
            System.out.println("  ✓ ONNX embedding processor initialized");
            System.out.println("  ✓ Model: all-MiniLM-L6-v2 (384 dimensions)");
            System.out.println();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("  ✗ ONNX Runtime failed to initialize");
            System.err.println();
            System.err.println("This is likely a Windows issue. To fix:");
            System.err.println();
            System.err.println("1. Install Visual C++ Redistributable:");
            System.err.println("   Download: https://aka.ms/vs/17/release/vc_redist.x64.exe");
            System.err.println("   Or run: winget install Microsoft.VCRedist.2015+.x64");
            System.err.println();
            System.err.println("2. Restart your terminal and try again");
            System.err.println();
            System.err.println("Alternative: Use TF-IDF or External API embeddings instead.");
            System.err.println("See docs/TROUBLESHOOTING_ONNX.md for details.");
            System.err.println();
            throw e;
        }

        try {
            // Step 2: Create Persistent Vector Database
            System.out.println("Step 2: Create Persistent Vector Database");
            System.out.println("-".repeat(80));

            // Check if database already exists
            Path dataPath = Paths.get(DATA_DIR);
            boolean dbExists = Files.exists(dataPath) &&
                             Files.list(dataPath).findAny().isPresent();

            VectorDB db = VectorDBFactory.createPersistent(DATA_DIR, true);
            System.out.println("  ✓ Persistent vector database created");
            System.out.println("  ✓ Storage location: " + DATA_DIR);
            System.out.println("  ✓ Index type: HNSW (Hierarchical Navigable Small World)");
            System.out.println();

            try {
                // Step 3: Index Documentation (if not already indexed)
                if (!dbExists || getVectorCount(db) == 0) {
                    indexDocumentation(db, embedder);
                } else {
                    System.out.println("Step 3: Load Existing Index");
                    System.out.println("-".repeat(80));
                    System.out.println("  ✓ Found existing vector database");
                    Map<String, Object> stats = db.getStats();
                    System.out.println("  ✓ Loaded " + getVectorCount(db) + " vectors");
                    System.out.println("  ✓ To rebuild index, delete directory: " + DATA_DIR);
                    System.out.println();
                }

                // Step 4: Interactive Question Answering
                performQuestionAnswering(db, embedder);

            } finally {
                db.close();
            }
        } finally {
            embedder.close();
        }
    }

    private static void indexDocumentation(VectorDB db, ONNXEmbeddingProcessor embedder) throws IOException {
        System.out.println("Step 3: Index Documentation");
        System.out.println("-".repeat(80));

        // Initialize document processor
        DocumentProcessor processor = new DocumentProcessor();
        processor.initialize(embedder);

        // Register markdown parser (TextParser supports .md files)
        processor.registerParser(new TextParser());

        // Configure chunking strategy
        Map<String, Object> chunkingConfig = new HashMap<>();
        chunkingConfig.put("chunk_size", CHUNK_SIZE);
        chunkingConfig.put("overlap", CHUNK_OVERLAP);
        processor.setChunkingStrategy(new FixedSizeChunkingStrategy(), chunkingConfig);

        System.out.println("  Document Processing Configuration:");
        System.out.println("    - Chunk size: " + CHUNK_SIZE + " characters");
        System.out.println("    - Overlap: " + CHUNK_OVERLAP + " characters");
        System.out.println();

        // Find all markdown files
        Path docsPath = Paths.get(DOCS_DIR);
        if (!Files.exists(docsPath)) {
            System.err.println("  ✗ Documentation directory not found: " + DOCS_DIR);
            return;
        }

        List<Path> markdownFiles;
        try (Stream<Path> paths = Files.walk(docsPath)) {
            markdownFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".md"))
                    .collect(Collectors.toList());
        }

        if (markdownFiles.isEmpty()) {
            System.err.println("  ✗ No markdown files found in: " + DOCS_DIR);
            return;
        }

        System.out.println("  Found " + markdownFiles.size() + " markdown files:");
        for (Path file : markdownFiles) {
            System.out.println("    - " + file.getFileName());
        }
        System.out.println();

        // Process each document
        long startTime = System.currentTimeMillis();
        int totalChunks = 0;

        for (Path file : markdownFiles) {
            System.out.println("  Processing: " + file.getFileName() + "...");
            try {
                List<String> chunkIds = processor.processDocument(file, db);
                totalChunks += chunkIds.size();
                System.out.println("    ✓ Created " + chunkIds.size() + " chunks");
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

    private static void performQuestionAnswering(VectorDB db, ONNXEmbeddingProcessor embedder) {
        System.out.println("Step 4: Question Answering with Semantic Search");
        System.out.println("-".repeat(80));
        System.out.println();

        // Sample questions about the documentation
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

        // Show database statistics
        System.out.println("=".repeat(80));
        System.out.println("Database Statistics");
        System.out.println("=".repeat(80));

        Map<String, Object> stats = db.getStats();
        displayStats(stats);

        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("✅ RAG Demo Complete!");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Next Steps:");
        System.out.println("  1. Integrate this with an LLM (OpenAI, Anthropic, etc.)");
        System.out.println("  2. Use retrieved context as input to generate answers");
        System.out.println("  3. Implement conversation history for multi-turn QA");
        System.out.println("  4. Add re-ranking for improved relevance");
        System.out.println();
    }

    private static void answerQuestion(String question, VectorDB db, ONNXEmbeddingProcessor embedder) {
        System.out.println("🔍 Question: \"" + question + "\"");
        System.out.println();

        long startTime = System.currentTimeMillis();

        // Generate query embedding
        double[] queryEmbedding = embedder.embed(question);

        // Search for top 3 most relevant chunks
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

            // Show preview of the text (first 200 chars)
            String preview = text != null ? text : "";
            if (preview.length() > 200) {
                preview = preview.substring(0, 200) + "...";
            }
            // Clean up the preview text
            preview = preview.replaceAll("\\s+", " ").trim();
            System.out.println("      " + preview);
            System.out.println();
        }

        System.out.println("   💡 In a full RAG system, this context would be sent to an LLM");
        System.out.println("      to generate a natural language answer.");
        System.out.println();
        System.out.println("-".repeat(80));
        System.out.println();
    }

    private static int getVectorCount(VectorDB db) {
        try {
            Map<String, Object> stats = db.getStats();

            // Try different possible locations for vector count
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

            if (stats.get("storage") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
                if (storageStats.get("vector_count") instanceof Number) {
                    return ((Number) storageStats.get("vector_count")).intValue();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    private static void displayStats(Map<String, Object> stats) {
        System.out.println("  Overall:");
        System.out.println("    - Vectors: " + getVectorCount(stats));

        if (stats.get("index") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
            System.out.println();
            System.out.println("  Index:");
            System.out.println("    - Type: " + indexStats.get("index_type"));
            System.out.println("    - Metric: " + indexStats.get("metric"));
            if (indexStats.get("m") != null) {
                System.out.println("    - M: " + indexStats.get("m"));
            }
            if (indexStats.get("ef_construction") != null) {
                System.out.println("    - EF Construction: " + indexStats.get("ef_construction"));
            }
        }

        if (stats.get("storage") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
            System.out.println();
            System.out.println("  Storage:");
            System.out.println("    - Type: " + storageStats.get("type"));
            if (storageStats.get("data_dir") != null) {
                System.out.println("    - Location: " + storageStats.get("data_dir"));
            }
        }
    }

    private static int getVectorCount(Map<String, Object> stats) {
        // Try different possible locations for vector count
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

        if (stats.get("storage") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> storageStats = (Map<String, Object>) stats.get("storage");
            if (storageStats.get("vector_count") instanceof Number) {
                return ((Number) storageStats.get("vector_count")).intValue();
            }
        }

        return 0;
    }

    private static void printSetupInstructions() {
        System.err.println("❌ ONNX model not found at: " + MODEL_PATH);
        System.err.println();
        System.err.println("Please export the Sentence Transformers model first:");
        System.err.println();
        System.err.println("1. Ensure Python dependencies are installed:");
        System.err.println("   pip install sentence-transformers optimum onnx onnxruntime transformers");
        System.err.println();
        System.err.println("2. Export the model:");
        System.err.println("   cd scripts");
        System.err.println("   python export_sentence_transformer.py all-MiniLM-L6-v2");
        System.err.println();
        System.err.println("3. Re-run this demo");
        System.err.println();
        System.err.println("For more information, see: docs/SENTENCE_TRANSFORMERS_GUIDE.md");
        System.err.println();
    }
}
