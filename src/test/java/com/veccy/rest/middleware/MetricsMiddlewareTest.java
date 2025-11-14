package com.veccy.rest.middleware;

import com.veccy.rest.metrics.MetricsCollector;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for MetricsMiddleware.
 */
class MetricsMiddlewareTest {

    private MetricsCollector metricsCollector;
    private MetricsMiddleware middleware;

    @BeforeEach
    void setUp() {
        metricsCollector = new MetricsCollector();
        middleware = new MetricsMiddleware(metricsCollector);
    }

    @Test
    void testConstructor() {
        assertNotNull(middleware);
    }

    @Test
    void testHandleSetsStartTime() throws Exception {
        Context ctx = mock(Context.class);

        middleware.handle(ctx);

        // Verify that start time was set as an attribute
        verify(ctx).attribute(eq("metrics-start-time"), anyLong());
    }

    @Test
    void testRecordAfterWithNoStartTime() {
        Context ctx = mock(Context.class);
        when(ctx.attribute("metrics-start-time")).thenReturn(null);

        // Should not throw when start time is null
        assertDoesNotThrow(() -> middleware.recordAfter(ctx));

        // No metrics should be recorded
        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(0L, metrics.get("total_requests"));
    }

    @Test
    void testRecordAfterWithStartTime() {
        Context ctx = mock(Context.class);
        long startTime = System.currentTimeMillis() - 100; // 100ms ago
        when(ctx.attribute("metrics-start-time")).thenReturn(startTime);
        when(ctx.status()).thenReturn(HttpStatus.OK);
        when(ctx.method()).thenReturn(HandlerType.GET);
        when(ctx.path()).thenReturn("/api/test");

        middleware.recordAfter(ctx);

        // Verify metrics were recorded
        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(1L, metrics.get("total_requests"));
        assertEquals(1L, metrics.get("successful_requests"));
    }

    @Test
    void testRecordAfterRecordsCorrectStatusCode() {
        Context ctx = mock(Context.class);
        long startTime = System.currentTimeMillis();
        when(ctx.attribute("metrics-start-time")).thenReturn(startTime);
        when(ctx.status()).thenReturn(HttpStatus.CREATED);
        when(ctx.method()).thenReturn(HandlerType.POST);
        when(ctx.path()).thenReturn("/api/resource");

        middleware.recordAfter(ctx);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Long> statusCodes = (Map<String, Long>) metrics.get("status_codes");
        assertEquals(1L, statusCodes.get("201"));
    }

