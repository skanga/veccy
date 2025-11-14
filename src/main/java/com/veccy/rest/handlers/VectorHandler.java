package com.veccy.rest.handlers;

import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.rest.config.ServerContext;
import com.veccy.rest.dto.ApiResponse;
import com.veccy.rest.dto.InsertVectorsRequest;
import com.veccy.rest.dto.PaginatedResponse;
import com.veccy.rest.dto.PaginationRequest;
import com.veccy.rest.dto.SearchRequest;
import com.veccy.rest.dto.UpdateVectorRequest;
import com.veccy.rest.validation.InputValidator;
import io.javalin.http.Context;

import java.util.*;

/**
 * Handler for vector CRUD operations with comprehensive input validation.
 */
public class VectorHandler {
    private final ServerContext serverContext;

    public VectorHandler(ServerContext serverContext) {
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
     * Insert vectors into a database.
     * POST /api/v1/databases/:name/vectors
     */
    public void insertVectors(Context ctx) {
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

            // Get expected dimensions
            int expectedDimensions = getExpectedDimensions(database);

            // Validate vectors
            List<float[]> floatVectors = toFloatList(request.getVectors());
            InputValidator.ValidationResult vectorValidation =
                InputValidator.validateVectorBatch(floatVectors, expectedDimensions);

            if (!vectorValidation.isValid()) {
                ctx.status(400).json(ApiResponse.error("Vector validation failed: " +
                    vectorValidation.getErrorMessage()));
                return;
            }

            // Validate metadata if present
            if (request.getMetadata() != null) {
                for (Map<String, Object> metadata : request.getMetadata()) {
                    InputValidator.ValidationResult metadataValidation =
                        InputValidator.validateMetadata(metadata);

                    if (!metadataValidation.isValid()) {
                        ctx.status(400).json(ApiResponse.error("Metadata validation failed: " +
                            metadataValidation.getErrorMessage()));
                        return;
                    }
                }
            }

            // Convert List<double[]> to double[][]
            double[][] vectors = request.getVectors().toArray(new double[0][]);

            List<String> ids = database.insert(vectors, request.getMetadata());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("ids", ids);
            responseData.put("count", ids.size());

            ctx.status(201).json(ApiResponse.success("Vectors inserted successfully", responseData));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to insert vectors: " + e.getMessage()));
        }
    }

    /**
     * Search for similar vectors.
     * GET /api/v1/databases/:name/vectors/search
     */
    public void searchVectors(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            SearchRequest request = ctx.bodyAsClass(SearchRequest.class);

            if (request.getQueryVector() == null) {
                ctx.status(400).json(ApiResponse.error("Query vector is required"));
                return;
            }

            int k = request.getK() > 0 ? request.getK() : 10;

            // Get expected dimensions and validate search params
            int expectedDimensions = getExpectedDimensions(database);
            float[] queryVectorFloat = toFloatArray(request.getQueryVector());

            InputValidator.ValidationResult searchValidation =
                InputValidator.validateSearchParams(queryVectorFloat, k, expectedDimensions);

            if (!searchValidation.isValid()) {
                ctx.status(400).json(ApiResponse.error("Search validation failed: " +
                    searchValidation.getErrorMessage()));
                return;
            }

            List<SearchResult> results = database.search(request.getQueryVector(), k);

            // Convert SearchResult objects to maps for JSON serialization
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (SearchResult result : results) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", result.getId());
                map.put("distance", result.getDistance());
                map.put("metadata", result.getMetadata());
                resultMaps.add(map);
            }

