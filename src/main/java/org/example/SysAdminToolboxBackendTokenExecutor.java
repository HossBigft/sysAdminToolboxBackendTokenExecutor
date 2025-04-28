package org.example;

import org.example.commands.picocli.ExecuteCliCommand;
import org.example.commands.picocli.InitCliCommand;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "sysadmintoolbox", description = "Safe root wrapper for executing system administration commands", mixinStandardHelpOptions = true)
public class SysAdminToolboxBackendTokenExecutor implements Callable<Integer> {

    @Option(names = {"--debug"}, description = "Enable debug output", scope = CommandLine.ScopeType.INHERIT)
    boolean debug;
    @Option(names = {"--verbose"}, description = "Enable debug output", scope = CommandLine.ScopeType.INHERIT)
    boolean verbose;


    public static void main(String[] args) {

        SysAdminToolboxBackendTokenExecutor app = new SysAdminToolboxBackendTokenExecutor();
        CommandLine commandLine = new CommandLine(app);

        commandLine.addSubcommand(new ExecuteCliCommand(app));
        commandLine.addSubcommand(new InitCliCommand(app));
        commandLine.parseArgs(args);


        if (app.debug) {
            new LogManager.Builder().globalLogLevel(LogLevel.DEBUG).apply();
        }
        if (app.verbose) {
            new LogManager.Builder().setVerbose().apply();
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }


    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

}