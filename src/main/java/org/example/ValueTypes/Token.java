package org.example.ValueTypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Utils.Utils;

import java.io.IOException;

public class Token implements ValueType {
    private final TokenData tokenData;

    public Token(String rawMessage) throws Exception {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("No value is given");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            tokenData = objectMapper.readValue(rawMessage, TokenData.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }

        if (isExpired()) {
            throw new IllegalArgumentException("Token already expired.");
        }


        if (!isSignatureValid()) {
            throw new IllegalArgumentException("Signature is invalid.");
        }
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > tokenData.getExpiry();
    }

    public boolean isSignatureValid() throws Exception {
        return Utils.verifyDigitalSignature(value(), tokenData.getSignature());
    }

    public String value() {
        return String.join("|",
                Long.toString(tokenData.getTimestamp()),
                tokenData.getNonce(),
                Long.toString(tokenData.getExpiry()),
                tokenData.getCommand());
    }

    public String getCommand() {
        return tokenData.getCommand();
    }

    @Override
    public String toString() {
        return String.join("|",
                Long.toString(tokenData.getTimestamp()),
                tokenData.getNonce(),
                Long.toString(tokenData.getExpiry()),
                tokenData.getCommand(),
                tokenData.getSignature());
    }
}
