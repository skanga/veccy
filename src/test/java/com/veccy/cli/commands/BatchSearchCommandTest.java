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

/**
 * Tests for BatchSearchCommand.
 */
class BatchSearchCommandTest {

    private BatchSearchCommand command;
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
        command = new BatchSearchCommand();
        client = VectorDBFactory.createSimple();
        context = new CLIContext();
        context.setClient(client);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Insert test vectors
        double[][] vectors = {{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}};
        client.insert(vectors, null);
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
        assertEquals("batch-search", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Search for similar vectors (batch mode)", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("batch-search"));
        assertTrue(usage.contains("--top-k"));
        assertTrue(usage.contains("--format"));
    }

    @Test
    void testBatchSearchWithCommandLineQueries() {
        command.execute(context, new String[]{"[1.0,0.0,0.0],[0.0,1.0,0.0]", "--top-k", "2"});

        String output = outContent.toString();
        assertTrue(output.contains("Query 1") || output.contains("query_index"));
        assertTrue(output.contains("2 queries"));
    }

    @Test
    void testBatchSearchFromFile() throws Exception {
        // Create query file
        String queryContent = """
                [1.0,0.0,0.0]
                [0.0,1.0,0.0]
                """;
        Path queryFile = tempDir.resolve("queries.txt");
        Files.writeString(queryFile, queryContent);

        command.execute(context, new String[]{"@" + queryFile.toString(), "--top-k", "2"});

        String output = outContent.toString();
        assertTrue(output.contains("2 queries"));
    }

    @Test
    void testBatchSearchWithTopK() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]", "--top-k", "1"});

        String output = outContent.toString();
        assertTrue(output.contains("Query 1"));
    }

    @Test
    void testBatchSearchJsonFormat() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]", "--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("\"results\""));
        assertTrue(output.contains("\"query_index\""));
        assertTrue(output.contains("\"matches\""));
    }

    @Test
    void testBatchSearchCsvFormat() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]", "--format", "csv"});

        String output = outContent.toString();
        assertTrue(output.contains("query_index"));
        assertTrue(output.contains("rank"));
    }

    @Test
    void testBatchSearchTableFormat() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]", "--format", "table"});

        String output = outContent.toString();
        assertTrue(output.contains("Query 1"));
        assertTrue(output.contains("─")); // Table separator
    }

    @Test
    void testBatchSearchWithShowVectors() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]", "--show-vectors"});

        String output = outContent.toString();
        assertTrue(output.contains("[1.0") || output.contains("1.000"));
    }

    @Test
    void testBatchSearchWithNoArgs() {
        command.execute(context, new String[]{});

        // Command prints usage to stderr when no args provided
        String output = outContent.toString();
        String error = errContent.toString();
        assertTrue(output.contains("Usage") || error.contains("Usage") ||
                   output.contains("batch-search") || error.contains("batch-search") ||
                   output.contains("queries") || error.contains("queries"));
    }

    @Test
    void testBatchSearchWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        assertThrows(Exception.class, () -> {
            command.execute(emptyContext, new String[]{"[1.0,0.0,0.0]"});
        });
    }

    @Test
    void testBatchSearchWithVerboseMode() throws Exception {
        context.setVerbose(true);

        String queryContent = "[1.0,0.0,0.0]\n[0.0,1.0,0.0]\n";
        Path queryFile = tempDir.resolve("queries.txt");
        Files.writeString(queryFile, queryContent);

        command.execute(context, new String[]{"@" + queryFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("Loaded") || output.contains("query"));
    }

    @Test
    void testBatchSearchFileWithComments() throws Exception {
        String queryContent = """
                # This is a comment
                [1.0,0.0,0.0]
                # Another comment
                [0.0,1.0,0.0]
                """;
        Path queryFile = tempDir.resolve("queries_with_comments.txt");
        Files.writeString(queryFile, queryContent);

        command.execute(context, new String[]{"@" + queryFile.toString()});

        String output = outContent.toString();
        assertTrue(output.contains("2 queries"));
    }

    @Test
    void testBatchSearchEmptyFile() throws Exception {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        command.execute(context, new String[]{"@" + emptyFile.toString()});

        String error = errContent.toString();
        assertTrue(error.contains("No query vectors"));
    }

    @Test
    void testBatchSearchInvalidVector() {
        command.execute(context, new String[]{"invalid_vector"});

        String error = errContent.toString();
        assertTrue(error.contains("Error"));
    }

    @Test
    void testBatchSearchMultipleQueries() {
        command.execute(context, new String[]{"[1.0,0.0,0.0],[0.0,1.0,0.0],[0.0,0.0,1.0]", "--top-k", "1"});

        String output = outContent.toString();
        assertTrue(output.contains("3 queries"));
    }

    @Test
    void testBatchSearchShowsTiming() {
        command.execute(context, new String[]{"[1.0,0.0,0.0]"});

        String output = outContent.toString();
        assertTrue(output.contains("ms"));
        assertTrue(output.contains("ms/query"));
    }
}
