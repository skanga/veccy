package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InitCommand.
 */
class InitCommandTest {

    private InitCommand command;
    private CLIContext context;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        command = new InitCommand();
        context = new CLIContext();

        // Capture System.out
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testGetName() {
        assertEquals("init", command.getName());
    }

    @Test
    void testGetDescription() {
        assertNotNull(command.getDescription());
        assertTrue(command.getDescription().contains("Initialize"));
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertNotNull(usage);
        assertTrue(usage.contains("init"));
        assertTrue(usage.contains("--path"));
        assertTrue(usage.contains("--index"));
        assertTrue(usage.contains("--storage"));
        assertTrue(usage.contains("--metric"));
    }

    @Test
    void testInitWithDefaults() throws Exception {
        String[] args = {};

        command.execute(context, args);

        assertNotNull(context.getClient());
        assertTrue(context.hasOpenDatabase());
        assertNull(context.getDatabasePath()); // Memory storage doesn't set path

        String output = outputStream.toString();
        assertTrue(output.contains("Database initialized successfully"));
        assertTrue(output.contains("Storage: memory"));
        assertTrue(output.contains("Index:   hnsw"));
        assertTrue(output.contains("Metric:  cosine"));
    }

    @Test
    void testInitWithMemoryStorage() throws Exception {
        String[] args = {"--storage", "memory"};

        command.execute(context, args);

        assertNotNull(context.getClient());
        assertTrue(context.hasOpenDatabase());

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: memory"));
    }

    @Test
    void testInitWithDiskStorage() throws Exception {
        String dbPath = tempDir.resolve("test_db").toString();
        String[] args = {"--storage", "disk", "--path", dbPath};

        command.execute(context, args);

        assertNotNull(context.getClient());
        assertTrue(context.hasOpenDatabase());
        assertEquals(dbPath, context.getDatabasePath().toString());

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: disk"));
        assertTrue(output.contains("Path:    " + dbPath));

        // Verify directory was created
        assertTrue(Files.exists(tempDir.resolve("test_db")));
    }

    @Test
    void testInitWithHybridStorage() throws Exception {
        String dbPath = tempDir.resolve("hybrid_db").toString();
        String[] args = {"--storage", "hybrid", "--path", dbPath};

        command.execute(context, args);

        assertNotNull(context.getClient());
        assertTrue(context.hasOpenDatabase());
        assertEquals(dbPath, context.getDatabasePath().toString());

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: hybrid"));
        assertTrue(output.contains("Path:    " + dbPath));
    }

