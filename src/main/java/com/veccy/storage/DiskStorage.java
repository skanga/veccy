package com.veccy.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veccy.base.Page;
import com.veccy.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Disk-based storage backend implementation.
 * <p>
 * This backend stores vectors and metadata on disk using Java serialization
 * and binary files. Vectors are stored as binary files, metadata as JSON.
 */
public class DiskStorage implements StorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(DiskStorage.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Object> config;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private Path dataDir;
    private Path vectorsDir;
    private Path metadataDir;
    private boolean initialized;
    private int vectorCount;
    private final ReadWriteLock lock;

    public DiskStorage(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.initialized = false;
        this.vectorCount = 0;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public void initialize() {
        lock.writeLock().lock();
        try {
            String dataDirPath = (String) config.getOrDefault("data_dir", "./veccy_data");
            dataDir = Paths.get(dataDirPath);
            vectorsDir = dataDir.resolve("vectors");
            metadataDir = dataDir.resolve("metadata");

            // Create directories
            Files.createDirectories(dataDir);
            Files.createDirectories(vectorsDir);
            Files.createDirectories(metadataDir);

            // Count existing vectors
            try (Stream<Path> files = Files.list(vectorsDir)) {
                vectorCount = (int) files.filter(p -> p.toString().endsWith(".vec")).count();
            }

            initialized = true;
            logger.info("Disk storage initialized at: {}", dataDir);
        } catch (Exception e) {
            throw new StorageException("Failed to initialize disk storage: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean storeVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.writeLock().lock();
        try {
            String safeId = sanitizeId(id);

            // Store vector as binary file
            Path vectorPath = vectorsDir.resolve(safeId + ".vec");
            try (DataOutputStream dos = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(vectorPath)))) {
                dos.writeInt(vector.length);
                for (double v : vector) {
                    dos.writeDouble(v);
                }
            }

            // Store metadata as JSON
            if (metadata != null) {
                Path metadataPath = metadataDir.resolve(safeId + ".json");
                objectMapper.writeValue(metadataPath.toFile(), metadata);
            } else {
                // Remove metadata file if exists
                Path metadataPath = metadataDir.resolve(safeId + ".json");
                Files.deleteIfExists(metadataPath);
            }

            vectorCount++;
            logger.debug("Stored vector with ID: {}", id);
            return true;
        } catch (Exception e) {
            throw new StorageException("Failed to store vector " + id + ": " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<VectorWithMetadata> retrieveVector(String id) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try {
            String safeId = sanitizeId(id);

            // Load vector
            Path vectorPath = vectorsDir.resolve(safeId + ".vec");
            if (!Files.exists(vectorPath)) {
                return Optional.empty();
            }

            double[] vector;
            try (DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(vectorPath)))) {
                int length = dis.readInt();
                vector = new double[length];
                for (int i = 0; i < length; i++) {
                    vector[i] = dis.readDouble();
                }
            }

            // Load metadata
            Map<String, Object> metadata = null;
            Path metadataPath = metadataDir.resolve(safeId + ".json");
            if (Files.exists(metadataPath)) {
                metadata = objectMapper.readValue(metadataPath.toFile(),
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
            }

            return Optional.of(new VectorWithMetadata(vector, metadata));
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve vector " + id + ": " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.writeLock().lock();
        try {
            String safeId = sanitizeId(id);

            Path vectorPath = vectorsDir.resolve(safeId + ".vec");
            boolean deleted = Files.deleteIfExists(vectorPath);

            Path metadataPath = metadataDir.resolve(safeId + ".json");
            Files.deleteIfExists(metadataPath);

            if (deleted) {
                vectorCount--;
                logger.debug("Deleted vector with ID: {}", id);
            }

            return deleted;
        } catch (Exception e) {
            throw new StorageException("Failed to delete vector " + id + ": " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean updateVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.writeLock().lock();
        try {
            String safeId = sanitizeId(id);
            Path vectorPath = vectorsDir.resolve(safeId + ".vec");

            if (!Files.exists(vectorPath)) {
                return false;
            }

            // Update vector if provided
            if (vector != null) {
                try (DataOutputStream dos = new DataOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(vectorPath)))) {
                    dos.writeInt(vector.length);
                    for (double v : vector) {
                        dos.writeDouble(v);
                    }
                }
            }

            // Update metadata
            Path metadataPath = metadataDir.resolve(safeId + ".json");
            if (metadata != null) {
                objectMapper.writeValue(metadataPath.toFile(), metadata);
            } else {
                Files.deleteIfExists(metadataPath);
            }

            logger.debug("Updated vector with ID: {}", id);
            return true;
        } catch (Exception e) {
            throw new StorageException("Failed to update vector " + id + ": " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<String> listVectors(Integer limit) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try (Stream<Path> files = Files.list(vectorsDir)) {
            Stream<String> ids = files
                    .filter(p -> p.toString().endsWith(".vec"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.substring(0, fileName.length() - 4); // Remove .vec extension
                    })
                    .sorted(); // Sort for consistent ordering

            if (limit != null && limit > 0) {
                ids = ids.limit(limit);
            }

            return ids.collect(Collectors.toList());
        } catch (Exception e) {
            throw new StorageException("Failed to list vectors: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        lock.readLock().lock();
        try (Stream<Path> files = Files.list(vectorsDir)) {
            // Get sorted list of IDs
            List<String> allIds = files
                    .filter(p -> p.toString().endsWith(".vec"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.substring(0, fileName.length() - 4);
                    })
                    .sorted()
                    .collect(Collectors.toList());

            int startIndex = 0;
            if (cursor.isPresent()) {
                String cursorId = cursor.get();
                int cursorIndex = allIds.indexOf(cursorId);
                if (cursorIndex >= 0) {
                    startIndex = cursorIndex + 1;
                } else {
                    logger.warn("Cursor {} not found, starting from beginning", cursorId);
                }
            }

            if (startIndex >= allIds.size()) {
                return Page.empty();
            }

            int endIndex = Math.min(startIndex + pageSize, allIds.size());
            List<String> pageItems = allIds.subList(startIndex, endIndex);

            if (endIndex < allIds.size()) {
                String nextCursor = allIds.get(endIndex - 1);
                logger.debug("Returning page with {} items, next cursor: {}", pageItems.size(), nextCursor);
                return Page.of(pageItems, nextCursor);
            } else {
                logger.debug("Returning final page with {} items", pageItems.size());
                return Page.last(pageItems);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to list vectors paginated: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<String> streamVectorIds() {
        if (!initialized) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try {
            return Files.list(vectorsDir)
                    .filter(p -> p.toString().endsWith(".vec"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return fileName.substring(0, fileName.length() - 4);
                    })
                    .sorted();
        } catch (Exception e) {
            throw new StorageException("Failed to stream vector IDs: " + e.getMessage(), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        lock.readLock().lock();
        try {
            long totalSize = 0;
            try (Stream<Path> files = Files.walk(dataDir)) {
                totalSize = files
                        .filter(Files::isRegularFile)
                        .mapToLong(p -> {
                            try {
                                return Files.size(p);
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .sum();
            }

            stats.put("type", "DiskStorage");
            stats.put("backend_type", "disk");
            stats.put("vector_count", vectorCount);
            stats.put("data_dir", dataDir.toString());
            stats.put("disk_usage_bytes", totalSize);
            stats.put("disk_usage_mb", totalSize / (1024.0 * 1024.0));
            stats.put("initialized", initialized);
            return stats;
        } catch (Exception e) {
            logger.error("Error collecting stats", e);
            stats.put("error", e.getMessage());
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Close the storage and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            lock.writeLock().lock();
            try {
                initialized = false;
                vectorCount = 0;
                logger.info("Disk storage closed");
            } catch (Exception e) {
                logger.error("Error closing disk storage: {}", e.getMessage(), e);
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            logger.debug("Disk storage already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Sanitize ID for filesystem use.
     */
    private String sanitizeId(String id) {
        // Replace characters that are problematic for filesystems
        return id.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
