package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

/**
 * Display database information.
 */
public class InfoCommand implements Command {

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getDescription() {
        return "Display database information";
    }

    @Override
    public String getUsage() {
        return "info";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        requireOpenDatabase(context);

        System.out.println("Database Information:");
        System.out.println("  Status:      " + (context.getClient().isInitialized() ? "Open" : "Closed"));
        if (context.getDatabasePath() != null) {
            System.out.println("  Path:        " + context.getDatabasePath());
        }
        System.out.println("  Output Mode: " + context.getOutputFormat());
        System.out.println("  Verbose:     " + context.isVerbose());
    }
}
