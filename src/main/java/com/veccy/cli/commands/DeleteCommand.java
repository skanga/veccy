package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Delete vectors by ID.
 */
public class DeleteCommand implements Command {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"remove", "rm"};
    }

    @Override
    public String getDescription() {
        return "Delete vectors by ID";
    }

    @Override
    public String getUsage() {
        return "delete <id> [<id>...]";
    }

    @Override
    public void execute(CLIContext context, String[] args) throws Exception {
        requireOpenDatabase(context);
        requireArgs(args, 1);

        List<String> ids = new ArrayList<>();
        for (String arg : args) {
            if (!arg.startsWith("-")) {
                ids.add(arg);
            }
        }

        if (ids.isEmpty()) {
            throw new IllegalArgumentException("No IDs provided. Usage: " + getUsage());
        }

        boolean success = context.getClient().delete(ids);

        if (success) {
            System.out.println("Successfully deleted " + ids.size() + " vector(s)");
            if (context.isVerbose()) {
                System.out.println("  IDs: " + String.join(", ", ids));
            }
        } else {
            System.err.println("Failed to delete some or all vectors");
        }
    }
}
