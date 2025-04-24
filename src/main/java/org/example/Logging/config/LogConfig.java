package org.example.Logging.config;

import org.example.Logging.core.LogLevel;

public class LogConfig {
    private LogLevel globalLogLevel = LogLevel.INFO;
    private boolean verbose = false;
    private String logDirectory;
    private String logFileName;

    public LogConfig(String logDirectory, String logFileName) {
        this.logDirectory = logDirectory;
        this.logFileName = logFileName;
    }

    public LogLevel getGlobalLogLevel() {
        return globalLogLevel;
    }

    public LogConfig setGlobalLogLevel(LogLevel level) {
        this.globalLogLevel = level;
        return this;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public LogConfig setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public String getLogFileName() {
        return logFileName;
    }

    public String getFullLogPath() {
        return logDirectory + "/" + logFileName;
    }

    public boolean isLoggable(LogLevel level) {
        return level.getValue() <= globalLogLevel.getValue();
    }
}