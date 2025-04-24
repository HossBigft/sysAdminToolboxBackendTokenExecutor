package org.example.Logging.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogEntry {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public LogEntry message(String message) {
        return field("Message", message);
    }

    public LogEntry field(String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }
        return this;
    }

    public LogEntry exception(Throwable t) {
        field("Exception", t.toString());

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        field("StackTrace", sw.toString());

        return this;
    }

    public LogEntry command(String... args) {
        String command = args[0];
        field("Command", command);

        StringBuilder argsStr = new StringBuilder();
        for (String arg : args) {
            argsStr.append(maskSecrets(arg)).append(" ");
        }
        field("Args", argsStr.toString().trim());

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

    public LogEntry action(String action, String target) {
        return field("Action", action).field("Target", target);
    }

    public LogEntry result(boolean success) {
        return field("Result", success ? "SUCCESS" : "FAILURE");
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}