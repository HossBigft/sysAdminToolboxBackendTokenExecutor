package org.example.config.key_ed25519;

public class KeyManagerException extends Exception {

    public KeyManagerException(String message) {
        super(message);
    }

    public KeyManagerException(String message,
                               Throwable cause) {
        super(message, cause);
    }
}