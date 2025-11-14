package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

/**
 * Base interface for all CLI commands.
 */
public interface Command {

    /**
     * Get the command name.
     */
    String getName();

    /**
     * Get command aliases.
     */
    default String[] getAliases() {
        return new String[0];
    }

    /**
     * Get command description.
     */
    String getDescription();

    /**
     * Get command usage.
     */
    String getUsage();

    /**
     * Execute the command.
     *
     * @param context the CLI context
     * @param args    command arguments
     * @throws Exception if command execution fails
     */
    void execute(CLIContext context, String[] args) throws Exception;

    /**
     * Validate required number of arguments.
     */
    default void requireArgs(String[] args, int min) {
        if (args.length < min) {
            throw new IllegalArgumentException("Insufficient arguments. Usage: " + getUsage());
        }
    }

    /**
     * Validate that database is open.
     */
    default void requireOpenDatabase(CLIContext context) {
        if (!context.hasOpenDatabase()) {
            throw new IllegalStateException("No database is open. Use 'init' command first.");
        }
    }
}
