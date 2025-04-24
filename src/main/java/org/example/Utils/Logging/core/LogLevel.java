package org.example.Utils.Logging.core;

public enum LogLevel {
    ERROR(0), WARN(1), INFO(2), DEBUG(3);

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}