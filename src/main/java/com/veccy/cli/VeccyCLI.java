package com.veccy.cli;

import com.veccy.cli.commands.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Main CLI entry point for Veccy.
 * Provides a command-line interface for managing vector databases.
 */
public class VeccyCLI {

    private static final String VERSION = "1.0.0";
    private static final Map<String, Command> commands = new HashMap<>();
    private static CLIContext context;

    static {
        // Register all commands
        registerCommand(new HelpCommand());
        registerCommand(new VersionCommand());
        registerCommand(new InitCommand());
        registerCommand(new InfoCommand());
        registerCommand(new StatsCommand());
        registerCommand(new InsertCommand());
        registerCommand(new SearchCommand());
        registerCommand(new DeleteCommand());
        registerCommand(new UpdateCommand());
        registerCommand(new ListCommand());
        registerCommand(new ListVectorsCommand());
        registerCommand(new ImportCommand());
        registerCommand(new ExportCommand());
        registerCommand(new BatchSearchCommand());
        registerCommand(new BatchUpdateCommand());
        registerCommand(new ReplCommand());
        registerCommand(new ExitCommand());
    }

    public static void main(String[] args) {
        context = new CLIContext();

        if (args.length == 0) {
            // Interactive mode
            startInteractiveMode();
        } else {
            // Single command mode
            executeSingleCommand(args);
        }
    }

    /**
     * Register a command.
     */
    private static void registerCommand(Command command) {
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }

    /**
     * Execute a single command and exit.
     */
    private static void executeSingleCommand(String[] args) {
        try {
            String commandName = args[0];
            String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

            Command command = commands.get(commandName);
            if (command == null) {
                System.err.println("Unknown command: " + commandName);
                System.err.println("Type 'veccy help' for available commands.");
                System.exit(1);
            }

            command.execute(context, commandArgs);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (context.isVerbose()) {
                e.printStackTrace();
            }
            System.exit(1);
        } finally {
            cleanup();
        }
    }

    /**
     * Start interactive REPL mode.
     */
    private static void startInteractiveMode() {
        printBanner();
        System.out.println("Type 'help' for available commands, 'exit' to quit.\n");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("veccy> ");
                String line = reader.readLine();

                if (line == null) {
                    break; // EOF
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.equals("exit") || line.equals("quit")) {
                    break;
                }

                executeInteractiveCommand(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        } finally {
            cleanup();
        }

        System.out.println("\nGoodbye!");
    }

    /**
     * Execute a command in interactive mode.
     */
    private static void executeInteractiveCommand(String line) {
        try {
            String[] parts = parseCommandLine(line);
            if (parts.length == 0) {
                return;
            }

            String commandName = parts[0];
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);

            Command command = commands.get(commandName);
            if (command == null) {
                System.err.println("Unknown command: " + commandName);
                System.err.println("Type 'help' for available commands.");
                return;
            }

            command.execute(context, args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (context.isVerbose()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parse command line into arguments, respecting quotes.
     */
    private static String[] parseCommandLine(String line) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;

        for (char c : line.toCharArray()) {
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args.toArray(new String[0]);
    }

    /**
     * Print welcome banner.
     */
    private static void printBanner() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");
        System.out.println("║   VECCY - Vector Database CLI          ║");
        System.out.println("║   Version " + VERSION + "                        ║");
        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Cleanup resources.
     */
    private static void cleanup() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * Get all registered commands.
     */
    public static Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }

    /**
     * Get CLI version.
     */
    public static String getVersion() {
        return VERSION;
    }
}
