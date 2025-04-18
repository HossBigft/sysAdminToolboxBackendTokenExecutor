package org.example.ValueTypes;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;

public class Token implements ValueType {
    final Long timestamp;
    final String nonce;
    final Long expiry;
    final String command;
    final String signature;

    public Token(String rawMessage) {

        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("No value is given");
        }

        String[] parts = rawMessage.split("\\|", 4);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Token has wrong number of fields: " + parts.length);
        }

        this.timestamp = Long.parseLong(parts[0]);
        this.nonce = parts[1];
        this.expiry = Long.parseLong(parts[2]);
        if (isExpired()) {
            throw new IllegalArgumentException("Token already expired");
        }
        this.command = parts[3];
        this.signature = parts[4];
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis() / 1000;
        return currentTime > expiry;
    }

    public String getCommand() {
        return command;
    }

    public String toString() {
        return String.join("|",
                Arrays.asList(Long.toString(timestamp), nonce, Long.toString(expiry), command, signature));
    }

    public boolean verifySignature(PublicKey publicKey) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(value().getBytes(StandardCharsets.UTF_8));
        return verifier.verify(signature.getBytes(StandardCharsets.UTF_8));
    }

    public String value() {
        return String.join("", Long.toString(timestamp), nonce, Long.toString(expiry), command, signature);
    }

}