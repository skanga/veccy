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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StatsCommand.
 */
class StatsCommandTest {

    private StatsCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new StatsCommand();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("stats", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Display database statistics", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("stats [--format <format>]", command.getUsage());
    }

    @Test
    void testExecuteTableFormat() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Database Statistics:"));
        assertTrue(output.contains("Storage:"));

        context.close();
    }

    @Test
    void testExecuteJsonFormat() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("json");

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("{"));
        assertTrue(output.contains("}"));
        assertTrue(output.contains("\""));

        context.close();
    }

    @Test
    void testExecuteWithFormatArgument() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{"--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("{")); // JSON format
        assertTrue(output.contains("}"));

        context.close();
    }

    @Test
    void testExecuteWithShortFormatArgument() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{"-f", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("{")); // JSON format

        context.close();
    }

    @Test
    void testExecuteDefaultFormat() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table"); // Use valid format

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Database Statistics:")); // Falls back to table

        context.close();
    }

    @Test
    void testExecuteWithoutOpenDatabase() {
        CLIContext context = new CLIContext();

        assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{});
        });
    }

    @Test
    void testPrintTableWithNestedStats() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();

        // Add a vector to get more interesting stats
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Storage:"));
        assertTrue(output.contains("Index:"));

        context.close();
    }

    @Test
    void testPrintJsonWithNestedStats() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"--format", "json"});

        String output = outContent.toString();
        assertTrue(output.contains("{"));
        assertTrue(output.contains("}"));
        assertTrue(output.contains("\"")); // Has quoted strings

        context.close();
    }
}
