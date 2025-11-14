package com.veccy.rest.handlers;

import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.rest.config.DatabaseMetadata;
import com.veccy.rest.config.ServerContext;
import com.veccy.rest.dto.ApiResponse;
import com.veccy.rest.dto.CreateDatabaseRequest;
import com.veccy.rest.dto.PaginatedResponse;
import com.veccy.rest.dto.PaginationRequest;
import com.veccy.rest.validation.InputValidator;
import io.javalin.http.Context;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handler for database management operations.
 */
public class DatabaseHandler {
    private static final Pattern VALID_DB_NAME = Pattern.compile("^[a-zA-Z0-9]+([_-]?[a-zA-Z0-9]+)*$");
    private static final int MIN_DB_NAME_LENGTH = 3;
    private static final int MAX_DB_NAME_LENGTH = 64;

    private static final String INVALID_DB_NAME_MESSAGE = "Invalid database name. Must be " + MIN_DB_NAME_LENGTH + "-" + MAX_DB_NAME_LENGTH +
                                                      " characters, start and end with alphanumeric, and can contain non-consecutive underscores or hyphens.";

    private final ServerContext serverContext;

    public DatabaseHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Validate database name.
     * - Must be between 3 and 64 characters.
     * - Must start and end with an alphanumeric character.
     * - Can contain alphanumeric characters, underscores, and hyphens.
     * - Cannot contain consecutive underscores or hyphens.
     */
    private boolean isValidDatabaseName(String name) {
        if (name == null || name.length() < MIN_DB_NAME_LENGTH || name.length() > MAX_DB_NAME_LENGTH) {
            return false;
        }
        return VALID_DB_NAME.matcher(name).matches();
    }

    /**
     * Create a new database.
     * POST /api/v1/databases
     */
    public void createDatabase(Context ctx) {
        CreateDatabaseRequest request = ctx.bodyAsClass(CreateDatabaseRequest.class);

        // Validate database name
        if (!isValidDatabaseName(request.getName())) {
            ctx.status(400).json(ApiResponse.error(INVALID_DB_NAME_MESSAGE));
            return;
        }

        // Validate dimensions
        InputValidator.ValidationResult dimensionValidation =
            InputValidator.validateDimension(request.getDimensions());

        if (!dimensionValidation.isValid()) {
            ctx.status(400).json(ApiResponse.error("Invalid dimensions: " +
                dimensionValidation.getErrorMessage()));
            return;
        }

        if (serverContext.hasDatabase(request.getName())) {
            ctx.status(409).json(ApiResponse.error("Database already exists: " + request.getName()));
            return;
        }

        try {
            // Create database using factory
            Map<String, Object> indexConfig = request.getIndexConfig() != null
                ? request.getIndexConfig()
                : new HashMap<>();

            Map<String, Object> storageConfig = request.getStorageConfig() != null
                ? request.getStorageConfig()
                : new HashMap<>();

            // Set default index type if not specified
            if (!indexConfig.containsKey("type")) {
                indexConfig.put("type", "hnsw");
                indexConfig.put("metric", "cosine");
                indexConfig.put("m", 16);
                indexConfig.put("ef_construction", 200);
                indexConfig.put("ef_search", 50);
            }

            // Set default storage type if not specified
            if (!storageConfig.containsKey("type")) {
                storageConfig.put("type", "memory");
            }

            VectorDB database = VectorDBFactory.createCustom(
                storageConfig,
                indexConfig,
                null,
                null
            );

            // Create metadata for dimension tracking
            DatabaseMetadata metadata = new DatabaseMetadata.Builder()
                .name(request.getName())
                .dimensions(request.getDimensions())
                .indexType((String) indexConfig.getOrDefault("type", "hnsw"))
                .storageType((String) storageConfig.getOrDefault("type", "memory"))
                .distanceMetric((String) indexConfig.getOrDefault("metric", "cosine"))
                .indexConfig(indexConfig)
                .storageConfig(storageConfig)
                .build();

            serverContext.registerDatabase(request.getName(), database, metadata);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("name", request.getName());
            responseData.put("dimensions", request.getDimensions());
            responseData.put("index_type", metadata.getIndexType());
            responseData.put("storage_type", metadata.getStorageType());
            responseData.put("distance_metric", metadata.getDistanceMetric());
            responseData.put("created_at", metadata.getCreatedAt());
            responseData.put("status", "created");

            ctx.status(201).json(ApiResponse.success("Database created successfully", responseData));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to create database: " + e.getMessage()));
        }
    }

