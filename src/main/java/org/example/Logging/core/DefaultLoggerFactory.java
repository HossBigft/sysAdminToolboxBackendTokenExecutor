package org.example.Logging.core;


import org.example.Logging.implementations.DefaultCliLogger;
import org.example.Logging.implementations.LogWriter;
import org.example.Logging.config.LogConfig;

public class DefaultLoggerFactory implements LoggerFactory {
    private final LogConfig config;
    private final LogWriter writer;

    public DefaultLoggerFactory(LogConfig config, LogWriter writer) {
        this.config = config;
        this.writer = writer;
    }

    @Override
    public CliLogger getLogger() {
        return new DefaultCliLogger(config, writer);
    }
}