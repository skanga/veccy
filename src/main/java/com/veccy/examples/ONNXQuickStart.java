package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;

import java.util.*;

/**
 * Quick start example for using ONNX Sentence Transformers with Veccy.
 * <p>
 * This is a minimal, self-contained example showing the essential steps.
 * <p>
 * Prerequisites:
 * 1. Export a Sentence Transformers model to ONNX:
 *    python scripts/export_sentence_transformer.py all-MiniLM-L6-v2
 * <p>
 * 2. Update MODEL_PATH below to point to your exported model
 * <p>
 * 3. Run this class
 */
public class ONNXQuickStart {

    // Update this path to your exported ONNX model
    private static final String MODEL_PATH = "./models/all-MiniLM-L6-v2-onnx/model.onnx";
    private static final int DIMENSIONS = 384;  // all-MiniLM-L6-v2 uses 384 dimensions

    public static void main(String[] args) {
        System.out.println("=== Veccy + ONNX Sentence Transformers Quick Start ===\n");

        // Check if model exists
        if (!java.nio.file.Files.exists(java.nio.file.Paths.get(MODEL_PATH))) {
            printSetupInstructions();
            return;
        }

        try {
            runExample();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runExample() {
        System.out.println("Step 1: Configure ONNX Embedding Processor");
        System.out.println("-".repeat(60));

        // Configure the embedding processor
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", MODEL_PATH);
        config.put("dimensions", DIMENSIONS);
        config.put("max_length", 128);

        System.out.println("  Model path: " + MODEL_PATH);
        System.out.println("  Dimensions: " + DIMENSIONS);
        System.out.println("  Max length: 128");
        System.out.println();

        // Initialize embedding processor
        System.out.println("Step 2: Initialize Embedding Processor");
        System.out.println("-".repeat(60));

        ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
        embedder.initialize(config);
        System.out.println("  ✓ Embedding processor initialized");
        System.out.println();

        try {
            // Create vector database
            System.out.println("Step 3: Create Vector Database");
            System.out.println("-".repeat(60));

            VectorDB db = VectorDBFactory.createHighPerformance();
            db.initialize();
            System.out.println("  ✓ Vector database created (HNSW index)");
            System.out.println();

            try {
                // Sample documents
                System.out.println("Step 4: Prepare Sample Documents");
                System.out.println("-".repeat(60));

                String[] documents = {
                    "Veccy is a high-performance vector database for Java",
                    "ONNX Runtime enables efficient neural network inference",
                    "Sentence Transformers create semantic text embeddings",
                    "Machine learning models can understand natural language",
                    "Vector similarity search finds semantically similar content"
                };

                for (int i = 0; i < documents.length; i++) {
                    System.out.println("  " + (i+1) + ". " + documents[i]);
                }
                System.out.println();

                // Generate embeddings
                System.out.println("Step 5: Generate Embeddings");
                System.out.println("-".repeat(60));

                long startTime = System.currentTimeMillis();
                double[][] embeddings = embedder.embedBatch(Arrays.asList(documents));
                long embedTime = System.currentTimeMillis() - startTime;

                System.out.println("  ✓ Generated " + embeddings.length + " embeddings");
                System.out.println("  ✓ Time taken: " + embedTime + "ms");
                System.out.println("  ✓ Embedding shape: [" + embeddings.length + " x " + embeddings[0].length + "]");
                System.out.println();

                // Prepare metadata
                List<Map<String, Object>> metadata = new ArrayList<>();
                for (int i = 0; i < documents.length; i++) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("text", documents[i]);
                    meta.put("doc_id", i + 1);
                    metadata.add(meta);
                }

                // Insert into database
                System.out.println("Step 6: Index Documents");
                System.out.println("-".repeat(60));

                startTime = System.currentTimeMillis();
                List<String> ids = db.insert(embeddings, metadata);
                long indexTime = System.currentTimeMillis() - startTime;

                System.out.println("  ✓ Indexed " + ids.size() + " documents");
                System.out.println("  ✓ Time taken: " + indexTime + "ms");
                System.out.println();

                // Perform semantic search
                System.out.println("Step 7: Semantic Search");
                System.out.println("-".repeat(60));

                String[] queries = {
                    "How do vector databases work?",
                    "What is machine learning?",
                    "Tell me about text embeddings"
                };

                for (String query : queries) {
                    System.out.println("\n🔍 Query: \"" + query + "\"");

                    startTime = System.currentTimeMillis();
                    double[] queryEmbedding = embedder.embed(query);
                    List<SearchResult> results = db.search(queryEmbedding, 2);
                    long searchTime = System.currentTimeMillis() - startTime;

                    System.out.println("   Top 2 matches (in " + searchTime + "ms):");

                    for (int i = 0; i < results.size(); i++) {
                        SearchResult result = results.get(i);
                        String text = (String) result.getMetadata().get("text");
                        int docId = (Integer) result.getMetadata().get("doc_id");
                        double similarity = 1.0 - result.getDistance(); // Convert distance to similarity

                        System.out.printf("   %d. [Doc %d] %.1f%% match%n",
                            i + 1, docId, similarity * 100);
                        System.out.println("      \"" + text + "\"");
                    }
                }

                // Show statistics
                System.out.println("\n" + "=".repeat(60));
                System.out.println("Database Statistics");
                System.out.println("=".repeat(60));

                Map<String, Object> stats = db.getStats();
                System.out.println("  Vectors:     " + stats.get("vector_count"));
                System.out.println("  Dimensions:  " + stats.get("dimensions"));
                System.out.println("  Index type:  " + stats.get("index_type"));

                System.out.println("\n✅ Example completed successfully!");
                System.out.println();

            } finally {
                db.close();
            }

        } finally {
            embedder.close();
        }
    }

    private static void printSetupInstructions() {
        System.err.println("❌ ONNX model not found at: " + MODEL_PATH);
        System.err.println();
        System.err.println("Please follow these steps to set up:");
        System.err.println();
        System.err.println("1. Install Python dependencies:");
        System.err.println("   pip install sentence-transformers optimum onnx onnxruntime transformers");
        System.err.println();
        System.err.println("2. Export a Sentence Transformers model:");
        System.err.println("   cd scripts");
        System.err.println("   python export_sentence_transformer.py all-MiniLM-L6-v2");
        System.err.println();
        System.err.println("3. Update MODEL_PATH in this file if needed");
        System.err.println();
        System.err.println("4. Re-run this example");
        System.err.println();
        System.err.println("Alternative: Quick Python export script");
        System.err.println("-------------------------------------------");
        System.err.println("from optimum.onnxruntime import ORTModelForFeatureExtraction");
        System.err.println("from transformers import AutoTokenizer");
        System.err.println();
        System.err.println("model = ORTModelForFeatureExtraction.from_pretrained(");
        System.err.println("    'sentence-transformers/all-MiniLM-L6-v2', export=True)");
        System.err.println("tokenizer = AutoTokenizer.from_pretrained(");
        System.err.println("    'sentence-transformers/all-MiniLM-L6-v2')");
        System.err.println();
        System.err.println("model.save_pretrained('./models/all-MiniLM-L6-v2-onnx')");
        System.err.println("tokenizer.save_pretrained('./models/all-MiniLM-L6-v2-onnx')");
        System.err.println();
        System.err.println("For more information, see: docs/SENTENCE_TRANSFORMERS_GUIDE.md");
        System.err.println();
    }
}
