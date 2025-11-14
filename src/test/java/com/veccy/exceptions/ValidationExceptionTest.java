package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationException.
 */
class ValidationExceptionTest {

    @Test
    void testDefaultConstructor() {
        ValidationException exception = new ValidationException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Invalid vector dimensions";
        IllegalArgumentException cause = new IllegalArgumentException("Expected positive value");
        ValidationException exception = new ValidationException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        NullPointerException cause = new NullPointerException("Vector cannot be null");
        ValidationException exception = new ValidationException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        ValidationException exception = new ValidationException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(ValidationException.class, () -> {
            throw new ValidationException("Validation error");
        });
    }

    @Test
    void testTypicalValidationErrors() {
        // Null input
        ValidationException nullError = new ValidationException("Input cannot be null");
        assertTrue(nullError.getMessage().contains("null"));

        // Empty input
        ValidationException emptyError = new ValidationException("Input cannot be empty");
        assertTrue(emptyError.getMessage().contains("empty"));

        // Invalid range
        ValidationException rangeError = new ValidationException("Value must be between 0 and 1");
        assertTrue(rangeError.getMessage().contains("between"));

        // Invalid format
        ValidationException formatError = new ValidationException("Invalid vector format");
        assertTrue(formatError.getMessage().contains("Invalid"));

        // Dimension mismatch
        ValidationException dimensionError = new ValidationException("Vector dimension must match index dimension");
        assertTrue(dimensionError.getMessage().contains("dimension"));
    }

    @Test
    void testValidationErrorWithFieldName() {
        String message = "Field 'k' must be positive, got: -5";
        ValidationException exception = new ValidationException(message);

        assertTrue(exception.getMessage().contains("k"));
        assertTrue(exception.getMessage().contains("-5"));
    }

    @Test
    void testMultipleValidationErrors() {
        String message = "Multiple validation errors: [vector is null, metadata is invalid, id is empty]";
        ValidationException exception = new ValidationException(message);

        assertTrue(exception.getMessage().contains("Multiple"));
        assertTrue(exception.getMessage().contains("vector"));
        assertTrue(exception.getMessage().contains("metadata"));
        assertTrue(exception.getMessage().contains("id"));
    }
}
