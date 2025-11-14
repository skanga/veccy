package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SearchCommand.
 */
class SearchCommandTest {

    private SearchCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new SearchCommand();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("search", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(2, aliases.length);
        assertEquals("find", aliases[0]);
        assertEquals("query", aliases[1]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Search for similar vectors", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("search <vector> [--k <count>] [--format <format>]", command.getUsage());
    }

    @Test
    void testExecuteTableFormat() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids = client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{"1.0,2.0,3.0"});

        String output = outContent.toString();
        assertTrue(output.contains("Search Results:"));
        assertTrue(output.contains("Rank"));
        assertTrue(output.contains("ID"));
        assertTrue(output.contains("Distance"));
        assertTrue(output.contains(ids.get(0)));

        context.close();
    }

    @Test
    void testExecuteJsonFormat() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
        assertTrue(output.contains("\"id\""));
        assertTrue(output.contains("\"distance\""));

        context.close();
    }

    @Test
    void testExecuteCsvFormat() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "--format", "csv"});

        String output = outContent.toString();
        assertTrue(output.contains("rank,id,distance,metadata"));

        context.close();
    }

    @Test
    void testExecuteWithKParameter() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}, {2.0, 3.0, 4.0}, {3.0, 4.0, 5.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "--k", "2"});

        String output = outContent.toString();
        assertTrue(output.contains("Search Results:"));

        context.close();
    }

    @Test
    void testExecuteWithShortKParameter() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "-k", "5"});

        String output = outContent.toString();
        assertTrue(output.contains("Search Results:"));

        context.close();
    }

    @Test
    void testExecuteWithBrackets() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"[1.0,2.0,3.0]"});

        String output = outContent.toString();
        assertTrue(output.contains("Search Results:"));

        context.close();
    }

    @Test
    void testExecuteWithMetadata() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("label", "test");
        metadata.put("value", 42);

        List<Map<String, Object>> metadataList = List.of(metadata);
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, metadataList);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0"});

        String output = outContent.toString();
        assertTrue(output.contains("Search Results:"));

        context.close();
    }

    @Test
    void testExecuteWithNoResults() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0"});

        String output = outContent.toString();
        assertTrue(output.contains("No results found"));

        context.close();
    }

    @Test
    void testExecuteWithoutOpenDatabase() {
        CLIContext context = new CLIContext();

        assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{"1.0,2.0,3.0"});
        });
    }

    @Test
    void testExecuteWithoutArgs() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{});
        });

        context.close();
    }

    @Test
    void testExecuteWithNoQueryVector() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"--k", "5"});
        });

        context.close();
    }

    @Test
    void testExecuteWithInvalidVector() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"invalid,vector,data"});
        });

        context.close();
    }
}
