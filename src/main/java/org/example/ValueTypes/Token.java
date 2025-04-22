package org.example.ValueTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Utils.Utils;

import java.io.IOException;

public class Token implements ValueType {
    private final JsonNode jsonNode;
    private final long timestamp;
    private final String nonce;
    private final long expiry;
    private final String command;
    private final String signature;
    private final String originalJson;

    public Token(String rawMessage) throws Exception {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("No value is given");
        }

        this.originalJson = rawMessage;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonNode = objectMapper.readTree(rawMessage);

            // Extract values manually from the JsonNode
            if (!jsonNode.has("timestamp") || !jsonNode.has("nonce") ||
                    !jsonNode.has("expiry") || !jsonNode.has("command") ||
                    !jsonNode.has("signature")) {
                throw new IllegalArgumentException("Missing required fields in token");
            }

            this.timestamp = jsonNode.get("timestamp").asLong();
            this.nonce = jsonNode.get("nonce").asText();
            this.expiry = jsonNode.get("expiry").asLong();
            this.command = jsonNode.get("command").asText();
            this.signature = jsonNode.get("signature").asText();

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
        return currentTime > expiry;
    }

    public boolean isSignatureValid() throws Exception {
        // Create a new JSON object without the signature field to match Python's signing approach
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

        // Create a clean object with the exact same field order as Python
        JsonNode tokenDataNode = mapper.createObjectNode()
                .put("timestamp", timestamp)
                .put("nonce", nonce)
                .put("expiry", expiry)
                .put("command", command);

        // Convert to string with compact formatting (no whitespace)
        String dataToVerify = mapper.writeValueAsString(tokenDataNode);

        return Utils.verifyDigitalSignature(dataToVerify, signature);
    }

    public String value() {

        return String.join("|",
                Long.toString(timestamp),
                nonce,
                Long.toString(expiry),
                command);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return String.join("|",
                Long.toString(timestamp),
                nonce,
                Long.toString(expiry),
                command,
                signature);
    }
}