package org.example.picocli.subcommands;

import org.example.config.core.AppConfiguration;
import org.example.config.database.DataBaseUserSetup;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import org.example.main;
import org.example.operations.OperationResult;
import picocli.CommandLine;

@CommandLine.Command(name = "checkdb", description = "runs check on database connection")

public class CheckSubCommand extends AbstractSubCommand {
    @CommandLine.Option(names = {"--debug"}, description = "Enable debug output. Also prints everything logged.")
    static boolean debug;
    @CommandLine.Option(names = {"--verbose"}, description = "Print logged information.")
    static boolean verbose;

    public CheckSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        setupLogging();
        AppConfiguration.getInstance().initialize();

        if (new DataBaseUserSetup().isDbUserAbleToConnect()) {
            System.out.println(OperationResult.success("Database user is able to connect").toPrettyJson());
        } else {
            System.out.println(OperationResult.failure("Database user can't connect").toPrettyJson());

        }
        return 0;
    }

    private static void setupLogging() {
        if (debug) {
            new LogManager.Builder().globalLogLevel(LogLevel.DEBUG).apply();
        }
        if (verbose) {
            new LogManager.Builder().setVerbose().apply();
        }
    }
}

