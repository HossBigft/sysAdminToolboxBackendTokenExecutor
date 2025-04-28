package org.example.core;

public sealed interface AvailableCommand permits AvailableCommand.BIND, AvailableCommand.Plesk {

    static AvailableCommand valueOf(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected format: TYPE.VALUE, got: " + qualifiedName);
        }

        return switch (parts[0].toUpperCase()) {
            case "NS" -> BIND.valueOf(parts[1].toUpperCase());
            case "PLESK" -> Plesk.valueOf(parts[1].toUpperCase());
            default -> throw new IllegalArgumentException("Unknown enum type: " + parts[0]);
        };
    }

    enum BIND implements AvailableCommand {
        REMOVE_DNS_ZONE,
        GET_ZONE_MASTER
    }

    enum Plesk implements AvailableCommand {
        GET_LOGIN_LINK,
        FETCH_SUBSCRIPTION_INFO,
        GET_TESTMAIL_CREDENTIALS,
        RESTART_DNS_SERVICE
    }
}