package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.indices.HNSWIndex;
import com.veccy.storage.MemoryStorage;

import java.util.*;

/**
 * Example demonstrating batch operations for efficient multi-vector processing.
 */
public class BatchOperationsExample {

    public static void main(String[] args) {
        // Initialize Veccy with HNSW index
        HNSWConfig indexConfig = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .metric(Metric.COSINE)
                .build();

        try (VectorDBClient client = new VectorDBClient(
                new MemoryStorage(new HashMap<>()),
                new HNSWIndex(indexConfig))) {

            client.initialize();

            int dimensions = 128;

            // Example 1: Bulk data ingestion
            System.out.println("=== Example 1: Bulk Data Ingestion ===");
            bulkIngestion(client, dimensions);

            // Example 2: Batch update after reprocessing
            System.out.println("\n=== Example 2: Batch Update ===");
            batchUpdateExample(client, dimensions);

            // Example 3: Batch search for recommendations
            System.out.println("\n=== Example 3: Batch Search ===");
            batchSearchExample(client, dimensions);

            // Example 4: Metadata-only updates
            System.out.println("\n=== Example 4: Metadata Updates ===");
            metadataUpdateExample(client);

        } // Automatically closed
    }

    /**
     * Example 1: Bulk data ingestion with batch insert.
     */
    private static void bulkIngestion(VectorDBClient client, int dimensions) {
        // Generate 1000 document embeddings
        int documentCount = 1000;
        double[][] documentVectors = new double[documentCount][dimensions];
        List<Map<String, Object>> documentMetadata = new ArrayList<>();

        for (int i = 0; i < documentCount; i++) {
            // Generate random embedding (in practice, use a real embedding model)
            documentVectors[i] = generateRandomVector(dimensions);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("doc_id", "doc_" + i);
            metadata.put("category", i % 5); // 5 categories
            metadata.put("timestamp", System.currentTimeMillis());
            documentMetadata.add(metadata);
        }

        // Insert all documents at once
        long startTime = System.nanoTime();
        List<String> ids = client.insert(documentVectors, documentMetadata);
        long duration = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("Inserted " + ids.size() + " documents in " + duration + "ms");
        System.out.println("Throughput: " + (documentCount * 1000 / duration) + " docs/sec");
    }

    /**
     * Example 2: Batch update vectors after reprocessing.
     */
    private static void batchUpdateExample(VectorDBClient client, int dimensions) {
        // Get all vector IDs (in practice, query from database)
        Map<String, Object> stats = client.getStats();
        Map<?, ?> indexStats = (Map<?, ?>) stats.get("index");
        int vectorCount = (int) indexStats.get("vector_count");

        // Simulate updating 100 vectors with new embeddings
        int updateCount = Math.min(100, vectorCount);
        List<String> idsToUpdate = new ArrayList<>();
        List<double[]> newVectors = new ArrayList<>();

        // In practice, these would be specific document IDs
        for (int i = 0; i < updateCount; i++) {
            idsToUpdate.add("doc_" + i); // Use actual IDs
            newVectors.add(generateRandomVector(dimensions));
        }

        // Batch update
        long startTime = System.nanoTime();
        List<Boolean> results = client.batchUpdate(idsToUpdate, newVectors, null);
        long duration = (System.nanoTime() - startTime) / 1_000_000;

        // Check results
        long successCount = results.stream().filter(r -> r).count();
        long failureCount = results.stream().filter(r -> !r).count();

        System.out.println("Updated " + successCount + " vectors successfully in " + duration + "ms");
        if (failureCount > 0) {
            System.out.println("Failed to update " + failureCount + " vectors");
        }
        System.out.println("Throughput: " + (updateCount * 1000 / duration) + " updates/sec");
    }

    /**
     * Example 3: Batch search for personalized recommendations.
     */
    private static void batchSearchExample(VectorDBClient client, int dimensions) {
        // Simulate 50 user queries
        int userCount = 50;
        double[][] userQueries = new double[userCount][dimensions];

        for (int i = 0; i < userCount; i++) {
            userQueries[i] = generateRandomVector(dimensions);
        }

        // Batch search
        long startTime = System.nanoTime();
        List<List<SearchResult>> allResults = client.batchSearch(userQueries, 10);
        long duration = (System.nanoTime() - startTime) / 1_000_000;

        System.out.println("Searched " + userCount + " queries in " + duration + "ms");
        System.out.println("Throughput: " + (userCount * 1000 / duration) + " queries/sec");

        // Process results
        for (int i = 0; i < Math.min(3, allResults.size()); i++) {
            List<SearchResult> userResults = allResults.get(i);
            System.out.println("\nUser " + i + " recommendations:");
            for (int j = 0; j < Math.min(3, userResults.size()); j++) {
                SearchResult result = userResults.get(j);
                System.out.println("  " + (j + 1) + ". " + result.id() +
                        " (distance: " + String.format("%.4f", result.distance()) + ")");
            }
        }
    }

    /**
     * Example 4: Update metadata without changing vectors.
     */
    private static void metadataUpdateExample(VectorDBClient client) {
        // Update metadata for 10 documents (e.g., add tags, update timestamps)
        List<String> idsToUpdate = new ArrayList<>();
        List<Map<String, Object>> newMetadata = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            idsToUpdate.add("doc_" + i);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("doc_id", "doc_" + i);
            metadata.put("category", i % 5);
            metadata.put("timestamp", System.currentTimeMillis());
            metadata.put("updated", true);
            metadata.put("tags", Arrays.asList("tag1", "tag2"));
            newMetadata.add(metadata);
        }

        // Batch update (pass null for vectors to only update metadata)
        List<Boolean> results = client.batchUpdate(idsToUpdate, null, newMetadata);

        long successCount = results.stream().filter(r -> r).count();
        System.out.println("Updated metadata for " + successCount + " documents");
    }

    /**
     * Generate a random vector for testing purposes.
     */
    private static double[] generateRandomVector(int dimensions) {
        Random random = new Random();
        double[] vector = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextGaussian();
        }
        // Normalize
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        for (int i = 0; i < dimensions; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
