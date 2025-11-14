package com.veccy.integration;

import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.config.AnnoyConfig;
import com.veccy.config.IVFConfig;
import com.veccy.config.LSHConfig;
import com.veccy.config.Metric;
import com.veccy.factory.VectorDBFactory;
import com.veccy.indices.AnnoyIndex;
import com.veccy.indices.IVFIndex;
import com.veccy.indices.LSHIndex;
import com.veccy.storage.DiskStorage;
import com.veccy.storage.HybridStorage;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for advanced indices (IVF, LSH, and Annoy) with various components.
 */
class AdvancedIndicesIntegrationTest {

    private Path tempDir;
    private List<AutoCloseable> resourcesToClose;
    private Random testRandom;  // Shared random generator for consistent test vectors

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.resourcesToClose = new ArrayList<>();
        this.testRandom = new Random(42);  // Initialize once per test
    }

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable resource : resourcesToClose) {
            if (resource != null) {
                resource.close();
            }
        }
        resourcesToClose.clear();
    }

    @Test
    void testIVFIndexWithMemoryStorage() {
        IVFConfig config = IVFConfig.builder()
                .numClusters(10)
                .numProbes(3)
                .build();

        IVFIndex index = new IVFIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testIVFIndexWithDiskStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());

        IVFConfig indexConfig = IVFConfig.builder()
                .numClusters(10)
                .numProbes(3)
                .build();

        IVFIndex index = new IVFIndex(indexConfig);
        StorageBackend storage = new DiskStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testIVFIndexWithHybridStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());
        storageConfig.put("cache_size", 20);

        IVFConfig indexConfig = IVFConfig.builder()
                .numClusters(10)
                .numProbes(3)
                .build();

        IVFIndex index = new IVFIndex(indexConfig);
        StorageBackend storage = new HybridStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search multiple times to test caching
        double[][] vectors = generateRandomVectors(40, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(40, ids.size());

        double[] query = vectors[0];

        // First search
        List<SearchResult> results1 = index.search(query, 5);
        assertFalse(results1.isEmpty());

        // Second search (should benefit from cache)
        List<SearchResult> results2 = index.search(query, 5);
        assertEquals(results1.size(), results2.size());
    }

    @Test
    void testLSHIndexWithMemoryStorage() {
        LSHConfig config = LSHConfig.builder()
                .numTables(5)
                .numHashBits(8)
                .build();

        LSHIndex index = new LSHIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testLSHIndexWithDiskStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());

        LSHConfig indexConfig = LSHConfig.builder()
                .numTables(5)
                .numHashBits(8)
                .build();

        LSHIndex index = new LSHIndex(indexConfig);
        StorageBackend storage = new DiskStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testLSHIndexWithHybridStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());
        storageConfig.put("cache_size", 20);

        LSHConfig indexConfig = LSHConfig.builder()
                .numTables(5)
                .numHashBits(8)
                .build();

        LSHIndex index = new LSHIndex(indexConfig);
        StorageBackend storage = new HybridStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(40, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(40, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testIVFIndexViaFactory() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "ivf");
        indexConfig.put("num_clusters", 10);
        indexConfig.put("num_probes", 3);
        indexConfig.put("metric", Metric.COSINE);
        indexConfig.put("random_seed", 42L);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Test basic operations
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testLSHIndexViaFactory() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "lsh");
        indexConfig.put("num_tables", 5);
        indexConfig.put("num_hash_bits", 8);
        indexConfig.put("metric", Metric.COSINE);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Test basic operations
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testIVFIndexEndToEndWorkflow() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "ivf");
        indexConfig.put("num_clusters", 10);
        indexConfig.put("random_seed", 42L);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // 1. Insert initial batch
        double[][] vectors1 = generateRandomVectors(30, 64);
        List<Map<String, Object>> metadata1 = new ArrayList<>();
        for (int i = 0; i < vectors1.length; i++) {
            metadata1.add(Map.of("batch", 1, "index", i));
        }
        List<String> ids1 = client.insert(vectors1, metadata1);

        // 2. Insert second batch
        double[][] vectors2 = generateRandomVectors(20, 64);
        List<Map<String, Object>> metadata2 = new ArrayList<>();
        for (int i = 0; i < vectors2.length; i++) {
            metadata2.add(Map.of("batch", 2, "index", i));
        }
        List<String> ids2 = client.insert(vectors2, metadata2);

        // 3. Search
        double[] query = vectors1[0];
        List<SearchResult> results = client.search(query, 10);
        assertFalse(results.isEmpty());
        assertEquals(1, results.get(0).getMetadata().get("batch"));

        // 4. Update a vector
        double[] newVector = generateRandomVector(64);
        boolean updated = client.update(ids1.get(0), newVector, Map.of("batch", 1, "updated", true));
        assertTrue(updated);

        // 5. Delete some vectors
        boolean deleted = client.delete(ids2.subList(0, 5));
        assertTrue(deleted);

        // 6. Verify final state
        Map<String, Object> stats = client.getStats();
        assertEquals(45, ((Map<?, ?>) stats.get("index")).get("vector_count"));
    }

    @Test
    void testLSHIndexEndToEndWorkflow() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "lsh");
        indexConfig.put("num_tables", 5);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // 1. Insert initial batch
        double[][] vectors1 = generateRandomVectors(30, 64);
        List<Map<String, Object>> metadata1 = new ArrayList<>();
        for (int i = 0; i < vectors1.length; i++) {
            metadata1.add(Map.of("batch", 1, "index", i));
        }
        List<String> ids1 = client.insert(vectors1, metadata1);

        // 2. Search
        double[] query = vectors1[0];
        List<SearchResult> results = client.search(query, 10);
        assertFalse(results.isEmpty());

        // 3. Update and delete
        double[] newVector = generateRandomVector(64);
        client.update(ids1.get(0), newVector, Map.of("updated", true));
        client.delete(ids1.subList(1, 6));

        // 4. Verify final state
        Map<String, Object> stats = client.getStats();
        assertEquals(25, ((Map<?, ?>) stats.get("index")).get("vector_count"));
    }

    @Test
    void testIVFIndexWithDifferentMetrics() {
        String[] metricStrings = {"cosine", "euclidean", "dot_product"};

        for (String metricStr : metricStrings) {
            Map<String, Object> storageConfig = new HashMap<>();
            storageConfig.put("type", "memory");

            Map<String, Object> indexConfig = new HashMap<>();
            indexConfig.put("type", "ivf");
            indexConfig.put("metric", Metric.fromString(metricStr));
            indexConfig.put("num_clusters", 10);
            indexConfig.put("random_seed", 42L);

            VectorDBClient client = VectorDBFactory.createCustom(
                    storageConfig, indexConfig, null, null);

            resourcesToClose.add(client);

            double[][] vectors = generateRandomVectors(30, 64);
            client.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = client.search(query, 5);

            assertFalse(results.isEmpty(), "Should find results with metric: " + metricStr);
        }
    }

    @Test
    void testLSHIndexWithDifferentMetrics() {
        String[] metricStrings = {"cosine", "euclidean"};

        for (String metricStr : metricStrings) {
            Map<String, Object> storageConfig = new HashMap<>();
            storageConfig.put("type", "memory");

            Map<String, Object> indexConfig = new HashMap<>();
            indexConfig.put("type", "lsh");
            indexConfig.put("metric", Metric.fromString(metricStr));
            indexConfig.put("num_tables", 5);

            VectorDBClient client = VectorDBFactory.createCustom(
                    storageConfig, indexConfig, null, null);

            resourcesToClose.add(client);

            double[][] vectors = generateRandomVectors(30, 64);
            client.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = client.search(query, 5);

            assertFalse(results.isEmpty(), "Should find results with metric: " + metricStr);
        }
    }

    @Test
    void testCompareIVFAndLSHSearchResults() {
        double[][] vectors = generateRandomVectors(100, 64);
        double[] query = vectors[0];

        // IVF Index
        Map<String, Object> ivfStorageConfig = new HashMap<>();
        ivfStorageConfig.put("type", "memory");

        Map<String, Object> ivfIndexConfig = new HashMap<>();
        ivfIndexConfig.put("type", "ivf");
        ivfIndexConfig.put("num_clusters", 10);
        ivfIndexConfig.put("num_probes", 3);

        VectorDBClient ivfClient = VectorDBFactory.createCustom(
                ivfStorageConfig, ivfIndexConfig, null, null);
        resourcesToClose.add(ivfClient);

        ivfClient.insert(vectors, null);
        List<SearchResult> ivfResults = ivfClient.search(query, 10);

        // LSH Index
        Map<String, Object> lshStorageConfig = new HashMap<>();
        lshStorageConfig.put("type", "memory");

        Map<String, Object> lshIndexConfig = new HashMap<>();
        lshIndexConfig.put("type", "lsh");
        lshIndexConfig.put("num_tables", 5);
        lshIndexConfig.put("num_hash_bits", 8);

        VectorDBClient lshClient = VectorDBFactory.createCustom(
                lshStorageConfig, lshIndexConfig, null, null);
        resourcesToClose.add(lshClient);

        lshClient.insert(vectors, null);
        List<SearchResult> lshResults = lshClient.search(query, 10);

        // Both should find the query vector with high accuracy
        assertTrue(ivfResults.get(0).getDistance() < 0.01);
        assertTrue(lshResults.get(0).getDistance() < 0.01);
    }

    @Test
    void testIVFIndexScalability() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "ivf");
        indexConfig.put("num_clusters", 20);
        indexConfig.put("num_probes", 5);
        indexConfig.put("random_seed", 42L);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Insert large dataset in batches
        int totalVectors = 500;
        int batchSize = 100;

        for (int batch = 0; batch < totalVectors / batchSize; batch++) {
            double[][] vectors = generateRandomVectors(batchSize, 64);
            client.insert(vectors, null);
        }

        Map<String, Object> stats = client.getStats();
        assertEquals(totalVectors, ((Map<?, ?>) stats.get("index")).get("vector_count"));

        // Search should still be efficient
        double[] query = generateRandomVector(64);
        List<SearchResult> results = client.search(query, 20);

        assertTrue(results.size() <= 20);
    }

    @Test
    void testLSHIndexScalability() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "lsh");
        indexConfig.put("num_tables", 7);
        indexConfig.put("num_hash_bits", 10);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Insert large dataset
        int totalVectors = 500;
        int batchSize = 100;

        for (int batch = 0; batch < totalVectors / batchSize; batch++) {
            double[][] vectors = generateRandomVectors(batchSize, 64);
            client.insert(vectors, null);
        }

        Map<String, Object> stats = client.getStats();
        assertEquals(totalVectors, ((Map<?, ?>) stats.get("index")).get("vector_count"));

        // Search
        double[] query = generateRandomVector(64);
        List<SearchResult> results = client.search(query, 20);

        assertTrue(results.size() <= 20);
    }

    @Test
    void testIVFIndexPersistenceCompatibility() {
        // IVF index should work with persistence-enabled client
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "disk");
        storageConfig.put("data_dir", tempDir.toString());

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "ivf");
        indexConfig.put("num_clusters", 10);
        indexConfig.put("random_seed", 42L);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testLSHIndexPersistenceCompatibility() {
        // LSH index should work with persistence-enabled client
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "disk");
        storageConfig.put("data_dir", tempDir.toString());

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "lsh");
        indexConfig.put("num_tables", 5);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testAnnoyIndexWithMemoryStorage() {
        AnnoyConfig config = AnnoyConfig.builder()
                .numTrees(5)
                .maxLeafSize(10)
                .build();

        AnnoyIndex index = new AnnoyIndex(config);
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(50, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(50, ids.size());

        index.build();

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 10);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testAnnoyIndexWithDiskStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());

        AnnoyConfig indexConfig = AnnoyConfig.builder()
                .numTrees(5)
                .maxLeafSize(10)
                .build();

        AnnoyIndex index = new AnnoyIndex(indexConfig);
        StorageBackend storage = new DiskStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(30, ids.size());

        index.build();

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getDistance() < 0.1);
    }

    @Test
    void testAnnoyIndexWithHybridStorage() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", tempDir.toString());
        storageConfig.put("cache_size", 20);

        AnnoyConfig indexConfig = AnnoyConfig.builder()
                .numTrees(5)
                .maxLeafSize(10)
                .build();

        AnnoyIndex index = new AnnoyIndex(indexConfig);
        StorageBackend storage = new HybridStorage(storageConfig);
        storage.initialize();
        index.initialize(storage);

        resourcesToClose.add(index);
        resourcesToClose.add(storage);

        // Insert and search
        double[][] vectors = generateRandomVectors(40, 64);
        List<String> ids = index.insert(vectors, null);

        assertEquals(40, ids.size());

        index.build();

        double[] query = vectors[0];
        List<SearchResult> results = index.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testAnnoyIndexViaFactory() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "annoy");
        indexConfig.put("num_trees", 5);
        indexConfig.put("max_leaf_size", 10);
        indexConfig.put("metric", Metric.COSINE);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Test basic operations
        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    @Test
    void testAnnoyIndexEndToEndWorkflow() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "annoy");
        indexConfig.put("num_trees", 5);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // 1. Insert initial batch
        double[][] vectors1 = generateRandomVectors(30, 64);
        List<Map<String, Object>> metadata1 = new ArrayList<>();
        for (int i = 0; i < vectors1.length; i++) {
            metadata1.add(Map.of("batch", 1, "index", i));
        }
        List<String> ids1 = client.insert(vectors1, metadata1);

        // 2. Insert second batch
        double[][] vectors2 = generateRandomVectors(20, 64);
        List<Map<String, Object>> metadata2 = new ArrayList<>();
        for (int i = 0; i < vectors2.length; i++) {
            metadata2.add(Map.of("batch", 2, "index", i));
        }
        List<String> ids2 = client.insert(vectors2, metadata2);

        // 3. Search
        double[] query = vectors1[0];
        List<SearchResult> results = client.search(query, 10);
        assertFalse(results.isEmpty());

        // 4. Update a vector
        double[] newVector = generateRandomVector(64);
        boolean updated = client.update(ids1.get(0), newVector, Map.of("batch", 1, "updated", true));
        assertTrue(updated);

        // 5. Delete some vectors
        boolean deleted = client.delete(ids2.subList(0, 5));
        assertTrue(deleted);

        // 6. Verify final state
        Map<String, Object> stats = client.getStats();
        assertEquals(45, ((Map<?, ?>) stats.get("index")).get("vector_count"));
    }

    @Test
    void testAnnoyIndexWithDifferentMetrics() {
        String[] metricStrings = {"cosine", "euclidean", "dot_product"};

        for (String metricStr : metricStrings) {
            Map<String, Object> storageConfig = new HashMap<>();
            storageConfig.put("type", "memory");

            Map<String, Object> indexConfig = new HashMap<>();
            indexConfig.put("type", "annoy");
            indexConfig.put("metric", Metric.fromString(metricStr));
            indexConfig.put("num_trees", 5);

            VectorDBClient client = VectorDBFactory.createCustom(
                    storageConfig, indexConfig, null, null);

            resourcesToClose.add(client);

            double[][] vectors = generateRandomVectors(30, 64);
            client.insert(vectors, null);

            double[] query = vectors[0];
            List<SearchResult> results = client.search(query, 5);

            assertFalse(results.isEmpty(), "Should find results with metric: " + metricStr);
        }
    }

    @Test
    void testCompareAllIndices() {
        double[][] vectors = generateRandomVectors(100, 64);
        double[] query = vectors[0];

        // IVF Index
        Map<String, Object> ivfStorageConfig = new HashMap<>();
        ivfStorageConfig.put("type", "memory");

        Map<String, Object> ivfIndexConfig = new HashMap<>();
        ivfIndexConfig.put("type", "ivf");
        ivfIndexConfig.put("num_clusters", 10);

        VectorDBClient ivfClient = VectorDBFactory.createCustom(
                ivfStorageConfig, ivfIndexConfig, null, null);
        resourcesToClose.add(ivfClient);

        ivfClient.insert(vectors, null);
        List<SearchResult> ivfResults = ivfClient.search(query, 10);

        // LSH Index
        Map<String, Object> lshStorageConfig = new HashMap<>();
        lshStorageConfig.put("type", "memory");

        Map<String, Object> lshIndexConfig = new HashMap<>();
        lshIndexConfig.put("type", "lsh");
        lshIndexConfig.put("num_tables", 5);

        VectorDBClient lshClient = VectorDBFactory.createCustom(
                lshStorageConfig, lshIndexConfig, null, null);
        resourcesToClose.add(lshClient);

        lshClient.insert(vectors, null);
        List<SearchResult> lshResults = lshClient.search(query, 10);

        // Annoy Index
        Map<String, Object> annoyStorageConfig = new HashMap<>();
        annoyStorageConfig.put("type", "memory");

        Map<String, Object> annoyIndexConfig = new HashMap<>();
        annoyIndexConfig.put("type", "annoy");
        annoyIndexConfig.put("num_trees", 5);

        VectorDBClient annoyClient = VectorDBFactory.createCustom(
                annoyStorageConfig, annoyIndexConfig, null, null);
        resourcesToClose.add(annoyClient);

        annoyClient.insert(vectors, null);
        List<SearchResult> annoyResults = annoyClient.search(query, 10);

        // All should find the query vector with high accuracy
        assertTrue(ivfResults.get(0).getDistance() < 0.01);
        assertTrue(lshResults.get(0).getDistance() < 0.01);
        assertTrue(annoyResults.get(0).getDistance() < 0.01);
    }

    @Test
    void testAnnoyIndexScalability() {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "memory");

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "annoy");
        indexConfig.put("num_trees", 10);
        indexConfig.put("max_leaf_size", 10);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        // Insert large dataset in batches
        int totalVectors = 500;
        int batchSize = 100;

        for (int batch = 0; batch < totalVectors / batchSize; batch++) {
            double[][] vectors = generateRandomVectors(batchSize, 64);
            client.insert(vectors, null);
        }

        Map<String, Object> stats = client.getStats();
        assertEquals(totalVectors, ((Map<?, ?>) stats.get("index")).get("vector_count"));

        // Search should still be efficient
        double[] query = generateRandomVector(64);
        List<SearchResult> results = client.search(query, 20);

        assertTrue(results.size() <= 20);
    }

    @Test
    void testAnnoyIndexPersistenceCompatibility() {
        // Annoy index should work with persistence-enabled client
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("type", "disk");
        storageConfig.put("data_dir", tempDir.toString());

        Map<String, Object> indexConfig = new HashMap<>();
        indexConfig.put("type", "annoy");
        indexConfig.put("num_trees", 5);

        VectorDBClient client = VectorDBFactory.createCustom(
                storageConfig, indexConfig, null, null);

        resourcesToClose.add(client);

        double[][] vectors = generateRandomVectors(30, 64);
        List<String> ids = client.insert(vectors, null);

        assertEquals(30, ids.size());

        double[] query = vectors[0];
        List<SearchResult> results = client.search(query, 5);

        assertFalse(results.isEmpty());
    }

    // Helper methods

    private double[][] generateRandomVectors(int count, int dimensions) {
        double[][] vectors = new double[count][dimensions];

        for (int i = 0; i < count; i++) {
            for (int d = 0; d < dimensions; d++) {
                vectors[i][d] = testRandom.nextDouble() * 10.0 - 5.0;  // Range [-5, 5]
            }
        }

        return vectors;
    }

    private double[] generateRandomVector(int dimensions) {
        return generateRandomVectors(1, dimensions)[0];
    }
}
