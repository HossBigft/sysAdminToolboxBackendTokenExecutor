package org.example.logging.facade;

import org.example.logging.config.LogConfig;
import org.example.logging.core.CliLogger;
import org.example.logging.core.LogLevel;
import org.example.logging.implementations.LogWriter;
import org.example.logging.implementations.StandardLogger;
import org.example.utils.ShellUtils;

public class LogManager {
    private static LogManager instance;
    private final CliLogger logger;

    private LogManager(LogConfig config) {
        LogWriter writer = new LogWriter(config, ShellUtils.resolveShellUser());
        this.logger = new StandardLogger(config, writer);
    }

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new Builder().build();
        }
        return instance;
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

        public void apply() {
            instance = new LogManager(config);
        }
    }


}