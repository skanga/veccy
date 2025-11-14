package com.veccy.rest.middleware;

import com.veccy.rest.dto.ApiResponse;
import io.javalin.http.Context;
import io.javalin.http.ExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the REST API.
 * Provides secure error messages in production while maintaining detailed logging.
 */
public class VeccyExceptionHandler implements ExceptionHandler<Exception> {

    private final boolean productionMode;
    private final Logger logger;

    public VeccyExceptionHandler(boolean productionMode) {
        this.productionMode = productionMode;
        this.logger = LoggerFactory.getLogger(VeccyExceptionHandler.class);
    }

    public VeccyExceptionHandler(boolean productionMode, Logger logger) {
        this.productionMode = productionMode;
        this.logger = logger;
    }

    @Override
    public void handle(@NotNull Exception e, @NotNull Context ctx) {
        // Get correlation ID from context (set by RequestLogger)
        String correlationId = ctx.attribute("correlationId");
        if (correlationId == null) {
            correlationId = "unknown";
        }

        // Log error with correlation ID and full stack trace
        this.logger.error("Error handling request [correlation-id={}] path={} method={} ip={}",
            correlationId, ctx.path(), ctx.method(), ctx.ip(), e);

        // Determine status code based on exception type
        int statusCode = determineStatusCode(e);

        // Build error response based on environment
        ApiResponse<Map<String, Object>> response;

        if (productionMode) {
            // Production: Generic error message with correlation ID only
            response = buildProductionErrorResponse(correlationId, statusCode);
        } else {
            // Development: Detailed error information
            response = buildDevelopmentErrorResponse(e, correlationId, ctx, statusCode);
        }

        ctx.status(statusCode);
        ctx.json(response);
    }

    /**
     * Build production-safe error response with minimal information.
     */
    private ApiResponse<Map<String, Object>> buildProductionErrorResponse(String correlationId, int statusCode) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("correlation_id", correlationId);

        String genericMessage = getGenericMessageForStatus(statusCode);

        ApiResponse<Map<String, Object>> response = ApiResponse.error(genericMessage);
        response.setData(errorDetails);

        return response;
    }

    /**
     * Build development error response with detailed information.
     */
    private ApiResponse<Map<String, Object>> buildDevelopmentErrorResponse(
            Exception e, String correlationId, Context ctx, int statusCode) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("correlation_id", correlationId);
        errorDetails.put("exception_type", e.getClass().getSimpleName());
        errorDetails.put("exception_class", e.getClass().getName());
        errorDetails.put("path", ctx.path());
        errorDetails.put("method", ctx.method().toString());

        // Include stack trace in development
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace != null && stackTrace.length > 0) {
            errorDetails.put("stack_trace_top", stackTrace[0].toString());
        }

        // Cause chain
        if (e.getCause() != null) {
            errorDetails.put("cause", e.getCause().getClass().getSimpleName());
            errorDetails.put("cause_message", e.getCause().getMessage());
        }

        String errorMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";

        ApiResponse<Map<String, Object>> response = ApiResponse.error(errorMessage);
        response.setData(errorDetails);

        return response;
    }

    /**
     * Get generic error message based on status code.
     */
    private String getGenericMessageForStatus(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Bad request. Please check your input.";
            case 401:
                return "Authentication required.";
            case 403:
                return "Access forbidden.";
            case 404:
                return "Resource not found.";
            case 409:
                return "Resource conflict.";
            case 429:
                return "Too many requests. Please try again later.";
            case 500:
            default:
                return "An internal error occurred. Please contact support with the correlation ID.";
        }
    }

    private int determineStatusCode(Exception e) {
        String exceptionName = e.getClass().getSimpleName().toLowerCase();

        if (exceptionName.contains("notfound")) {
            return 404;
        } else if (exceptionName.contains("illegal") || exceptionName.contains("invalid")) {
            return 400;
        } else if (exceptionName.contains("unauthorized") || exceptionName.contains("forbidden")) {
            return 403;
        } else if (exceptionName.contains("conflict")) {
            return 409;
        } else {
            return 500;
        }
    }
}
