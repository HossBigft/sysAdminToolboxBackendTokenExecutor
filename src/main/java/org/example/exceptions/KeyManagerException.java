package org.example.exceptions;

public class KeyManagerException extends Exception {

    /**
     * Constructs a new KeyManagerException with the specified detail message.
     *
     * @param message the detail message
     */
    public KeyManagerException(String message) {
        super(message);
    }

    /**
     * Constructs a new KeyManagerException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public KeyManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}