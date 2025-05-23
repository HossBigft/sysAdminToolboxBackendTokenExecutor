package org.example;

import org.example.config.core.AppConfiguration;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import org.example.picocli.subcommands.ExecuteSubCommand;
import org.example.picocli.subcommands.HealthCheckSubCommand;
import org.example.picocli.subcommands.InitSubCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "sysadmintoolbox", description = "Safe root wrapper for executing system administration commands", version = "0.3.2", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @Option(names = {"--debug"}, description = "Enable debug output. Also prints everything logged.", scope = CommandLine.ScopeType.INHERIT)
    boolean debug;
    @Option(names = {"--verbose"}, description = "Print logged information.", scope = CommandLine.ScopeType.INHERIT)
    boolean verbose;

    public static void main(String[] args) {
        main app = new main();
        CommandLine commandLine = new CommandLine(app);

        commandLine.addSubcommand(new ExecuteSubCommand(app));
        commandLine.addSubcommand(new InitSubCommand(app));
        commandLine.addSubcommand(new HealthCheckSubCommand(app));

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

        try {
            commandLine.parseArgs(args);
        } catch (CommandLine.UnmatchedArgumentException e) {
            PrintWriter err = commandLine.getErr();
            err.println("Unknown command: " + Arrays.toString(args));
            commandLine.usage(err);
            return;
        }
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

    private static void setupLogging(main app) {
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