package com.veccy.base;

import com.veccy.exceptions.IndexException;
import com.veccy.storage.StorageBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract base class for all index implementations.
 * <p>
 * This class provides common functionality shared across all index types:
 * - Initialization lifecycle management
 * - State validation
 * - Resource cleanup
 * <p>
 * Subclasses must implement:
 * - {@link #doInitialize()} for index-specific initialization
 * - {@link #doClose()} for index-specific cleanup
 * - All abstract methods from {@link Index} interface
 * <p>
 * Configuration is now handled by type-safe config classes specific to each index type.
 */
public abstract class AbstractIndex implements Index {

    private static final Logger logger = LoggerFactory.getLogger(AbstractIndex.class);

    /**
     * Storage backend for vector persistence.
     */
    protected StorageBackend storageBackend;

    /**
     * Initialization state flag.
     * Volatile ensures visibility across threads.
     */
    protected volatile boolean initialized = false;

    /**
     * Close state flag for idempotent close operations.
     * AtomicBoolean ensures thread-safe close-once semantics.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Default constructor.
     * Subclasses should accept their specific typed configuration.
     */
    protected AbstractIndex() {
    }

    /**
     * Initialize the index with a storage backend.
     * <p>
     * This method is final to enforce consistent initialization behavior.
     * Subclasses should override {@link #doInitialize()} for custom logic.
     *
     * @param storageBackend the storage backend to use
     * @throws IndexException if already initialized or initialization fails
     */
    @Override
    public final void initialize(StorageBackend storageBackend) {
        if (initialized) {
            logger.warn("{} already initialized", getClass().getSimpleName());
            throw new IndexException("Index already initialized");
        }

        if (storageBackend == null) {
            throw new IndexException("Storage backend cannot be null");
        }

        this.storageBackend = storageBackend;

        try {
            doInitialize();
            this.initialized = true;
            logger.info("{} initialized successfully", getClass().getSimpleName());
        } catch (Exception e) {
            this.initialized = false;
            logger.error("Failed to initialize {}: {}", getClass().getSimpleName(), e.getMessage());
            throw new IndexException("Failed to initialize index: " + e.getMessage(), e);
        }
    }

    /**
     * Template method for index-specific initialization.
     * <p>
     * Called by {@link #initialize(StorageBackend)} after setting up
     * storage backend. Subclasses should implement their specific
     * initialization logic here.
     *
     * @throws Exception if initialization fails
     */
    protected abstract void doInitialize() throws Exception;

    /**
     * Check if the index has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    @Override
    public final boolean isInitialized() {
        return initialized;
    }

    /**
     * Ensure the index is initialized before operations.
     * <p>
     * Call this method at the beginning of all index operations.
     *
     * @throws IndexException if index is not initialized
     */
    protected final void ensureInitialized() {
        if (!initialized) {
            throw new IndexException("Index not initialized. Call initialize() first.");
        }
    }

    /**
     * Close the index and release resources.
     * <p>
     * This method is final to ensure consistent cleanup behavior and is idempotent -
     * calling close() multiple times has no effect after the first call.
     * <p>
     * Subclasses should override {@link #doClose()} for custom cleanup.
     * <p>
     * Thread-safe: Multiple concurrent calls to close() are safe, and doClose()
     * will only be executed once.
     */
    @Override
    public final void close() {
        // Idempotent close - only execute doClose() once
        if (closed.compareAndSet(false, true)) {
            try {
                doClose();
                logger.info("{} closed successfully", getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Error closing {}: {}", getClass().getSimpleName(), e.getMessage(), e);
                // Don't rethrow - close() should not throw exceptions
            } finally {
                this.initialized = false;
            }
        } else {
            logger.debug("{} already closed, ignoring duplicate close() call", getClass().getSimpleName());
        }
    }

    /**
     * Template method for index-specific cleanup.
     * <p>
     * Called by {@link #close()} to release index-specific resources.
     * Subclasses should implement their cleanup logic here.
     * <p>
     * Note: This method should not throw exceptions. Handle errors internally
     * and log them appropriately.
     */
    protected abstract void doClose();
}
