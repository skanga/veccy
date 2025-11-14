package com.veccy.storage;

import com.veccy.base.Page;
import com.veccy.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * In-memory storage backend implementation.
 * <p>
 * This backend stores vectors and metadata in memory using concurrent data structures.
 * It's fast but not persistent across restarts. Thread-safe for concurrent access.
 */
public class MemoryStorage implements StorageBackend {

    private final Logger logger;
    private final Map<String, Object> config;
    private final Map<String, double[]> vectors;
    private final Map<String, Map<String, Object>> metadata;
    private final ReadWriteLock lock;
    private final AtomicBoolean initialized;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger vectorCount;
    private final AtomicLong memoryUsage;

    public MemoryStorage(Map<String, Object> config) {
        this(config, LoggerFactory.getLogger(MemoryStorage.class));
    }

    public MemoryStorage(Map<String, Object> config, Logger logger) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.vectors = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.initialized = new AtomicBoolean(false);
        this.vectorCount = new AtomicInteger(0);
        this.memoryUsage = new AtomicLong(0);
        this.logger = logger;
    }

    @Override
    public void initialize() {
        lock.writeLock().lock();
        try {
            vectors.clear();
            metadata.clear();
            vectorCount.set(0);
            memoryUsage.set(0);
            initialized.set(true);
            logger.info("Memory storage initialized");
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean storeVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        try {
            lock.writeLock().lock();
            try {
                // Store a copy of the vector
                vectors.put(id, vector.clone());

                // Store metadata if provided
                if (metadata != null) {
                    this.metadata.put(id, new HashMap<>(metadata));
                } else {
                    this.metadata.remove(id);
                }

                // Update stats
                updateStats();

                logger.debug("Stored vector with ID: {}", id);
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            throw new StorageException("Failed to store vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<VectorWithMetadata> retrieveVector(String id) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try {
            if (!vectors.containsKey(id)) {
                return Optional.empty();
            }

            double[] vector = vectors.get(id).clone();
            Map<String, Object> meta = metadata.get(id);
            Map<String, Object> metaCopy = meta != null ? new HashMap<>(meta) : null;

            return Optional.of(new VectorWithMetadata(vector, metaCopy));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        try {
            lock.writeLock().lock();
            try {
                boolean existed = vectors.remove(id) != null;
                metadata.remove(id);

                if (existed) {
                    updateStats();
                    logger.debug("Deleted vector with ID: {}", id);
                }

                return existed;
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            throw new StorageException("Failed to delete vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        try {
            lock.writeLock().lock();
            try {
                if (!vectors.containsKey(id)) {
                    return false;
                }

                // Update vector if provided
                if (vector != null) {
                    vectors.put(id, vector.clone());
                }

                // Update metadata
                if (metadata != null) {
                    this.metadata.put(id, new HashMap<>(metadata));
                } else {
                    this.metadata.remove(id);
                }

                // Update stats
                updateStats();

                logger.debug("Updated vector with ID: {}", id);
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        } catch (Exception e) {
            throw new StorageException("Failed to update vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listVectors(Integer limit) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try {
            List<String> vectorIds = new ArrayList<>(vectors.keySet());

            if (limit != null && limit > 0 && limit < vectorIds.size()) {
                return vectorIds.subList(0, limit);
            }

            return vectorIds;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }

        lock.readLock().lock();
        try {
            // Get sorted list of IDs for consistent pagination
            List<String> allIds = new ArrayList<>(vectors.keySet());
            Collections.sort(allIds);

            int startIndex = 0;
            if (cursor.isPresent()) {
                String cursorId = cursor.get();
                int cursorIndex = allIds.indexOf(cursorId);
                if (cursorIndex >= 0) {
                    startIndex = cursorIndex + 1;
                } else {
                    // Cursor not found - might have been deleted
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
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Stream<String> streamVectorIds() {
        if (!initialized.get()) {
            throw new StorageException("Storage backend not initialized");
        }

        lock.readLock().lock();
        try {
            // Return a sorted stream for consistency
            return new ArrayList<>(vectors.keySet()).stream().sorted();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized.get()) {
            stats.put("status", "not_initialized");
            return stats;
        }

        lock.readLock().lock();
        try {
            stats.put("type", "MemoryStorage");
            stats.put("backend_type", "memory");
            stats.put("vector_count", vectorCount.get());
            stats.put("memory_usage_bytes", memoryUsage.get());
            stats.put("memory_usage_mb", memoryUsage.get() / (1024.0 * 1024.0));
            stats.put("initialized", initialized.get());
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
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
                vectors.clear();
                metadata.clear();
                vectorCount.set(0);
                memoryUsage.set(0);
                initialized.set(false);
                logger.info("Memory storage closed");
            } catch (Exception e) {
                logger.error("Error closing memory storage: {}", e.getMessage(), e);
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            logger.debug("Memory storage already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Update internal statistics (must be called with write lock held).
     */
    private void updateStats() {
        vectorCount.set(vectors.size());

        // Calculate memory usage
        long totalMemory = 0;
        for (double[] vector : vectors.values()) {
            totalMemory += (long) vector.length * Double.BYTES;
        }

        // Approximate metadata size
        for (Map<String, Object> meta : metadata.values()) {
            totalMemory += estimateMetadataSize(meta);
        }

        memoryUsage.set(totalMemory);
    }

    /**
     * Estimate the memory size of a metadata map.
     */
    private long estimateMetadataSize(Map<String, Object> meta) {
        if (meta == null) {
            return 0;
        }
        // Rough approximation: string representation byte length
        return meta.toString().getBytes().length;
    }
}
