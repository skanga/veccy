package com.veccy.rest.middleware;

import com.veccy.rest.dto.ApiResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Token bucket based rate limiter middleware.
 * Limits requests per IP address to prevent abuse.
 */
public class RateLimiter implements Handler {
    private final Logger logger;
    private final boolean enabled;
    private final int maxRequestsPerMinute;
    private final Map<String, TokenBucket> buckets;
    private final ScheduledExecutorService cleanupExecutor;

    /**
     * Token bucket for individual client.
     */
    private static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile long lastAccessTime;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastAccessTime = System.currentTimeMillis();
        }

        public boolean tryConsume() {
            lastAccessTime = System.currentTimeMillis();
            return tokens.getAndUpdate(current -> current > 0 ? current - 1 : 0) > 0;
        }

        public void refill() {
            tokens.set(capacity);
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public int getTokens() {
            return tokens.get();
        }
    }

    public RateLimiter(Map<String, Object> securityConfig) {
        this(securityConfig, LoggerFactory.getLogger(RateLimiter.class));
    }

    public RateLimiter(Map<String, Object> securityConfig, Logger logger) {
        this.logger = logger;
        this.enabled = (boolean) securityConfig.getOrDefault("rateLimitEnabled", false);
        this.maxRequestsPerMinute = ((Number) securityConfig.getOrDefault("maxRequestsPerMinute", 100)).intValue();
        this.buckets = new ConcurrentHashMap<>();

        if (enabled) {
            // Refill tokens every minute
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
            this.cleanupExecutor.scheduleAtFixedRate(this::refillBuckets, 1, 1, TimeUnit.MINUTES);

            // Cleanup old buckets every 5 minutes
            this.cleanupExecutor.scheduleAtFixedRate(this::cleanupOldBuckets, 5, 5, TimeUnit.MINUTES);

            logger.info("Rate limiting enabled: {} requests per minute per IP", maxRequestsPerMinute);
        } else {
            this.cleanupExecutor = null;
            logger.info("Rate limiting disabled");
        }
    }

    @Override
    public void handle(@NotNull Context ctx) {
        if (!enabled) {
            return;
        }

        String clientIp = ctx.ip();
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, k -> new TokenBucket(maxRequestsPerMinute));

        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for IP: {} on {} {}", clientIp, ctx.method(), ctx.path());

            // Add rate limit headers
            ctx.header("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
            ctx.header("X-RateLimit-Remaining", "0");
            ctx.header("Retry-After", "60");

            ctx.status(429).json(ApiResponse.error(
                "Rate limit exceeded. Maximum " + maxRequestsPerMinute + " requests per minute allowed."
            ));
            return;
        }

        // Add rate limit headers for successful requests
        ctx.header("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        int remaining = bucket.getTokens();
        if (remaining > 0) {
            ctx.header("X-RateLimit-Remaining", String.valueOf(remaining));
        }
    }

    /**
     * Refill all token buckets.
     */
    private void refillBuckets() {
        int refilled = 0;
        for (TokenBucket bucket : buckets.values()) {
            bucket.refill();
            refilled++;
        }
        logger.debug("Refilled {} token buckets", refilled);
    }

    /**
     * Remove buckets that haven't been accessed in the last 10 minutes.
     */
    private void cleanupOldBuckets() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10);
        int initialSize = buckets.size();

        buckets.entrySet().removeIf(entry ->
            entry.getValue().getLastAccessTime() < cutoffTime
        );

        int removed = initialSize - buckets.size();
        if (removed > 0) {
            logger.debug("Cleaned up {} inactive rate limit buckets", removed);
        }
    }

    /**
     * Shutdown the cleanup executor.
     */
    public void shutdown() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdown();
            try {
                if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                cleanupExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Rate limiter shutdown complete");
        }
    }

    /**
     * Get current rate limit stats.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "enabled", enabled,
            "maxRequestsPerMinute", maxRequestsPerMinute,
            "activeClients", buckets.size()
        );
    }

    public boolean isEnabled() {
        return enabled;
    }
}
