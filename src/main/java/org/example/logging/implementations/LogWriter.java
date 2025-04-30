package org.example.logging.implementations;

import org.example.config.security.FileSecurityManager;
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
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DIR_PERMISSIONS = "rwxr-x---";
    private static final FileSecurityManager.FileAccessPolicy
            logDirAccessPolicy = new FileSecurityManager.FileAccessPolicy(DIR_PERMISSIONS,
            EnvironmentConstants.SUPERADMIN_USER, EnvironmentConstants.SUPERADMIN_USER);
    private static final String FILE_PERMISSIONS = "rw-r-----";
    private static final FileSecurityManager.FileAccessPolicy
            logFileAccessPolicy = new FileSecurityManager.FileAccessPolicy(FILE_PERMISSIONS,
            EnvironmentConstants.SUPERADMIN_USER, EnvironmentConstants.SUPERADMIN_USER);

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
        Path logDir = Paths.get(config.getLogDirectory());
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
            try {
                new FileSecurityManager().enforceFileAccessPolicy(logDir.toFile(), logDirAccessPolicy);
            } catch (Exception e) {
                System.err.println("Warning: Could not set permissions on log directory: " + e.getMessage());
            }
        }

        Path logFilePath = Paths.get(config.getFullLogPath());
        if (!Files.exists(logFilePath)) {
            Files.createFile(logFilePath);
            try {
                new FileSecurityManager().enforceFileAccessPolicy(logFilePath.toFile(), logFileAccessPolicy);

            } catch (Exception e) {
                System.err.println("Warning: Could not set permissions on log file: " + e.getMessage());
            }
        }
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

        try (FileWriter fw = new FileWriter(config.getFullLogPath(), true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }
}