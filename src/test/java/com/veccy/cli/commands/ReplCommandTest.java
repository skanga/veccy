package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReplCommand.
 */
class ReplCommandTest {

    private ReplCommand command;
    private CLIContext context;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new ReplCommand();
        context = new CLIContext();
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
        assertEquals("repl", command.getName());
    }

    @Test
    void testGetDescription() {
        assertEquals("Start interactive REPL mode", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("repl", command.getUsage());
    }

    @Test
    void testExecute() throws Exception {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Already in REPL mode"));
        assertTrue(output.contains("help"));
        assertTrue(output.contains("exit"));
    }

    @Test
    void testExecuteWithArgs() throws Exception {
        // Command should ignore any arguments
        command.execute(context, new String[]{"arg1", "arg2"});

        String output = outContent.toString();
        assertTrue(output.contains("Already in REPL mode"));
    }

    @Test
    void testExecuteMultipleTimes() throws Exception {
        command.execute(context, new String[]{});
        String output1 = outContent.toString();

        outContent.reset();

        command.execute(context, new String[]{});
        String output2 = outContent.toString();

        // Should produce the same message each time
        assertTrue(output1.contains("Already in REPL mode"));
        assertTrue(output2.contains("Already in REPL mode"));
    }

    @Test
    void testExecuteWithNullArgs() throws Exception {
        command.execute(context, null);

        String output = outContent.toString();
        assertTrue(output.contains("Already in REPL mode"));
    }
}
