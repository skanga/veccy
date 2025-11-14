package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.client.VectorDBClient;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.factory.VectorDBFactory;
import com.veccy.indices.HNSWIndex;
import com.veccy.processing.DocumentProcessor;
import com.veccy.processing.chunking.SentenceChunkingStrategy;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;
import com.veccy.processing.parsers.TextParser;
import com.veccy.storage.MemoryStorage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Example demonstrating how to use Veccy with ONNX Sentence Transformers models.
 * <p>
 * This example shows:
 * 1. Setting up an ONNX embedding processor with a Sentence Transformers model
 * 2. Creating a vector database with HNSW index
 * 3. Processing and indexing documents
 * 4. Performing semantic search
 * <p>
 * Prerequisites:
 * - Download a Sentence Transformers ONNX model (e.g., all-MiniLM-L6-v2)
 * - Place the .onnx file in a known location
 * <p>
 * Model recommendations (small, fast models):
 * - all-MiniLM-L6-v2: 384 dimensions, 22M parameters, very fast
 * - paraphrase-MiniLM-L3-v2: 384 dimensions, 17M parameters, fastest
 * - all-MiniLM-L12-v2: 384 dimensions, 33M parameters, balanced
 * <p>
 * To export Sentence Transformers to ONNX:
 * ```python
 * from sentence_transformers import SentenceTransformer
 * from optimum.onnxruntime import ORTModelForFeatureExtraction
 * from transformers import AutoTokenizer
 *
 * # Load model
 * model_name = "sentence-transformers/all-MiniLM-L6-v2"
 * model = ORTModelForFeatureExtraction.from_pretrained(model_name, export=True)
 * tokenizer = AutoTokenizer.from_pretrained(model_name)
 *
 * # Save ONNX model
 * model.save_pretrained("./models/all-MiniLM-L6-v2-onnx")
 * tokenizer.save_pretrained("./models/all-MiniLM-L6-v2-onnx")
 * ```
 */
public class SentenceTransformersExample {

    public static void main(String[] args) {
        System.out.println("=== Veccy with Sentence Transformers ONNX Example ===\n");

        // Example 1: Basic setup with ONNX embeddings
        basicExample();

        // Example 2: Document processing pipeline
        documentProcessingExample();

        // Example 3: Semantic search with real text
        semanticSearchExample();
    }

    /**
     * Example 1: Basic setup with ONNX embedding processor
     */
    private static void basicExample() {
        System.out.println("--- Example 1: Basic ONNX Setup ---");

        // Configure ONNX embedding processor
        Map<String, Object> embeddingConfig = new HashMap<>();
        embeddingConfig.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
        embeddingConfig.put("max_length", 128);
        embeddingConfig.put("dimensions", 384);  // all-MiniLM-L6-v2 outputs 384-dim embeddings

        // Create embedding processor
        ONNXEmbeddingProcessor embeddingProcessor = new ONNXEmbeddingProcessor();

        try {
            // Initialize (will fail if model not found - this is just an example)
            embeddingProcessor.initialize(embeddingConfig);

            // Generate embeddings for sample texts
            String[] texts = {
                "The cat sits on the mat",
                "A feline rests on a rug",
                "Dogs are playing in the park"
            };

            System.out.println("Generating embeddings for " + texts.length + " texts...");

            // Batch embedding (more efficient)
            double[][] embeddings = embeddingProcessor.embedBatch(Arrays.asList(texts));

            System.out.println("Generated embeddings:");
            for (int i = 0; i < texts.length; i++) {
                System.out.printf("  Text %d: %d dimensions, first 5 values: [%.4f, %.4f, %.4f, %.4f, %.4f...]%n",
                    i + 1,
                    embeddings[i].length,
                    embeddings[i][0], embeddings[i][1], embeddings[i][2],
                    embeddings[i][3], embeddings[i][4]);
            }

            // Create vector database with these embeddings
            VectorDB db = VectorDBFactory.createSimple();
            db.initialize();

            try {
                // Prepare metadata
                List<Map<String, Object>> metadata = new ArrayList<>();
                for (int i = 0; i < texts.length; i++) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("text", texts[i]);
                    meta.put("index", i);
                    metadata.add(meta);
                }

                // Insert vectors
                List<String> ids = db.insert(embeddings, metadata);
                System.out.println("\nInserted " + ids.size() + " vectors into database");

                // Search for similar texts
                String query = "A cat on a carpet";
                System.out.println("\nSearching for: \"" + query + "\"");

                double[] queryEmbedding = embeddingProcessor.embed(query);
                List<SearchResult> results = db.search(queryEmbedding, 2);

                System.out.println("Top 2 similar texts:");
                for (SearchResult result : results) {
                    String originalText = (String) result.getMetadata().get("text");
                    System.out.printf("  - \"%s\" (distance: %.4f)%n",
                        originalText, result.getDistance());
                }

            } finally {
                db.close();
            }

        } catch (Exception e) {
            System.err.println("Note: This example requires an ONNX model file.");
            System.err.println("Error: " + e.getMessage());
            System.err.println("\nTo run this example:");
            System.err.println("1. Download or export a Sentence Transformers model to ONNX");
            System.err.println("2. Place it at: ./models/all-MiniLM-L6-v2.onnx");
            System.err.println("3. Re-run this example");
        } finally {
            if (embeddingProcessor.isInitialized()) {
                embeddingProcessor.close();
            }
        }

