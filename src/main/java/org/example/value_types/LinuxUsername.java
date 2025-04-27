package org.example.value_types;


import java.util.regex.Pattern;

public record LinuxUsername(String name) implements ValueType {
    private static final Pattern LINUX_USERNAME_PATTERN =
            Pattern.compile("^[a-z_]([a-z0-9_-]{0,31}|[a-z0-9_-]{0,30}\\$)$");

    public LinuxUsername {
        if (name.length() < 3 ||
                name.length() > 32 ||
                !LINUX_USERNAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("name " +
                    name +
                    " must be a valid username (e.g., 'johndoe') matching: " + LINUX_USERNAME_PATTERN.pattern());
        }
        if (name.equalsIgnoreCase("root")) {
            throw new IllegalArgumentException("name " + name + " must not be root,");
        }
    }

    public static boolean isValid(String candidate) {
        return candidate != null &&
                candidate.length() >= 3 &&
                candidate.length() <= 32 &&
                LINUX_USERNAME_PATTERN.matcher(candidate).matches();
    }

    public String value() {
        return name;
    }

}