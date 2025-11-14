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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BatchUpdateCommand.
 */
class BatchUpdateCommandTest {

    private BatchUpdateCommand command;
    private CLIContext context;
    private VectorDBClient client;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        command = new BatchUpdateCommand();
        client = VectorDBFactory.createSimple();
        context = new CLIContext();
        context.setClient(client);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testGetName() {
        assertEquals("batch-update", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Update multiple vectors (batch mode)", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("batch-update"));
        assertTrue(usage.contains("--format"));
    }

    @Test
    void testBatchUpdateCsvFromFile() throws Exception {
        // Insert initial vectors
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        List<String> ids = client.insert(vectors, null);

        // Create CSV update file
        String csvContent = String.format("""
                %s,[5.0,6.0],
                %s,[7.0,8.0],
                """, ids.get(0), ids.get(1));
        Path updateFile = tempDir.resolve("updates.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Total: 2"));
        assertTrue(output.contains("Success: 2"));
    }

    @Test
    void testBatchUpdateJsonFromFile() throws Exception {
        // Insert initial vectors
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        // Create JSON update file
        String jsonContent = String.format("""
                [
                  {"id": "%s", "vector": [9.0, 10.0]}
                ]
                """, ids.get(0));
        Path updateFile = tempDir.resolve("updates.json");
        Files.writeString(updateFile, jsonContent);

        command.execute(context, new String[]{"@" + updateFile.toString(), "--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("Success"));
    }

    @Test
    void testBatchUpdateWithMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("""
                %s,[3.0,4.0],{"label":"updated"}
                """, ids.get(0));
        Path updateFile = tempDir.resolve("updates_meta.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Success"));
    }

    @Test
    void testBatchUpdateWithNoArgs() {
        command.execute(context, new String[]{});

        // Command prints usage to stderr when no args provided
        String output = outContent.toString();
        String error = errContent.toString();
        assertTrue(output.contains("Usage") || error.contains("Usage") ||
                   output.contains("batch-update") || error.contains("batch-update") ||
                   output.contains("updates") || error.contains("updates"));
    }

    @Test
    void testBatchUpdateWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        assertThrows(Exception.class, () -> {
            command.execute(emptyContext, new String[]{"@updates.csv"});
        });
    }

    @Test
    void testBatchUpdateWithEmptyFile() throws Exception {
        Path emptyFile = tempDir.resolve("empty.csv");
        Files.writeString(emptyFile, "");

        command.execute(context, new String[]{"@" + emptyFile.toString()});

        String error = errContent.toString();
        assertTrue(error.contains("No updates"));
    }

    @Test
    void testBatchUpdateWithComments() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("""
                # This is a comment
                %s,[3.0,4.0],
                # Another comment
                """, ids.get(0));
        Path updateFile = tempDir.resolve("updates_comments.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Total: 1"));
    }

    @Test
    void testBatchUpdateWithVerboseMode() throws Exception {
        context.setVerbose(true);

        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("%s,[3.0,4.0],\n", ids.get(0));
        Path updateFile = tempDir.resolve("updates.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Loaded") || output.contains("update"));
    }

    @Test
    void testBatchUpdateShowsSuccessCount() throws Exception {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("""
                %s,[5.0,6.0],
                %s,[7.0,8.0],
                """, ids.get(0), ids.get(1));
        Path updateFile = tempDir.resolve("updates.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Success: 2"));
        assertTrue(output.contains("Failed: 0"));
    }

    @Test
    void testBatchUpdateShowsFailureCount() throws Exception {
        // Create update for non-existent IDs
        String csvContent = """
                nonexistent1,[1.0,2.0],
                nonexistent2,[3.0,4.0],
                """;
        Path updateFile = tempDir.resolve("updates_fail.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Failed: 2"));
    }

    @Test
    void testBatchUpdateShowsTiming() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("%s,[3.0,4.0],\n", ids.get(0));
        Path updateFile = tempDir.resolve("updates.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("ms"));
        assertTrue(output.contains("ms/update"));
    }

    @Test
    void testBatchUpdateDirectCommandLineNotSupported() {
        command.execute(context, new String[]{"direct-input"});

        String error = errContent.toString();
        assertTrue(error.contains("Use @file"));
    }

    @Test
    void testBatchUpdateJsonWithMultipleUpdates() throws Exception {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        List<String> ids = client.insert(vectors, null);

        String jsonContent = String.format("""
                [
                  {"id": "%s", "vector": [5.0, 6.0], "metadata": {"updated": true}},
                  {"id": "%s", "vector": [7.0, 8.0]}
                ]
                """, ids.get(0), ids.get(1));
        Path updateFile = tempDir.resolve("updates.json");
        Files.writeString(updateFile, jsonContent);

        command.execute(context, new String[]{"@" + updateFile.toString(), "--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("Total: 2"));
    }

    @Test
    void testBatchUpdateWithInvalidCsvFormat() throws Exception {
        String csvContent = "invalid csv format\n";
        Path updateFile = tempDir.resolve("invalid.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        // Should handle gracefully and skip invalid lines
        String output = outContent.toString();
        String error = errContent.toString();
        assertTrue(output.contains("Total") || error.contains("Warning") ||
                   output.contains("Warning") || error.contains("Skipping"));
    }

    @Test
    void testBatchUpdateMixedSuccessAndFailure() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<String> ids = client.insert(vectors, null);

        String csvContent = String.format("""
                %s,[3.0,4.0],
                nonexistent,[5.0,6.0],
                """, ids.get(0));
        Path updateFile = tempDir.resolve("mixed.csv");
        Files.writeString(updateFile, csvContent);

        command.execute(context, new String[]{"@" + updateFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Success: 1"));
        assertTrue(output.contains("Failed: 1"));
    }
}
