package org.example.logging.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class LogEntry {
    private static final String[] SENSITIVE_FIELD_NAMES = {
            "PASSWORD", "SECRET", "TOKEN", "CREDENTIAL", "AUTH"
    };
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public LogEntry message(String message) {
        return field("Message", message);
    }

    public LogEntry field(String key,
                          Object value) {
        if (value != null) {
            Object maskedValue = shouldMaskField(key) ?
                    maskValue(value) : value;
            fields.put(key, maskedValue);
        }
        return this;
    }

    private boolean shouldMaskField(String key) {
        if (key == null) return false;

        String upperKey = key.toUpperCase();
        for (String sensitiveField : SENSITIVE_FIELD_NAMES) {
            if (upperKey.contains(sensitiveField)) {
                return true;
            }
        }
        return false;
    }

    private Object maskValue(Object value) {
        if (value == null) return null;

        if (value instanceof String) {
            return "REDACTED";
        } else if (value instanceof char[]) {
            return "REDACTED";
        } else {
            // For other types, convert to string but mask it
            return "REDACTED";
        }
    }

    public LogEntry exception(Throwable t) {
        field("Exception", t.toString());

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        field("StackTrace", sw.toString());

        return this;
    }

    public LogEntry command(String... args) {
        if (args.length > 0) {
            String command = args[0];
            field("Command", command);

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
        masked = masked.replaceAll("(?i)(DATABASE_PASSWORD=)([^\\s|\\]]+)", "$1REDACTED");
        masked = masked.replaceAll("(?i)([A-Z_]+PASSWORD=)([^\\s|\\]]+)", "$1REDACTED");


        masked = masked.replaceAll("(?i)([A-Z_]*(SECRET|KEY|TOKEN|CREDENTIAL|AUTH)[A-Z_]*=)([^\\s|\\]]+)",
                "$1REDACTED");

        return masked;
    }

    public LogEntry action(String action,
                           String target) {
        return field("Action", action).field("Target", target);
    }

    public LogEntry result(boolean success) {
        return field("Result", success ? "SUCCESS" : "FAILURE");
    }

    public Map<String, Object> getFields() {
        return new LinkedHashMap<>(fields);
    }

    /**
     * Get raw fields without applying any additional masking
     * Internal use only for special cases
     */
    protected Map<String, Object> getRawFields() {
        return fields;
    }

    @Override
    public String toString() {
        return getMaskedFields().toString();
    }

    /**
     * Applies deep masking to all string values in fields map
     * Use this when logging the entire entry
     */
    public Map<String, Object> getMaskedFields() {
        return fields.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> maskPotentialSecrets(entry.getValue())
                ));
    }

    private Object maskPotentialSecrets(Object value) {
        if (value == null) return null;

        if (value instanceof String) {
            return maskSecrets((String) value);
        } else {
            return value;
        }
    }
}