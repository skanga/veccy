package com.veccy.integration;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.config.HNSWConfig;
import com.veccy.config.Metric;
import com.veccy.indices.HNSWIndex;
import com.veccy.storage.MemoryStorage;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for type-safe configuration.
 */
class TypeSafeConfigIntegrationTest {

    @Test
    void testHNSWIndex_WithTypeSafeConfig() {
        // Create type-safe configuration
        HNSWConfig config = HNSWConfig.builder()
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .metric(Metric.COSINE)
                .build();

        // Create index with type-safe config
        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        // Create client
        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        // Insert test vectors
        double[][] vectors = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.0, 0.0, 1.0}
        };

        List<Map<String, Object>> metadata = new ArrayList<>();
        for (int i = 0; i < vectors.length; i++) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("id", i);
            metadata.add(meta);
        }

        List<String> ids = client.insert(vectors, metadata);
        assertEquals(3, ids.size());

        // Search
        double[] query = {1.0, 0.0, 0.0};
        List<SearchResult> results = client.search(query, 2);

        assertEquals(2, results.size());
        assertEquals(ids.get(0), results.get(0).id());

        client.close();
    }

    @Test
    void testHNSWIndex_WithBuilder() {
        // Use builder directly from HNSWIndex
        HNSWConfig config = HNSWIndex.builder()
                .m(32)
                .efConstruction(400)
                .efSearch(100)
                .metric(Metric.EUCLIDEAN)
                .randomSeed(42L)
                .build();

        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        // Insert and search
        double[][] vectors = {
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        List<String> ids = client.insert(vectors, null);
        assertEquals(2, ids.size());

        double[] query = {1.0, 2.0, 3.0};
        List<SearchResult> results = client.search(query, 1);
        assertEquals(1, results.size());

        client.close();
    }

    @Test
    void testHNSWIndex_WithDefaults() {
        // Use default configuration
        HNSWConfig config = HNSWConfig.defaults();
        HNSWIndex index = new HNSWIndex(config);
        MemoryStorage storage = new MemoryStorage(new HashMap<>());
        storage.initialize();

        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        // Verify defaults work
        double[][] vectors = {{1.0, 0.0}, {0.0, 1.0}};
        List<String> ids = client.insert(vectors, null);
        assertEquals(2, ids.size());

        client.close();
    }

    @Test
    void testHNSWIndex_WithDifferentMetrics() {
        // HNSWIndex currently supports COSINE and EUCLIDEAN metrics
        for (Metric metric : new Metric[]{Metric.COSINE, Metric.EUCLIDEAN}) {
            HNSWConfig config = HNSWConfig.builder()
                    .metric(metric)
                    .build();

            HNSWIndex index = new HNSWIndex(config);
            MemoryStorage storage = new MemoryStorage(new HashMap<>());
            storage.initialize();

            VectorDBClient client = new VectorDBClient(storage, index);
            client.initialize();

            // Insert test data
            double[][] vectors = {
                    {1.0, 0.0},
                    {0.0, 1.0}
            };
            List<String> ids = client.insert(vectors, null);
            assertEquals(2, ids.size());

            // Search should work with supported metrics
            double[] query = {1.0, 0.0};
            List<SearchResult> results = client.search(query, 1);
            assertEquals(1, results.size());

            client.close();
        }
    }

    @Test
    void testHNSWIndex_WithRandomSeed() {
        // Two indices with same random seed should produce identical results
        HNSWConfig config1 = HNSWConfig.builder()
                .randomSeed(12345L)
                .build();

        HNSWConfig config2 = HNSWConfig.builder()
                .randomSeed(12345L)
                .build();

        HNSWIndex index1 = new HNSWIndex(config1);
        HNSWIndex index2 = new HNSWIndex(config2);

        MemoryStorage storage1 = new MemoryStorage(new HashMap<>());
        storage1.initialize();
        VectorDBClient client1 = new VectorDBClient(storage1, index1);
        client1.initialize();

        MemoryStorage storage2 = new MemoryStorage(new HashMap<>());
        storage2.initialize();
        VectorDBClient client2 = new VectorDBClient(storage2, index2);
        client2.initialize();

        // Insert same vectors
        double[][] vectors = new double[100][10];
        Random random = new Random(999);
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 10; j++) {
                vectors[i][j] = random.nextGaussian();
            }
        }

        client1.insert(vectors, null);
        client2.insert(vectors, null);

        // Search should be deterministic
        double[] query = {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        List<SearchResult> results1 = client1.search(query, 10);
        List<SearchResult> results2 = client2.search(query, 10);

        assertEquals(results1.size(), results2.size());

        client1.close();
        client2.close();
    }
}
