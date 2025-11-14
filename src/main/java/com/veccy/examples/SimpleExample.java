package com.veccy.examples;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.indices.FlatIndex;
import com.veccy.storage.MemoryStorage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple example demonstrating basic usage of Veccy vector database.
 */
public class SimpleExample {

    public static void main(String[] args) {
        // Create a simple vector database with memory storage and flat index
        Map<String, Object> storageConfig = new HashMap<>();
        MemoryStorage storage = new MemoryStorage(storageConfig);

        FlatConfig indexConfig = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        FlatIndex index = new FlatIndex(indexConfig);

        VectorDBClient client = new VectorDBClient(storage, index);

        try (client) {
            client.initialize();
            // Insert some vectors
            double[][] vectors = {
                    {1.0, 0.0, 0.0},
                    {0.0, 1.0, 0.0},
                    {0.0, 0.0, 1.0},
                    {0.5, 0.5, 0.0}
            };

            List<String> ids = client.insert(vectors, null);
            System.out.println("Inserted " + ids.size() + " vectors");
            System.out.println("Vector IDs: " + ids);

            // Search for similar vectors
            double[] query = {0.9, 0.1, 0.0};
            List<SearchResult> results = client.search(query, 3);

            System.out.println("\nSearch results for query [0.9, 0.1, 0.0]:");
            for (int i = 0; i < results.size(); i++) {
                SearchResult result = results.get(i);
                System.out.printf("%d. ID: %s, Distance: %.4f%n",
                        i + 1, result.getId(), result.getDistance());
            }

            // Get statistics
            Map<String, Object> stats = client.getStats();
            System.out.println("\nDatabase statistics:");
            System.out.println(stats);

        }
    }
}
