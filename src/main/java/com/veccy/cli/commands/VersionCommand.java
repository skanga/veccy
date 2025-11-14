package com.veccy.cli.commands;

import com.veccy.cli.CLIContext;
import com.veccy.cli.VeccyCLI;

/**
 * Display version information.
 */
public class VersionCommand implements Command {

    @Override
    public String getName() {
        return "version";
    }

    @Override
    public String[] getAliases() {
        return new String[]{"v", "--version"};
    }

    @Override
    public String getDescription() {
        return "Display version information";
    }

    @Override
    public String getUsage() {
        return "version";
    }

    @Override
    public void execute(CLIContext context, String[] args) {
        System.out.println("Veccy CLI version " + VeccyCLI.getVersion());
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }
}
