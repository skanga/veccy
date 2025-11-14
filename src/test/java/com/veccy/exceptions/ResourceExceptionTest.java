package com.veccy.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ResourceException.
 */
class ResourceExceptionTest {

    @Test
    void testDefaultConstructor() {
        ResourceException exception = new ResourceException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testMessageConstructor() {
        String message = "Resource not available";
        ResourceException exception = new ResourceException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testMessageAndCauseConstructor() {
        String message = "Failed to acquire resource";
        IllegalStateException cause = new IllegalStateException("Resource locked");
        ResourceException exception = new ResourceException(message, cause);

        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseConstructor() {
        OutOfMemoryError cause = new OutOfMemoryError("Java heap space");
        ResourceException exception = new ResourceException(cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void testInheritance() {
        ResourceException exception = new ResourceException("test");

        assertInstanceOf(VeccyException.class, exception);
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(ResourceException.class, () -> {
            throw new ResourceException("Resource error");
        });
    }

    @Test
    void testTypicalResourceErrors() {
        // Out of memory
        ResourceException memoryError = new ResourceException("Out of memory: cannot allocate vector buffer");
        assertTrue(memoryError.getMessage().contains("memory"));

        // Resource exhausted
        ResourceException exhausted = new ResourceException("Thread pool exhausted");
        assertTrue(exhausted.getMessage().contains("exhausted"));

        // Resource locked
        ResourceException locked = new ResourceException("Resource is locked by another process");
        assertTrue(locked.getMessage().contains("locked"));

        // Resource not found
        ResourceException notFound = new ResourceException("Resource not found: /data/model.bin");
        assertTrue(notFound.getMessage().contains("not found"));

        // Resource limit exceeded
        ResourceException limitExceeded = new ResourceException("Maximum number of connections exceeded");
        assertTrue(limitExceeded.getMessage().contains("exceeded"));
    }

    @Test
    void testMemoryResourceErrors() {
        // Heap space
        ResourceException heapError = new ResourceException("Insufficient heap space for index");
        assertTrue(heapError.getMessage().contains("heap"));

        // Buffer allocation
        ResourceException bufferError = new ResourceException("Cannot allocate buffer: size exceeds available memory");
        assertTrue(bufferError.getMessage().contains("buffer"));

        // GC overhead
        ResourceException gcError = new ResourceException("GC overhead limit exceeded");
        assertTrue(gcError.getMessage().contains("GC"));
    }

    @Test
    void testThreadResourceErrors() {
        // Thread pool
        ResourceException threadPoolError = new ResourceException("Cannot create thread: thread pool at capacity");
        assertTrue(threadPoolError.getMessage().contains("thread"));

        // Deadlock
        ResourceException deadlockError = new ResourceException("Potential deadlock detected");
        assertTrue(deadlockError.getMessage().contains("deadlock"));

        // Thread interrupted
        ResourceException interruptedError = new ResourceException("Thread interrupted while waiting for resource");
        assertTrue(interruptedError.getMessage().contains("interrupted"));
    }

    @Test
    void testFileResourceErrors() {
        // File descriptor
        ResourceException fdError = new ResourceException("Too many open files");
        assertTrue(fdError.getMessage().contains("files"));

        // Disk space
        ResourceException diskError = new ResourceException("Insufficient disk space");
        assertTrue(diskError.getMessage().contains("disk"));

        // File handle
        ResourceException handleError = new ResourceException("Cannot obtain file handle");
        assertTrue(handleError.getMessage().contains("handle"));
    }

    @Test
    void testNetworkResourceErrors() {
        // Connection limit
        ResourceException connError = new ResourceException("Maximum connections reached");
        assertTrue(connError.getMessage().contains("connections"));

        // Bandwidth
        ResourceException bandwidthError = new ResourceException("Bandwidth limit exceeded");
        assertTrue(bandwidthError.getMessage().contains("Bandwidth"));

        // Socket
        ResourceException socketError = new ResourceException("No more sockets available");
        assertTrue(socketError.getMessage().contains("socket"));
    }

    @Test
    void testExceptionWithResourceDetails() {
        String message = "Resource exhausted: [memory=95%, threads=1000/1000, files=1024/1024]";
        ResourceException exception = new ResourceException(message);

        assertTrue(exception.getMessage().contains("memory"));
        assertTrue(exception.getMessage().contains("threads"));
        assertTrue(exception.getMessage().contains("files"));
    }
}
