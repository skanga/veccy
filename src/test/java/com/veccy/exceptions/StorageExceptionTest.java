package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StorageException.
 */
class StorageExceptionTest {

    @Test
    void testDefaultConstructor() {
        StorageException exception = new StorageException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageConstructor() {
        String message = "Storage operation failed";
        StorageException exception = new StorageException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Failed to write to disk";
        IOException cause = new IOException("Disk full");
        StorageException exception = new StorageException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        IOException cause = new IOException("Read error");
        StorageException exception = new StorageException(cause);

        assertEquals(cause, exception.getCause());
        assertTrue(exception.getMessage().contains("IOException"));
    }

    @Test
    void testInheritance() {
        StorageException exception = new StorageException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(StorageException.class, () -> {
            throw new StorageException("Storage error");
        });
    }

    @Test
    void testCatchAsVeccyException() {
        try {
            throw new StorageException("Storage failed");
        } catch (VeccyException e) {
            assertInstanceOf(StorageException.class, e);
            assertEquals("Storage failed", e.getMessage());
        }
    }

    @Test
    void testTypicalStorageErrors() {
        // Disk full
        StorageException diskFull = new StorageException("Disk full");
        assertTrue(diskFull.getMessage().contains("Disk"));

        // Permission denied
        StorageException permissionDenied = new StorageException("Permission denied");
        assertTrue(permissionDenied.getMessage().contains("Permission"));

        // File not found
        StorageException fileNotFound = new StorageException("File not found");
        assertTrue(fileNotFound.getMessage().contains("File"));

        // Corrupted data
        StorageException corrupted = new StorageException("Data corrupted");
        assertTrue(corrupted.getMessage().contains("corrupted"));
    }

    @Test
    void testExceptionChaining() {
        IOException rootCause = new IOException("Disk error");
        StorageException exception = new StorageException("Storage operation failed", rootCause);

        assertEquals("Storage operation failed", exception.getMessage());
        assertEquals(rootCause, exception.getCause());
        assertEquals("Disk error", exception.getCause().getMessage());
    }
}
