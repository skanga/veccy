package com.veccy.rest.middleware;

import com.veccy.rest.metrics.MetricsCollector;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

/**
 * Middleware that collects metrics for each request.
 */
public class MetricsMiddleware implements Handler {
    private final MetricsCollector metricsCollector;

    public MetricsMiddleware(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Record request start time
        long startTime = System.currentTimeMillis();
        ctx.attribute("metrics-start-time", startTime);

        // Process the request (this doesn't actually process, just sets up for after)
        // In Javalin 6, we need to use an after handler to capture response info
    }

    /**
     * Record metrics after the request is processed.
     * Use this as an "after" handler.
     */
    public void recordAfter(Context ctx) {
        if (ctx == null) {
            return;
        }
        Long startTime = ctx.attribute("metrics-start-time");
        if (startTime != null) {
            long responseTime = System.currentTimeMillis() - startTime;
            int statusCode = ctx.status().getCode();
            String method = ctx.method().name();
            String path = ctx.path();

            metricsCollector.recordRequest(method, path, statusCode, responseTime);
        }
    }
}
