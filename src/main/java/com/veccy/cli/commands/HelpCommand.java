package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.cli.VeccyCLI;

import java.util.Map;
import java.util.TreeMap;

/**
 * Display help information about commands.
 */
public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"h", "?"};
    }

    @Override
    public String getDescription() {
        return "Display help information";
    }

    @Override
    public String getUsage() {
        return "help [command]";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        if (args.length > 0) {
            // Show help for specific command
            showCommandHelp(args[0]);
        } else {
            // Show general help
            showGeneralHelp();
        }
    }

    private void showGeneralHelp() {
        System.out.println("Veccy CLI - Vector Database Command Line Interface");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  veccy <command> [options]");
        System.out.println("  veccy                       # Start interactive mode");
        System.out.println();
        System.out.println("COMMANDS:");
        System.out.println();

        // Get unique commands (no aliases)
        Map<String, Command> uniqueCommands = new TreeMap<>();
        for (Map.Entry<String, Command> entry : VeccyCLI.getCommands().entrySet()) {
            Command cmd = entry.getValue();
            if (entry.getKey().equals(cmd.getName())) {
                uniqueCommands.put(cmd.getName(), cmd);
            }
        }

        // Group commands by category
        System.out.println("  Database Management:");
        printCommand(uniqueCommands.get("init"));
        printCommand(uniqueCommands.get("info"));
        printCommand(uniqueCommands.get("stats"));

        System.out.println();
        System.out.println("  Vector Operations:");
        printCommand(uniqueCommands.get("insert"));
        printCommand(uniqueCommands.get("search"));
        printCommand(uniqueCommands.get("delete"));
        printCommand(uniqueCommands.get("update"));
        printCommand(uniqueCommands.get("list"));

        System.out.println();
        System.out.println("  Batch Operations:");
        printCommand(uniqueCommands.get("batch-search"));
        printCommand(uniqueCommands.get("batch-update"));

        System.out.println();
        System.out.println("  Data Management:");
        printCommand(uniqueCommands.get("import"));
        printCommand(uniqueCommands.get("export"));

        System.out.println();
        System.out.println("  General:");
        printCommand(uniqueCommands.get("help"));
        printCommand(uniqueCommands.get("version"));
        printCommand(uniqueCommands.get("repl"));
        printCommand(uniqueCommands.get("exit"));

        System.out.println();
        System.out.println("Use 'help <command>' for more information about a specific command.");
    }

    private void printCommand(Command cmd) {
        if (cmd != null) {
            System.out.printf("  %-15s %s%n", cmd.getName(), cmd.getDescription());
        }
    }

    private void showCommandHelp(String commandName) {
        Command cmd = VeccyCLI.getCommands().get(commandName);
        if (cmd == null) {
            System.err.println("Unknown command: " + commandName);
            return;
        }

        System.out.println("Command: " + cmd.getName());
        System.out.println();
        System.out.println("Description:");
        System.out.println("  " + cmd.getDescription());
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  " + cmd.getUsage());

        if (cmd.getAliases().length > 0) {
            System.out.println();
            System.out.println("Aliases:");
            System.out.println("  " + String.join(", ", cmd.getAliases()));
        }
    }
}
