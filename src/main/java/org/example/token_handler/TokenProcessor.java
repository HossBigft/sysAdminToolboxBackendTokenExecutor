package org.example.token_handler;

import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.AvailableOperation;
import org.example.operations.OperationRequest;
import org.example.value_types.Token;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;


public class TokenProcessor {

    public Optional<OperationRequest> processToken(Token token) throws SignatureValidationFailException {
        getLogger().
                infoEntry().message("Processing command token").field("Token", token.value()).log();
        Optional<OperationRequest> command = Optional.empty();

        if (!TokenManager.TokenValidator.isValid(token)) {
            getLogger().
                    warnEntry().message("Token validation failed").field("Token", token.value()).log();
            throw new SignatureValidationFailException();
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

    public static OperationRequest parseCommand(String commandLine) {
        if (commandLine.isBlank()) {
            throw new IllegalArgumentException("No command provided");
        }
        String[] args = commandLine.split(" ");
        if (args.length == 0) {
            throw new IllegalArgumentException("Invalid command input");
        }

        AvailableOperation commandName = AvailableOperation.valueOf(args[0]);
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
        return new OperationRequest(commandName, commandArgs);
    }

    public static class SignatureValidationFailException extends Exception {

        public SignatureValidationFailException(String message) {
            super(message);
        }
        public SignatureValidationFailException() {
            super("Signature validation failed. Signature or public key are invalid.");
        }
        public SignatureValidationFailException(String message,
                                                Throwable cause) {
            super(message, cause);
        }
    }
}