        System.out.println();
    }

    /**
     * Example 2: Complete document processing pipeline with ONNX
     */
    private static void documentProcessingExample() {
        System.out.println("--- Example 2: Document Processing Pipeline ---");

        try {
            // Configure ONNX embedding processor
            Map<String, Object> embeddingConfig = new HashMap<>();
            embeddingConfig.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
            embeddingConfig.put("max_length", 128);
            embeddingConfig.put("dimensions", 384);

            ONNXEmbeddingProcessor embeddingProcessor = new ONNXEmbeddingProcessor();
            embeddingProcessor.initialize(embeddingConfig);

            // Create document processor
            TextParser parser = new TextParser();
            SentenceChunkingStrategy chunker = new SentenceChunkingStrategy();
            DocumentProcessor processor = new DocumentProcessor();

            processor.initialize(embeddingProcessor);
            processor.registerParser(parser);
            processor.setChunkingStrategy(chunker, null);

            // Create vector database
            VectorDB db = VectorDBFactory.createHighPerformance(); // 384, 16, 200
            db.initialize();

            try {
                // Process a sample document
                String sampleDoc = """
                    Artificial Intelligence is transforming the world.
                    Machine learning models can now understand natural language.
                    Deep learning has revolutionized computer vision.
                    Neural networks power modern AI systems.
                    """;

                // Save to temporary file
                Path tempFile = Paths.get("temp_doc.txt");
                java.nio.file.Files.writeString(tempFile, sampleDoc);

                // Process document and store in database
                List<String> chunkIds = processor.processDocument(tempFile, db);

                System.out.println("Processed document into " + chunkIds.size() + " chunks");
                System.out.println("Chunk IDs: " + chunkIds);

                // Search for relevant chunks
                String query = "What is AI?";
                double[] queryEmbedding = embeddingProcessor.embed(query);
                List<SearchResult> results = db.search(queryEmbedding, 2);

                System.out.println("\nQuery: \"" + query + "\"");
                System.out.println("Relevant chunks:");
                for (SearchResult result : results) {
                    String text = (String) result.getMetadata().get("text");
                    System.out.printf("  - %s (distance: %.4f)%n",
                        text.substring(0, Math.min(50, text.length())), result.getDistance());
                }

                // Cleanup
                java.nio.file.Files.deleteIfExists(tempFile);

            } finally {
                db.close();
                embeddingProcessor.close();
            }

        } catch (Exception e) {
            System.err.println("Note: This example requires an ONNX model file.");
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Example 3: Semantic search example with various queries
     */
    private static void semanticSearchExample() {
        System.out.println("--- Example 3: Semantic Search ---");

        try {
            // Configure embedding processor
            Map<String, Object> embeddingConfig = new HashMap<>();
            embeddingConfig.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
            embeddingConfig.put("max_length", 128);
            embeddingConfig.put("dimensions", 384);

            ONNXEmbeddingProcessor embeddingProcessor = new ONNXEmbeddingProcessor();
            embeddingProcessor.initialize(embeddingConfig);

            // Create HNSW index for fast approximate search
            Map<String, Object> storageConfig = new HashMap<>();
            MemoryStorage storage = new MemoryStorage(storageConfig);

            HNSWConfig hnswConfig = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();

            HNSWIndex index = new HNSWIndex(hnswConfig);
            VectorDBClient db = new VectorDBClient(storage, index);
            db.initialize();

            try {
                // Knowledge base: various facts about different topics
                String[] knowledgeBase = {
                    "Python is a high-level programming language",
                    "Machine learning is a subset of artificial intelligence",
                    "The Eiffel Tower is located in Paris, France",
                    "Java is an object-oriented programming language",
                    "Deep learning uses neural networks with multiple layers",
                    "The Great Wall of China is visible from space",
                    "JavaScript is used for web development",
                    "Natural language processing enables computers to understand human language",
                    "The Statue of Liberty is in New York City",
                    "C++ is known for its performance and low-level control"
                };

                System.out.println("Indexing " + knowledgeBase.length + " facts...");

                // Generate embeddings and index
                double[][] embeddings = embeddingProcessor.embedBatch(Arrays.asList(knowledgeBase));

                List<Map<String, Object>> metadata = new ArrayList<>();
                for (int i = 0; i < knowledgeBase.length; i++) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("fact", knowledgeBase[i]);
                    meta.put("id", i);
                    metadata.add(meta);
                }

                db.insert(embeddings, metadata);

                // Perform semantic searches
                String[] queries = {
                    "Tell me about programming languages",
                    "What are some famous landmarks?",
                    "Explain AI and ML"
                };

                for (String query : queries) {
                    System.out.println("\n📍 Query: \"" + query + "\"");

                    double[] queryEmbedding = embeddingProcessor.embed(query);
                    List<SearchResult> results = db.search(queryEmbedding, 3);

                    System.out.println("   Top 3 relevant facts:");
                    for (int i = 0; i < results.size(); i++) {
                        SearchResult result = results.get(i);
                        String fact = (String) result.getMetadata().get("fact");
                        double similarity = 1.0 - result.getDistance(); // Convert distance to similarity
                        System.out.printf("   %d. [%.1f%% match] %s%n",
                            i + 1, similarity * 100, fact);
                    }
                }

                // Statistics
                Map<String, Object> stats = db.getStats();
                System.out.println("\nDatabase Statistics:");
                System.out.println("  Vectors indexed: " + stats.get("vector_count"));
                System.out.println("  Dimensions: " + stats.get("dimensions"));
                System.out.println("  Index type: " + stats.get("index_type"));

            } finally {
                db.close();
                embeddingProcessor.close();
            }

        } catch (Exception e) {
            System.err.println("Note: This example requires an ONNX model file.");
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * Helper method to demonstrate configuration options
     */
    public static Map<String, Object> getModelConfigurations() {
        Map<String, Object> configs = new HashMap<>();

        // all-MiniLM-L6-v2 (recommended for most use cases)
        Map<String, Object> miniLM = new HashMap<>();
        miniLM.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
        miniLM.put("max_length", 128);
        miniLM.put("dimensions", 384);
        configs.put("all-MiniLM-L6-v2", miniLM);

        // paraphrase-MiniLM-L3-v2 (fastest, smallest)
        Map<String, Object> paraPhrase = new HashMap<>();
        paraPhrase.put("model_path", "./models/paraphrase-MiniLM-L3-v2.onnx");
        paraPhrase.put("max_length", 128);
        paraPhrase.put("dimensions", 384);
        configs.put("paraphrase-MiniLM-L3-v2", paraPhrase);

        // all-mpnet-base-v2 (highest quality, larger)
        Map<String, Object> mpnet = new HashMap<>();
        mpnet.put("model_path", "./models/all-mpnet-base-v2.onnx");
        mpnet.put("max_length", 384);
        mpnet.put("dimensions", 768);
        configs.put("all-mpnet-base-v2", mpnet);

        return configs;
    }
}
