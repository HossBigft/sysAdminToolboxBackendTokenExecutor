package org.example.token_handler;

import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;
import org.example.ValueTypes.Token;

import java.io.IOException;
import java.util.Optional;


public class TokenProcessor {

    private static final CliLogger logger = LogManager.getInstance().getLogger();

    public Optional<String> processToken(Token token) {
        logger.infoEntry().message("Processing command token").field("Token", token.value()).log();
        Optional<String> command = Optional.empty();

        if (!TokenManager.TokenValidator.isValid(token)) {
            logger.warnEntry().message("Token validation failed").field("Token", token.value()).log();
            return command;
        }

        logger.debugEntry().message("Token signature validated successfully").field("Token", token.value()).log();

        boolean tokenUsed = TokenLifecycleManager.isTokenUsed(token);
        if (tokenUsed) {
            logger.warnEntry().message("Token has already been used").field("Token", token.value()).log();
            return command;
        }

        logger.debugEntry().message("Token is not used yet").field("Token", token.value()).log();

        try {
            TokenLifecycleManager.markTokenAsUsed(token);
        } catch (IOException e) {
            logger.errorEntry()
                    .message("Failed to mark token as used")
                    .field("Token", token.value())
                    .exception(e)
                    .log();
            return command;
        }

        logger.debugEntry().message("Token processed successfully").field("Command", token.command()).log();

        return Optional.of(token.command());
    }
}