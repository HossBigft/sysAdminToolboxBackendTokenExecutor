package org.example;

import org.example.commands.picocli.ExecuteCliCommand;
import org.example.commands.picocli.HealthCheckCliCommand;
import org.example.commands.picocli.InitCliCommand;
import org.example.config.core.AppConfiguration;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "sysadmintoolbox", description = "Safe root wrapper for executing system administration commands", version = "0.2", mixinStandardHelpOptions = true)
public class SysAdminToolboxBackendTokenExecutor implements Callable<Integer> {

    @Option(names = {"--debug"}, description = "Enable debug output", scope = CommandLine.ScopeType.INHERIT)
    boolean debug;
    @Option(names = {"--verbose"}, description = "Print log information", scope = CommandLine.ScopeType.INHERIT)
    boolean verbose;

    public static void main(String[] args) {
        SysAdminToolboxBackendTokenExecutor app = new SysAdminToolboxBackendTokenExecutor();
        CommandLine commandLine = new CommandLine(app);

        commandLine.addSubcommand(new ExecuteCliCommand(app));
        commandLine.addSubcommand(new InitCliCommand(app));
        commandLine.addSubcommand(new HealthCheckCliCommand(app));

        if (args.length == 0) {
            String sshOriginalCommand = System.getenv("SSH_ORIGINAL_COMMAND");
            if (sshOriginalCommand == null) {
                System.err.println("Error: SSH_ORIGINAL_COMMAND is missing");
                System.exit(1);
            }
            try {
                args = tokenizeSSHORIGINALCOMMAND(sshOriginalCommand);
            } catch (IOException e) {
                System.err.println("Error parsing SSH_ORIGINAL_COMMAND: " + e.getMessage());
                System.exit(1);
            }
        }


        commandLine.parseArgs(args);
        setupLogging(app);

        commandLine.setExecutionStrategy(parseResult -> {
            String subcommandName = parseResult.hasSubcommand() ?
                    parseResult.subcommand().commandSpec().name() : null;

            if (!"init".equals(subcommandName)) {
                AppConfiguration.getInstance().initializeLazily();
            }

            return new CommandLine.RunLast().execute(parseResult);
        });

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    private static String[] tokenizeSSHORIGINALCOMMAND(String cmdString) throws IOException {
        StreamTokenizer tok = new StreamTokenizer(new StringReader(cmdString));
        tok.resetSyntax();
        tok.wordChars(' ', 255);
        tok.whitespaceChars(0, ' ');
        tok.quoteChar('"');
        tok.quoteChar('\'');
        tok.commentChar('#');

        List<String> arguments = new ArrayList<>();
        while (tok.nextToken() != StreamTokenizer.TT_EOF) {
            arguments.add(tok.sval);
        }
        return arguments.toArray(String[]::new);
    }

    private static void setupLogging(SysAdminToolboxBackendTokenExecutor app) {
        if (app.debug) {
            new LogManager.Builder().globalLogLevel(LogLevel.DEBUG).apply();
        }
        if (app.verbose) {
            new LogManager.Builder().setVerbose().apply();
        }
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }
}