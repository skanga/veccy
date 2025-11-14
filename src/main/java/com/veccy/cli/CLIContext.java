package com.veccy.cli;

import com.veccy.client.VectorDBClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Context for CLI operations, maintaining state across commands.
 */
public class CLIContext implements AutoCloseable {

    private VectorDBClient client;
    private Path databasePath;
    private boolean verbose = false;
    private String outputFormat = "table"; // table, json, csv
    private final Map<String, Object> properties = new HashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Get the current database client.
     */
    public VectorDBClient getClient() {
        return client;
    }

    /**
     * Set the database client.
     */
    public void setClient(VectorDBClient client) {
        if (this.client != null && this.client != client) {
            this.client.close();
        }
        this.client = client;
    }

    /**
     * Check if a database is currently open.
     */
    public boolean hasOpenDatabase() {
        return client != null && client.isInitialized();
    }

    /**
     * Get the database path.
     */
    public Path getDatabasePath() {
        return databasePath;
    }

    /**
     * Set the database path.
     */
    public void setDatabasePath(String path) {
        this.databasePath = path != null ? Paths.get(path) : null;
    }

    /**
     * Get verbose mode.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Set verbose mode.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Get output format.
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * Set output format (table, json, csv).
     */
    public void setOutputFormat(String format) {
        if (!format.equals("table") && !format.equals("json") && !format.equals("csv")) {
            throw new IllegalArgumentException("Invalid output format. Must be: table, json, or csv");
        }
        this.outputFormat = format;
    }

    /**
     * Set a property.
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Get a property.
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get a property with default value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        Object value = properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Close the CLI context and release resources.
     * <p>
     * This method is idempotent - calling close() multiple times has no effect
     * after the first call. Thread-safe.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
                client = null;
            }
        }
    }
}
