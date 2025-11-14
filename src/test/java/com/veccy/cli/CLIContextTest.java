package com.veccy.cli;

import com.veccy.base.Index;
import com.veccy.client.VectorDBClient;
import com.veccy.config.FlatConfig;
import com.veccy.config.Metric;
import com.veccy.indices.FlatIndex;
import com.veccy.storage.MemoryStorage;
import com.veccy.storage.StorageBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CLIContext.
 */
class CLIContextTest {

    private CLIContext context;
    private VectorDBClient testClient;

    @BeforeEach
    void setUp() {
        context = new CLIContext();

        // Create a real client for testing
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        Index index = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        testClient = new VectorDBClient(storage, index);
        testClient.initialize();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        if (testClient != null && testClient != context.getClient()) {
            testClient.close();
        }
    }

    @Test
    void testInitialState() {
        assertNull(context.getClient());
        assertNull(context.getDatabasePath());
        assertFalse(context.isVerbose());
        assertEquals("table", context.getOutputFormat());
        assertFalse(context.hasOpenDatabase());
    }

    @Test
    void testSetAndGetClient() {
        context.setClient(testClient);
        assertEquals(testClient, context.getClient());
    }

    @Test
    void testSetClientClosesOldClient() {
        StorageBackend storage1 = new MemoryStorage(new HashMap<>());
        Index index1 = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient oldClient = new VectorDBClient(storage1, index1);
        oldClient.initialize();

        StorageBackend storage2 = new MemoryStorage(new HashMap<>());
        Index index2 = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient newClient = new VectorDBClient(storage2, index2);
        newClient.initialize();

        context.setClient(oldClient);
        assertTrue(oldClient.isInitialized());

        context.setClient(newClient);
        assertFalse(oldClient.isInitialized()); // Old client should be closed
        assertEquals(newClient, context.getClient());

        newClient.close();
    }

    @Test
    void testSetClientSameInstanceDoesNotClose() {
        context.setClient(testClient);
        assertTrue(testClient.isInitialized());

        context.setClient(testClient); // Set same instance
        assertTrue(testClient.isInitialized()); // Should still be initialized
    }

    @Test
    void testHasOpenDatabase() {
        assertFalse(context.hasOpenDatabase()); // No client set

        context.setClient(testClient);
        assertTrue(context.hasOpenDatabase());

        testClient.close();
        assertFalse(context.hasOpenDatabase());
    }

    @Test
    void testSetAndGetDatabasePath() {
        String pathString = "/tmp/test.db";
        context.setDatabasePath(pathString);

        Path expected = Paths.get(pathString);
        assertEquals(expected, context.getDatabasePath());
    }

    @Test
    void testSetDatabasePathNull() {
        context.setDatabasePath("/tmp/test.db");
        context.setDatabasePath(null);

        assertNull(context.getDatabasePath());
    }

    @Test
    void testSetAndGetVerbose() {
        assertFalse(context.isVerbose());

        context.setVerbose(true);
        assertTrue(context.isVerbose());

        context.setVerbose(false);
        assertFalse(context.isVerbose());
    }

    @Test
    void testSetAndGetOutputFormat() {
        assertEquals("table", context.getOutputFormat());

        context.setOutputFormat("json");
        assertEquals("json", context.getOutputFormat());

        context.setOutputFormat("csv");
        assertEquals("csv", context.getOutputFormat());

        context.setOutputFormat("table");
        assertEquals("table", context.getOutputFormat());
    }

    @Test
    void testSetOutputFormatInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            context.setOutputFormat("xml");
        });

        assertTrue(exception.getMessage().contains("Invalid output format"));
    }

    @Test
    void testSetAndGetProperty() {
        assertNull(context.getProperty("test"));

        context.setProperty("test", "value");
        assertEquals("value", context.getProperty("test"));

        context.setProperty("number", 42);
        assertEquals(42, context.getProperty("number"));
    }

    @Test
    void testGetPropertyWithDefault() {
        assertEquals("default", context.getProperty("missing", "default"));
        assertEquals(100, context.getProperty("missing", 100));

        context.setProperty("existing", "value");
        assertEquals("value", context.getProperty("existing", "default"));
    }

    @Test
    void testGetPropertyTypeCast() {
        context.setProperty("string", "hello");
        context.setProperty("number", 42);
        context.setProperty("bool", true);

        String strValue = context.getProperty("string", "default");
        assertEquals("hello", strValue);

        Integer intValue = context.getProperty("number", 0);
        assertEquals(42, intValue);

        Boolean boolValue = context.getProperty("bool", false);
        assertTrue(boolValue);
    }

    @Test
    void testCloseWithClient() {
        context.setClient(testClient);
        assertTrue(testClient.isInitialized());

        context.close();

        assertFalse(testClient.isInitialized()); // Client should be closed
        assertNull(context.getClient());
    }

    @Test
    void testCloseWithoutClient() {
        // Should not throw
        assertDoesNotThrow(() -> context.close());
    }

    @Test
    void testCloseIsIdempotent() {
        context.setClient(testClient);

        context.close();
        assertNull(context.getClient());

        context.close(); // Second close should have no effect
        assertNull(context.getClient());
    }

    @Test
    void testCloseHandlesClientException() {
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        Index index = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        context.setClient(client);
        client.close(); // Close it before context tries to close

        // Should not throw, even though client is already closed
        assertDoesNotThrow(() -> context.close());
        assertNull(context.getClient());
    }

    @Test
    void testMultipleProperties() {
        context.setProperty("key1", "value1");
        context.setProperty("key2", 123);
        context.setProperty("key3", true);

        assertEquals("value1", context.getProperty("key1"));
        assertEquals(123, context.getProperty("key2"));
        assertEquals(true, context.getProperty("key3"));

        // Overwrite property
        context.setProperty("key1", "newValue");
        assertEquals("newValue", context.getProperty("key1"));
    }

    @Test
    void testTryWithResources() throws Exception {
        StorageBackend storage = new MemoryStorage(new HashMap<>());
        Index index = new FlatIndex(FlatConfig.builder().metric(Metric.COSINE).build());
        VectorDBClient client = new VectorDBClient(storage, index);
        client.initialize();

        try (CLIContext ctx = new CLIContext()) {
            ctx.setClient(client);
            assertNotNull(ctx.getClient());
            assertTrue(client.isInitialized());
        }

        // Client should be closed after try-with-resources
        assertFalse(client.isInitialized());
    }

    @Test
    void testAllOutputFormats() {
        String[] validFormats = {"table", "json", "csv"};

        for (String format : validFormats) {
            assertDoesNotThrow(() -> context.setOutputFormat(format));
            assertEquals(format, context.getOutputFormat());
        }
    }

    @Test
    void testInvalidOutputFormats() {
        String[] invalidFormats = {"xml", "yaml", "text", "", "TABLE", "JSON"};

        for (String format : invalidFormats) {
            assertThrows(IllegalArgumentException.class,
                () -> context.setOutputFormat(format),
                "Format '" + format + "' should be invalid");
        }
    }
}
