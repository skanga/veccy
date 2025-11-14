package com.veccy.rest.middleware;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Enhanced middleware for logging HTTP requests with correlation IDs, timing, and status codes.
 */
public class RequestLogger implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(RequestLogger.class);
    private static final int MAX_BODY_LOG_LENGTH = 500;

    private final boolean logRequestBodies;
    private final boolean logResponseBodies;

    public RequestLogger() {
        this(false, false);
    }

    public RequestLogger(boolean logRequestBodies, boolean logResponseBodies) {
        this.logRequestBodies = logRequestBodies;
        this.logResponseBodies = logResponseBodies;
    }

    @Override
    public void handle(@NotNull Context ctx) {
        long startTime = System.currentTimeMillis();

        // Generate correlation ID for request tracking
        String correlationId = UUID.randomUUID().toString();
        ctx.attribute("correlationId", correlationId);
        ctx.attribute("startTime", startTime);

        // Add correlation ID to response headers
        ctx.header("X-Correlation-ID", correlationId);

        // Log request with correlation ID
        String method = ctx.method().toString();
        String path = ctx.path();
        String ip = ctx.ip();
        String userAgent = ctx.userAgent() != null ? ctx.userAgent() : "unknown";

        logger.info("→ {} {} from {} user-agent={} [correlation-id={}]",
            method, path, ip, truncate(userAgent, 100), correlationId);

        // Optionally log request body (for debugging)
        if (logRequestBodies && ctx.body() != null && !ctx.body().isEmpty()) {
            String body = sanitize(ctx.body());
            logger.debug("→ Request body [correlation-id={}]: {}",
                correlationId, truncate(body, MAX_BODY_LOG_LENGTH));
        }

        // Response logging happens after the handler completes
        // We can't easily access response details in Javalin 6 middleware
        // So we just log the request for now
        // Response logging would require using a response wrapper or result interceptor
    }

    /**
     * Truncate string to max length with ellipsis.
     */
    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * Sanitize log output by removing potential sensitive data patterns.
     */
    private String sanitize(String str) {
        if (str == null) {
            return null;
        }

        // Remove potential API keys, passwords, tokens
        str = str.replaceAll("\"api[_-]?key\"\\s*:\\s*\"[^\"]+\"", "\"api_key\":\"***\"");
        str = str.replaceAll("\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"***\"");
        str = str.replaceAll("\"token\"\\s*:\\s*\"[^\"]+\"", "\"token\":\"***\"");
        str = str.replaceAll("\"secret\"\\s*:\\s*\"[^\"]+\"", "\"secret\":\"***\"");

        return str;
    }
}
