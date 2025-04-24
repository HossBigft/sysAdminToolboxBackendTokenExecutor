package org.example.Logging.implementations;


import org.example.Logging.config.LogConfig;
import org.example.Logging.core.LogLevel;
import org.example.Logging.core.CliLogger;
import org.example.Logging.model.LogEntry;

public class DefaultCliLogger implements CliLogger {
    private final LogWriter writer;

    public DefaultCliLogger(LogConfig config, LogWriter writer) {
        this.writer = writer;
    }

    @Override
    public void debug(String message) {
        writer.write(LogLevel.DEBUG, new LogEntry().message(message));
    }

    @Override
    public void info(String message) {
        writer.write(LogLevel.INFO, new LogEntry().message(message));
    }

    @Override
    public void warn(String message) {
        writer.write(LogLevel.WARN, new LogEntry().message(message));
    }

    @Override
    public void error(String message) {
        writer.write(LogLevel.ERROR, new LogEntry().message(message));
    }

    @Override
    public void error(String message, Throwable t) {
        writer.write(LogLevel.ERROR, new LogEntry().message(message).exception(t));
    }

    @Override
    public EntryBuilder debugEntry() {
        return new DefaultEntryBuilder(LogLevel.DEBUG, writer);
    }

    @Override
    public EntryBuilder infoEntry() {
        return new DefaultEntryBuilder(LogLevel.INFO, writer);
    }

    @Override
    public EntryBuilder warnEntry() {
        return new DefaultEntryBuilder(LogLevel.WARN, writer);
    }

    @Override
    public EntryBuilder errorEntry() {
        return new DefaultEntryBuilder(LogLevel.ERROR, writer);
    }
}