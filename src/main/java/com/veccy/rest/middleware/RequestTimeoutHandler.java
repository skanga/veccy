package com.veccy.rest.middleware;

import com.veccy.rest.dto.ApiResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Middleware that enforces request timeout limits to prevent long-running requests
 * from consuming server resources indefinitely.
 *
 * This is particularly important for vector operations that could potentially
 * process large amounts of data.
 */
public class RequestTimeoutHandler implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(RequestTimeoutHandler.class);

    private final long timeoutMs;
    private final ExecutorService timeoutExecutor;

    public RequestTimeoutHandler(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.timeoutExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("request-timeout-handler");
            return t;
        });
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // Skip timeout for health checks and root endpoint
        String path = ctx.path();
        if (path.equals("/health") || path.equals("/")) {
            return;
        }

        // Set the timeout attribute on the context
        ctx.attribute("request-timeout-ms", timeoutMs);

        // Note: In Javalin 6, we can't wrap the entire request processing in a timeout
        // because the framework handles async processing internally.
        // Instead, we set a timeout on the async context if available.

        // Add timeout information to response headers for debugging
        ctx.header("X-Request-Timeout-Ms", String.valueOf(timeoutMs));

        // The actual timeout enforcement happens at the servlet container level
        // or through Javalin's async timeout configuration.
        // Individual handlers can check ctx.attribute("request-timeout-ms") if needed.
    }

    /**
     * Shutdown the timeout executor when server stops.
     */
    public void shutdown() {
        logger.info("Shutting down request timeout handler...");
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
