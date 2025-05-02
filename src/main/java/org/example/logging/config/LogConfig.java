package org.example.logging.config;

import org.example.constants.EnvironmentConstants;
import org.example.logging.core.LogLevel;

public class LogConfig {
    public static final String DEFAULT_LOG_DIRECTORY = "/var/log/" + EnvironmentConstants.APP_NAME + "/";
    public static final String DEFAULT_LOG_FILE = "audit.log";
    private String logDirectory;
    private String logFile;
    private LogLevel globalLogLevel = LogLevel.INFO;
    private boolean verbose = false;

    public LogConfig(String logDirectory,
                     String logFile) {
        this.logDirectory = logDirectory;
        this.logFile = logFile;
    }

    public static LogConfig getDefaultConfig() {
        return new LogConfig(
                DEFAULT_LOG_DIRECTORY,
                DEFAULT_LOG_FILE
        );
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getLogPath() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public LogLevel getGlobalLogLevel() {
        return globalLogLevel;
    }

    public void setGlobalLogLevel(LogLevel globalLogLevel) {
        this.globalLogLevel = globalLogLevel;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getFullLogPath() {
        return logDirectory + logFile;
    }

    public boolean isLoggable(LogLevel level) {
        return level.getValue() <= globalLogLevel.getValue();
    }
}