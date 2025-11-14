package com.veccy.factory;

import com.veccy.base.Index;
import com.veccy.client.VectorDBClient;
import com.veccy.config.*;
import com.veccy.exceptions.ConfigurationException;
import com.veccy.indices.AnnoyIndex;
import com.veccy.indices.FlatIndex;
import com.veccy.indices.HNSWIndex;
import com.veccy.indices.IVFIndex;
import com.veccy.indices.LSHIndex;
import com.veccy.persistence.PersistenceManager;
import com.veccy.persistence.TensorPersistence;
import com.veccy.quantization.ProductQuantizer;
import com.veccy.quantization.Quantizer;
import com.veccy.quantization.ScalarQuantizer;
import com.veccy.storage.DiskStorage;
import com.veccy.storage.HybridStorage;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory class for creating vector database instances with various configurations.
 * Provides convenient methods to create vector databases for common use cases.
 */
public class VectorDBFactory {

    /**
     * Create a simple in-memory vector database for small datasets.
     * Uses exact search with cosine similarity.
     *
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createSimple() {
        Map<String, Object> storageConfig = new HashMap<>();
        StorageBackend storage = new MemoryStorage(storageConfig);

        FlatConfig config = FlatConfig.builder()
                .metric(Metric.COSINE)
                .build();
        Index index = new FlatIndex(config);

        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        return client;
    }

    /**
     * Create a persistent vector database with disk storage.
     *
     * @param dataDir directory to store vector data
     * @param useHNSW true to use HNSW index, false for flat index
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createPersistent(String dataDir, boolean useHNSW) {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", dataDir);
        StorageBackend storage = new DiskStorage(storageConfig);

        Index index;
        if (useHNSW) {
            HNSWConfig config = HNSWConfig.builder()
                    .metric(Metric.COSINE)
                    .m(16)
                    .efConstruction(200)
                    .efSearch(50)
                    .build();
            index = new HNSWIndex(config);
        } else {
            FlatConfig config = FlatConfig.builder()
                    .metric(Metric.COSINE)
                    .build();
            index = new FlatIndex(config);
        }

        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        return client;
    }

    /**
     * Create a high-performance vector database with HNSW index.
     *
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createHighPerformance() {
        Map<String, Object> storageConfig = new HashMap<>();
        StorageBackend storage = new MemoryStorage(storageConfig);

        HNSWConfig config = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(32)
                .efConstruction(400)
                .efSearch(100)
                .build();
        Index index = new HNSWIndex(config);

        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        return client;
    }

    /**
     * Create a memory-optimized vector database with quantization.
     *
     * @param bits number of bits for scalar quantization (default: 8)
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createMemoryOptimized(int bits) {
        Map<String, Object> storageConfig = new HashMap<>();
        StorageBackend storage = new MemoryStorage(storageConfig);

        HNSWConfig config = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .build();
        Index index = new HNSWIndex(config);

        Map<String, Object> quantConfig = new HashMap<>();
        quantConfig.put("bits", bits);
        Quantizer quantizer = new ScalarQuantizer(quantConfig);

        VectorDBClient client = new VectorDBClient(storage, index, quantizer, null, new HashMap<>());
        client.initialize();

        return client;
    }

    /**
     * Create a vector database with persistence support.
     *
     * @param dataDir directory for vector data
     * @param persistenceDir directory for persistence files
     * @param compression enable compression for persistence
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createWithPersistence(String dataDir, String persistenceDir, boolean compression) {
        Map<String, Object> storageConfig = new HashMap<>();
        storageConfig.put("data_dir", dataDir);
        StorageBackend storage = new DiskStorage(storageConfig);

        HNSWConfig config = HNSWConfig.builder()
                .metric(Metric.COSINE)
                .m(16)
                .efConstruction(200)
                .efSearch(50)
                .build();
        Index index = new HNSWIndex(config);

        Map<String, Object> persistConfig = new HashMap<>();
        persistConfig.put("data_dir", persistenceDir);
        persistConfig.put("compression", compression);
        PersistenceManager persistence = new TensorPersistence(persistConfig);

        VectorDBClient client = new VectorDBClient(storage, index, null, persistence, new HashMap<>());
        client.initialize();

        return client;
    }

    /**
     * Create a custom vector database with full control over components.
     *
     * @param storageConfig storage backend configuration
     * @param indexConfig index configuration
     * @param quantConfig optional quantizer configuration (can be null)
     * @param persistConfig optional persistence configuration (can be null)
     * @return configured and initialized VectorDBClient
     */
    public static VectorDBClient createCustom(
            Map<String, Object> storageConfig,
            Map<String, Object> indexConfig,
            Map<String, Object> quantConfig,
            Map<String, Object> persistConfig) {

        // Create storage backend
        StorageBackend storage = createStorageBackend(storageConfig);

        // Create index
        Index index = createIndex(indexConfig);

        // Create optional quantizer
        Quantizer quantizer = null;
        if (quantConfig != null && !quantConfig.isEmpty()) {
            quantizer = createQuantizer(quantConfig);
        }

        // Create optional persistence manager
        PersistenceManager persistence = null;
        if (persistConfig != null && !persistConfig.isEmpty()) {
            persistence = createPersistenceManager(persistConfig);
        }

        VectorDBClient client = new VectorDBClient(storage, index, quantizer, persistence, new HashMap<>());
        client.initialize();

        return client;
    }

