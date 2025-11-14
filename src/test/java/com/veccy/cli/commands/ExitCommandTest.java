package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExitCommand.
 */
class ExitCommandTest {

    private ExitCommand command;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new ExitCommand();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("exit", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();
        assertNotNull(aliases);
        assertEquals(2, aliases.length);
        assertEquals("quit", aliases[0]);
        assertEquals("q", aliases[1]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Exit the CLI", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("exit", command.getUsage());
    }

    // Note: We can't test the actual execute() method because it calls System.exit()
    // which would terminate the test process. In a real scenario, you might use
    // a SecurityManager or refactor to make it testable.
}
