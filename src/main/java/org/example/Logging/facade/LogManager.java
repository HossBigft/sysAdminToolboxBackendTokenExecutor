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
    private static final String LOG_DIRECTORY = "/var/log/" + EnvironmentConstants.APP_USER + "/";
    private static final String LOG_FILE = "audit.log";

    private static LoggerFactory loggerFactory;
    private static CliLogger cliLogger;

    static {
        LogConfig config = new LogConfig(LOG_DIRECTORY, LOG_FILE);
        LogWriter writer = new LogWriter(config, ShellUtils.resolveShellUser());
        loggerFactory = new DefaultLoggerFactory(config, writer);
        cliLogger = loggerFactory.getLogger();
    }


    public static DefaultCliLogger getLogger() {
        return (DefaultCliLogger) cliLogger;
    }


    public static class Builder {
        private final LogConfig config;

        private Builder(LogConfig config) {
            this.config = config;
        }

        public static Builder config() {
            LogConfig config = new LogConfig(LOG_DIRECTORY, LOG_FILE);
            return new Builder(config);
        }

        public Builder globalLogLevel(LogLevel level) {
            config.setGlobalLogLevel(level);
            return this;
        }

        public Builder setVerbose() {
            config.setVerbose(true);
            return this;
        }

        public LogManager build() {
            LogWriter writer = new LogWriter(config, ShellUtils.resolveShellUser());
            loggerFactory = new DefaultLoggerFactory(config, writer);
            cliLogger = loggerFactory.getLogger();
            return null; 
        }
    }
}