package org.example.exceptions;

public class KeyManagerException extends Exception {

    public KeyManagerException(String message) {
        super(message);
    }

    public KeyManagerException(String message,
                               Throwable cause) {
        super(message, cause);
    }
}