    /**
     * List all databases with pagination support.
     * GET /api/v1/databases?page=1&pageSize=20
     */
    public void listDatabases(Context ctx) {
        // Parse pagination parameters
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int pageSize = ctx.queryParamAsClass("pageSize", Integer.class)
            .getOrDefault(PaginationRequest.getDefaultPageSize());

        PaginationRequest pagination = new PaginationRequest(page, pageSize);

        // Get all databases
        List<Map<String, Object>> allDatabases = new ArrayList<>();

        for (String name : serverContext.getDatabaseNames()) {
            VectorDB db = serverContext.getDatabase(name);
            DatabaseMetadata metadata = serverContext.getMetadata(name);

            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("name", name);
            dbInfo.put("initialized", db.isInitialized());

            // Add metadata if available
            if (metadata != null) {
                dbInfo.put("dimensions", metadata.getDimensions());
                dbInfo.put("index_type", metadata.getIndexType());
                dbInfo.put("storage_type", metadata.getStorageType());
                dbInfo.put("distance_metric", metadata.getDistanceMetric());
                dbInfo.put("created_at", metadata.getCreatedAt());
            }

            // Add runtime stats
            try {
                Map<String, Object> stats = db.getStats();
                dbInfo.put("vector_count", stats.get("vector_count"));
                // Override dimensions from stats if different (shouldn't happen)
                if (metadata == null && stats.containsKey("dimensions")) {
                    dbInfo.put("dimensions", stats.get("dimensions"));
                }
            } catch (Exception e) {
                dbInfo.put("stats_error", "Unable to fetch stats");
            }

            allDatabases.add(dbInfo);
        }

        // Apply pagination
        int totalItems = allDatabases.size();
        int offset = pagination.getOffset();
        int limit = pagination.getLimit();

        List<Map<String, Object>> paginatedDatabases = allDatabases.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        // Create paginated response
        PaginatedResponse<Map<String, Object>> response =
            PaginatedResponse.of(paginatedDatabases, page, pageSize, totalItems);

        ctx.json(ApiResponse.success(response));
    }

    /**
     * Get database details.
     * GET /api/v1/databases/:name
     */
    public void getDatabase(Context ctx) {
        String name = ctx.pathParam("name");

        // Validate name
        if (!isValidDatabaseName(name)) {
            ctx.status(400).json(ApiResponse.error(INVALID_DB_NAME_MESSAGE));
            return;
        }

        VectorDB database = serverContext.getDatabase(name);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + name));
            return;
        }

        DatabaseMetadata metadata = serverContext.getMetadata(name);

        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("name", name);
        dbInfo.put("initialized", database.isInitialized());

        // Add metadata
        if (metadata != null) {
            dbInfo.putAll(metadata.toMap());
        }

        // Add runtime stats
        try {
            Map<String, Object> stats = database.getStats();
            dbInfo.putAll(stats);
            ctx.json(ApiResponse.success(dbInfo));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to get database info: " + e.getMessage()));
        }
    }

    /**
     * Delete a database.
     * DELETE /api/v1/databases/:name
     */
    public void deleteDatabase(Context ctx) {
        String name = ctx.pathParam("name");

        // Validate name
        if (!isValidDatabaseName(name)) {
            ctx.status(400).json(ApiResponse.error(INVALID_DB_NAME_MESSAGE));
            return;
        }

        VectorDB database = serverContext.removeDatabase(name);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + name));
            return;
        }

        try {
            database.close();
            ctx.json(ApiResponse.success("Database deleted successfully",
                Map.of("name", name, "status", "deleted")));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to delete database: " + e.getMessage()));
        }
    }

    /**
     * Get database statistics.
     * GET /api/v1/databases/:name/stats
     */
    public void getDatabaseStats(Context ctx) {
        String name = ctx.pathParam("name");

        // Validate name
        if (!isValidDatabaseName(name)) {
            ctx.status(400).json(ApiResponse.error(INVALID_DB_NAME_MESSAGE));
            return;
        }

        VectorDB database = serverContext.getDatabase(name);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + name));
            return;
        }

        try {
            Map<String, Object> stats = database.getStats();
            ctx.json(ApiResponse.success(stats));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }
}
