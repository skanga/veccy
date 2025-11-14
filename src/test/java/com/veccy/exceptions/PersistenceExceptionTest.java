package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystemException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PersistenceException.
 */
class PersistenceExceptionTest {

    @Test
    void testDefaultConstructor() {
        PersistenceException exception = new PersistenceException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Failed to persist vector data";
        PersistenceException exception = new PersistenceException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Serialization failed";
        IOException cause = new IOException("Write error");
        PersistenceException exception = new PersistenceException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        FileSystemException cause = new FileSystemException("Cannot write to read-only filesystem");
        PersistenceException exception = new PersistenceException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        PersistenceException exception = new PersistenceException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(PersistenceException.class, () -> {
            throw new PersistenceException("Persistence error");
        });
    }

    @Test
    void testTypicalPersistenceErrors() {
        // Serialization error
        PersistenceException serializationError = new PersistenceException("Failed to serialize vectors");
        assertTrue(serializationError.getMessage().contains("serialize"));

        // Deserialization error
        PersistenceException deserializationError = new PersistenceException("Failed to deserialize index data");
        assertTrue(deserializationError.getMessage().contains("deserialize"));

        // File corruption
        PersistenceException corruptionError = new PersistenceException("Corrupted persistence file");
        assertTrue(corruptionError.getMessage().contains("Corrupted"));

        // Version mismatch
        PersistenceException versionError = new PersistenceException("Incompatible persistence format version");
        assertTrue(versionError.getMessage().contains("version"));

        // Checksum failure
        PersistenceException checksumError = new PersistenceException("Data integrity check failed");
        assertTrue(checksumError.getMessage().contains("integrity"));
    }

    @Test
    void testPersistenceOperationErrors() {
        // Save operation
        PersistenceException saveError = new PersistenceException("Failed to save index to disk");
        assertTrue(saveError.getMessage().contains("save"));

        // Load operation
        PersistenceException loadError = new PersistenceException("Failed to load index from disk");
        assertTrue(loadError.getMessage().contains("load"));

        // Checkpoint operation
        PersistenceException checkpointError = new PersistenceException("Failed to create checkpoint");
        assertTrue(checkpointError.getMessage().contains("checkpoint"));
    }

    @Test
    void testExceptionWithFilePath() {
        String message = "Failed to write to file: /data/vectors/index.bin";
        PersistenceException exception = new PersistenceException(message);

        assertTrue(exception.getMessage().contains("/data/vectors/index.bin"));
    }
}
