package org.example.Utils.Logging.core;


import org.example.Utils.Logging.implementations.DefaultCliLogger;
import org.example.Utils.Logging.implementations.LogWriter;
import org.example.Utils.Logging.config.LogConfig;

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