    @Test
    void testRecordAfterRecordsErrorStatusCode() {
        Context ctx = mock(Context.class);
        long startTime = System.currentTimeMillis();
        when(ctx.attribute("metrics-start-time")).thenReturn(startTime);
        when(ctx.status()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(ctx.method()).thenReturn(HandlerType.GET);
        when(ctx.path()).thenReturn("/api/error");

        middleware.recordAfter(ctx);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(1L, metrics.get("total_requests"));
        assertEquals(0L, metrics.get("successful_requests"));
        assertEquals(1L, metrics.get("failed_requests"));
    }

    @Test
    void testRecordAfterRecordsResponseTime() throws InterruptedException {
        Context ctx = mock(Context.class);
        long startTime = System.currentTimeMillis();
        when(ctx.attribute("metrics-start-time")).thenReturn(startTime);
        when(ctx.status()).thenReturn(HttpStatus.OK);
        when(ctx.method()).thenReturn(HandlerType.GET);
        when(ctx.path()).thenReturn("/api/test");

        // Wait a bit to ensure some response time
        Thread.sleep(10);

        middleware.recordAfter(ctx);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        Double avgResponseTime = (Double) metrics.get("avg_response_time_ms");
        assertNotNull(avgResponseTime);
        assertTrue(avgResponseTime > 0, "Response time should be greater than 0");
    }

    @Test
    void testRecordAfterWithDifferentMethods() {
        // Test GET
        Context getCtx = mock(Context.class);
        when(getCtx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(getCtx.status()).thenReturn(HttpStatus.OK);
        when(getCtx.method()).thenReturn(HandlerType.GET);
        when(getCtx.path()).thenReturn("/api/resource");
        middleware.recordAfter(getCtx);

        // Test POST
        Context postCtx = mock(Context.class);
        when(postCtx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(postCtx.status()).thenReturn(HttpStatus.CREATED);
        when(postCtx.method()).thenReturn(HandlerType.POST);
        when(postCtx.path()).thenReturn("/api/resource");
        middleware.recordAfter(postCtx);

        // Test PUT
        Context putCtx = mock(Context.class);
        when(putCtx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(putCtx.status()).thenReturn(HttpStatus.OK);
        when(putCtx.method()).thenReturn(HandlerType.PUT);
        when(putCtx.path()).thenReturn("/api/resource/1");
        middleware.recordAfter(putCtx);

        // Test DELETE
        Context deleteCtx = mock(Context.class);
        when(deleteCtx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(deleteCtx.status()).thenReturn(HttpStatus.NO_CONTENT);
        when(deleteCtx.method()).thenReturn(HandlerType.DELETE);
        when(deleteCtx.path()).thenReturn("/api/resource/1");
        middleware.recordAfter(deleteCtx);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(4L, metrics.get("total_requests"));
    }

    @Test
    void testRecordAfterWithDifferentPaths() {
        Context ctx1 = mock(Context.class);
        when(ctx1.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(ctx1.status()).thenReturn(HttpStatus.OK);
        when(ctx1.method()).thenReturn(HandlerType.GET);
        when(ctx1.path()).thenReturn("/api/users");
        middleware.recordAfter(ctx1);

        Context ctx2 = mock(Context.class);
        when(ctx2.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
        when(ctx2.status()).thenReturn(HttpStatus.OK);
        when(ctx2.method()).thenReturn(HandlerType.GET);
        when(ctx2.path()).thenReturn("/api/products");
        middleware.recordAfter(ctx2);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> endpoints = (Map<String, Map<String, Object>>) metrics.get("endpoints");

        // Should have two different endpoints tracked
        assertTrue(endpoints.size() >= 2);
    }

    @Test
    void testMultipleRequestsRecorded() {
        for (int i = 0; i < 10; i++) {
            Context ctx = mock(Context.class);
            when(ctx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
            when(ctx.status()).thenReturn(HttpStatus.OK);
            when(ctx.method()).thenReturn(HandlerType.GET);
            when(ctx.path()).thenReturn("/api/test");
            middleware.recordAfter(ctx);
        }

        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(10L, metrics.get("total_requests"));
        assertEquals(10L, metrics.get("successful_requests"));
    }

    @Test
    void testHandleAndRecordAfterWorkflow() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status()).thenReturn(HttpStatus.OK);
        when(ctx.method()).thenReturn(HandlerType.GET);
        when(ctx.path()).thenReturn("/api/test");

        // Simulate the full workflow
        middleware.handle(ctx);

        // Verify start time was set
        verify(ctx).attribute(eq("metrics-start-time"), anyLong());

        // Capture the start time that was set
        long[] capturedTime = new long[1];
        doAnswer(invocation -> {
            capturedTime[0] = invocation.getArgument(1);
            return null;
        }).when(ctx).attribute(eq("metrics-start-time"), anyLong());

        middleware.handle(ctx);

        // Now simulate recordAfter being called
        when(ctx.attribute("metrics-start-time")).thenReturn(capturedTime[0]);
        middleware.recordAfter(ctx);

        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(1L, metrics.get("total_requests"));
    }

    @Test
    void testRecordAfterWithNullContext() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> middleware.recordAfter(null));
    }

    @Test
    void testSuccessRateCalculation() {
        // Record 7 successful and 3 failed requests
        for (int i = 0; i < 7; i++) {
            Context ctx = mock(Context.class);
            when(ctx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
            when(ctx.status()).thenReturn(HttpStatus.OK);
            when(ctx.method()).thenReturn(HandlerType.GET);
            when(ctx.path()).thenReturn("/api/test");
            middleware.recordAfter(ctx);
        }

        for (int i = 0; i < 3; i++) {
            Context ctx = mock(Context.class);
            when(ctx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
            when(ctx.status()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
            when(ctx.method()).thenReturn(HandlerType.GET);
            when(ctx.path()).thenReturn("/api/test");
            middleware.recordAfter(ctx);
        }

        Map<String, Object> metrics = metricsCollector.getMetrics();
        assertEquals(10L, metrics.get("total_requests"));
        assertEquals(7L, metrics.get("successful_requests"));
        assertEquals(3L, metrics.get("failed_requests"));

        Double successRate = (Double) metrics.get("success_rate");
        assertEquals(0.7, successRate, 0.01);
    }

    @Test
    void testDifferentStatusCodesTracked() {
        int[] statusCodes = {200, 201, 204, 400, 404, 500};

        for (int statusCode : statusCodes) {
            Context ctx = mock(Context.class);
            when(ctx.attribute("metrics-start-time")).thenReturn(System.currentTimeMillis());
            when(ctx.status()).thenReturn(HttpStatus.forStatus(statusCode));
            when(ctx.method()).thenReturn(HandlerType.GET);
            when(ctx.path()).thenReturn("/api/test");
            middleware.recordAfter(ctx);
        }

        Map<String, Object> metrics = metricsCollector.getMetrics();
        @SuppressWarnings("unchecked")
        Map<String, Long> statusCodeMap = (Map<String, Long>) metrics.get("status_codes");

        assertEquals(6, statusCodeMap.size());
        for (int code : statusCodes) {
            assertEquals(1L, statusCodeMap.get(String.valueOf(code)));
        }
    }
}
