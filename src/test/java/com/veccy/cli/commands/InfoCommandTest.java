package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InfoCommand.
 */
class InfoCommandTest {

    private InfoCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new InfoCommand();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("info", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Display database information", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("info", command.getUsage());
    }

    @Test
    void testExecuteWithOpenDatabase() throws Exception {
        // Create a real context with initialized client
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);
        config.put("distance_function", "euclidean");

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setDatabasePath("/test/db");
        context.setOutputFormat("json");
        context.setVerbose(true);

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Database Information:"));
        assertTrue(output.contains("Status:"));
        assertTrue(output.contains("Path:"));
        assertTrue(output.contains("test") && output.contains("db")); // Path might be formatted differently
        assertTrue(output.contains("Output Mode:"));
        assertTrue(output.contains("json"));
        assertTrue(output.contains("Verbose:"));

        context.close();
    }

    @Test
    void testExecuteWithNullDatabasePath() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("dimensions", 3);

        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setDatabasePath(null);
        context.setOutputFormat("table");
        context.setVerbose(false);

        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Database Information:"));
        assertTrue(output.contains("Status:"));
        assertTrue(output.contains("Output Mode:"));
        assertTrue(output.contains("table"));
        assertTrue(output.contains("Verbose:"));
        assertTrue(output.contains("false"));
        assertFalse(output.contains("Path:")); // Path should not be printed when null

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
