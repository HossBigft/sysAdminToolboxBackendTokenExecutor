package org.example.Logging.config;

import org.example.Constants.EnvironmentConstants;
import org.example.Logging.core.LogLevel;

public class LogConfig {
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
                "/var/log/" + EnvironmentConstants.APP_USER + "/",
                "audit.log"
        );
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getLogFile() {
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
        return logDirectory + "\"" + logFile;
    }

    public boolean isLoggable(LogLevel level) {
        return level.getValue() <= globalLogLevel.getValue();
    }
}