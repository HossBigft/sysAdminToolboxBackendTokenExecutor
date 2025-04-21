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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * LogManager provides centralized logging capabilities with configurable log levels
 * for the SysAdminToolBox application using a fluent API.
 */
public class LogManager {
    // Configuration constants
    private static final String LOG_DIRECTORY = "/var/log/sysAdminToolBox/";
    private static final String LOG_FILE = "audit.log";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String USER = ShellUtils.resolveUser();

    // Sensitive command detection
    private static final Set<String> SENSITIVE_COMMANDS = new HashSet<>(Arrays.asList(
            "mysql", "psql", "openssl", "ssh", "scp", "keytool",
            "htpasswd", "usermod", "useradd", "passwd", "su"
    ));

    private static final Set<String> SENSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
            "password", "secret", "key", "token", "credential",
            "auth", "-p", "--password", "shadow"
    ));

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

    // Default log level
    private static LogLevel globalLogLevel = LogLevel.INFO;

    // Category-specific log levels
    private static final Map<String, LogLevel> categoryLogLevels = new ConcurrentHashMap<>();

    // Fluent API logger instance
    private static final Log log = new Log();

    // Initialize the log file upon class loading
    static {
        try {
            initializeLogFile();
        } catch (IOException e) {
            System.err.println("Failed to initialize audit log: " + e.getMessage());
        }
    }

    /**
     * Create and set permissions for log directory and file
     */
    private static void initializeLogFile() throws IOException {
        // Create and secure log directory
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

        // Create and secure log file
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
     * Set the global log level
     */
    public static void setGlobalLogLevel(LogLevel level) {
        globalLogLevel = level;
        writeLog(LogLevel.INFO, "LOG_SYSTEM", "Changed global log level to " + level);
    }

    /**
     * Set log level for a specific category
     */
    public static void setCategoryLogLevel(String category, LogLevel level) {
        categoryLogLevels.put(category.toUpperCase(), level);
        writeLog(LogLevel.INFO, "LOG_SYSTEM", "Set log level for category " + category + " to " + level);
    }

    /**
     * Remove category-specific log level
     */
    public static void removeCategoryLogLevel(String category) {
        categoryLogLevels.remove(category.toUpperCase());
        writeLog(LogLevel.INFO, "LOG_SYSTEM", "Removed specific log level for category " + category);
    }

    /**
     * Get the log level for a category
     */
    public static LogLevel getLogLevel(String category) {
        return categoryLogLevels.getOrDefault(category.toUpperCase(), globalLogLevel);
    }

    /**
     * Get the global log level
     */
    public static LogLevel getGlobalLogLevel() {
        return globalLogLevel;
    }

    /**
     * Check if a message with the given level should be logged for a specific category
     */
    private static boolean isLoggable(LogLevel level, String category) {
        LogLevel categoryLevel = categoryLogLevels.getOrDefault(category.toUpperCase(), globalLogLevel);
        return level.getValue() <= categoryLevel.getValue();
    }

    /**
     * Core logging method - all logging goes through here
     */
    private static synchronized void writeLog(LogLevel level, String category, String message) {
        if (!isLoggable(level, category)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logEntry = String.format("[%s] [%s] User=%s Category=%s Message=%s",
                timestamp, level, USER, category, message);

        // Print to console for immediate feedback
        System.out.println(logEntry);

        // Write to log file
        try (FileWriter fw = new FileWriter(new File(LOG_DIRECTORY, LOG_FILE), true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to audit log: " + e.getMessage());
        }
    }

    /**
     * Determine if a command is sensitive based on common patterns
     */
    public static boolean isSensitiveCommand(String[] args) {
        if (args == null || args.length == 0) return false;

        // Check command name
        if (SENSITIVE_COMMANDS.contains(args[0].toLowerCase())) {
            return true;
        }

        // Check arguments for sensitive keywords
        for (String arg : args) {
            for (String keyword : SENSITIVE_KEYWORDS) {
                if (arg.toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get the fluent API logger instance
     */
    public static Log getLog() {
        return log;
    }

    /**
     * Main fluent API entry point
     */
    public static Log log() {
        return log;
    }

    /**
     * Fluent API for logging
     */
    public static class Log {
        /**
         * Command logger
         */
        public CommandLogger command(String command, String... args) {
            return new CommandLogger(command, args);
        }

        /**
         * Action logger
         */
        public ActionLogger action(String action, String target) {
            return new ActionLogger(action, target);
        }

        /**
         * Action logger with success indicator
         */
        public ActionLogger action(String action, String target, boolean success) {
            return new ActionLogger(action, target, success);
        }

        /**
         * Category logger
         */
        public CategoryLogger category(String category) {
            return new CategoryLogger(category);
        }

        /**
         * Config change logger
         */
        public ConfigChangeLogger configChange(String component, String property, String oldValue, String newValue) {
            return new ConfigChangeLogger(component, property, oldValue, newValue);
        }

        /**
         * Direct error logging
         */
        public void error(String category, String message) {
            writeLog(LogLevel.ERROR, category, message);
        }

        /**
         * Direct warning logging
         */
        public void warn(String category, String message) {
            writeLog(LogLevel.WARN, category, message);
        }

        /**
         * Direct info logging
         */
        public void info(String category, String message) {
            writeLog(LogLevel.INFO, category, message);
        }

        /**
         * Direct debug logging
         */
        public void debug(String category, String message) {
            writeLog(LogLevel.DEBUG, category, message);
        }

        /**
         * Direct debug logging with lazy evaluation
         */
        public void debug(String category, Supplier<String> messageSupplier) {
            if (isLoggable(LogLevel.DEBUG, category)) {
                writeLog(LogLevel.DEBUG, category, messageSupplier.get());
            }
        }
    }

    /**
     * CommandLogger for fluent command logging
     */
    public static class CommandLogger {
        private final String command;
        private final String[] args;

        private CommandLogger(String command, String... args) {
            this.command = command;
            this.args = args;
        }

        /**
         * Log command at ERROR level
         */
        public void error() {
            logCommandInternal(LogLevel.ERROR);
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

        /**
         * Log command with automatic level selection based on sensitivity
         */
        public void auto() {
            String[] fullCommand = new String[args.length + 1];
            fullCommand[0] = command;
            System.arraycopy(args, 0, fullCommand, 1, args.length);

            if (isSensitiveCommand(fullCommand)) {
                writeLog(LogLevel.INFO, "COMMAND", command + " [sensitive command]");
                if (isLoggable(LogLevel.DEBUG, "SENSITIVE_COMMAND")) {
                    writeLog(LogLevel.DEBUG, "SENSITIVE_COMMAND", "Executing: " + command + " " + String.join(" ", args));
                }
            } else {
                logCommandInternal(LogLevel.INFO);
            }
        }

        private void logCommandInternal(LogLevel level) {
            StringBuilder message = new StringBuilder("Command=" + command);

            if (args != null && args.length > 0) {
                message.append(" Args=");
                for (int i = 0; i < args.length; i++) {
                    String arg = args[i];
                    // Mask sensitive information like passwords
                    if (arg.toLowerCase().contains("password") ||
                            (i > 0 && args[i-1].toLowerCase().contains("password"))) {
                        message.append("[REDACTED] ");
                    } else {
                        message.append(arg).append(" ");
                    }
                }
            }

            writeLog(level, "COMMAND", message.toString().trim());
        }
    }

    /**
     * ActionLogger for fluent action logging
     */
    public static class ActionLogger {
        private final String action;
        private final String target;
        private final Boolean success;

        private ActionLogger(String action, String target) {
            this.action = action;
            this.target = target;
            this.success = null;
        }

        private ActionLogger(String action, String target, boolean success) {
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

        private void logActionInternal(LogLevel level) {
            if (success == null) {
                writeLog(level, action, "Target=" + target);
            } else {
                writeLog(level, action, "Target=" + target + " Result=" + (success ? "SUCCESS" : "FAILURE"));
            }
        }
    }

    /**
     * CategoryLogger for fluent category-based logging
     */
    public static class CategoryLogger {
        private final String category;

        private CategoryLogger(String category) {
            this.category = category;
        }

        /**
         * Log message at ERROR level
         */
        public void error(String message) {
            writeLog(LogLevel.ERROR, category, message);
        }

        /**
         * Log message at WARN level
         */
        public void warn(String message) {
            writeLog(LogLevel.WARN, category, message);
        }

        /**
         * Log message at INFO level
         */
        public void info(String message) {
            writeLog(LogLevel.INFO, category, message);
        }

        /**
         * Log message at DEBUG level
         */
        public void debug(String message) {
            writeLog(LogLevel.DEBUG, category, message);
        }

        /**
         * Log message at DEBUG level with lazy evaluation
         */
        public void debug(Supplier<String> messageSupplier) {
            if (isLoggable(LogLevel.DEBUG, category)) {
                writeLog(LogLevel.DEBUG, category, messageSupplier.get());
            }
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

        private ConfigChangeLogger(String component, String property, String oldValue, String newValue) {
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

        private void logConfigChangeInternal(LogLevel level) {
            writeLog(level, "CONFIG_CHANGE",
                    String.format("Component=%s Property=%s OldValue=%s NewValue=%s",
                            component, property, oldValue, newValue));
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

        /**
         * Set a category-specific log level
         */
        public Builder categoryLogLevel(String category, LogLevel level) {
            setCategoryLogLevel(category, level);
            return this;
        }

        /**
         * Set multiple category-specific log levels
         */
        public Builder categoryLogLevels(Map<String, LogLevel> levels) {
            levels.forEach((category, level) -> setCategoryLogLevel(category, level));
            return this;
        }

        /**
         * Apply the configuration
         */
        public void apply() {
            log().info("LOG_SYSTEM", "Applied log configuration");
        }
    }
}