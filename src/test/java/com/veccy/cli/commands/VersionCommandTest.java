package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for VersionCommand.
 */
class VersionCommandTest {

    private VersionCommand command;
    private CLIContext context;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        command = new VersionCommand();
        context = new CLIContext();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void testGetName() {
        assertEquals("version", command.getName());
    }

    @Test
    void testGetAliases() {
        String[] aliases = command.getAliases();

        assertNotNull(aliases);
        assertEquals(2, aliases.length);
        assertEquals("v", aliases[0]);
        assertEquals("--version", aliases[1]);
    }

    @Test
    void testGetDescription() {
        assertEquals("Display version information", command.getDescription());
    }

    @Test
    void testGetUsage() {
        assertEquals("version", command.getUsage());
    }

    @Test
    void testExecute() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        assertTrue(output.contains("Veccy CLI version"));
        assertTrue(output.contains("Java version:"));
        assertTrue(output.contains("OS:"));
    }

    @Test
    void testExecuteWithArgs() {
        // Should ignore args
        command.execute(context, new String[]{"arg1", "arg2"});

        String output = outContent.toString();
        assertTrue(output.contains("Veccy CLI version"));
    }

    @Test
    void testExecuteOutputContainsJavaVersion() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        String javaVersion = System.getProperty("java.version");
        assertTrue(output.contains(javaVersion));
    }

    @Test
    void testExecuteOutputContainsOSInfo() {
        command.execute(context, new String[]{});

        String output = outContent.toString();
        String osName = System.getProperty("os.name");
        assertTrue(output.contains(osName));
    }

    @Test
    void testExecuteMultipleTimes() {
        command.execute(context, new String[]{});
        String output1 = outContent.toString();

        outContent.reset();

        command.execute(context, new String[]{});
        String output2 = outContent.toString();

        // Both outputs should be identical
        assertEquals(output1, output2);
    }
}
