package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VeccyException.
 */
class VeccyExceptionTest {

    @Test
    void testDefaultConstructor() {
        VeccyException exception = new VeccyException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageConstructor() {
        String message = "Test error message";
        VeccyException exception = new VeccyException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Test error with cause";
        Throwable cause = new IOException("IO error");
        VeccyException exception = new VeccyException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("IO error", exception.getCause().getMessage());
    }

    @Test
    void testCauseConstructor() {
        Throwable cause = new IllegalArgumentException("Invalid argument");
        VeccyException exception = new VeccyException(cause);

        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("IllegalArgumentException"));
    }

    @Test
    void testExceptionIsRuntimeException() {
        VeccyException exception = new VeccyException("test");
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testExceptionCanBeThrown() {
        assertThrows(VeccyException.class, () -> {
            throw new VeccyException("Test exception");
        });
    }

    @Test
    void testExceptionCanBeCaught() {
        try {
            throw new VeccyException("Test message");
        } catch (VeccyException e) {
            assertEquals("Test message", e.getMessage());
        }
    }

    @Test
    void testExceptionWithNullMessage() {
        VeccyException exception = new VeccyException((String) null);
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testExceptionWithNullCause() {
        VeccyException exception = new VeccyException("message", null);
        assertNotNull(exception);
        assertEquals("message", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testExceptionWithEmptyMessage() {
        VeccyException exception = new VeccyException("");
        assertNotNull(exception);
        assertEquals("", exception.getMessage());
    }

    @Test
    void testExceptionChaining() {
        IOException rootCause = new IOException("Disk error");
        RuntimeException middleCause = new RuntimeException("Processing failed", rootCause);
        VeccyException exception = new VeccyException("Operation failed", middleCause);

        assertEquals("Operation failed", exception.getMessage());
        assertEquals(middleCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testExceptionStackTrace() {
        VeccyException exception = new VeccyException("Test");
        StackTraceElement[] stackTrace = exception.getStackTrace();

        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    void testExceptionToString() {
        VeccyException exception = new VeccyException("Test message");
        String toString = exception.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("VeccyException"));
        assertTrue(toString.contains("Test message"));
    }

    @Test
    void testExceptionWithLongMessage() {
        String longMessage = "a".repeat(10000);
        VeccyException exception = new VeccyException(longMessage);

        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    void testExceptionWithSpecialCharacters() {
        String message = "Error: \"quoted\" & <special> chars\n\t新しい";
        VeccyException exception = new VeccyException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMultipleExceptionInstances() {
        VeccyException ex1 = new VeccyException("Error 1");
        VeccyException ex2 = new VeccyException("Error 2");
        VeccyException ex3 = new VeccyException("Error 3");

        assertNotSame(ex1, ex2);
        assertNotSame(ex2, ex3);
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    void testExceptionInheritance() {
        VeccyException exception = new VeccyException("test");

        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(Exception.class, exception);
        assertInstanceOf(Throwable.class, exception);
    }
}
