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
 * Tests for ListCommand.
 */
class ListCommandTest {

    private ListCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new ListCommand();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("list", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(1, aliases.length);
        assertEquals("ls", aliases[0]);
    }

    @Test
    void testGetDescription() {
        assertEquals("List vectors in the database", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("list [--limit <n>] [--format <format>]", command.getUsage());
    }

    @Test
    void testExecuteTableFormat() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids1 = client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);
        List<String> ids2 = client.insert(new double[][]{{4.0, 5.0, 6.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setOutputFormat("table");

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Vector IDs"));
        assertTrue(output.contains("Index"));
        assertTrue(output.contains("ID"));
        // Verify at least one of the inserted IDs appears
        assertTrue(output.contains(ids1.get(0)) || output.contains(ids2.get(0)));

        context.close();
    }

    @Test
    void testExecuteJsonFormat() throws Exception {
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
        assertTrue(output.contains("\"total\""));
        assertTrue(output.contains("\"ids\""));

        context.close();
    }

    @Test
    void testExecuteCsvFormat() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids = client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"--format", "csv"});

        String output = outContent.toString();
        assertTrue(output.contains("index,id"));
        assertTrue(output.contains("," + ids.get(0)));

        context.close();
    }

    @Test
    void testExecuteWithLimit() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        for (int i = 0; i < 10; i++) {
            client.insert(new double[][]{{i, i, i}}, null);
        }

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"--limit", "5"});

        String output = outContent.toString();
        assertTrue(output.contains("showing 5"));

        context.close();
    }

    @Test
    void testExecuteWithShortLimit() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"-l", "1"});

        String output = outContent.toString();
        assertTrue(output.contains("Vector IDs"));

        context.close();
    }

    @Test
    void testExecuteEmptyDatabase() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("No vectors found"));

        context.close();
    }

    @Test
    void testExecuteWithUnknownOption() {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"--unknown"});
        });

        context.close();
    }

    @Test
    void testExecuteWithoutOpenDatabase() {
        CLIContext context = new CLIContext();

        assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{});
        });
    }
}
