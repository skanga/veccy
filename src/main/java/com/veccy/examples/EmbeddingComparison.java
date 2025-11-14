package com.veccy.examples;

import com.veccy.processing.embeddings.ExternalAPIEmbeddingProcessor;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;
import com.veccy.processing.embeddings.TfidfEmbeddingProcessor;

import java.util.*;

/**
 * Demonstrates the different embedding processors available in Veccy.
 * <p>
 * Shows how to use:
 * 1. ONNX Embedding Processor (local neural embeddings)
 * 2. External API Embedding Processor (OpenAI, Cohere, etc.)
 * 3. TF-IDF Embedding Processor (statistical embeddings)
 */
public class EmbeddingComparison {

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Veccy Embedding Processors Comparison");
        System.out.println("=".repeat(80));
        System.out.println();

        // Sample texts for embedding
        List<String> sampleTexts = List.of(
                "Veccy is a high-performance vector database for Java",
                "Machine learning models create semantic embeddings",
                "Vector similarity search enables semantic search"
        );

        String queryText = "How does semantic search work?";

        // Demo each processor
        demoONNXProcessor(sampleTexts, queryText);
        demoExternalAPIProcessor(sampleTexts, queryText);
        demoTFIDFProcessor(sampleTexts, queryText);

        System.out.println("=".repeat(80));
        System.out.println("Summary");
        System.out.println("=".repeat(80));
        System.out.println();
        System.out.println("Processor Selection Guide:");
        System.out.println("  - ONNX: Best for production (high quality, free, offline)");
        System.out.println("  - External API: Best for prototypes (easy setup, latest models)");
        System.out.println("  - TF-IDF: Best for testing (instant, simple, baseline)");
        System.out.println();
        System.out.println("See docs/EMBEDDING_OPTIONS.md for detailed comparison.");
        System.out.println();
    }

    /**
     * Demonstrates ONNX Embedding Processor.
     */
    private static void demoONNXProcessor(List<String> texts, String query) {
        System.out.println("1. ONNX Embedding Processor");
        System.out.println("-".repeat(80));

        // Check if model exists
        String modelPath = "./models/all-MiniLM-L6-v2.onnx";
        if (!java.nio.file.Files.exists(java.nio.file.Paths.get(modelPath))) {
            System.out.println("  ⚠ ONNX model not found: " + modelPath);
            System.out.println("  ℹ Export with: cd scripts && python export_sentence_transformer.py all-MiniLM-L6-v2");
            System.out.println();
            return;
        }

        try {
            // Configure
            Map<String, Object> config = new HashMap<>();
            config.put("model_path", modelPath);
            config.put("dimensions", 384);
            config.put("max_length", 128);

            // Initialize
            ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
            embedder.initialize(config);

            System.out.println("  ✓ Model: all-MiniLM-L6-v2 (384 dimensions)");
            System.out.println("  ✓ Type: Local neural embeddings");
            System.out.println("  ✓ Cost: Free (after model export)");
            System.out.println();

            // Generate embeddings
            long startTime = System.currentTimeMillis();
            double[][] embeddings = embedder.embedBatch(texts);
            double[] queryEmbedding = embedder.embed(query);
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println("  Performance:");
            System.out.println("    - Generated " + embeddings.length + " document embeddings");
            System.out.println("    - Generated 1 query embedding");
            System.out.println("    - Time: " + elapsed + "ms");
            System.out.println();

            // Show embedding sample
            System.out.println("  Sample embedding (first 10 values):");
            System.out.print("    [");
            for (int i = 0; i < Math.min(10, queryEmbedding.length); i++) {
                System.out.printf("%.4f", queryEmbedding[i]);
                if (i < 9) System.out.print(", ");
            }
            System.out.println("...]");
            System.out.println();

            // Calculate similarities
            System.out.println("  Similarity scores:");
            for (int i = 0; i < texts.size(); i++) {
                double similarity = cosineSimilarity(queryEmbedding, embeddings[i]);
                System.out.printf("    %d. %.3f - \"%s\"%n",
                    i + 1, similarity, truncate(texts.get(i), 50));
            }
            System.out.println();

            embedder.close();

        } catch (Exception e) {
            System.err.println("  ✗ Error: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Demonstrates External API Embedding Processor.
     */
    private static void demoExternalAPIProcessor(List<String> texts, String query) {
        System.out.println("2. External API Embedding Processor");
        System.out.println("-".repeat(80));

        // Check for API key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("  ⚠ OPENAI_API_KEY not set");
            System.out.println("  ℹ Set with: export OPENAI_API_KEY=\"sk-...\"");
            System.out.println("  ℹ Or use Cohere: export COHERE_API_KEY=\"...\"");
            System.out.println();
            System.out.println("  Example configuration (not executed):");
            System.out.println();
            System.out.println("  ```java");
            System.out.println("  Map<String, Object> config = new HashMap<>();");
            System.out.println("  config.put(\"provider\", \"openai\");");
            System.out.println("  config.put(\"api_key\", System.getenv(\"OPENAI_API_KEY\"));");
            System.out.println("  config.put(\"model\", \"text-embedding-3-small\");");
            System.out.println("  config.put(\"dimensions\", 1536);");
            System.out.println();
            System.out.println("  ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();");
            System.out.println("  embedder.initialize(config);");
            System.out.println();
            System.out.println("  double[] embedding = embedder.embed(\"Your text\");");
            System.out.println("  ```");
            System.out.println();
            return;
        }

        try {
            // Configure for OpenAI
            Map<String, Object> config = new HashMap<>();
            config.put("provider", "openai");
            config.put("api_key", apiKey);
            config.put("model", "text-embedding-3-small");
            config.put("dimensions", 1536);
            config.put("timeout_seconds", 30);

            // Initialize
            ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
            embedder.initialize(config);

            System.out.println("  ✓ Provider: OpenAI");
            System.out.println("  ✓ Model: text-embedding-3-small (1536 dimensions)");
            System.out.println("  ✓ Cost: $0.00002 per 1K tokens");
            System.out.println();

            // Generate embeddings
            System.out.println("  Generating embeddings (this may take a few seconds)...");
            long startTime = System.currentTimeMillis();
            double[][] embeddings = embedder.embedBatch(texts);
            double[] queryEmbedding = embedder.embed(query);
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println();
            System.out.println("  Performance:");
            System.out.println("    - Generated " + embeddings.length + " document embeddings");
            System.out.println("    - Generated 1 query embedding");
            System.out.println("    - Time: " + elapsed + "ms");
            System.out.println();

            // Calculate similarities
            System.out.println("  Similarity scores:");
            for (int i = 0; i < texts.size(); i++) {
                double similarity = cosineSimilarity(queryEmbedding, embeddings[i]);
                System.out.printf("    %d. %.3f - \"%s\"%n",
                    i + 1, similarity, truncate(texts.get(i), 50));
            }
            System.out.println();

            embedder.close();

        } catch (Exception e) {
            System.err.println("  ✗ Error: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * Demonstrates TF-IDF Embedding Processor.
     */
    private static void demoTFIDFProcessor(List<String> texts, String query) {
        System.out.println("3. TF-IDF Embedding Processor");
        System.out.println("-".repeat(80));

        try {
            // Configure
            Map<String, Object> config = new HashMap<>();
            config.put("max_vocab_size", 10000);

            // Initialize
            TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
            embedder.initialize(config);

            System.out.println("  ✓ Type: Statistical (TF-IDF)");
            System.out.println("  ✓ Max vocabulary: 10,000 terms");
            System.out.println("  ✓ Cost: Free");
            System.out.println();

            // Train on documents
            System.out.println("  Training on sample corpus...");
            List<String> trainingCorpus = new ArrayList<>(texts);
            // Add more documents for better training
            trainingCorpus.addAll(List.of(
                    "Vector databases store high-dimensional embeddings",
                    "Semantic search uses meaning rather than keywords",
                    "Natural language processing enables text understanding",
                    "Embeddings capture semantic relationships between words",
                    "Machine learning powers modern search systems"
            ));

            long startTime = System.currentTimeMillis();
            embedder.train(trainingCorpus);
            long trainTime = System.currentTimeMillis() - startTime;

            System.out.println("  ✓ Training complete");
            System.out.println("    - Vocabulary size: " + embedder.getDimensions());
            System.out.println("    - Training time: " + trainTime + "ms");
            System.out.println();

            // Generate embeddings
            startTime = System.currentTimeMillis();
            double[][] embeddings = embedder.embedBatch(texts);
            double[] queryEmbedding = embedder.embed(query);
            long elapsed = System.currentTimeMillis() - startTime;

            System.out.println("  Performance:");
            System.out.println("    - Generated " + embeddings.length + " document embeddings");
            System.out.println("    - Generated 1 query embedding");
            System.out.println("    - Time: " + elapsed + "ms");
            System.out.println();

            // Calculate similarities
            System.out.println("  Similarity scores:");
            for (int i = 0; i < texts.size(); i++) {
                double similarity = cosineSimilarity(queryEmbedding, embeddings[i]);
                System.out.printf("    %d. %.3f - \"%s\"%n",
                    i + 1, similarity, truncate(texts.get(i), 50));
            }
            System.out.println();

            System.out.println("  Note: TF-IDF scores may be lower than neural methods");
            System.out.println("        because it uses keyword matching, not semantics.");
            System.out.println();

            embedder.close();

        } catch (Exception e) {
            System.err.println("  ✗ Error: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
        }
    }

    /**
     * Calculates cosine similarity between two vectors.
     */
    private static double cosineSimilarity(double[] v1, double[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);

        if (norm1 < 1e-10 || norm2 < 1e-10) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    /**
     * Truncates a string to maximum length.
     */
    private static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
