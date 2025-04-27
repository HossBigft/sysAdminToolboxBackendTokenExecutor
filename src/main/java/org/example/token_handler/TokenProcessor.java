package org.example.token_handler;

import org.example.CommandRequest;
import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;
import org.example.AvailableCommand;
import org.example.ValueTypes.Token;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;


public class TokenProcessor {

    public Optional<CommandRequest> processToken(Token token) {
        getLogger().
                infoEntry().message("Processing command token").field("Token", token.value()).log();
        Optional<CommandRequest> command = Optional.empty();

        if (!TokenManager.TokenValidator.isValid(token)) {
            getLogger().
                    warnEntry().message("Token validation failed").field("Token", token.value()).log();
            return command;
        }

        getLogger().
                debugEntry().message("Token signature validated successfully").field("Token", token.value()).log();

        boolean tokenUsed = TokenLifecycleManager.isTokenUsed(token);
        if (tokenUsed) {
            getLogger().
                    warnEntry().message("Token has already been used").field("Token", token.value()).log();
            return command;
        }

        getLogger().
                debugEntry().message("Token is not used yet").field("Token", token.value()).log();

        try {
            TokenLifecycleManager.markTokenAsUsed(token);
        } catch (IOException e) {
            getLogger().
                    errorEntry()
                    .message("Failed to mark token as used")
                    .field("Token", token.value())
                    .exception(e)
                    .log();
            return command;
        }

        getLogger().
                debugEntry().message("Token processed successfully").field("Command", token.command()).log();

        return Optional.of(parseCommand(token.command()));
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public static CommandRequest parseCommand(String commandLine) {
        if (commandLine.isBlank()) {
            throw new IllegalArgumentException("No command provided");
        }
        String[] args = commandLine.split(" ");
        if (args.length == 0) {
            throw new IllegalArgumentException("Invalid command input");
        }

        AvailableCommand commandName = AvailableCommand.valueOf(args[0]);
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
        return new CommandRequest(commandName, commandArgs);
    }
}