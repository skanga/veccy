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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExportCommand.
 */
class ExportCommandTest {

    private ExportCommand command;
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
        command = new ExportCommand();
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
        assertEquals("export", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Export vectors to a file", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("export"));
        assertTrue(usage.contains("--format"));
    }

    @Test
    void testExportJsonWithAutoDetection() throws Exception {
        // Insert test vectors
        double[][] vectors = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("test.json");
        command.execute(context, new String[]{exportFile.toString()});

        // Verify file exists and contains data
        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("["));
        assertTrue(content.contains("\"id\""));
        assertTrue(content.contains("\"vector\""));
        assertTrue(content.contains("1.0"));
        assertTrue(content.contains("2.0"));
        assertTrue(content.contains("3.0"));

        String output = outContent.toString();
        assertTrue(output.contains("Successfully exported"));
        assertTrue(output.contains("2 vector(s)"));
    }

    @Test
    void testExportCsvWithAutoDetection() throws Exception {
        // Insert test vectors
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("test.csv");
        command.execute(context, new String[]{exportFile.toString()});

        // Verify file exists and contains data
        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("id,vector,metadata"));
        assertTrue(content.contains("1.0"));
        assertTrue(content.contains("2.0"));

        String output = outContent.toString();
        assertTrue(output.contains("Successfully exported"));
        assertTrue(output.contains("2 vector(s)"));
    }

    @Test
    void testExportWithExplicitJsonFormat() throws Exception {
        double[][] vectors = {{7.0, 8.0, 9.0}};
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("data.txt");
        command.execute(context, new String[]{exportFile.toString(), "--format", "json"});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("\"vector\""));
        assertTrue(content.contains("7.0"));
    }

    @Test
    void testExportWithExplicitCsvFormat() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("data.txt");
        command.execute(context, new String[]{exportFile.toString(), "--format", "csv"});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("id,vector,metadata"));
    }

    @Test
    void testExportWithMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        List<Map<String, Object>> metadata = List.of(
                Map.of("label", "first", "value", 100),
                Map.of("label", "second", "value", 200)
        );
        client.insert(vectors, metadata);

        Path exportFile = tempDir.resolve("with_meta.json");
        command.execute(context, new String[]{exportFile.toString()});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("\"metadata\""));
        assertTrue(content.contains("first"));
        assertTrue(content.contains("second"));
    }

    @Test
    void testExportEmptyDatabase() throws Exception {
        Path exportFile = tempDir.resolve("empty.json");
        command.execute(context, new String[]{exportFile.toString()});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("[")); // Empty JSON array
        assertTrue(content.contains("]"));

        String output = outContent.toString();
        assertTrue(output.contains("0 vector(s)"));
    }

    @Test
    void testExportOverwritesExistingFile() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("test.json");
        Files.writeString(exportFile, "old content");
        assertTrue(Files.exists(exportFile));

        command.execute(context, new String[]{exportFile.toString()});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertFalse(content.contains("old content"));
        assertTrue(content.contains("\"vector\""));

        String output = outContent.toString();
        assertTrue(output.contains("Warning"));
        assertTrue(output.contains("overwritten"));
    }

    @Test
    void testExportWithoutFormatSpecification() {
        Path exportFile = tempDir.resolve("noext");

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{exportFile.toString()});
        });
    }

    @Test
    void testExportWithUnsupportedFormat() {
        Path exportFile = tempDir.resolve("test.txt");

        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{exportFile.toString(), "--format", "xml"});
        });
    }

    @Test
    void testExportWithNoArgs() {
        assertThrows(Exception.class, () -> {
            command.execute(context, new String[]{});
        });
    }

    @Test
    void testExportWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        assertThrows(Exception.class, () -> {
            command.execute(emptyContext, new String[]{"test.json"});
        });
    }

    @Test
    void testExportLargeDataset() throws Exception {
        // Insert many vectors
        int count = 100;
        double[][] vectors = new double[count][10];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < 10; j++) {
                vectors[i][j] = i * 10 + j;
            }
        }
        client.insert(vectors, null);

        Path exportFile = tempDir.resolve("large.json");
        command.execute(context, new String[]{exportFile.toString()});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.length() > 1000); // Should be substantial

        String output = outContent.toString();
        assertTrue(output.contains("100 vector(s)"));
    }

    @Test
    void testExportCsvWithMetadata() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        List<Map<String, Object>> metadata = List.of(
                Map.of("key1", "value1", "key2", "value2")
        );
        client.insert(vectors, metadata);

        Path exportFile = tempDir.resolve("meta.csv");
        command.execute(context, new String[]{exportFile.toString()});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("key1"));
        assertTrue(content.contains("value1"));
    }

    @Test
    void testExportFormatFlag() throws Exception {
        double[][] vectors = {{1.0, 2.0}};
        client.insert(vectors, null);

        // Test -f short form
        Path exportFile = tempDir.resolve("test.data");
        command.execute(context, new String[]{exportFile.toString(), "-f", "json"});

        assertTrue(Files.exists(exportFile));
        String content = Files.readString(exportFile);
        assertTrue(content.contains("\"vector\""));
    }
}
