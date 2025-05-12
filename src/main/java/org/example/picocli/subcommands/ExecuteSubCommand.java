package org.example.picocli.subcommands;

import org.example.main;
import org.example.operations.AvailableOperation;
import org.example.operations.Operation;
import org.example.operations.OperationRequest;
import org.example.operations.OperationResult;
import org.example.operations.dns.NsOperationFactory;
import org.example.operations.plesk.PleskOperationFactory;
import org.example.token_handler.TokenProcessor;
import org.example.value_types.Token;
import picocli.CommandLine;

import javax.naming.CommunicationException;
import java.util.Base64;

@CommandLine.Command(
        name = "execute",
        description = "Executes command from signed base64 token. Usage: execute [TOKEN] "
)
public class ExecuteSubCommand extends AbstractSubCommand {
    @CommandLine.Parameters(index = "0", description = "The signed token")
    private String encodedJson;

    @CommandLine.Unmatched
    private java.util.List<String> unmatchedArgs;


    public ExecuteSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        if (unmatchedArgs != null && !unmatchedArgs.isEmpty()) {
            System.err.println("Unexpected extra arguments: " + unmatchedArgs);
            System.err.println("Usage: execute <signed-token>");
            System.err.println(
                    "Hint: You should pass a single base64-encoded signed token, not separate command components.");
            return 2;
        }

        try {
            OperationRequest operationRequest = decodeAndProcessToken(encodedJson);
            OperationResult result = executeCommand(operationRequest);
            System.out.println(result.toPrettyJson());
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid command: " + e.getMessage());
            printAvailableCommands();
            return 2;
        } catch (Exception e) {
            System.err.println("Failed to execute token: " + encodedJson);
            e.printStackTrace();
            return 1;
        }
    }

    private OperationRequest decodeAndProcessToken(String encodedToken) throws Exception {
        String rawJson = new String(Base64.getDecoder().decode(encodedToken));
        Token token = Token.fromJson(rawJson);
        return new TokenProcessor()
                .processToken(token)
                .orElseThrow(CommunicationException::new);
    }

    private OperationResult executeCommand(OperationRequest operationRequest) throws Exception {
        Operation executor = getExecutorForCommand(operationRequest);
        return executor.execute();
    }

    private void printAvailableCommands() {
        System.err.println("Available commands:");
        System.err.println("PLESK:");
        for (AvailableOperation.Plesk cmd : AvailableOperation.Plesk.values()) {
            System.err.println("  PLESK." + cmd.name());
        }

        System.err.println("NS:");
        for (AvailableOperation.NS cmd : AvailableOperation.NS.values()) {
            System.err.println("  NS." + cmd.name());
        }
    }

    private Operation getExecutorForCommand(OperationRequest operationRequest) {
        return switch (operationRequest.commandName()) {
            case AvailableOperation.Plesk pleskCommand -> new PleskOperationFactory().build(operationRequest);
            case AvailableOperation.NS nsCommand -> new NsOperationFactory().build(operationRequest);
        };
    }
}
