package org.example.Utils.Logging.facade;

import org.example.Constants.EnvironmentConstants;
import org.example.Utils.Logging.config.LogConfig;
import org.example.Utils.Logging.core.DefaultLoggerFactory;
import org.example.Utils.Logging.core.LogLevel;
import org.example.Utils.Logging.core.CliLogger;
import org.example.Utils.Logging.core.LoggerFactory;
import org.example.Utils.Logging.implementations.DefaultCliLogger;
import org.example.Utils.Logging.implementations.LogWriter;
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

    public static void debug(String message) {
        cliLogger.debug(message);
    }

    public static void info(String message) {
        cliLogger.info(message);
    }

    public static void warn(String message) {
        cliLogger.warn(message);
    }

    public static void error(String message) {
        cliLogger.error(message);
    }

    public static void error(String message, Throwable t) {
        cliLogger.error(message, t);
    }

    public static DefaultCliLogger getExtendedLogger() {
        return (DefaultCliLogger) cliLogger;
    }

    // For unit testing
    static void setLoggerFactory(LoggerFactory factory) {
        loggerFactory = factory;
        cliLogger = factory.getLogger();
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
            return null; // We're only using this for configuration
        }
    }
}