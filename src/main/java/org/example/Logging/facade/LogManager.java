package org.example.Logging.facade;

import org.example.Constants.EnvironmentConstants;
import org.example.Logging.config.LogConfig;
import org.example.Logging.core.CliLogger;
import org.example.Logging.core.DefaultLoggerFactory;
import org.example.Logging.core.LogLevel;
import org.example.Logging.core.LoggerFactory;
import org.example.Logging.implementations.DefaultCliLogger;
import org.example.Logging.implementations.LogWriter;
import org.example.Utils.ShellUtils;

public class LogManager {
    private static LogManager instance;
    private final CliLogger logger;

    private LogManager(LogConfig config) {
        LogWriter writer = new LogWriter(config, ShellUtils.resolveShellUser());
        LoggerFactory loggerFactory = new DefaultLoggerFactory(config, writer);
        this.logger = loggerFactory.getLogger();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new Builder().build();
        }
        return instance;
    }

    public static void initialize(LogConfig config) {
        synchronized (LogManager.class) {
            instance = new LogManager(config);
        }
    }

    public CliLogger getLogger() {
        return logger;
    }

    public static class Builder {
        private final LogConfig config;

        public Builder() {
            this.config = LogConfig.getDefaultConfig();
        }

        public Builder globalLogLevel(LogLevel level) {
            config.setGlobalLogLevel(level);
            return this;
        }

        public Builder setVerbose() {
            config.setVerbose(true);
            return this;
        }

        public Builder logDirectory(String directory) {
            config.setLogDirectory(directory);
            return this;
        }

        public Builder logFile(String filename) {
            config.setLogFile(filename);
            return this;
        }

        public LogManager build() {
            return new LogManager(config);
        }
    }
}