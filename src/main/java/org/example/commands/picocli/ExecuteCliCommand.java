package org.example.commands.picocli;

import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.commands.AvailableCommand;
import org.example.commands.Command;
import org.example.commands.CommandRequest;
import org.example.commands.dns.NsExecutorFactory;
import org.example.commands.plesk.PleskCommandExecutorFactory;
import org.example.token_handler.TokenProcessor;
import org.example.value_types.Token;
import picocli.CommandLine;

import javax.naming.CommunicationException;
import java.util.Base64;
import java.util.Optional;

@CommandLine.Command(
        name = "execute",
        description = "Executes command from signed base64 token. Usage: execute [TOKEN] "
)
public class ExecuteCliCommand extends AbstractCliCommand {
    @CommandLine.Parameters(index = "0", description = "The signed token")
    private String encodedJson;

    @CommandLine.Unmatched
    private java.util.List<String> unmatchedArgs;


    public ExecuteCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        if (unmatchedArgs != null && !unmatchedArgs.isEmpty()) {
            System.err.println("Unexpected extra arguments: " + unmatchedArgs);
            System.err.println("Usage: execute <signed-token>");
            System.err.println("Hint: You should pass a single base64-encoded signed token, not separate command components.");
            return 2;
        }

        try {
            CommandRequest commandRequest = decodeAndProcessToken(encodedJson);
            Optional<?> result = executeCommand(commandRequest);
            result.ifPresent(System.out::println);
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

    private void printAvailableCommands() {
        System.err.println("Available commands:");
        System.err.println("PLESK:");
        for (AvailableCommand.Plesk cmd : AvailableCommand.Plesk.values()) {
            System.err.println("  PLESK." + cmd.name());
        }

        System.err.println("NS:");
        for (AvailableCommand.NS cmd : AvailableCommand.NS.values()) {
            System.err.println("  NS." + cmd.name());
        }
    }

    private CommandRequest decodeAndProcessToken(String encodedToken) throws Exception {
        String rawJson = new String(Base64.getDecoder().decode(encodedToken));
        Token token = Token.fromJson(rawJson);
        return new TokenProcessor()
                .processToken(token)
                .orElseThrow(CommunicationException::new);
    }

    private Optional<?> executeCommand(CommandRequest commandRequest) throws Exception {
        Command<?> executor = getExecutorForCommand(commandRequest);
        return executor.execute();
    }

    private Command<?> getExecutorForCommand(CommandRequest commandRequest) {
        return switch (commandRequest.commandName()) {
            case AvailableCommand.Plesk pleskCommand -> new PleskCommandExecutorFactory().build(commandRequest);
            case AvailableCommand.NS nsCommand -> new NsExecutorFactory().build(commandRequest);
        };
    }
}
