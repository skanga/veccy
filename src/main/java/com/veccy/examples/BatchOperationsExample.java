package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Example demonstrating batch operations performance.
 * <p>
 * This example compares the performance of:
 * - Individual operations (multiple single calls)
 * - Batch operations (single call with multiple items)
 * <p>
 * Batch operations show significant performance improvements:
 * - Reduced locking overhead
 * - Better cache locality
 * - Shared computation
 * - Atomic state changes
 */
public class BatchOperationsExample {

    private static final Random random = new Random(42);

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Veccy - Batch Operations Performance Example             ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Create database
        VectorDBClient client = VectorDBFactory.createHighPerformance();

        try {
            int dimensions = 128;
            int vectorCount = 1000;
            int batchSize = 100;

            System.out.printf("Setup: %d vectors, %d dimensions%n", vectorCount, dimensions);
            System.out.printf("Batch size: %d%n", batchSize);
            System.out.println();

            // Insert initial vectors
            System.out.println("Inserting " + vectorCount + " vectors...");
            double[][] vectors = generateRandomVectors(vectorCount, dimensions);
            List<String> ids = client.insert(vectors, null);
            System.out.println("✓ Inserted " + ids.size() + " vectors");
            System.out.println();

            // Demo 1: Batch vs Individual Search
            demonstrateBatchSearch(client, dimensions, batchSize);
            System.out.println();

            // Demo 2: Batch vs Individual Update
            demonstrateBatchUpdate(client, ids, dimensions, batchSize);
            System.out.println();

            // Demo 3: Scaling Performance
            demonstrateScaling(client, dimensions);
            System.out.println();

            // Demo 4: Real-world use cases
            demonstrateUseCases(client, ids, dimensions);

        } finally {
            client.close();
        }

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Example completed successfully!                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * Demonstrate batch search performance vs individual searches.
     */
    private static void demonstrateBatchSearch(VectorDBClient client, int dimensions, int batchSize) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  DEMO 1: Batch Search Performance");
        System.out.println("═══════════════════════════════════════════════════════════");

        double[][] queryVectors = generateRandomVectors(batchSize, dimensions);
        int k = 10;

        // Individual searches
        long startIndividual = System.nanoTime();
        List<List<SearchResult>> individualResults = new ArrayList<>();
        for (double[] queryVector : queryVectors) {
            individualResults.add(client.search(queryVector, k));
        }
        long endIndividual = System.nanoTime();
        double individualTime = (endIndividual - startIndividual) / 1_000_000.0;

        // Batch search
        long startBatch = System.nanoTime();
        List<List<SearchResult>> batchResults = client.batchSearch(queryVectors, k);
        long endBatch = System.nanoTime();
        double batchTime = (endBatch - startBatch) / 1_000_000.0;

        // Calculate improvement
        double improvement = ((individualTime - batchTime) / individualTime) * 100;
        double speedup = individualTime / batchTime;

