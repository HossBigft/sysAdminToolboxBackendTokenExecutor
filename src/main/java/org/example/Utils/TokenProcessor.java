package org.example.Utils;

import org.example.Config.TokenLifecycleManager;
import org.example.Utils.Logging.LogManager;
import org.example.ValueTypes.Token;


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

        if (TokenLifecycleManager.isTokenUsed(token)) {
            LogManager.log().warn()
                    .message("Token has already been used")
                    .field("Token", token.value())
                    .log();
            return null;
        }

        TokenLifecycleManager.markTokenAsUsed(token);

        LogManager.log().info()
                .message("Token processed successfully")
                .field("Command", token.command())
                .log();

        return token.command();
    }
}