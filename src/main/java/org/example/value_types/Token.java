package org.example.value_types;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public record Token(
        long timestamp,
        String nonce,
        long expiry,
        String command,
        String signature
) implements ValueType {

    public Token {
        if (nonce == null || command == null || signature == null) {
            throw new IllegalArgumentException("Token fields cannot be null");
        }
    }

    public static Token fromJson(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("No value is given");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(rawMessage, new TypeReference<>() {
            });

            long timestamp = ((Number) map.get("timestamp")).longValue();
            String nonce = (String) map.get("nonce");
            long expiry = ((Number) map.get("expiry")).longValue();
            String command = (String) map.get("operation");
            String signature = (String) map.get("signature");

            return new Token(timestamp, nonce, expiry, command, signature);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > expiry;
    }

    public String getMessage() {
        return String.join("|",
                Long.toString(timestamp),
                nonce,
                Long.toString(expiry),
                command);
    }

    public String value() {
        return String.join("|",
                Long.toString(timestamp),
                nonce,
                Long.toString(expiry),
                command,
                signature);
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