    @Test
    void testInitWithFlatIndex() throws Exception {
        String[] args = {"--index", "flat"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Index:   flat"));
    }

    @Test
    void testInitWithHNSWIndex() throws Exception {
        String[] args = {"--index", "hnsw"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Index:   hnsw"));
    }

    @Test
    void testInitWithIVFIndex() throws Exception {
        String[] args = {"--index", "ivf"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Index:   ivf"));
    }

    @Test
    void testInitWithLSHIndex() throws Exception {
        String[] args = {"--index", "lsh"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Index:   lsh"));
    }

    @Test
    void testInitWithAnnoyIndex() throws Exception {
        String[] args = {"--index", "annoy"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Index:   annoy"));
    }

    @Test
    void testInitWithCosineMetric() throws Exception {
        String[] args = {"--metric", "cosine"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Metric:  cosine"));
    }

    @Test
    void testInitWithEuclideanMetric() throws Exception {
        String[] args = {"--metric", "euclidean"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Metric:  euclidean"));
    }

    @Test
    void testInitWithDotProductMetric() throws Exception {
        String[] args = {"--metric", "dot_product"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Metric:  dot_product"));
    }

    @Test
    void testInitWithManhattanMetric() throws Exception {
        String[] args = {"--metric", "manhattan"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Metric:  manhattan"));
    }

    @Test
    void testInitWithShortOptions() throws Exception {
        String dbPath = tempDir.resolve("short_db").toString();
        String[] args = {"-s", "disk", "-p", dbPath, "-i", "flat", "-m", "euclidean"};

        command.execute(context, args);

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: disk"));
        assertTrue(output.contains("Index:   flat"));
        assertTrue(output.contains("Metric:  euclidean"));
        assertTrue(output.contains("Path:    " + dbPath));
    }

    @Test
    void testInitWithAllOptions() throws Exception {
        String dbPath = tempDir.resolve("full_db").toString();
        String[] args = {
            "--storage", "hybrid",
            "--path", dbPath,
            "--index", "hnsw",
            "--metric", "cosine"
        };

        command.execute(context, args);

        assertNotNull(context.getClient());
        assertTrue(context.hasOpenDatabase());

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: hybrid"));
        assertTrue(output.contains("Index:   hnsw"));
        assertTrue(output.contains("Metric:  cosine"));
        assertTrue(output.contains("Path:    " + dbPath));
    }

    @Test
    void testInitDiskStorageWithoutPath() {
        String[] args = {"--storage", "disk"};

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, args);
        });

        assertTrue(exception.getMessage().contains("--path required for disk storage"));
    }

    @Test
    void testInitHybridStorageWithoutPath() {
        String[] args = {"--storage", "hybrid"};

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, args);
        });

        assertTrue(exception.getMessage().contains("--path required for hybrid storage"));
    }

    @Test
    void testInitWithInvalidStorageType() {
        String[] args = {"--storage", "invalid"};

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, args);
        });

        assertTrue(exception.getMessage().contains("Unknown storage type"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    void testInitWithInvalidIndexType() {
        String[] args = {"--index", "invalid"};

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, args);
        });

        assertTrue(exception.getMessage().contains("Unknown index type"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    void testInitWithUnknownOption() {
        String[] args = {"--unknown", "value"};

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, args);
        });

        assertTrue(exception.getMessage().contains("Unknown option"));
        assertTrue(exception.getMessage().contains("--unknown"));
    }

    @Test
    void testInitClosesExistingDatabase() throws Exception {
        // First init
        String[] args1 = {};
        command.execute(context, args1);

        VectorDBClient firstClient = context.getClient();
        assertNotNull(firstClient);

        // Clear output from first init
        outputStream.reset();

        // Second init should close first database
        String[] args2 = {"--index", "flat"};
        command.execute(context, args2);

        VectorDBClient secondClient = context.getClient();
        assertNotNull(secondClient);
        assertNotSame(firstClient, secondClient);

        String output = outputStream.toString();
        assertTrue(output.contains("Closing existing database"));
    }

    @Test
    void testInitCaseInsensitiveOptions() throws Exception {
        String[] args = {
            "--storage", "MEMORY",
            "--index", "HNSW",
            "--metric", "COSINE"
        };

        // Should not throw - options are converted to lowercase
        assertDoesNotThrow(() -> command.execute(context, args));

        String output = outputStream.toString();
        assertTrue(output.contains("Storage: memory"));
        assertTrue(output.contains("Index:   hnsw"));
        assertTrue(output.contains("Metric:  cosine"));
    }

    @Test
    void testInitCreatesDirectoryForDiskStorage() throws Exception {
        Path dbPath = tempDir.resolve("nested").resolve("path").resolve("db");
        String[] args = {"--storage", "disk", "--path", dbPath.toString()};

        assertFalse(Files.exists(dbPath));

        command.execute(context, args);

        assertTrue(Files.exists(dbPath));
    }

    @Test
    void testInitWithMissingOptionValue() throws Exception {
        String[] args = {"--storage"}; // Missing value

        // When --storage is last argument with no value, it's treated as if empty string
        // The loop ends before ++i, so storage type remains "memory" (default)
        // This actually succeeds with default memory storage
        assertDoesNotThrow(() -> command.execute(context, args));
    }

    @Test
    void testInitMultipleTimes() throws Exception {
        // First init
        command.execute(context, new String[]{});
        VectorDBClient client1 = context.getClient();

        // Second init
        command.execute(context, new String[]{"--index", "flat"});
        VectorDBClient client2 = context.getClient();

        // Third init
        command.execute(context, new String[]{"--index", "ivf"});
        VectorDBClient client3 = context.getClient();

        assertNotSame(client1, client2);
        assertNotSame(client2, client3);
        assertNotNull(context.getClient());
    }
}
