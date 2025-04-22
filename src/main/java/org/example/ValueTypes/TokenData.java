package org.example.ValueTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenData {
    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("expiry")
    private Long expiry;

    @JsonProperty("command")
    private String command;

    @JsonProperty("signature")
    private String signature;
    
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getExpiry() {
        return expiry;
    }

    public void setExpiry(Long expiry) {
        this.expiry = expiry;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
