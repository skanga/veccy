package com.veccy.cli.commands;

import com.veccy.base.Index;
import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.config.*;
import com.veccy.indices.*;
import com.veccy.storage.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Initialize a vector database.
 */
public class InitCommand implements Command {

    @Override
    public String getName() {
        return "init";
    }

    @Override
    public String getDescription() {
        return "Initialize a vector database";
    }

    @Override
    public String getUsage() {
        return "init [--path <path>] [--index <type>] [--storage <type>] [--metric <metric>]";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        String path = null;
        String indexType = "hnsw";
        String storageType = "memory";
        String metric = "cosine";

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--path":
                case "-p":
                    if (i + 1 < args.length) {
                        path = args[++i];
                    }
                    break;
                case "--index":
                case "-i":
                    if (i + 1 < args.length) {
                        indexType = args[++i].toLowerCase();
                    }
                    break;
                case "--storage":
                case "-s":
                    if (i + 1 < args.length) {
                        storageType = args[++i].toLowerCase();
                    }
                    break;
                case "--metric":
                case "-m":
                    if (i + 1 < args.length) {
                        metric = args[++i].toLowerCase();
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }

        // Close existing database if any
        if (context.hasOpenDatabase()) {
            System.out.println("Closing existing database...");
            context.getClient().close();
        }

        // Create storage
        StorageBackend storage = createStorage(storageType, path);

        // Create index
        Index index = createIndex(indexType, metric);

        // Create and initialize client
        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        context.setClient(client);
        context.setDatabasePath(path);

        System.out.println("✓ Database initialized successfully");
        System.out.println("  Storage: " + storageType);
        System.out.println("  Index:   " + indexType);
        System.out.println("  Metric:  " + metric);
        if (path != null) {
            System.out.println("  Path:    " + path);
        }
    }

    private StorageBackend createStorage(String type, String path) throws Exception {
        return switch (type) {
            case "memory" -> new MemoryStorage(new HashMap<>());
            case "disk" -> {
                if (path == null) {
                    throw new IllegalArgumentException("--path required for disk storage");
                }
                Path dbPath = Paths.get(path);
                Files.createDirectories(dbPath);

                HashMap<String, Object> config = new HashMap<>();
                config.put("data_dir", path);
                yield new DiskStorage(config);
            }
            case "hybrid" -> {
                if (path == null) {
                    throw new IllegalArgumentException("--path required for hybrid storage");
                }
                Path dbPath = Paths.get(path);
                Files.createDirectories(dbPath);

                HashMap<String, Object> config = new HashMap<>();
                config.put("data_dir", path);
                yield new HybridStorage(config);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown storage type: " + type + ". Valid options: memory, disk, hybrid");
        };
    }

    private Index createIndex(String type, String metricStr) {
        Metric metric = Metric.fromString(metricStr);

        return switch (type) {
            case "flat" -> new FlatIndex(FlatConfig.builder()
                    .metric(metric)
                    .build());

            case "hnsw" -> new HNSWIndex(HNSWConfig.builder()
                    .metric(metric)
                    .m(16)
                    .efConstruction(200)
                    .efSearch(50)
                    .build());

            case "ivf" -> new IVFIndex(IVFConfig.builder()
                    .metric(metric)
                    .numClusters(100)
                    .numProbes(10)
                    .build());

            case "lsh" -> new LSHIndex(LSHConfig.builder()
                    .metric(metric)
                    .numTables(5)
                    .numHashBits(8)
                    .build());

            case "annoy" -> new AnnoyIndex(AnnoyConfig.builder()
                    .metric(metric)
                    .numTrees(10)
                    .maxLeafSize(10)
                    .build());

            default -> throw new IllegalArgumentException(
                    "Unknown index type: " + type + ". Valid options: flat, hnsw, ivf, lsh, annoy");
        };
    }
}
