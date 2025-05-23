package org.example.picocli.subcommands;

import org.example.config.core.AppConfiguration;
import org.example.main;
import org.example.operations.OperationResult;
import picocli.CommandLine;

@CommandLine.Command(
        name = "init",
        description = "Initialises and repairs environment setup. Optionally accepts a key link to fetch and save a public key."
)
public class InitSubCommand extends AbstractSubCommand {

    @CommandLine.Parameters(
            index = "0",
            description = "Optional link to fetch public key",
            arity = "0..1"  // This makes the parameter optional (0 or 1 arguments)
    )
    private String keyLink;

    public InitSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            if (keyLink != null && !keyLink.isEmpty()) {
                System.out.println("Fetching public key from: " + keyLink);
                AppConfiguration.getInstance().setPublicKeyURI(keyLink);
                System.out.println("Public key fetched and saved successfully");
            }
            AppConfiguration.getInstance().initialize();
            return 0;
        } catch (Exception e) {
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.INTERNAL_ERROR,
                            "Failed to set up environment: " + e.getMessage())
                    .toPrettyJson());
            return 1;
        }
    }
}