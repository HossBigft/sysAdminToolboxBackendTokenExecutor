package org.example;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ValueTypes {
    static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$");
    Predicate<String> isDomain = DOMAIN_PATTERN.asMatchPredicate();

    public ValueTypes() {
    }

    public record DomainName(String name) {
        private static final Predicate<String> IS_VALID = Pattern
                .compile("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$")
                .asMatchPredicate();

        public DomainName {
            if (!IS_VALID.test(name)) {
                throw new IllegalArgumentException("Invalid domain: " + name);
            }
        }

        public static boolean isValid(String candidate) {
            return IS_VALID.test(candidate);
        }

        public String value() {
            return name;
        }
    }
}