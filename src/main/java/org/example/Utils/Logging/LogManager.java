package org.example.Utils.Logging;

import org.example.Config.PermissionManager;
import org.example.Constants.EnvironmentConstants;
import org.example.Utils.ShellUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class LogManager {
    private static final String LOG_DIRECTORY = "/var/log/" + EnvironmentConstants.APP_USER + "/";
    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USER = ShellUtils.resolveShellUser();

    private static final Log log = new Log();
    private static LogLevel globalLogLevel = LogLevel.INFO;
    private static boolean verboseFlag = false;

    static {
        try {
            initializeLogFile();
        } catch (IOException e) {
            System.err.println("Failed to initialize audit log: " + e.getMessage());
        }
    }

    protected static void setGlobalLogLevel(LogLevel level) {
        globalLogLevel = level;
    }

    protected static void enableVerbose() {
        verboseFlag = true;
    }

    public static void debug(String message) {
        log.debug(message);
    }


    public static void info(String message) {
        log.info(message);
    }

    public static void warn(String message) {
        log.warn(message);
    }

    public static void error(String message) {
        log.error(message);
    }

    public static void error(String message,
                             Throwable t) {
        log.error(message, t);
    }

    private static void initializeLogFile() throws IOException {
        Path logDir = Paths.get(LOG_DIRECTORY);
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
            try {
                Files.setPosixFilePermissions(logDir, PosixFilePermissions.fromString("rwxr-x---"));
                PermissionManager.setOwner(logDir, "root");
                PermissionManager.setGroup(logDir, "root");
            } catch (Exception e) {
                System.err.println("Warning: Could not set permissions on log directory: " + e.getMessage());
            }
        }

        Path logFilePath = Paths.get(LOG_DIRECTORY, LOG_FILE);
        if (!Files.exists(logFilePath)) {
            Files.createFile(logFilePath);
            try {
                Files.setPosixFilePermissions(logFilePath, PosixFilePermissions.fromString("rw-r-----"));
                PermissionManager.setOwner(logFilePath, "root");
                PermissionManager.setGroup(logFilePath, "root");
            } catch (Exception e) {
                System.err.println("Warning: Could not set permissions on log file: " + e.getMessage());
            }
        }
    }

    /**
     * Simplified text logging method that leverages the map-based method
     */
    private static synchronized void writeLog(LogLevel level,
                                              String message) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("Message", message);
        writeLog(level, fields);
    }

    /**
     * Core logging method - all logging goes through here
     */
    private static synchronized void writeLog(LogLevel level,
                                              Map<String, Object> fields) {
        if (!isLoggable(level)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        StringBuilder logEntry = new StringBuilder();

        logEntry.append(String.format("[%s] [%s] User=%s", timestamp, level, USER));

        // Format all fields with consistent separator
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            logEntry.append(" | ").append(entry.getKey()).append("=").append(entry.getValue());
        }

        if (level == LogLevel.DEBUG || verboseFlag) {
            System.out.println(logEntry);
        }

        try (FileWriter fw = new FileWriter(new File(LOG_DIRECTORY, LOG_FILE), true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }

    /**
     * Check if a message with the given level should be logged
     */
    private static boolean isLoggable(LogLevel level) {
        return level.getValue() <= globalLogLevel.getValue();
    }

    /**
     * Main fluent API entry point
     */
    public static Log log() {
        return log;
    }

    // Log levels, ordered by severity (lowest to highest value)
    public enum LogLevel {
        ERROR(0), WARN(1), INFO(2), DEBUG(3);

        private final int value;

        LogLevel(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Unified log entry builder for all log types
     */
    public static class LogEntryBuilder {
        private final Map<String, Object> fields = new HashMap<>();
        private LogLevel level;

        public LogEntryBuilder(LogLevel level) {
            this.level = level;
        }

        public LogEntryBuilder exception(Throwable t) {
            fields.put("Exception", t.toString());

            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            fields.put("StackTrace", sw.toString());

            return this;
        }

        public LogEntryBuilder message(String message) {
            field("Message", message);
            StringWriter sw = new StringWriter();
            return this;
        }

        public LogEntryBuilder field(String key,
                                     Object value) {
            if (value != null) {
                fields.put(key, value);
            }
            return this;
        }

        public LogEntryBuilder command(String command,
                                       String... args) {
            field("Command", command);

            if (args != null && args.length > 0) {
                StringBuilder argsStr = new StringBuilder();
                for (String arg : args) {
                    argsStr.append(maskSecrets(arg)).append(" ");
                }
                field("Args", argsStr.toString().trim());
            }

            return this;
        }

        private String maskSecrets(String input) {
            if (input == null) return null;

            String masked = input;
            masked = masked.replaceAll("(?i)(IDENTIFIED BY\\s+)'[^']*'", "$1'REDACTED'");
            masked = masked.replaceAll("(?i)(SET PASSWORD\\s*=\\s*)'[^']*'", "$1'REDACTED'");
            masked = masked.replaceAll("(?i)(--password=)([^\\s]+)", "$1REDACTED");
            masked = masked.replaceAll("(?i)(password\\s*[:=]\\s*)([^\\s'\"]+)", "$1REDACTED");

            return masked;
        }

        public LogEntryBuilder action(String action,
                                      String target) {
            return field("Action", action).field("Target", target);
        }

        public LogEntryBuilder result(boolean success) {
            return field("Result", success ? "SUCCESS" : "FAILURE");
        }

        public void log() {
            writeLog(level, fields);
        }

    }

    /**
     * Fluent API for logging
     */
    public static class Log {
        /**
         * Command logger
         */
        public CommandLogger command(String... args) {
            return new CommandLogger(args);
        }


        public void error(String message) {
            new LogEntryBuilder(LogLevel.ERROR)
                    .field("Message", message)
                    .log();
        }

        public void error(Throwable t) {
            new LogEntryBuilder(LogLevel.ERROR)
                    .exception(t)
                    .log();
        }

        public void error(String message,
                          Throwable t) {
            new LogEntryBuilder(LogLevel.ERROR)
                    .field("Message", message)
                    .exception(t)
                    .log();
        }

        /**
         * Create a new log entry at WARN level
         */
        public LogEntryBuilder warn() {
            return new LogEntryBuilder(LogLevel.WARN);
        }

        /**
         * Direct warning logging
         */
        public void warn(String message) {
            writeLog(LogLevel.WARN, message);
        }

        /**
         * Create a new log entry at INFO level
         */
        public LogEntryBuilder info() {
            return new LogEntryBuilder(LogLevel.INFO);
        }

        /**
         * Direct info logging
         */
        public void info(String message) {
            writeLog(LogLevel.INFO, message);
        }

        /**
         * Create a new log entry at DEBUG level
         */
        public LogEntryBuilder debug() {
            return new LogEntryBuilder(LogLevel.DEBUG);
        }

        /**
         * Direct debug logging
         */
        public void debug(String message) {
            writeLog(LogLevel.DEBUG, message);
        }

        /**
         * Direct debug logging with lazy evaluation
         */
        public void debug(Supplier<String> messageSupplier) {
            if (isLoggable(LogLevel.DEBUG)) {
                writeLog(LogLevel.DEBUG, messageSupplier.get());
            }
        }
    }

    /**
     * CommandLogger for backward compatibility
     */
    public static class CommandLogger {
        private final String command;
        private final String[] args;

        private CommandLogger(String... args) {
            this.command = args[0];
            this.args = Arrays.stream(args, 1, args.length).toArray(String[]::new);
        }

        public void error(Throwable t) {
            new LogEntryBuilder(LogLevel.ERROR)
                    .command(command, args)
                    .exception(t)
                    .log();
        }

        public void warn() {
            new LogEntryBuilder(LogLevel.WARN)
                    .command(command, args)
                    .log();
        }

        public void info() {
            new LogEntryBuilder(LogLevel.INFO)
                    .command(command, args)
                    .log();
        }

        public void debug() {
            new LogEntryBuilder(LogLevel.DEBUG)
                    .command(command, args)
                    .log();
        }
    }


    /**
     * Builder for configuring the LogManager
     */
    public static class Builder {
        private Builder() {
            // Private constructor to enforce use of factory method
        }

        public static Builder config() {
            return new Builder();
        }

        public Builder globalLogLevel(LogLevel level) {
            setGlobalLogLevel(level);
            return this;
        }

        public Builder setVerbose() {
            enableVerbose();
            return this;
        }
    }
}