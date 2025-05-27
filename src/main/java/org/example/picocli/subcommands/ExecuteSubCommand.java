package org.example.picocli.subcommands;

import org.example.config.core.AppConfiguration;
import org.example.logging.core.LogLevel;
import org.example.logging.facade.LogManager;
import org.example.main;
import org.example.operations.AvailableOperation;
import org.example.operations.Operation;
import org.example.operations.OperationRequest;
import org.example.operations.OperationResult;
import org.example.operations.dns.DnsOperationFactory;
import org.example.operations.plesk.PleskOperationFactory;
import org.example.token_handler.TokenProcessor;
import org.example.value_types.Token;
import picocli.CommandLine;

import javax.naming.CommunicationException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@CommandLine.Command(
        name = "execute",
        description = "Executes command from signed base64 token. Usage: execute [TOKEN] "
)


public class ExecuteSubCommand extends AbstractSubCommand {


    @CommandLine.Option(names = {"--debug"}, description = "Enable debug output. Also prints everything logged.")
    static boolean debug;
    @CommandLine.Option(names = {"--verbose"}, description = "Print logged information.")
    static boolean verbose;
    @CommandLine.Parameters(index = "0", description = "The signed token")
    private String encodedJson;
    @CommandLine.Unmatched
    private java.util.List<String> unmatchedArgs;


    public ExecuteSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        setupLogging();
        AppConfiguration.getInstance().initializeLazily();
        if (unmatchedArgs != null && !unmatchedArgs.isEmpty()) {
            System.err.println("Unexpected extra arguments: " + unmatchedArgs);
            System.err.println("Usage: execute <signed-token>");
            System.err.println(
                    "Hint: You should pass a single base64-encoded signed token, not separate command components.");
            return 1;
        }

        try {
            OperationRequest operationRequest = decodeAndProcessToken(encodedJson);
            try {
                OperationResult result = executeCommand(operationRequest);
                System.out.println(result.toPrettyJson());
                return 0;
            } catch (IllegalArgumentException e) {
                List<String> availableCommandsList = getAvailableCommandsList();
                System.out.println(OperationResult.notFound("Unknown operation: [" +
                        operationRequest.toString() + "] " +
                        availableCommandsList).toPrettyJson());
                return 0;
            }
        } catch (TokenProcessor.SignatureValidationFailException e) {
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.UNAUTHORIZED, e.getMessage())
                    .toPrettyJson());
            return 0;
        } catch (Exception e) {
            System.out.println(OperationResult.failure(OperationResult.ExecutionStatus.INTERNAL_ERROR,
                            e.getMessage())
                    .toPrettyJson());
            return 0;
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

    private OperationRequest decodeAndProcessToken(String encodedToken) throws CommunicationException,
            TokenProcessor.SignatureValidationFailException {
        String rawJson = new String(Base64.getDecoder().decode(encodedToken));
        Token token = Token.fromJson(rawJson);
        return new TokenProcessor()
                .processToken(token)
                .orElseThrow(CommunicationException::new);
    }

    private OperationResult executeCommand(OperationRequest operationRequest) {
        Operation executor = getExecutorForCommand(operationRequest);
        return executor.execute();
    }

    private List<String> getAvailableCommandsList() {
        List<String> availableCommands = new ArrayList<>();
        availableCommands.add("Available commands:");
        availableCommands.add("PLESK:");
        for (AvailableOperation.Plesk cmd : AvailableOperation.Plesk.values()) {
            availableCommands.add("  PLESK." + cmd.name());
        }

        availableCommands.add("NS:");
        for (AvailableOperation.DNS cmd : AvailableOperation.DNS.values()) {
            availableCommands.add("  NS." + cmd.name());
        }
        return availableCommands;
    }

    private Operation getExecutorForCommand(OperationRequest operationRequest) {
        return switch (operationRequest.commandName()) {
            case AvailableOperation.Plesk pleskCommand -> new PleskOperationFactory().build(operationRequest);
            case AvailableOperation.DNS DNSCommand -> new DnsOperationFactory().build(operationRequest);
        };
    }
}
