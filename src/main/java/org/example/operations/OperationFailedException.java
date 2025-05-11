package org.example.operations;

public class OperationFailedException extends Exception {
    public OperationFailedException(String message) {
        super(message);
    }

    public OperationFailedException(String message,
                                    Throwable cause) {
        super(message,
                cause);
    }
}


