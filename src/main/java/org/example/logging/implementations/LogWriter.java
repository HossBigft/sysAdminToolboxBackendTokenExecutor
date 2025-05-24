package org.example.logging.implementations;

import org.example.config.security.FileAccessPolicy;
import org.example.constants.EnvironmentConstants;
import org.example.logging.config.LogConfig;
import org.example.logging.core.LogLevel;
import org.example.logging.model.LogEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class LogWriter {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LOGDIR_PERMISSIONS = "rwxr-x---";

    private static final String LOGFILE_PERMISSIONS = "rw-r-----";


    private final LogConfig config;
    private final String userName;

    public LogWriter(LogConfig config,
                     String userName) {
        this.config = config;
        this.userName = userName;

        try {
            initializeLogFile();
        } catch (IOException e) {
            System.err.println("Failed to initialize audit log: " + e.getMessage());
        }
    }

    private void initializeLogFile() throws IOException {
        Path logDirPath = getLogDir();
        if (!Files.exists(logDirPath)) {
//            System.out.println("Creating log dir " + logDirPath);
            Files.createDirectories(logDirPath);
            try {
                getLogDirAccessPolicy().enforce();
            } catch (Exception e) {
                System.err.println(
                        "Warning: Could not set permissions on log directory: " + logDirPath + " " + e.getMessage());
            }
        }

        Path logFilePath = Paths.get(config.getFullLogPath());
        FileAccessPolicy logFileAccessPolicy = getLogFileAccessPolicy();
        if (!Files.exists(logFilePath)) {
//            System.out.println("Creating log file " + logFilePath);
            Files.createFile(logFilePath);
            try {
                logFileAccessPolicy.enforce();
            } catch (Exception e) {
                System.err.println(
                        "Warning: Could not set permissions on log file: " + logFilePath + " " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private Path getLogDir() {
        return Paths.get(config.getLogDirectory());
    }

    private FileAccessPolicy getLogDirAccessPolicy() {
        return new FileAccessPolicy(getLogDir()).permissions(LOGDIR_PERMISSIONS)
                .owner(EnvironmentConstants.SUPERADMIN_USER)
                .group(EnvironmentConstants.SUPERADMIN_USER);
    }

    private FileAccessPolicy getLogFileAccessPolicy() {
        return new FileAccessPolicy(getLogFile()).permissions(LOGFILE_PERMISSIONS)
                .owner(EnvironmentConstants.SUPERADMIN_USER)
                .group(EnvironmentConstants.SUPERADMIN_USER);
    }

    private Path getLogFile() {
        return Paths.get(config.getFullLogPath());
    }

    public synchronized void write(LogLevel level,
                                   LogEntry entry) {
        if (!config.isLoggable(level)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        StringBuilder logEntry = new StringBuilder();

        logEntry.append(String.format("[%s] [%s] User=%s", timestamp, level, userName));

        for (var field : entry.getFields().entrySet()) {
            logEntry.append(" | ").append(field.getKey()).append("=").append(field.getValue());
        }
        if (config.getGlobalLogLevel() == LogLevel.DEBUG || config.isVerbose()) {
            System.out.println(logEntry);
        }

        try (FileWriter fw = new FileWriter(config.getFullLogPath(), true); PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }
}