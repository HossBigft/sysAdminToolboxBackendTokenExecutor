package org.example.exceptions;

public class CommandFailedException extends Exception {
    public CommandFailedException(String message) {
        super(message);
    }

    public CommandFailedException(String message,
                                  Throwable cause) {
        super(message,
                cause);
    }
}

