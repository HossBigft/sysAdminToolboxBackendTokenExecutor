package org.example.Utils;

import org.example.Config.TokenLifecycleManager;
import org.example.Utils.Logging.LogManager;
import org.example.ValueTypes.Token;

import java.io.IOException;


public class TokenProcessor {


    public String processToken(Token token) {
        LogManager.log().info()
                .message("Processing command token")
                .field("Token", token.value())
                .log();


        if (!TokenValidator.isValid(token)) {
            LogManager.log().warn()
                    .message("Token validation failed")
                    .field("Token", token.value())
                    .log();
            return null;
        }

        LogManager.log().debug()
                .message("Token signature validated successfully")
                .field("Token", token.value())
                .log();

        boolean tokenUsed = TokenLifecycleManager.isTokenUsed(token);
        if (tokenUsed) {
            LogManager.log().warn()
                    .message("Token has already been used")
                    .field("Token", token.value())
                    .log();
            return null;
        }

        LogManager.log().debug()
                .message("Token is not used yet")
                .field("Token", token.value())
                .log();

        try {
            TokenLifecycleManager.markTokenAsUsed(token);
        } catch (IOException e) {
            new LogManager.LogEntryBuilder(LogManager.LogLevel.ERROR).message("Failed to mark token as used")
                    .field("Token", token.value())
                    .exception(e)
                    .log();
            return null;
        }
        
        LogManager.log().info()
                .message("Token processed successfully")
                .field("Command", token.command())
                .log();

        return token.command();
    }
}