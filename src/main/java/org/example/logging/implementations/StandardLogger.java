package org.example.logging.implementations;


import org.example.logging.config.LogConfig;
import org.example.logging.core.CliLogger;
import org.example.logging.core.LogLevel;
import org.example.logging.model.LogEntry;

public class StandardLogger implements CliLogger {
    private final LogWriter writer;
    private final LogConfig config;

    public StandardLogger(LogConfig config,
                          LogWriter writer) {
        this.writer = writer;
        this.config = config;
    }

    @Override
    public void debug(String message) {
        logMessage(LogLevel.DEBUG, message);
    }

    private void logMessage(LogLevel level,
                            String message) {
        if (config.isLoggable(level)) {
            writer.write(level, new LogEntry().message(message));
        }
    }

    @Override
    public void info(String message) {
        logMessage(LogLevel.INFO, message);
    }

    @Override
    public void warn(String message) {
        logMessage(LogLevel.WARN, message);
    }

    @Override
    public void error(String message) {
        logMessage(LogLevel.ERROR, message);
    }

    @Override
    public void error(String message,
                      Throwable t) {
        writer.write(LogLevel.ERROR, new LogEntry().message(message).exception(t));
    }

    @Override
    public EntryBuilder debugEntry() {
        return createEntryBuilder(LogLevel.DEBUG);
    }

    private EntryBuilder createEntryBuilder(LogLevel level) {
        if (config.isLoggable(level)) {
            return new LogEntryBuilder(level, writer);
        } else {
            return NoOpEntryBuilder.INSTANCE;
        }
    }

    @Override
    public EntryBuilder infoEntry() {
        return createEntryBuilder(LogLevel.INFO);
    }

    @Override
    public EntryBuilder warnEntry() {
        return createEntryBuilder(LogLevel.WARN);
    }

    @Override
    public EntryBuilder errorEntry() {
        return createEntryBuilder(LogLevel.ERROR);
    }

    private static class LogEntryBuilder implements EntryBuilder {
        private final LogLevel level;
        private final LogWriter writer;
        private final LogEntry entry;

        public LogEntryBuilder(LogLevel level,
                               LogWriter writer) {
            this.level = level;
            this.writer = writer;
            this.entry = new LogEntry();
        }

        @Override
        public EntryBuilder message(String message) {
            entry.message(message);
            return this;
        }

        @Override
        public EntryBuilder field(String key,
                                  Object value) {
            entry.field(key, value);
            return this;
        }

        @Override
        public EntryBuilder exception(Throwable t) {
            entry.exception(t);
            return this;
        }

        @Override
        public EntryBuilder command(String... args) {
            entry.command(args);
            return this;
        }

        @Override
        public void log() {
            writer.write(level, entry);
        }
    }

    private static class NoOpEntryBuilder implements EntryBuilder {
        static final NoOpEntryBuilder INSTANCE = new NoOpEntryBuilder();

        private NoOpEntryBuilder() {
        }

        @Override
        public EntryBuilder message(String message) {
            return this;
        }

        @Override
        public EntryBuilder field(String key,
                                  Object value) {
            return this;
        }

        @Override
        public EntryBuilder exception(Throwable t) {
            return this;
        }

        @Override
        public EntryBuilder command(String... args) {
            return this;
        }

        @Override
        public void log() {
        }
    }

}