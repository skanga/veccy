package com.veccy.rest.middleware;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

/**
 * Middleware that adds API version headers to responses.
 * This enables clients to discover the API version and plan for migrations.
 */
public class ApiVersionInterceptor implements Handler {
    private static final String API_VERSION = "1.0.0";
    private static final String MIN_SUPPORTED_VERSION = "1.0.0";
    private static final String DEPRECATION_NOTICE = null;  // Set when version is deprecated

    @Override
    public void handle(@NotNull Context ctx) {
        // Add version headers to response
        ctx.header("X-API-Version", API_VERSION);
        ctx.header("X-API-Min-Version", MIN_SUPPORTED_VERSION);

        // Add deprecation notice if this version is being phased out
        if (DEPRECATION_NOTICE != null) {
            ctx.header("X-API-Deprecation", DEPRECATION_NOTICE);
        }

        // Optionally validate client's requested version
        String requestedVersion = ctx.header("Accept-Version");
        if (requestedVersion != null && !isVersionSupported(requestedVersion)) {
            ctx.status(406).json(java.util.Map.of(
                "error", true,
                "message", "API version " + requestedVersion + " is not supported",
                "current_version", API_VERSION,
                "min_supported_version", MIN_SUPPORTED_VERSION
            ));
        }
    }

    /**
     * Check if a requested version is supported.
     */
    private boolean isVersionSupported(String version) {
        // For now, we only support version 1.0.0
        // In future, implement proper semver comparison
        return "1.0.0".equals(version) || "1".equals(version);
    }
}
