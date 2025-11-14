package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for HelpCommand.
 */
class HelpCommandTest {

    private HelpCommand command;
    private CLIContext context;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        command = new HelpCommand();
        context = new CLIContext();
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
        assertEquals("help", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();

        assertNotNull(aliases);
        assertEquals(2, aliases.length);
        assertEquals("h", aliases[0]);
        assertEquals("?", aliases[1]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Display help information", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("help [command]", command.getUsage());
    }

    @Test
    void testExecuteGeneralHelp() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Veccy CLI"));
        assertTrue(output.contains("USAGE:"));
        assertTrue(output.contains("COMMANDS:"));
    }

    @Test
    void testExecuteGeneralHelpShowsCategories() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Database Management:"));
        assertTrue(output.contains("Vector Operations:"));
        assertTrue(output.contains("Batch Operations:"));
        assertTrue(output.contains("Data Management:"));
        assertTrue(output.contains("General:"));
    }

    @Test
    void testExecuteSpecificCommandHelp() {
        command.execute(context, new String[]{"version"});

        String output = outContent.toString();
        assertTrue(output.contains("Command: version"));
        assertTrue(output.contains("Description:"));
        assertTrue(output.contains("Usage:"));
    }

    @Test
    void testExecuteUnknownCommand() {
        command.execute(context, new String[]{"unknown_command"});

        String error = errContent.toString();
        assertTrue(error.contains("Unknown command"));
        assertTrue(error.contains("unknown_command"));
    }

    @Test
    void testExecuteHelpForHelp() {
        command.execute(context, new String[]{"help"});

        String output = outContent.toString();
        assertTrue(output.contains("Command: help"));
        assertTrue(output.contains("Display help information"));
    }

    @Test
    void testExecuteMultipleArgs() {
        // Should only use first arg
        command.execute(context, new String[]{"version", "extra", "args"});

        String output = outContent.toString();
        assertTrue(output.contains("Command: version"));
    }

    @Test
    void testExecuteEmptyArgs() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Veccy CLI"));
        assertFalse(output.contains("Command:")); // Not specific command help
    }
}
