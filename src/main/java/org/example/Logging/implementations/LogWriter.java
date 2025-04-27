package org.example.Logging.implementations;

import org.example.Logging.config.LogConfig;
import org.example.Logging.core.LogLevel;
import org.example.Logging.model.LogEntry;
import org.example.config.security.FileSecurityManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogWriter {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                Files.setPosixFilePermissions(logDir, PosixFilePermissions.fromString("rwxr-x---"));
                FileSecurityManager.setOwner(logDir, "root");
                FileSecurityManager.setGroup(logDir, "root");
            } catch (Exception e) {
                System.err.println("Warning: Could not set permissions on log directory: " + e.getMessage());
            }
        }

        Path logFilePath = Paths.get(config.getFullLogPath());
        if (!Files.exists(logFilePath)) {
            Files.createFile(logFilePath);
            try {
                Files.setPosixFilePermissions(logFilePath, PosixFilePermissions.fromString("rw-r-----"));
                FileSecurityManager.setOwner(logFilePath, "root");
                FileSecurityManager.setGroup(logFilePath, "root");
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