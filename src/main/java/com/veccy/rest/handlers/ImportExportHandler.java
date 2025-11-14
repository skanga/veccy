package com.veccy.rest.handlers;

import com.veccy.base.Page;
import com.veccy.base.VectorDB;
import com.veccy.client.VectorDBClient;
import com.veccy.rest.config.ServerContext;
import com.veccy.rest.dto.ApiResponse;
import com.veccy.rest.dto.ExportResponse;
import com.veccy.rest.dto.ImportRequest;
import com.veccy.rest.validation.InputValidator;
import com.veccy.storage.StorageBackend;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handler for import/export operations.
 */
public class ImportExportHandler {
    private static final Logger logger = LoggerFactory.getLogger(ImportExportHandler.class);
    private static final int DEFAULT_PAGE_SIZE = 1000;
    private static final int MAX_EXPORT_LIMIT = 100000;

    private final ServerContext serverContext;

    public ImportExportHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Import data into a database.
     * POST /api/v1/databases/:name/import
     *
     * Accepts JSON payload with array of vectors, each containing:
     * - id (optional): vector ID
     * - vector: array of doubles
     * - metadata (optional): key-value pairs
     */
    public void importData(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            ImportRequest request = ctx.bodyAsClass(ImportRequest.class);

            if (request.getVectors() == null || request.getVectors().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Vectors are required for import"));
                return;
            }

            logger.info("Importing {} vectors into database: {}", request.getVectors().size(), dbName);

            // Get expected dimensions
            int expectedDimensions = getExpectedDimensions(database);
            if (expectedDimensions <= -1) {
                 // If getExpectedDimensions returned -1, it means we couldn't determine the dimension.
                 // This is a server-side issue, so return 500.
                 ctx.status(500).json(ApiResponse.error("Failed to determine database dimensions. Please check server logs."));
                 return;
            }
            // Prepare vectors and metadata for insertion
            List<double[]> vectors = new ArrayList<>();
            List<Map<String, Object>> metadataList = new ArrayList<>();

            int vectorIndex = 0;
            for (ImportRequest.VectorData vectorData : request.getVectors()) {
                if (vectorData.getVector() == null) {
                    ctx.status(400).json(ApiResponse.error(
                        "Vector at index " + vectorIndex + " is null"));
                    return;
                }

                // Validate vector
                float[] floatVector = toFloatArray(vectorData.getVector());
                InputValidator.ValidationResult vectorValidation =
                    InputValidator.validateVector(floatVector, expectedDimensions);

                if (!vectorValidation.isValid()) {
                    ctx.status(400).json(ApiResponse.error(
                        "Vector at index " + vectorIndex + " validation failed: " +
                        vectorValidation.getErrorMessage()));
                    return;
                }

                // Validate metadata if present
                if (vectorData.getMetadata() != null) {
                    InputValidator.ValidationResult metadataValidation =
                        InputValidator.validateMetadata(vectorData.getMetadata());

                    if (!metadataValidation.isValid()) {
                        ctx.status(400).json(ApiResponse.error(
                            "Metadata at index " + vectorIndex + " validation failed: " +
                            metadataValidation.getErrorMessage()));
                        return;
                    }
                }

                vectors.add(vectorData.getVector());
                metadataList.add(vectorData.getMetadata());
                vectorIndex++;
            }

            // Insert vectors
            double[][] vectorArray = vectors.toArray(new double[0][]);
            List<String> insertedIds = database.insert(vectorArray, metadataList);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("imported_count", insertedIds.size());
            responseData.put("ids", insertedIds);
            responseData.put("database", dbName);

            logger.info("Successfully imported {} vectors into database: {}",
                insertedIds.size(), dbName);

            ctx.status(201).json(ApiResponse.success(
                "Successfully imported " + insertedIds.size() + " vectors", responseData));

        } catch (Exception e) {
            logger.error("Failed to import data into database {}: {}", dbName, e.getMessage(), e);
            ctx.status(500).json(ApiResponse.error("Failed to import data: " + e.getMessage()));
        }
    }

    /**
     * Export data from a database.
     * GET /api/v1/databases/:name/export?limit=1000
     *
     * Query parameters:
     * - limit (optional): maximum number of vectors to export (default: 1000, max: 100000)
     */
    public void exportData(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            // Get limit parameter
            String limitParam = ctx.queryParam("limit");
            int limit = DEFAULT_PAGE_SIZE;

            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam);
                    if (limit <= 0) {
                        ctx.status(400).json(ApiResponse.error("Limit must be positive"));
                        return;
                    }
                    if (limit > MAX_EXPORT_LIMIT) {
                        ctx.status(400).json(ApiResponse.error(
                            "Limit exceeds maximum allowed: " + MAX_EXPORT_LIMIT));
                        return;
                    }
                } catch (NumberFormatException e) {
                    ctx.status(400).json(ApiResponse.error("Invalid limit parameter"));
                    return;
                }
            }

            logger.info("Exporting data from database: {} (limit: {})", dbName, limit);

            // Check if database is VectorDBClient to access storage backend
            if (!(database instanceof VectorDBClient)) {
                ctx.status(500).json(ApiResponse.error(
                    "Database does not support export operation"));
                return;
            }

            VectorDBClient client = (VectorDBClient) database;
            StorageBackend storage = client.getStorageBackend();

            // Collect vectors
            List<ExportResponse.VectorData> exportedVectors = new ArrayList<>();
            int count = 0;

            Optional<String> cursor = Optional.empty();
            boolean hasMore = true;

            while (hasMore && count < limit) {
                int pageSize = Math.min(DEFAULT_PAGE_SIZE, limit - count);
                Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

                for (String id : page.items()) {
                    if (count >= limit) {
                        break;
                    }

                    // Retrieve vector and metadata
                    Optional<StorageBackend.VectorWithMetadata> dataOpt = storage.retrieveVector(id);

                    if (dataOpt.isPresent()) {
                        StorageBackend.VectorWithMetadata data = dataOpt.get();
                        ExportResponse.VectorData vectorData = new ExportResponse.VectorData(
                            id,
                            data.getVector(),
                            data.getMetadata()
                        );
                        exportedVectors.add(vectorData);
                        count++;
                    }
                }

                hasMore = page.hasMore() && count < limit;
                cursor = page.nextCursor();
            }

            // Build response
            ExportResponse exportResponse = new ExportResponse();
            exportResponse.setDatabase(dbName);
            exportResponse.setCount(count);
            exportResponse.setVectors(exportedVectors);
            exportResponse.setStats(database.getStats());

            logger.info("Successfully exported {} vectors from database: {}", count, dbName);

            ctx.status(200).json(ApiResponse.success(
                "Successfully exported " + count + " vectors", exportResponse));

        } catch (Exception e) {
            logger.error("Failed to export data from database {}: {}", dbName, e.getMessage(), e);
            ctx.status(500).json(ApiResponse.error("Failed to export data: " + e.getMessage()));
        }
    }

    /**
     * Helper method to convert double[] to float[] for validation.
     */
    private float[] toFloatArray(double[] doubleArray) {
        float[] floatArray = new float[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            floatArray[i] = (float) doubleArray[i];
        }
        return floatArray;
    }

    /**
     * Helper method to get expected dimensions from database stats.
     * Returns a positive integer for the dimension, 0 if the database is empty,
     * or -1 if the dimension cannot be determined.
     */
    private int getExpectedDimensions(VectorDB database) {
        try {
            Map<String, Object> stats = database.getStats();

            // Case 1: Check top-level dimensions field
            if (stats != null && stats.get("dimensions") instanceof Number dims) {
                int dimension = dims.intValue();
                if (dimension > 0) {
                    return dimension;
                }
            }

            // Case 2: Check nested index stats for dimensions
            if (stats != null && stats.get("index") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
                if (indexStats.get("dimensions") instanceof Number dims) {
                    int dimension = dims.intValue();
                    if (dimension > 0) {
                        return dimension;
                    }
                }
            }

            // Case 3: Try to infer dimensions from existing vectors (for VectorDBClient)
            if (database instanceof VectorDBClient client) {
                StorageBackend storage = client.getStorageBackend();
                // Get the first vector ID and retrieve its dimension
                Page<String> firstPage = client.listVectorIdsPaginated(1, Optional.empty());
                if (!firstPage.items().isEmpty()) {
                    String firstId = firstPage.items().get(0);
                    Optional<StorageBackend.VectorWithMetadata> vectorOpt = storage.retrieveVector(firstId);
                    if (vectorOpt.isPresent()) {
                        double[] vector = vectorOpt.get().getVector();
                        if (vector != null && vector.length > 0) {
                            return vector.length;
                        }
                    }
                }
            }

            // Case 4: Check if the DB is empty
            Object vectorCountObj = null;
            if (stats != null && stats.get("vector_count") instanceof Number) {
                vectorCountObj = stats.get("vector_count");
            } else if (stats != null && stats.get("index") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> indexStats = (Map<String, Object>) stats.get("index");
                if (indexStats.get("vector_count") instanceof Number) {
                    vectorCountObj = indexStats.get("vector_count");
                }
            }

            if (vectorCountObj instanceof Number count && count.longValue() == 0) {
                return 0; // Empty DB - first import will set the dimension
            }

            // Case 5: Failure - The DB is not empty, but we could not determine the dimension
            logger.warn("Could not determine vector dimensions from non-empty database. Stats content: {}", stats);
            return -1;

        } catch (Exception e) {
            logger.error("Exception while retrieving database stats", e);
            return -1;
        }
    }
}
