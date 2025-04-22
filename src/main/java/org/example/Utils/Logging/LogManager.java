package org.example.Utils.Logging;

import org.example.Config.PermissionManager;
import org.example.Utils.ShellUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;


public class LogManager {
    // Configuration constants
    private static final String LOG_DIRECTORY = "/var/log/sysAdminToolBox/";
    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USER = ShellUtils.resolveShellUser();

    private static final Log log = new Log();
    private static LogLevel globalLogLevel = LogLevel.INFO;

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
     * Core logging method - all logging goes through here
     */
    private static synchronized void writeLog(LogLevel level,
                                              String message) {
        if (!isLoggable(level)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] User=%s | %s",
                timestamp, level, USER, formatMessage(message));

        if (level == LogLevel.DEBUG) {
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
     * Check if a message with the given level should be logged for a specific category
     */
    private static boolean isLoggable(LogLevel level) {
        return level.getValue() <= globalLogLevel.getValue();
    }

    private static String formatMessage(String raw) {
        return raw.replace("Message[", "")
                .replace("]", "")
                .replaceAll(" (?=\\w+=)", " | ");
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
     * Fluent API for logging
     */
    public static class Log {
        /**
         * Command logger
         */
        public CommandLogger command(String command,
                                     String... args) {
            return new CommandLogger(command, args);
        }

        /**
         * Action logger
         */
        public ActionLogger action(String action,
                                   String target) {
            return new ActionLogger(action, target);
        }

        /**
         * Action logger with success indicator
         */
        public ActionLogger action(String action,
                                   String target,
                                   boolean success) {
            return new ActionLogger(action, target, success);
        }

        /**
         * Config change logger
         */
        public ConfigChangeLogger configChange(String component,
                                               String property,
                                               String oldValue,
                                               String newValue) {
            return new ConfigChangeLogger(component, property, oldValue, newValue);
        }

        /**
         * Direct error logging
         */
        public void error(String message) {
            writeLog(LogLevel.ERROR, message);
        }

        /**
         * Direct warning logging
         */
        public void warn(String message) {
            writeLog(LogLevel.WARN, message);
        }

        /**
         * Direct info logging
         */
        public void info(String message) {
            writeLog(LogLevel.INFO, message);
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
     * CommandLogger for fluent command logging
     */
    public static class CommandLogger {
        private final String command;
        private final String[] args;

        private CommandLogger(String command,
                              String... args) {
            this.command = command;
            this.args = args;
        }

        /**
         * Log command at ERROR level
         */
        public void error() {
            logCommandInternal(LogLevel.ERROR);
        }

        private void logCommandInternal(LogLevel level) {
            StringBuilder message = new StringBuilder("Command=" + command);

            if (args != null && args.length > 0) {
                message.append(" Args=");
                for (String arg : args) {
                    message.append(maskSecrets(arg)).append(" ");
                }
            }

            writeLog(level, message.toString().trim());
        }

        private String maskSecrets(String input) {
            if (input == null) return null;

            String masked = input;

            masked = masked.replaceAll("(?i)(IDENTIFIED BY\\s+)'[^']*'", "$1'[REDACTED]'");
            masked = masked.replaceAll("(?i)(SET PASSWORD\\s*=\\s*)'[^']*'", "$1'[REDACTED]'");
            masked = masked.replaceAll("(?i)(--password=)([^\\s]+)", "$1[REDACTED]");
            masked = masked.replaceAll("(?i)(password\\s*[:=]\\s*)([^\\s'\"]+)", "$1[REDACTED]");

            return masked;
        }

        /**
         * Log command at WARN level
         */
        public void warn() {
            logCommandInternal(LogLevel.WARN);
        }

        /**
         * Log command at INFO level
         */
        public void info() {
            logCommandInternal(LogLevel.INFO);
        }

        /**
         * Log command at DEBUG level
         */
        public void debug() {
            logCommandInternal(LogLevel.DEBUG);
        }

    }

    /**
     * ActionLogger for fluent action logging
     */
    public static class ActionLogger {
        private final String action;
        private final String target;
        private final Boolean success;

        private ActionLogger(String action,
                             String target) {
            this.action = action;
            this.target = target;
            this.success = null;
        }

        private ActionLogger(String action,
                             String target,
                             boolean success) {
            this.action = action;
            this.target = target;
            this.success = success;
        }

        /**
         * Log action at ERROR level
         */
        public void error() {
            logActionInternal(LogLevel.ERROR);
        }

        private void logActionInternal(LogLevel level) {
            if (success == null) {
                writeLog(level, "Target=" + target + " Action= " + action);
            } else {
                writeLog(level,
                        "Target=" + target + " Action= " + action + " Result=" + (success ? "SUCCESS" : "FAILURE"));
            }
        }

        /**
         * Log action at WARN level
         */
        public void warn() {
            logActionInternal(LogLevel.WARN);
        }

        /**
         * Log action at INFO level
         */
        public void info() {
            logActionInternal(LogLevel.INFO);
        }

        /**
         * Log action at DEBUG level
         */
        public void debug() {
            logActionInternal(LogLevel.DEBUG);
        }
    }


    /**
     * ConfigChangeLogger for fluent configuration change logging
     */
    public static class ConfigChangeLogger {
        private final String component;
        private final String property;
        private final String oldValue;
        private final String newValue;

        private ConfigChangeLogger(String component,
                                   String property,
                                   String oldValue,
                                   String newValue) {
            this.component = component;
            this.property = property;

            // Redact sensitive values
            if (property.toLowerCase().contains("password")) {
                this.oldValue = "[REDACTED]";
                this.newValue = "[REDACTED]";
            } else {
                this.oldValue = oldValue;
                this.newValue = newValue;
            }
        }

        /**
         * Log config change at ERROR level
         */
        public void error() {
            logConfigChangeInternal(LogLevel.ERROR);
        }

        private void logConfigChangeInternal(LogLevel level) {
            writeLog(level,
                    String.format("Component=%s Property=%s OldValue=%s NewValue=%s",
                            component, property, oldValue, newValue));
        }

        /**
         * Log config change at WARN level
         */
        public void warn() {
            logConfigChangeInternal(LogLevel.WARN);
        }

        /**
         * Log config change at INFO level
         */
        public void info() {
            logConfigChangeInternal(LogLevel.INFO);
        }

        /**
         * Log config change at DEBUG level
         */
        public void debug() {
            logConfigChangeInternal(LogLevel.DEBUG);
        }
    }

    /**
     * Builder for configuring the LogManager
     */
    public static class Builder {
        private Builder() {
            // Private constructor to enforce use of factory method
        }

        /**
         * Create a new Builder instance
         */
        public static Builder config() {
            return new Builder();
        }

        /**
         * Set the global log level
         */
        public Builder globalLogLevel(LogLevel level) {
            setGlobalLogLevel(level);
            return this;
        }

    }
}