package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

/**
 * Exit the CLI.
 */
public class ExitCommand implements Command {

    @Override
    public String getName() {
        return "exit";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"quit", "q"};
    }

    @Override
    public String getDescription() {
        return "Exit the CLI";
    }

    @Override
    public String getUsage() {
        return "exit";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        System.out.println("Goodbye!");

        // Close the context (which closes the database)
        context.close();

        // Exit the application
        System.exit(0);
    }
}
