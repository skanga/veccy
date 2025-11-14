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
 * Tests for InsertCommand.
 */
class InsertCommandTest {

    private InsertCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new InsertCommand();
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
        assertEquals("insert", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(1, aliases.length);
        assertEquals("add", aliases[0]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Insert vectors into the database", command.getDescription());
    }

    @Test
    void testGetUsage() {
        String usage = command.getUsage();
        assertTrue(usage.contains("insert <vector>"));
        assertTrue(usage.contains("--metadata"));
    }

    @Test
    void testExecuteWithBasicVector() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0"});

        String output = outContent.toString();
        assertTrue(output.contains("Inserted 1 vector"));
        assertTrue(output.contains("ID:"));

        context.close();
    }

    @Test
    void testExecuteWithBrackets() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"[1.0,2.0,3.0]"});

        String output = outContent.toString();
        assertTrue(output.contains("Inserted 1 vector"));

        context.close();
    }

    @Test
    void testExecuteWithMetadata() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "--metadata", "label=test"});

        String output = outContent.toString();
        assertTrue(output.contains("Inserted 1 vector"));
        assertTrue(output.contains("Metadata:"));
        assertTrue(output.contains("label"));
        assertTrue(output.contains("test"));

        context.close();
    }

    @Test
    void testExecuteWithShortMetadataFlag() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "-m", "key=value"});

        String output = outContent.toString();
        assertTrue(output.contains("Inserted 1 vector"));
        assertTrue(output.contains("Metadata:"));

        context.close();
    }

    @Test
    void testExecuteWithMultipleMetadata() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"1.0,2.0,3.0", "-m", "label=test", "-m", "value=42"});

        String output = outContent.toString();
        assertTrue(output.contains("Inserted 1 vector"));
        assertTrue(output.contains("Metadata:"));

        context.close();
    }

    @Test
    void testExecuteFromFile() throws Exception {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        command.execute(context, new String[]{"--file", "vectors.txt"});

        String error = errContent.toString();
        assertTrue(error.contains("not yet implemented"));

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
    void testExecuteWithNoVector() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"--metadata", "key=value"});
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

    @Test
    void testExecuteFileWithoutPath() {
        VectorDBClient client = VectorDBFactory.createSimple();
        CLIContext context = new CLIContext();
        context.setClient(client);

        assertThrows(IllegalArgumentException.class, () -> {
            command.execute(context, new String[]{"--file"});
        });

        context.close();
    }
}
