package org.example.Logging.implementations;


import org.example.Logging.core.LogLevel;
import org.example.Logging.core.CliLogger;
import org.example.Logging.model.LogEntry;

class DefaultEntryBuilder implements CliLogger.EntryBuilder {
    private final LogEntry entry = new LogEntry();
    private final LogLevel level;
    private final LogWriter writer;

    DefaultEntryBuilder(LogLevel level, LogWriter writer) {
        this.level = level;
        this.writer = writer;
    }

    @Override
    public CliLogger.EntryBuilder message(String message) {
        entry.message(message);
        return this;
    }

    @Override
    public CliLogger.EntryBuilder field(String key, Object value) {
        entry.field(key, value);
        return this;
    }

    @Override
    public CliLogger.EntryBuilder exception(Throwable t) {
        entry.exception(t);
        return this;
    }

    @Override
    public CliLogger.EntryBuilder command(String... args) {
        entry.command(args);
        return this;
    }


    @Override
    public void log() {
        writer.write(level, entry);
    }
}