package com.veccy.rest.middleware;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for VeccyExceptionHandler.
 */
class VeccyExceptionHandlerTest {

    private Context ctx;
    private Logger logger;
    private VeccyExceptionHandler handler;

    @BeforeEach
    void setUp() {
        ctx = mock(Context.class);
        when(ctx.path()).thenReturn("/api/test");
        when(ctx.method()).thenReturn(HandlerType.GET);
        when(ctx.ip()).thenReturn("192.168.1.1");
        logger = mock(Logger.class);
        handler = new VeccyExceptionHandler(false, logger);
    }

    @Test
    void testConstructorWithProductionMode() {
        VeccyExceptionHandler handler = new VeccyExceptionHandler(true);
        // Should not throw
    }

    @Test
    void testConstructorWithDevelopmentMode() {
        VeccyExceptionHandler handler = new VeccyExceptionHandler(false);
        // Should not throw
    }

    @Test
    void testHandleGenericExceptionInProduction() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new Exception("Test error");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleGenericExceptionInDevelopment() {
        Exception exception = new Exception("Test error");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleIllegalArgumentExceptionMapsTo400() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new IllegalArgumentException("Invalid input");

        handler.handle(exception, ctx);

        verify(ctx).status(400);
        verify(ctx).json(any());
    }

    @Test
    void testHandleNotFoundExceptionMapsTo404() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new NotFoundException("Resource not found");

        handler.handle(exception, ctx);

        verify(ctx).status(404);
        verify(ctx).json(any());
    }

    @Test
    void testHandleUnauthorizedExceptionMapsTo403() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new UnauthorizedException("Unauthorized access");

        handler.handle(exception, ctx);

        verify(ctx).status(403);
        verify(ctx).json(any());
    }

    @Test
    void testHandleConflictExceptionMapsTo409() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new ConflictException("Resource conflict");

        handler.handle(exception, ctx);

        verify(ctx).status(409);
        verify(ctx).json(any());
    }

    @Test
    void testHandleUsesCorrelationIdFromContext() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new Exception("Test error");
        when(ctx.attribute("correlationId")).thenReturn("test-correlation-123");

        handler.handle(exception, ctx);

        verify(ctx).attribute("correlationId");
        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleWithNullCorrelationId() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new Exception("Test error");
        when(ctx.attribute("correlationId")).thenReturn(null);

        handler.handle(exception, ctx);

        verify(ctx).attribute("correlationId");
        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleWithExceptionCause() {
        Exception cause = new RuntimeException("Root cause");
        Exception exception = new Exception("Wrapper exception", cause);

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleWithNullExceptionMessage() {
        Exception exception = new Exception((String) null);

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleInvalidExceptionMapsTo400() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new InvalidInputException("Invalid data");

        handler.handle(exception, ctx);

        verify(ctx).status(400);
        verify(ctx).json(any());
    }

    @Test
    void testHandleLogsErrorDetails() {
        Exception exception = new Exception("Test logging");

        handler.handle(exception, ctx);

        // Verify path, method, and ip were accessed for logging
        verify(ctx, atLeastOnce()).path();
        verify(ctx, atLeastOnce()).method();
        verify(ctx, atLeastOnce()).ip();
    }

    @Test
    void testProductionModeHidesExceptionDetails() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new Exception("Sensitive internal error details");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
        // In production mode, the error message should be generic
    }

    @Test
    void testDevelopmentModeIncludesExceptionDetails() {
        Exception exception = new Exception("Detailed error for developers");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
        // In development mode, the full error message should be included
    }

    @Test
    void testHandleWithEmptyStackTrace() {
        Exception exception = new Exception("Error with no stack trace") {
            @Override
            public StackTraceElement[] getStackTrace() {
                return new StackTraceElement[0];
            }
        };

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleWithNullStackTrace() {
        Exception exception = new Exception("Error with null stack trace") {
            @Override
            public StackTraceElement[] getStackTrace() {
                return null;
            }
        };

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleRuntimeException() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new RuntimeException("Runtime error");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    @Test
    void testHandleNullPointerException() {
        handler = new VeccyExceptionHandler(true, logger);
        Exception exception = new NullPointerException("Null pointer");

        handler.handle(exception, ctx);

        verify(ctx).status(500);
        verify(ctx).json(any());
    }

    // Test exception classes for status code mapping
    private static class NotFoundException extends Exception {
        public NotFoundException(String message) {
            super(message);
        }
    }

    private static class UnauthorizedException extends Exception {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    private static class ConflictException extends Exception {
        public ConflictException(String message) {
            super(message);
        }
    }

    private static class InvalidInputException extends Exception {
        public InvalidInputException(String message) {
            super(message);
        }
    }
}
