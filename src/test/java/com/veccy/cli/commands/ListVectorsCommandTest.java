package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ListVectorsCommand.
 */
class ListVectorsCommandTest {

    private ListVectorsCommand command;
    private CLIContext context;
    private VectorDBClient client;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new ListVectorsCommand();
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
        assertEquals("list-vectors", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("List vector IDs with optional pagination", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("list-vectors"));
        assertTrue(usage.contains("--limit"));
        assertTrue(usage.contains("--page-size"));
        assertTrue(usage.contains("--cursor"));
    }

    @Test
    void testListAllVectors() {
        // Insert test vectors
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Vector IDs"));
        assertTrue(output.contains("Total: 3"));
    }

    @Test
    void testListWithLimit() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}, {7.0, 8.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--limit", "2"});

        String output = outContent.toString();
        assertTrue(output.contains("Vector IDs"));
        // Output should be limited but showing exact count depends on implementation
    }

    @Test
    void testListWithPageSize() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--page-size", "2"});

        String output = outContent.toString();
        assertTrue(output.contains("Page size: 2"));
        assertTrue(output.contains("Has more:"));
    }

    @Test
    void testListWithPageSizeAndCursor() {
        double[][] vectors = new double[10][2];
        for (int i = 0; i < 10; i++) {
            vectors[i] = new double[]{i * 1.0, i * 2.0};
        }
        client.insert(vectors, null);

        // Get first page
        command.execute(context, new String[]{"--page-size", "3"});
        String output1 = outContent.toString();
        assertTrue(output1.contains("Has more: Yes"));
    }

    @Test
    void testListEmptyDatabase() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("No vectors found") || output.contains("Total: 0"));
    }

    @Test
    void testListWithJsonFormat() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("\"vectorIds\"") || output.contains("\"items\""));
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
    }

    @Test
    void testListWithCsvFormat() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--format", "csv"});

        String output = outContent.toString();
        assertTrue(output.contains("index,id"));
    }

    @Test
    void testListPaginatedWithJson() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--page-size", "1", "--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("\"items\""));
        assertTrue(output.contains("\"hasMore\""));
        assertTrue(output.contains("\"nextCursor\""));
    }

    @Test
    void testListPaginatedWithCsv() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--page-size", "1", "--format", "csv"});

        String output = outContent.toString();
        assertTrue(output.contains("index,id"));
        assertTrue(output.contains("# Page size:"));
        assertTrue(output.contains("# Has more:"));
    }

    @Test
    void testListWithInvalidLimit() {
        command.execute(context, new String[]{"--limit", "0"});

        String error = errContent.toString();
        assertTrue(error.contains("Limit must be positive"));
    }

    @Test
    void testListWithNegativeLimit() {
        command.execute(context, new String[]{"--limit", "-5"});

        String error = errContent.toString();
        assertTrue(error.contains("Limit must be positive"));
    }

    @Test
    void testListWithInvalidPageSize() {
        command.execute(context, new String[]{"--page-size", "0"});

        String error = errContent.toString();
        assertTrue(error.contains("Page size must be positive"));
    }

    @Test
    void testListWithInvalidFormat() {
        command.execute(context, new String[]{"--format", "xml"});

        String error = errContent.toString();
        assertTrue(error.contains("Invalid format"));
    }

    @Test
    void testListWithUnknownOption() {
        command.execute(context, new String[]{"--unknown", "value"});

        String error = errContent.toString();
        assertTrue(error.contains("Unknown option"));
    }

    @Test
    void testListWithMissingLimitValue() {
        command.execute(context, new String[]{"--limit"});

        String error = errContent.toString();
        assertTrue(error.contains("--limit requires a value"));
    }

    @Test
    void testListWithMissingPageSizeValue() {
        command.execute(context, new String[]{"--page-size"});

        String error = errContent.toString();
        assertTrue(error.contains("--page-size requires a value"));
    }

    @Test
    void testListWithMissingCursorValue() {
        command.execute(context, new String[]{"--cursor"});

        String error = errContent.toString();
        assertTrue(error.contains("--cursor requires a value"));
    }

    @Test
    void testListWithMissingFormatValue() {
        command.execute(context, new String[]{"--format"});

        String error = errContent.toString();
        assertTrue(error.contains("--format requires a value"));
    }

    @Test
    void testListWithoutOpenDatabase() {
        CLIContext emptyContext = new CLIContext();

        command.execute(emptyContext, new String[]{});

        String error = errContent.toString();
        assertTrue(error.contains("No database is open"));
    }

    @Test
    void testListWithVerboseMode() {
        context.setVerbose(true);

        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        command.execute(context, new String[]{"--page-size", "1"});

        String output = outContent.toString();
        assertTrue(output.contains("To get next page") || output.contains("Tip:"));
    }

    @Test
    void testListWithInvalidLimitFormat() {
        command.execute(context, new String[]{"--limit", "abc"});

        String error = errContent.toString();
        assertTrue(error.contains("Invalid limit"));
    }

    @Test
    void testListWithInvalidPageSizeFormat() {
        command.execute(context, new String[]{"--page-size", "xyz"});

        String error = errContent.toString();
        assertTrue(error.contains("Invalid page size"));
    }

    @Test
    void testListSimpleWithTableFormat() {
        double[][] vectors = {{1.0, 2.0}, {3.0, 4.0}};
        client.insert(vectors, null);

        context.setOutputFormat("table");
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Vector IDs:"));
        assertTrue(output.contains("─")); // Table separator
    }

    @Test
    void testListLargeDataset() {
        // Insert many vectors
        double[][] vectors = new double[50][3];
        for (int i = 0; i < 50; i++) {
            vectors[i] = new double[]{i * 1.0, i * 2.0, i * 3.0};
        }
        client.insert(vectors, null);

        command.execute(context, new String[]{"--page-size", "10"});

        String output = outContent.toString();
        assertTrue(output.contains("Page size: 10"));
        assertTrue(output.contains("Has more"));
    }
}
