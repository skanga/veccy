package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IndexException.
 */
class IndexExceptionTest {

    @Test
    void testDefaultConstructor() {
        IndexException exception = new IndexException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageConstructor() {
        String message = "Index build failed";
        IndexException exception = new IndexException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Failed to create HNSW index";
        IllegalArgumentException cause = new IllegalArgumentException("Invalid dimension");
        IndexException exception = new IndexException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        OutOfMemoryError cause = new OutOfMemoryError("Heap space");
        IndexException exception = new IndexException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        IndexException exception = new IndexException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(IndexException.class, () -> {
            throw new IndexException("Index error");
        });
    }

    @Test
    void testCatchAsVeccyException() {
        try {
            throw new IndexException("Index creation failed");
        } catch (VeccyException e) {
            assertInstanceOf(IndexException.class, e);
        }
    }

    @Test
    void testTypicalIndexErrors() {
        // Dimension mismatch
        IndexException dimensionError = new IndexException("Dimension mismatch: expected 768, got 512");
        assertTrue(dimensionError.getMessage().contains("Dimension"));

        // Index not initialized
        IndexException notInitialized = new IndexException("Index not initialized");
        assertTrue(notInitialized.getMessage().contains("not initialized"));

        // Index corrupted
        IndexException corrupted = new IndexException("Index data corrupted");
        assertTrue(corrupted.getMessage().contains("corrupted"));

        // Invalid parameter
        IndexException invalidParam = new IndexException("Invalid HNSW parameter: M must be positive");
        assertTrue(invalidParam.getMessage().contains("Invalid"));
    }

    @Test
    void testExceptionWithDetails() {
        String details = "HNSW Index build failed: M=16, efConstruction=200, vectors=1000000";
        IndexException exception = new IndexException(details);

        assertEquals(details, exception.getMessage());
        assertTrue(exception.getMessage().contains("HNSW"));
        assertTrue(exception.getMessage().contains("1000000"));
    }
}