            ctx.json(ApiResponse.success(resultMaps));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to search vectors: " + e.getMessage()));
        }
    }

    /**
     * Get a specific vector by ID.
     * GET /api/v1/databases/:name/vectors/:id
     */
    public void getVector(Context ctx) {
        String dbName = ctx.pathParam("name");
        String vectorId = ctx.pathParam("id");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        // Validate vector ID
        InputValidator.ValidationResult idValidation = InputValidator.validateVectorId(vectorId);
        if (!idValidation.isValid()) {
            ctx.status(400).json(ApiResponse.error("Invalid vector ID: " +
                idValidation.getErrorMessage()));
            return;
        }

        try {
            // Note: VectorDB interface doesn't have a direct get method
            // This would need to be implemented or we could return a message
            ctx.status(501).json(ApiResponse.error("Get vector by ID not yet implemented"));
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to get vector: " + e.getMessage()));
        }
    }

    /**
     * Update a vector.
     * PUT /api/v1/databases/:name/vectors/:id
     */
    public void updateVector(Context ctx) {
        String dbName = ctx.pathParam("name");
        String vectorId = ctx.pathParam("id");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        // Validate vector ID
        InputValidator.ValidationResult idValidation = InputValidator.validateVectorId(vectorId);
        if (!idValidation.isValid()) {
            ctx.status(400).json(ApiResponse.error("Invalid vector ID: " +
                idValidation.getErrorMessage()));
            return;
        }

        try {
            UpdateVectorRequest request = ctx.bodyAsClass(UpdateVectorRequest.class);

            if (request.getVector() == null) {
                ctx.status(400).json(ApiResponse.error("Vector data is required"));
                return;
            }

            // Get expected dimensions and validate vector
            int expectedDimensions = getExpectedDimensions(database);
            float[] vectorFloat = toFloatArray(request.getVector());

            InputValidator.ValidationResult vectorValidation =
                InputValidator.validateVector(vectorFloat, expectedDimensions);

            if (!vectorValidation.isValid()) {
                ctx.status(400).json(ApiResponse.error("Vector validation failed: " +
                    vectorValidation.getErrorMessage()));
                return;
            }

            // Validate metadata if present
            if (request.getMetadata() != null) {
                InputValidator.ValidationResult metadataValidation =
                    InputValidator.validateMetadata(request.getMetadata());

                if (!metadataValidation.isValid()) {
                    ctx.status(400).json(ApiResponse.error("Metadata validation failed: " +
                        metadataValidation.getErrorMessage()));
                    return;
                }
            }

            boolean success = database.update(vectorId, request.getVector(), request.getMetadata());

            if (success) {
                ctx.json(ApiResponse.success("Vector updated successfully",
                    Map.of("id", vectorId, "status", "updated")));
            } else {
                ctx.status(404).json(ApiResponse.error("Vector not found: " + vectorId));
            }
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to update vector: " + e.getMessage()));
        }
    }

    /**
     * Delete a vector.
     * DELETE /api/v1/databases/:name/vectors/:id
     */
    public void deleteVector(Context ctx) {
        String dbName = ctx.pathParam("name");
        String vectorId = ctx.pathParam("id");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        // Validate vector ID
        InputValidator.ValidationResult idValidation = InputValidator.validateVectorId(vectorId);
        if (!idValidation.isValid()) {
            ctx.status(400).json(ApiResponse.error("Invalid vector ID: " +
                idValidation.getErrorMessage()));
            return;
        }

        try {
            boolean success = database.delete(Collections.singletonList(vectorId));

            if (success) {
                ctx.json(ApiResponse.success("Vector deleted successfully",
                    Map.of("id", vectorId, "status", "deleted")));
            } else {
                ctx.status(404).json(ApiResponse.error("Vector not found: " + vectorId));
            }
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to delete vector: " + e.getMessage()));
        }
    }

    /**
     * List vectors (with pagination).
     * GET /api/v1/databases/:name/vectors?page=1&amp;pageSize=20
     *
     * @param ctx the Javalin context
     */
    public void listVectors(Context ctx) {
        String dbName = ctx.pathParam("name");
        VectorDB database = serverContext.getDatabase(dbName);

        if (database == null) {
            ctx.status(404).json(ApiResponse.error("Database not found: " + dbName));
            return;
        }

        try {
            // Parse pagination parameters from query string
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int pageSize = ctx.queryParamAsClass("pageSize", Integer.class)
                .getOrDefault(PaginationRequest.getDefaultPageSize());

            PaginationRequest pagination = new PaginationRequest(page, pageSize);

            // Note: VectorDB interface doesn't have a list method with pagination
            // For now, return information about the limitation with stats
            Map<String, Object> stats = database.getStats();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Vector listing requires VectorDB interface enhancement. Use search with empty filter as workaround.");
            response.put("stats", stats);
            response.put("pagination", Map.of(
                "page", pagination.getPage(),
                "pageSize", pagination.getPageSize(),
                "note", "Pagination will be available once VectorDB.list() method is implemented"
            ));

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.error("List operation not yet implemented");
            apiResponse.setData(response);
            ctx.status(501).json(apiResponse);
        } catch (Exception e) {
            ctx.status(500).json(ApiResponse.error("Failed to list vectors: " + e.getMessage()));
        }
    }
}
