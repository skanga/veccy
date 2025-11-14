package com.veccy.rest.middleware;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Middleware to validate Content-Type header for POST/PUT/PATCH requests.
 */
public class ContentTypeValidator implements Handler {
    private static final List<String> METHODS_REQUIRING_CONTENT_TYPE = List.of("POST", "PUT", "PATCH");

    @Override
    public void handle(@NotNull Context ctx) {
        String method = ctx.method().toString();

        if (METHODS_REQUIRING_CONTENT_TYPE.contains(method)) {
            String contentType = ctx.contentType();

            if (contentType == null || !contentType.contains("application/json")) {
                ctx.status(415).json(Map.of(
                    "error", true,
                    "message", "Content-Type must be application/json",
                    "received", contentType != null ? contentType : "none"
                ));
            }
        }
    }
}