    /**
     * Create storage backend from configuration.
     */
    private static StorageBackend createStorageBackend(Map<String, Object> config) {
        String type = (String) config.getOrDefault("type", "memory");

        return switch (type.toLowerCase()) {
            case "memory" -> new MemoryStorage(config);
            case "disk" -> new DiskStorage(config);
            case "hybrid" -> new HybridStorage(config);
            default -> throw new ConfigurationException("Unknown storage type: " + type);
        };
    }

    /**
     * Create index from configuration map.
     * <p>
     * This method converts legacy Map-based configs to type-safe configs.
     */
    private static Index createIndex(Map<String, Object> config) {
        String type = (String) config.getOrDefault("type", "flat");

        // Handle both String and Metric enum values
        Object metricObj = config.getOrDefault("metric", "cosine");
        Metric metric;
        if (metricObj instanceof Metric) {
            metric = (Metric) metricObj;
        } else if (metricObj instanceof String) {
            metric = Metric.fromString((String) metricObj);
        } else {
            throw new ConfigurationException("Invalid metric type: " + metricObj.getClass());
        }

        return switch (type.toLowerCase()) {
            case "flat" -> new FlatIndex(FlatConfig.builder()
                    .metric(metric)
                    .build());
            case "hnsw" -> new HNSWIndex(HNSWConfig.builder()
                    .metric(metric)
                    .m((int) config.getOrDefault("m", 16))
                    .efConstruction((int) config.getOrDefault("ef_construction", 200))
                    .efSearch((int) config.getOrDefault("ef_search", 50))
                    .build());
            case "ivf" -> new IVFIndex(IVFConfig.builder()
                    .metric(metric)
                    .numClusters((int) config.getOrDefault("num_clusters", 100))
                    .numProbes((int) config.getOrDefault("num_probes", 10))
                    .maxIterations((int) config.getOrDefault("max_iterations", 100))
                    .convergenceThreshold((double) config.getOrDefault("convergence_threshold", 0.001))
                    .build());
            case "lsh" -> new LSHIndex(LSHConfig.builder()
                    .metric(metric)
                    .numTables((int) config.getOrDefault("num_tables", 5))
                    .numHashBits((int) config.getOrDefault("num_hash_bits", 8))
                    .bucketWidth((double) config.getOrDefault("bucket_width", 4.0))
                    .build());
            case "annoy" -> new AnnoyIndex(AnnoyConfig.builder()
                    .metric(metric)
                    .numTrees((int) config.getOrDefault("num_trees", 10))
                    .maxLeafSize((int) config.getOrDefault("max_leaf_size", 10))
                    .searchK((int) config.getOrDefault("search_k", -1))
                    .build());
            default -> throw new ConfigurationException("Unknown index type: " + type);
        };
    }

    /**
     * Create quantizer from configuration.
     */
    private static Quantizer createQuantizer(Map<String, Object> config) {
        String type = (String) config.getOrDefault("type", "scalar");

        return switch (type.toLowerCase()) {
            case "scalar" -> new ScalarQuantizer(config);
            case "product" -> new ProductQuantizer(config);
            default -> throw new ConfigurationException("Unknown quantizer type: " + type);
        };
    }

    /**
     * Create persistence manager from configuration.
     */
    private static PersistenceManager createPersistenceManager(Map<String, Object> config) {
        String type = (String) config.getOrDefault("type", "tensor");

        switch (type.toLowerCase()) {
            case "tensor":
                return new TensorPersistence(config);
            // case "incremental":
            //     return new IncrementalPersistence(config);
            default:
                throw new ConfigurationException("Unknown persistence type: " + type);
        }
    }
}
