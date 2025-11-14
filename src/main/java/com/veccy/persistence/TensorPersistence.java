package com.veccy.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veccy.exceptions.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tensor-based persistence manager implementation.
 * <p>
 * This persistence manager saves vectors and index data using binary formats
 * for efficient storage and loading. State and index data are saved as
 * JSON for better portability. Vectors are saved in a custom binary format
 * optimized for double arrays.
 * <p>
 * Features:
 * - Optional compression using GZIP
 * - JSON serialization for state/index data
 * - Binary format for vector arrays
 * - Automatic directory management
 */
public class TensorPersistence implements PersistenceManager {

    private static final Logger logger = LoggerFactory.getLogger(TensorPersistence.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Object> config;
    private final boolean compression;
    private Path dataDir;
    private int saveCount;
    private int loadCount;
    private boolean initialized;

    public TensorPersistence(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.compression = (Boolean) this.config.getOrDefault("compression", false);
        this.saveCount = 0;
        this.loadCount = 0;
        this.initialized = false;
    }

    @Override
    public void initialize() {
        try {
            String dataDirPath = (String) config.getOrDefault("data_dir", "./veccy_persistence");
            dataDir = Paths.get(dataDirPath);

            // Create data directory
            Files.createDirectories(dataDir);

            initialized = true;
            logger.info("Tensor persistence initialized at: {}", dataDir);
        } catch (Exception e) {
            throw new PersistenceException("Failed to initialize tensor persistence: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean saveState(Map<String, Object> state, String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            // Ensure directory exists
            Files.createDirectories(filePath.getParent());

            // Save state as JSON
            if (compression) {
                try (OutputStream fos = Files.newOutputStream(filePath);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    objectMapper.writeValue(gzos, state);
                }
            } else {
                objectMapper.writeValue(filePath.toFile(), state);
            }

            saveCount++;
            logger.debug("Saved state to: {}", path);
            return true;
        } catch (Exception e) {
            throw new PersistenceException("Failed to save state to " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Map<String, Object>> loadState(String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            Map<String, Object> state;
            if (compression) {
                try (InputStream fis = Files.newInputStream(filePath);
                     GZIPInputStream gzis = new GZIPInputStream(fis)) {
                    state = objectMapper.readValue(gzis,
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                }
            } else {
                state = objectMapper.readValue(filePath.toFile(),
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
            }

            loadCount++;
            logger.debug("Loaded state from: {}", path);
            return Optional.of(state);
        } catch (Exception e) {
            throw new PersistenceException("Failed to load state from " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean saveVectors(double[][] vectors, List<String> ids, String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            // Ensure directory exists
            Files.createDirectories(filePath.getParent());

            // Custom binary format for vectors
            OutputStream os = Files.newOutputStream(filePath);
            if (compression) {
                os = new GZIPOutputStream(os);
            }

            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os))) {
                // Write header: number of vectors and dimensions
                dos.writeInt(vectors.length);
                dos.writeInt(vectors.length > 0 ? vectors[0].length : 0);

                // Write IDs
                for (String id : ids) {
                    dos.writeUTF(id);
                }

                // Write vectors
                for (double[] vector : vectors) {
                    for (double val : vector) {
                        dos.writeDouble(val);
                    }
                }
            }

            saveCount++;
            logger.debug("Saved {} vectors to: {}", vectors.length, path);
            return true;
        } catch (Exception e) {
            throw new PersistenceException("Failed to save vectors to " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<VectorsWithIds> loadVectors(String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            InputStream is = Files.newInputStream(filePath);
            if (compression) {
                is = new GZIPInputStream(is);
            }

            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(is))) {
                // Read header
                int numVectors = dis.readInt();
                int dimensions = dis.readInt();

                // Read IDs
                List<String> ids = new ArrayList<>(numVectors);
                for (int i = 0; i < numVectors; i++) {
                    ids.add(dis.readUTF());
                }

                // Read vectors
                double[][] vectors = new double[numVectors][dimensions];
                for (int i = 0; i < numVectors; i++) {
                    for (int d = 0; d < dimensions; d++) {
                        vectors[i][d] = dis.readDouble();
                    }
                }

                loadCount++;
                logger.debug("Loaded {} vectors from: {}", numVectors, path);
                return Optional.of(new VectorsWithIds(vectors, ids));
            }
        } catch (Exception e) {
            throw new PersistenceException("Failed to load vectors from " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean saveIndex(Map<String, Object> indexData, String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            // Ensure directory exists
            Files.createDirectories(filePath.getParent());

            // Save index data as JSON
            if (compression) {
                try (OutputStream fos = Files.newOutputStream(filePath);
                     GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                    objectMapper.writeValue(gzos, indexData);
                }
            } else {
                objectMapper.writeValue(filePath.toFile(), indexData);
            }

            saveCount++;
            logger.debug("Saved index to: {}", path);
            return true;
        } catch (Exception e) {
            throw new PersistenceException("Failed to save index to " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Map<String, Object>> loadIndex(String path) {
        if (!initialized) {
            throw new PersistenceException("Persistence manager not initialized");
        }

        try {
            Path filePath = dataDir.resolve(path);

            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            Map<String, Object> indexData;
            if (compression) {
                try (InputStream fis = Files.newInputStream(filePath);
                     GZIPInputStream gzis = new GZIPInputStream(fis)) {
                    indexData = objectMapper.readValue(gzis,
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
                }
            } else {
                indexData = objectMapper.readValue(filePath.toFile(),
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
            }

            loadCount++;
            logger.debug("Loaded index from: {}", path);
            return Optional.of(indexData);
        } catch (Exception e) {
            throw new PersistenceException("Failed to load index from " + path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        try {
            // Calculate total disk usage
            long totalSize = 0;
            int fileCount = 0;

            try (Stream<Path> files = Files.walk(dataDir)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    if (Files.isRegularFile(file)) {
                        totalSize += Files.size(file);
                        fileCount++;
                    }
                }
            }

            stats.put("type", "TensorPersistence");
            stats.put("persistence_type", "tensor");
            stats.put("data_directory", dataDir.toString());
            stats.put("compression_enabled", compression);
            stats.put("save_count", saveCount);
            stats.put("load_count", loadCount);
            stats.put("file_count", fileCount);
            stats.put("disk_usage_bytes", totalSize);
            stats.put("disk_usage_mb", totalSize / (1024.0 * 1024.0));
            stats.put("initialized", initialized);

            return stats;
        } catch (Exception e) {
            logger.error("Error collecting stats", e);
            stats.put("persistence_type", "tensor");
            stats.put("error", e.getMessage());
            stats.put("initialized", initialized);
            return stats;
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void close() {
        initialized = false;
        logger.info("Tensor persistence closed");
    }
}