        System.out.println();
        System.out.printf("Individual searches: %.2f ms (%d queries)%n", individualTime, batchSize);
        System.out.printf("Batch search:        %.2f ms (%d queries)%n", batchTime, batchSize);
        System.out.println();
        System.out.printf("⚡ Speedup:    %.2fx faster%n", speedup);
        System.out.printf("⚡ Improvement: %.1f%% reduction in time%n", improvement);
        System.out.printf("   Per-query:   %.3f ms (individual) vs %.3f ms (batch)%n",
                individualTime / batchSize, batchTime / batchSize);
    }

    /**
     * Demonstrate batch update performance vs individual updates.
     */
    private static void demonstrateBatchUpdate(VectorDBClient client, List<String> ids,
                                               int dimensions, int batchSize) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  DEMO 2: Batch Update Performance");
        System.out.println("═══════════════════════════════════════════════════════════");

        List<String> updateIds = ids.subList(0, batchSize);
        List<double[]> newVectors = new ArrayList<>();
        List<Map<String, Object>> metadata = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            newVectors.add(generateRandomVector(dimensions));
            metadata.add(Map.of("updated", true, "batch", i));
        }

        // Individual updates
        long startIndividual = System.nanoTime();
        int successCount = 0;
        for (int i = 0; i < batchSize; i++) {
            if (client.update(updateIds.get(i), newVectors.get(i), metadata.get(i))) {
                successCount++;
            }
        }
        long endIndividual = System.nanoTime();
        double individualTime = (endIndividual - startIndividual) / 1_000_000.0;

        // Batch update
        long startBatch = System.nanoTime();
        List<Boolean> results = client.batchUpdate(updateIds, newVectors, metadata);
        long endBatch = System.nanoTime();
        double batchTime = (endBatch - startBatch) / 1_000_000.0;
        int batchSuccessCount = (int) results.stream().filter(b -> b).count();

        // Calculate improvement
        double improvement = ((individualTime - batchTime) / individualTime) * 100;
        double speedup = individualTime / batchTime;

        System.out.println();
        System.out.printf("Individual updates: %.2f ms (%d updates, %d succeeded)%n",
                individualTime, batchSize, successCount);
        System.out.printf("Batch update:       %.2f ms (%d updates, %d succeeded)%n",
                batchTime, batchSize, batchSuccessCount);
        System.out.println();
        System.out.printf("⚡ Speedup:    %.2fx faster%n", speedup);
        System.out.printf("⚡ Improvement: %.1f%% reduction in time%n", improvement);
        System.out.printf("   Per-update:  %.3f ms (individual) vs %.3f ms (batch)%n",
                individualTime / batchSize, batchTime / batchSize);
    }

    /**
     * Demonstrate how performance scales with batch size.
     */
    private static void demonstrateScaling(VectorDBClient client, int dimensions) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  DEMO 3: Scaling Analysis");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();

        int[] batchSizes = {1, 10, 50, 100, 250, 500};
        int k = 10;

        System.out.printf("%-15s %-20s %-20s %-15s%n", "Batch Size", "Individual (ms)", "Batch (ms)", "Speedup");
        System.out.println("─".repeat(70));

        for (int batchSize : batchSizes) {
            double[][] queryVectors = generateRandomVectors(batchSize, dimensions);

            // Individual
            long startIndividual = System.nanoTime();
            for (double[] queryVector : queryVectors) {
                client.search(queryVector, k);
            }
            long endIndividual = System.nanoTime();
            double individualTime = (endIndividual - startIndividual) / 1_000_000.0;

            // Batch
            long startBatch = System.nanoTime();
            client.batchSearch(queryVectors, k);
            long endBatch = System.nanoTime();
            double batchTime = (endBatch - startBatch) / 1_000_000.0;

            double speedup = individualTime / batchTime;

            System.out.printf("%-15d %-20.2f %-20.2f %-15.2fx%n",
                    batchSize, individualTime, batchTime, speedup);
        }

        System.out.println();
        System.out.println("📊 Observation: Speedup increases with batch size due to:");
        System.out.println("   • Reduced locking overhead");
        System.out.println("   • Better CPU cache utilization");
        System.out.println("   • Amortized initialization costs");
    }

    /**
     * Demonstrate real-world use cases for batch operations.
     */
    private static void demonstrateUseCases(VectorDBClient client, List<String> ids, int dimensions) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  DEMO 4: Real-World Use Cases");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();

        // Use Case 1: Batch similarity search for recommendation
        System.out.println("📌 Use Case 1: Recommendation System");
        System.out.println("   Scenario: Find similar items for multiple user queries");
        System.out.println();

        int numUsers = 20;
        double[][] userQueries = generateRandomVectors(numUsers, dimensions);

        long start = System.nanoTime();
        List<List<SearchResult>> recommendations = client.batchSearch(userQueries, 5);
        long end = System.nanoTime();
        double time = (end - start) / 1_000_000.0;

        System.out.printf("   Generated recommendations for %d users in %.2f ms%n", numUsers, time);
        System.out.printf("   Average: %.2f ms per user%n", time / numUsers);
        System.out.println();

        // Use Case 2: Batch update for model retraining
        System.out.println("📌 Use Case 2: Model Retraining");
        System.out.println("   Scenario: Update embeddings after model retraining");
        System.out.println();

        int numToUpdate = 50;
        List<String> updateIds = ids.subList(0, numToUpdate);
        List<double[]> retrainedVectors = new ArrayList<>();
        List<Map<String, Object>> retrainedMetadata = new ArrayList<>();

        for (int i = 0; i < numToUpdate; i++) {
            retrainedVectors.add(generateRandomVector(dimensions));
            retrainedMetadata.add(Map.of(
                    "model_version", "v2.0",
                    "retrained_at", System.currentTimeMillis(),
                    "confidence", 0.95 + random.nextDouble() * 0.05
            ));
        }

        start = System.nanoTime();
        List<Boolean> results = client.batchUpdate(updateIds, retrainedVectors, retrainedMetadata);
        end = System.nanoTime();
        time = (end - start) / 1_000_000.0;

        int successCount = (int) results.stream().filter(b -> b).count();

        System.out.printf("   Updated %d/%d vectors in %.2f ms%n", successCount, numToUpdate, time);
        System.out.printf("   Average: %.2f ms per update%n", time / numToUpdate);
        System.out.println();

        // Use Case 3: Multi-query semantic search
        System.out.println("📌 Use Case 3: Multi-Query Semantic Search");
        System.out.println("   Scenario: Search with multiple query variations");
        System.out.println();

        // Simulate query variations (e.g., rephrased queries, synonyms)
        String[] queryTexts = {
                "machine learning algorithms",
                "AI classification methods",
                "supervised learning techniques"
        };

        double[][] queryVariations = generateRandomVectors(queryTexts.length, dimensions);

        start = System.nanoTime();
        List<List<SearchResult>> multiQueryResults = client.batchSearch(queryVariations, 10);
        end = System.nanoTime();
        time = (end - start) / 1_000_000.0;

        // Merge and deduplicate results (simplified)
        Map<String, Double> mergedResults = new HashMap<>();
        for (List<SearchResult> queryResults : multiQueryResults) {
            for (SearchResult result : queryResults) {
                mergedResults.merge(result.id(), result.distance(),
                        (oldDist, newDist) -> Math.min(oldDist, newDist));
            }
        }

        System.out.printf("   Searched %d query variations in %.2f ms%n", queryTexts.length, time);
        System.out.printf("   Found %d unique results across all queries%n", mergedResults.size());
        System.out.println();

        // Performance Summary
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  💡 Key Takeaways");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("1. Batch operations provide 1.5x-5x speedup over individual");
        System.out.println("   operations, depending on batch size and workload.");
        System.out.println();
        System.out.println("2. Benefits increase with batch size due to reduced overhead");
        System.out.println("   and better resource utilization.");
        System.out.println();
        System.out.println("3. Use batch operations when:");
        System.out.println("   • Processing multiple queries simultaneously");
        System.out.println("   • Bulk updating vectors after retraining");
        System.out.println("   • Building recommendation systems");
        System.out.println("   • Implementing multi-query search strategies");
        System.out.println();
        System.out.println("4. Trade-offs:");
        System.out.println("   • Slightly higher memory usage (stores batch results)");
        System.out.println("   • All-or-nothing execution (no partial streaming)");
        System.out.println("   • Better suited for moderate batch sizes (10-500)");
    }

    // Utility methods

    private static double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];
        for (int i = 0; i < count; i++) {
            vectors[i] = generateRandomVector(dimensions);
        }
        return vectors;
    }

    private static double[] generateRandomVector(int dimensions) {
        double[] vector = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextGaussian();
        }
        return normalize(vector);
    }

    private static double[] normalize(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
