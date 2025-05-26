package org.example.picocli.subcommands;

import org.example.config.core.AppConfiguration;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import org.example.main;
import org.example.operations.OperationResult;
import picocli.CommandLine;

@CommandLine.Command(name = "init", description = "Initialises and repairs environment setup. Optionally accepts a key link to fetch and save a public key.")
public class InitSubCommand extends AbstractSubCommand {

    @CommandLine.Parameters(index = "0", description = "Optional link to fetch public key", arity = "0..1"  // This makes the parameter optional (0 or 1 arguments)
    )
    private String keyLink;

    @CommandLine.Option(names = {"--debug"}, description = "Enable debug output. Also prints everything logged.")
    static boolean debug;
    @CommandLine.Option(names = {"--verbose"}, description = "Print logged information.")
    static boolean verbose;

    public InitSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        AppConfiguration.getInstance().initializeLazily();
        setupLogging();
        try {
            if (keyLink != null && !keyLink.isEmpty()) {
                AppConfiguration.getInstance().setPublicKeyURI(keyLink);
            }
            AppConfiguration.getInstance().initialize();
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.OK, "Initialisation succeed.")
                    .toPrettyJson());
            return 0;
        } catch (Exception e) {
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.INTERNAL_ERROR,
                    "Failed to set up environment: " + e.getMessage()).toPrettyJson());
            return 1;
        }
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