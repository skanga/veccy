package com.veccy.rest.middleware;

import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RateLimiter middleware.
 */
class RateLimiterTest {

    private RateLimiter rateLimiter;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
    }

    @AfterEach
    void tearDown() {
        if (rateLimiter != null) {
            rateLimiter.shutdown();
        }
    }

    @Test
    void testConstructorWithDisabledRateLimiting() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", false);

        rateLimiter = new RateLimiter(config, logger);

        assertFalse(rateLimiter.isEnabled());
        Map<String, Object> stats = rateLimiter.getStats();
        assertFalse((Boolean) stats.get("enabled"));
    }

    @Test
    void testConstructorWithEnabledRateLimiting() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 50);

        rateLimiter = new RateLimiter(config, logger);

        assertTrue(rateLimiter.isEnabled());
        Map<String, Object> stats = rateLimiter.getStats();
        assertTrue((Boolean) stats.get("enabled"));
        assertEquals(50, stats.get("maxRequestsPerMinute"));
    }

    @Test
    void testConstructorWithDefaultValues() {
        Map<String, Object> config = new HashMap<>();
        // rateLimitEnabled defaults to false
        // maxRequestsPerMinute defaults to 100

        rateLimiter = new RateLimiter(config, logger);

        assertFalse(rateLimiter.isEnabled());
        Map<String, Object> stats = rateLimiter.getStats();
        assertEquals(100, stats.get("maxRequestsPerMinute"));
    }

    @Test
    void testHandleWithDisabledRateLimiting() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", false);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("192.168.1.1");

        // Should not throw and should not set any headers
        rateLimiter.handle(ctx);

        // Verify no status or headers were set
        verify(ctx, never()).status(anyInt());
        verify(ctx, never()).header(anyString(), anyString());
        verify(ctx, never()).json(any());
    }

    @Test
    void testHandleWithEnabledRateLimitingAllowsRequests() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 10);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("192.168.1.1");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx.path()).thenReturn("/test");

        // First request should succeed
        rateLimiter.handle(ctx);

        // Verify rate limit headers were added
        verify(ctx).header("X-RateLimit-Limit", "10");
        verify(ctx).header(eq("X-RateLimit-Remaining"), anyString());
        verify(ctx, never()).status(429);
    }

    @Test
    void testHandleExceedsRateLimit() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 3);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("192.168.1.1");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx.path()).thenReturn("/test");
        when(ctx.status(anyInt())).thenReturn(ctx);

        // Consume all tokens (3 requests)
        for (int i = 0; i < 3; i++) {
            rateLimiter.handle(ctx);
        }

        // Fourth request should be rate limited
        rateLimiter.handle(ctx);

        verify(ctx).status(429);
        verify(ctx).header("X-RateLimit-Remaining", "0");
        verify(ctx).header("Retry-After", "60");
        verify(ctx).json(any());
    }

    @Test
    void testHandleDifferentIpsGetSeparateBuckets() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 2);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx1 = mock(Context.class);
        when(ctx1.ip()).thenReturn("192.168.1.1");
        when(ctx1.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx1.path()).thenReturn("/test");

        Context ctx2 = mock(Context.class);
        when(ctx2.ip()).thenReturn("192.168.1.2");
        when(ctx2.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx2.path()).thenReturn("/test");

        // Exhaust IP1's tokens
        rateLimiter.handle(ctx1);
        rateLimiter.handle(ctx1);

        // IP2 should still have tokens
        rateLimiter.handle(ctx2);

        verify(ctx1, never()).status(429);
        verify(ctx2, never()).status(429);

        Map<String, Object> stats = rateLimiter.getStats();
        assertEquals(2, stats.get("activeClients"));
    }

    @Test
    void testGetStatsReturnsCorrectValues() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 75);

        rateLimiter = new RateLimiter(config, logger);

        Map<String, Object> stats = rateLimiter.getStats();

        assertNotNull(stats);
        assertTrue((Boolean) stats.get("enabled"));
        assertEquals(75, stats.get("maxRequestsPerMinute"));
        assertEquals(0, stats.get("activeClients"));
    }

    @Test
    void testGetStatsAfterRequests() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 10);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("192.168.1.1");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx.path()).thenReturn("/test");

        rateLimiter.handle(ctx);

        Map<String, Object> stats = rateLimiter.getStats();
        assertEquals(1, stats.get("activeClients"));
    }

    @Test
    void testShutdownWithDisabledRateLimiter() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", false);

        rateLimiter = new RateLimiter(config, logger);

        // Should not throw
        assertDoesNotThrow(() -> rateLimiter.shutdown());
    }

    @Test
    void testShutdownWithEnabledRateLimiter() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 10);

        rateLimiter = new RateLimiter(config, logger);

        // Should not throw and should complete quickly
        assertDoesNotThrow(() -> rateLimiter.shutdown());
    }

    @Test
    void testShutdownIsIdempotent() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 10);

        rateLimiter = new RateLimiter(config, logger);

        rateLimiter.shutdown();
        rateLimiter.shutdown(); // Should not throw
        rateLimiter.shutdown(); // Should not throw
    }

    @Test
    void testIsEnabled() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);

        rateLimiter = new RateLimiter(config, logger);

        assertTrue(rateLimiter.isEnabled());
    }

    @Test
    void testIsDisabled() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", false);

        rateLimiter = new RateLimiter(config, logger);

        assertFalse(rateLimiter.isEnabled());
    }

    @Test
    void testRateLimitHeadersShowRemainingTokens() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", 5);

        rateLimiter = new RateLimiter(config, logger);

        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("192.168.1.1");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx.path()).thenReturn("/test");

        // First request
        rateLimiter.handle(ctx);
        verify(ctx).header("X-RateLimit-Limit", "5");
        verify(ctx).header("X-RateLimit-Remaining", "4");

        // Second request
        reset(ctx);
        when(ctx.ip()).thenReturn("192.168.1.1");
        when(ctx.method()).thenReturn(io.javalin.http.HandlerType.GET);
        when(ctx.path()).thenReturn("/test");

        rateLimiter.handle(ctx);
        verify(ctx).header("X-RateLimit-Remaining", "3");
    }

    @Test
    void testConstructorWithIntegerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", Integer.valueOf(200));

        rateLimiter = new RateLimiter(config, logger);

        Map<String, Object> stats = rateLimiter.getStats();
        assertEquals(200, stats.get("maxRequestsPerMinute"));
    }

    @Test
    void testConstructorWithLongConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("rateLimitEnabled", true);
        config.put("maxRequestsPerMinute", Long.valueOf(150));

        rateLimiter = new RateLimiter(config, logger);

        Map<String, Object> stats = rateLimiter.getStats();
        assertEquals(150, stats.get("maxRequestsPerMinute"));
    }
}

