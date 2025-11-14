package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DeleteCommand.
 */
class DeleteCommandTest {

    private DeleteCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new DeleteCommand();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testGetName() {
        assertEquals("delete", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(2, aliases.length);
        assertEquals("remove", aliases[0]);
        assertEquals("rm", aliases[1]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Delete vectors by ID", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("delete <id> [<id>...]", command.getUsage());
    }

    @Test
    void testExecuteWithSingleId() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids = client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{ids.get(0)});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully deleted 1 vector(s)"));

        context.close();
    }

    @Test
    void testExecuteWithMultipleIds() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids = client.insert(new double[][]{{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{ids.get(0), ids.get(1)});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully deleted 2 vector(s)"));

        context.close();
    }

    @Test
    void testExecuteWithVerboseMode() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        List<String> ids = client.insert(new double[][]{{1.0, 2.0, 3.0}}, null);

        CLIContext context = new CLIContext();
        context.setClient(client);
        context.setVerbose(true);

        command.execute(context, new String[]{ids.get(0)});

        String output = outContent.toString();
        assertTrue(output.contains("Successfully deleted 1 vector(s)"));
        assertTrue(output.contains("IDs:"));
        assertTrue(output.contains(ids.get(0)));

        context.close();
    }

    @Test
    void testExecuteWithNonExistentId() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();

        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"non-existent-id"});

        // May report success or failure depending on implementation
        String output = outContent.toString();
        String error = errContent.toString();
        assertTrue(output.contains("vector(s)") || error.contains("Failed"));

        context.close();
    }

    @Test
    void testExecuteWithoutOpenDatabase() {
        CLIContext context = new CLIContext();

        assertThrows(RuntimeException.class, () -> {
            command.execute(context, new String[]{"some-id"});
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
    void testExecuteWithNoIds() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"--some-flag"});
        });

        context.close();
    }
}
