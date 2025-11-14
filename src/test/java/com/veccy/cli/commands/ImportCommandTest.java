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
 * Tests for ImportCommand.
 */
class ImportCommandTest {

    private ImportCommand command;
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
        command = new ImportCommand();
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
        assertEquals("import", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Import vectors from a file", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("import"));
        assertTrue(usage.contains("--format"));
    }

    @Test
    void testImportJsonAutoDetect() throws Exception {
        // Create JSON file
        String jsonContent = """
                [
                  {"vector": [1.0, 2.0, 3.0]},
                  {"vector": [4.0, 5.0, 6.0]}
                ]
                """;
        Path importFile = tempDir.resolve("data.json");
        Files.writeString(importFile, jsonContent);

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully imported"));
        assertTrue(output.contains("2 vector(s)"));

        // Verify vectors were inserted
        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportCsvAutoDetect() throws Exception {
        // Create CSV file
        String csvContent = """
                1.0,2.0,3.0
                4.0,5.0,6.0
                """;
        Path importFile = tempDir.resolve("data.csv");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully imported"));
        assertTrue(output.contains("2 vector(s)"));

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportJsonWithMetadata() throws Exception {
        String jsonContent = """
                [
                  {"vector": [1.0, 2.0], "metadata": {"label": "first"}},
                  {"vector": [3.0, 4.0], "metadata": {"label": "second"}}
                ]
                """;
        Path importFile = tempDir.resolve("meta.json");
        Files.writeString(importFile, jsonContent);

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("2 vector(s)"));

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportCsvWithHeader() throws Exception {
        String csvContent = """
                id,vector,metadata
                vec1,1.0,2.0,3.0
                vec2,4.0,5.0,6.0
                """;
        Path importFile = tempDir.resolve("header.csv");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString()});

        // Should skip header and import 2 vectors
        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportCsvWithoutHeader() throws Exception {
        String csvContent = """
                1.0,2.0,3.0
                4.0,5.0,6.0
                """;
        Path importFile = tempDir.resolve("noheader.csv");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString()});

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportWithExplicitFormat() throws Exception {
        String csvContent = "1.0,2.0\n3.0,4.0\n";
        Path importFile = tempDir.resolve("data.txt");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString(), "--format", "csv"});

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportWithFormatFlag() throws Exception {
        String jsonContent = "[{\"vector\": [1.0, 2.0]}]";
        Path importFile = tempDir.resolve("data.txt");
        Files.writeString(importFile, jsonContent);

        command.execute(context, new String[]{importFile.toString(), "-f", "json"});

        List<String> ids = client.listVectorIds(null);
        assertEquals(1, ids.size());
    }

    @Test
    void testImportEmptyFile() throws Exception {
        Path importFile = tempDir.resolve("empty.csv");
        Files.writeString(importFile, "");

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{importFile.toString()});
        });
    }

    @Test
    void testImportNonExistentFile() {
        Path importFile = tempDir.resolve("nonexistent.json");

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{importFile.toString()});
        });
    }

    @Test
    void testImportWithoutFormatDetection() {
        Path importFile = tempDir.resolve("data.txt");

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{importFile.toString()});
        });
    }

    @Test
    void testImportUnsupportedFormat() throws Exception {
        String content = "some content";
        Path importFile = tempDir.resolve("data.json");
        Files.writeString(importFile, content);

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{importFile.toString(), "--format", "xml"});
        });
    }

    @Test
    void testImportWithNoArgs() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{});
        });
    }

    @Test
    void testImportWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        assertThrows(Exception.class, () -> {
            command.execute(emptyContext, new String[]{"test.json"});
        });
    }

    @Test
    void testImportCsvWithEmptyLines() throws Exception {
        String csvContent = """
                1.0,2.0,3.0

                4.0,5.0,6.0

                """;
        Path importFile = tempDir.resolve("empty_lines.csv");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString()});

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size()); // Empty lines should be skipped
    }

    @Test
    void testImportCsvWithMetadata() throws Exception {
        String csvContent = """
                1.0,2.0,label=test,value=100
                3.0,4.0,label=prod,value=200
                """;
        Path importFile = tempDir.resolve("csv_meta.csv");
        Files.writeString(importFile, csvContent);

        command.execute(context, new String[]{importFile.toString()});

        List<String> ids = client.listVectorIds(null);
        assertEquals(2, ids.size());
    }

    @Test
    void testImportJsonEmptyArray() throws Exception {
        String jsonContent = "[]";
        Path importFile = tempDir.resolve("empty.json");
        Files.writeString(importFile, jsonContent);

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("0 vector(s)"));

        List<String> ids = client.listVectorIds(null);
        assertEquals(0, ids.size());
    }

    @Test
    void testImportJsonInvalidFormat() throws Exception {
        String jsonContent = "not a json array";
        Path importFile = tempDir.resolve("invalid.json");
        Files.writeString(importFile, jsonContent);

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{importFile.toString()});
        });
    }

    @Test
    void testImportLargeDataset() throws Exception {
        // Create a large JSON file
        StringBuilder jsonContent = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) jsonContent.append(",");
            jsonContent.append(String.format("{\"vector\": [%d.0, %d.1, %d.2]}", i, i, i));
        }
        jsonContent.append("]");

        Path importFile = tempDir.resolve("large.json");
        Files.writeString(importFile, jsonContent.toString());

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("100 vector(s)"));

        List<String> ids = client.listVectorIds(null);
        assertEquals(100, ids.size());
    }

    @Test
    void testImportShowsAverageTime() throws Exception {
        String jsonContent = """
                [
                  {"vector": [1.0, 2.0]},
                  {"vector": [3.0, 4.0]}
                ]
                """;
        Path importFile = tempDir.resolve("data.json");
        Files.writeString(importFile, jsonContent);

        command.execute(context, new String[]{importFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Average:"));
        assertTrue(output.contains("ms per vector"));
    }
}
