package com.veccy.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.veccy.base.Page;
import com.veccy.exceptions.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Hybrid storage backend combining in-memory cache with disk persistence.
 * <p>
 * Features:
 * - Caffeine-based LRU cache for hot data
 * - DiskStorage for persistence
 * - Write-through caching strategy
 * - Configurable cache size and eviction policies
 * - Cache statistics tracking (hits, misses, evictions)
 * <p>
 * Configuration:
 * - data_dir: Directory for disk storage
 * - cache_size: Maximum number of vectors in cache (default 1000)
 * - cache_expire_minutes: Minutes until cached items expire (default no expiration)
 * - cache_initial_capacity: Initial cache capacity (default 100)
 */
public class HybridStorage implements StorageBackend {

    private static final Logger logger = LoggerFactory.getLogger(HybridStorage.class);

    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final int DEFAULT_INITIAL_CAPACITY = 100;

    private Map<String, Object> config;
    private DiskStorage diskStorage;
    private Cache<String, VectorWithMetadata> cache;
    private boolean initialized;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public HybridStorage(Map<String, Object> config) {
        this.config = config != null ? new HashMap<>(config) : new HashMap<>();
        this.initialized = false;
    }

    @Override
    public void initialize() {
        if (initialized) {
            logger.warn("HybridStorage already initialized");
            return;
        }

        // Initialize disk storage
        diskStorage = new DiskStorage(config);
        diskStorage.initialize();

        // Configure cache
        int cacheSize = getConfigInt("cache_size", DEFAULT_CACHE_SIZE);
        int initialCapacity = getConfigInt("cache_initial_capacity", DEFAULT_INITIAL_CAPACITY);

        Caffeine<String, VectorWithMetadata> cacheBuilder = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .initialCapacity(initialCapacity)
                .removalListener((RemovalListener<String, VectorWithMetadata>) (key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        evictions.incrementAndGet();
                        logger.debug("Cache evicted vector: {} (cause: {})", key, cause);
                    }
                });

        // Optional: time-based expiration
        if (config.containsKey("cache_expire_minutes")) {
            int expireMinutes = getConfigInt("cache_expire_minutes", 0);
            if (expireMinutes > 0) {
                cacheBuilder.expireAfterAccess(Duration.ofMinutes(expireMinutes));
            }
        }

        cache = cacheBuilder.build();

        initialized = true;
        logger.info("Hybrid storage initialized: cache_size={}, disk_dir={}",
                cacheSize, config.get("data_dir"));
    }

    @Override
    public boolean storeVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        try {
            // Write-through: write to both disk and cache
            boolean diskSuccess = diskStorage.storeVector(id, vector, metadata);

            if (diskSuccess) {
                cache.put(id, new VectorWithMetadata(vector.clone(), metadata));
                logger.debug("Stored vector with ID: {} (disk + cache)", id);
            }

            return diskSuccess;
        } catch (Exception e) {
            throw new StorageException("Failed to store vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<VectorWithMetadata> retrieveVector(String id) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        try {
            // Try cache first
            VectorWithMetadata cached = cache.getIfPresent(id);
            if (cached != null) {
                cacheHits.incrementAndGet();
                logger.debug("Cache hit for vector: {}", id);
                return Optional.of(cached);
            }

            // Cache miss - load from disk
            cacheMisses.incrementAndGet();
            logger.debug("Cache miss for vector: {}", id);

            Optional<VectorWithMetadata> fromDisk = diskStorage.retrieveVector(id);

            // Populate cache if found
            fromDisk.ifPresent(vectorWithMetadata -> cache.put(id, vectorWithMetadata));

            return fromDisk;
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteVector(String id) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        try {
            // Delete from both cache and disk
            cache.invalidate(id);
            boolean diskSuccess = diskStorage.deleteVector(id);

            if (diskSuccess) {
                logger.debug("Deleted vector with ID: {} (disk + cache)", id);
            }

            return diskSuccess;
        } catch (Exception e) {
            throw new StorageException("Failed to delete vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateVector(String id, double[] vector, Map<String, Object> metadata) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        try {
            // Write-through: update both disk and cache
            boolean diskSuccess = diskStorage.updateVector(id, vector, metadata);

            if (diskSuccess) {
                cache.put(id, new VectorWithMetadata(vector.clone(), metadata));
                logger.debug("Updated vector with ID: {} (disk + cache)", id);
            }

            return diskSuccess;
        } catch (Exception e) {
            throw new StorageException("Failed to update vector " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listVectors(Integer limit) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        // List from disk (source of truth)
        return diskStorage.listVectors(limit);
    }

    @Override
    public Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        // Delegate to disk storage (source of truth)
        return diskStorage.listVectorsPaginated(pageSize, cursor);
    }

    @Override
    public Stream<String> streamVectorIds() {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        // Delegate to disk storage (source of truth)
        return diskStorage.streamVectorIds();
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        if (!initialized) {
            stats.put("status", "not_initialized");
            return stats;
        }

        // Disk storage stats
        Map<String, Object> diskStats = diskStorage.getStats();

        // Cache stats
        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("size", cache.estimatedSize());
        cacheStats.put("hits", cacheHits.get());
        cacheStats.put("misses", cacheMisses.get());
        cacheStats.put("evictions", evictions.get());

        long total = cacheHits.get() + cacheMisses.get();
        if (total > 0) {
            cacheStats.put("hit_rate", (double) cacheHits.get() / total);
        } else {
            cacheStats.put("hit_rate", 0.0);
        }

        stats.put("type", "HybridStorage");
        stats.put("initialized", initialized);
        stats.put("cache", cacheStats);
        stats.put("disk", diskStats);

        return stats;
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
            try {
                if (diskStorage != null) {
                    try {
                        diskStorage.close();
                    } catch (Exception e) {
                        logger.error("Error closing disk storage: {}", e.getMessage(), e);
                    }
                }

                if (cache != null) {
                    try {
                        cache.invalidateAll();
                        cache.cleanUp();
                    } catch (Exception e) {
                        logger.error("Error closing cache: {}", e.getMessage(), e);
                    }
                }

                initialized = false;
                logger.info("Hybrid storage closed (hits: {}, misses: {}, evictions: {})",
                        cacheHits.get(), cacheMisses.get(), evictions.get());
            } catch (Exception e) {
                logger.error("Error closing hybrid storage: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("Hybrid storage already closed, ignoring duplicate close() call");
        }
    }

    /**
     * Manually invalidate the entire cache.
     * Useful for testing or forcing disk reads.
     */
    public void invalidateCache() {
        if (cache != null) {
            cache.invalidateAll();
            logger.debug("Cache invalidated");
        }
    }

    /**
     * Preload vectors into cache.
     *
     * @param ids Vector IDs to preload
     * @return Number of vectors successfully preloaded
     */
    public int preloadCache(List<String> ids) {
        if (!initialized) {
            throw new StorageException("Storage not initialized");
        }

        int loaded = 0;
        for (String id : ids) {
            Optional<VectorWithMetadata> vector = diskStorage.retrieveVector(id);
            if (vector.isPresent()) {
                cache.put(id, vector.get());
                loaded++;
            }
        }

        logger.info("Preloaded {} vectors into cache", loaded);
        return loaded;
    }

    /**
     * Get cache hit rate.
     *
     * @return Hit rate as percentage (0.0 to 1.0)
     */
    public double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    private int getConfigInt(String key, int defaultValue) {
        if (!config.containsKey(key)) {
            return defaultValue;
        }
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
