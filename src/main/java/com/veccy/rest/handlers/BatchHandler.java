package com.veccy.rest.handlers;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.rest.config.ServerContext;
import com.veccy.rest.dto.ApiResponse;
import com.veccy.rest.dto.BatchDeleteRequest;
import com.veccy.rest.dto.BatchSearchRequest;
import com.veccy.rest.dto.InsertVectorsRequest;
import com.veccy.rest.validation.InputValidator;
import io.javalin.http.Context;

import java.util.*;

/**
 * Handler for batch vector operations with comprehensive input validation.
 */
public class BatchHandler {
    private final ServerContext serverContext;

    public BatchHandler(ServerContext serverContext) {
        this.serverContext = serverContext;
    }

    /**
     * Convert double[] to float[] for validation.
     */
    private float[] toFloatArray(double[] doubleArray) {
        float[] floatArray = new float[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            floatArray[i] = (float) doubleArray[i];
        }
        return floatArray;
    }

    /**
     * Convert List<double[]> to List<float[]> for validation.
     */
    private List<float[]> toFloatList(List<double[]> doubleList) {
        List<float[]> floatList = new ArrayList<>();
        for (double[] doubles : doubleList) {
            floatList.add(toFloatArray(doubles));
        }
        return floatList;
    }

    /**
     * Get expected dimensions from database stats, or 0 if not available.
     */
    private int getExpectedDimensions(VectorDB database) {
        try {
            Map<String, Object> stats = database.getStats();
            Object dims = stats.get("dimensions");
            if (dims instanceof Number) {
                return ((Number) dims).intValue();
            }
        } catch (Exception e) {
            // Ignore - database might be empty
        }
        return 0;
    }

    /**
     * Batch insert vectors.
     * POST /api/v1/databases/:name/vectors/batch
     */
    public void batchInsert(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            InsertVectorsRequest request = ctx.bodyAsClass(InsertVectorsRequest.class);

            if (request.getVectors() == null || request.getVectors().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Vectors are required"));
                return;
            }

            // TRANSACTION SAFETY: Validate ALL inputs BEFORE performing any database operations
            // This ensures we don't end up with partial inserts if validation fails midway

            // Get expected dimensions
            int expectedDimensions = getExpectedDimensions(database);

            // Validate all vectors in the batch
            List<float[]> floatVectors = toFloatList(request.getVectors());
            InputValidator.ValidationResult vectorValidation =
                InputValidator.validateVectorBatch(floatVectors, expectedDimensions);

            if (!vectorValidation.isValid()) {
                ctx.status(400).json(ApiResponse.error("Batch vector validation failed: " +
                    vectorValidation.getErrorMessage()));
                return;
            }

            // Validate all metadata entries if present
            if (request.getMetadata() != null) {
                // Check that vector count matches metadata count
                if (request.getMetadata().size() != request.getVectors().size()) {
                    ctx.status(400).json(ApiResponse.error(
                        String.format("Vector count (%d) must match metadata count (%d)",
                            request.getVectors().size(), request.getMetadata().size())));
                    return;
                }

                // Validate each metadata entry
                for (int i = 0; i < request.getMetadata().size(); i++) {
                    Map<String, Object> metadata = request.getMetadata().get(i);
                    InputValidator.ValidationResult metadataValidation =
                        InputValidator.validateMetadata(metadata);

                    if (!metadataValidation.isValid()) {
                        ctx.status(400).json(ApiResponse.error(
                            String.format("Metadata validation failed at index %d: %s",
                                i, metadataValidation.getErrorMessage())));
                        return;
                    }
                }
            }

            // ALL VALIDATIONS PASSED - Now perform the database operation

            double[][] vectors = request.getVectors().toArray(new double[0][]);
            List<String> ids = database.insert(vectors, request.getMetadata());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("ids", ids);
            responseData.put("count", ids.size());
            responseData.put("operation", "batch_insert");

            ctx.status(201).json(ApiResponse.success("Batch insert completed successfully", responseData));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to batch insert vectors: " + e.getMessage()));
        }
    }

    /**
     * Batch search for similar vectors.
     * POST /api/v1/databases/:name/vectors/batch-search
     */
    public void batchSearch(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            BatchSearchRequest request = ctx.bodyAsClass(BatchSearchRequest.class);

            if (request.getQueryVectors() == null || request.getQueryVectors().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Query vectors are required"));
                return;
            }

            int k = request.getK() > 0 ? request.getK() : 10;

            // Get expected dimensions
            int expectedDimensions = getExpectedDimensions(database);

            // Validate all query vectors
            List<float[]> floatVectors = toFloatList(request.getQueryVectors());
            InputValidator.ValidationResult vectorValidation =
                InputValidator.validateVectorBatch(floatVectors, expectedDimensions);

            if (!vectorValidation.isValid()) {
                ctx.status(400).json(ApiResponse.error("Query vector validation failed: " +
                    vectorValidation.getErrorMessage()));
                return;
            }

            // Validate k
            if (k <= 0 || k > InputValidator.getMaxSearchK()) {
                ctx.status(400).json(ApiResponse.error("Invalid k value. Must be between 1 and " +
                    InputValidator.getMaxSearchK()));
                return;
            }

            List<List<Map<String, Object>>> allResults = new ArrayList<>();

            for (double[] queryVector : request.getQueryVectors()) {
                List<SearchResult> results = database.search(queryVector, k);

                List<Map<String, Object>> resultMaps = new ArrayList<>();
                for (SearchResult result : results) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", result.getId());
                    map.put("distance", result.getDistance());
                    map.put("metadata", result.getMetadata());
                    resultMaps.add(map);
                }

                allResults.add(resultMaps);
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("results", allResults);
            responseData.put("query_count", request.getQueryVectors().size());
            responseData.put("k", k);

            ctx.json(ApiResponse.success(responseData));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to batch search vectors: " + e.getMessage()));
        }
    }

    /**
     * Batch delete vectors.
     * DELETE /api/v1/databases/:name/vectors/batch
     */
    public void batchDelete(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            BatchDeleteRequest request = ctx.bodyAsClass(BatchDeleteRequest.class);

            if (request.getIds() == null || request.getIds().isEmpty()) {
                ctx.status(400).json(ApiResponse.error("Vector IDs are required"));
                return;
            }

            List<String> ids = request.getIds();

            // Validate each ID
            for (String id : ids) {
                InputValidator.ValidationResult idValidation = InputValidator.validateVectorId(id);
                if (!idValidation.isValid()) {
                    ctx.status(400).json(ApiResponse.error("Invalid vector ID '" + id + "': " +
                        idValidation.getErrorMessage()));
                    return;
                }
            }

            boolean success = database.delete(ids);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", success);
            responseData.put("count", ids.size());
            responseData.put("operation", "batch_delete");

            if (success) {
                ctx.json(ApiResponse.success("Batch delete completed successfully", responseData));
            } else {
                ctx.status(500).json(ApiResponse.error("Some vectors could not be deleted"));
            }
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to batch delete vectors: " + e.getMessage()));
        }
    }
}
