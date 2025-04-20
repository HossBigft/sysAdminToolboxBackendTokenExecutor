package org.example.ValueTypes;

import org.example.Config.KeyManager;
import org.example.Utils.Utils;

import java.util.Arrays;

public class Token implements ValueType {
    final Long timestamp;
    final String nonce;
    final Long expiry;
    final String command;
    final String signature;

    public Token(String rawMessage) throws Exception {

        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("No value is given");
        }

        String[] parts = rawMessage.split("\\|");
        System.out.println("Splitted message: " + Arrays.toString(parts));
        if (parts.length != 5) {
            throw new IllegalArgumentException("Token has wrong number of fields: " + parts.length);
        }

        this.timestamp = Long.parseLong(parts[0]);
        this.nonce = parts[1];
        this.expiry = Long.parseLong(parts[2]);
        if (isExpired()) {
            throw new IllegalArgumentException("Token already expired.");
        }
        this.command = parts[3];
        this.signature = parts[4];
        if (!isSignatureValid()) {
            throw new IllegalArgumentException("Signature is invalid.");
        }
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > expiry;
    }

    public boolean isSignatureValid() throws Exception {
        return Utils.verifyDigitalSignature(value(), signature);
    }

    public String value() {
        return String.join("|", Long.toString(timestamp), nonce, Long.toString(expiry), command);
    }

    public String getCommand() {
        return command;
    }

    public String toString() {
        return String.join("|",
                Arrays.asList(Long.toString(timestamp), nonce, Long.toString(expiry), command, signature));
    }

}