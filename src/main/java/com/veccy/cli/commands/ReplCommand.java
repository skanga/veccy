package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

/**
 * Start interactive REPL mode.
 */
public class ReplCommand implements Command {

    @Override
    public String getName() {
        return "repl";
    }

    @Override
    public String getDescription() {
        return "Start interactive REPL mode";
    }

    @Override
    public String getUsage() {
        return "repl";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        // If this command is executed, we're already in REPL mode
        // (since it can only be called from within the CLI)
        System.out.println("Already in REPL mode.");
        System.out.println("Type 'help' for available commands or 'exit' to quit.");
    }
}
