package org.example.operations;

public sealed interface AvailableOperation permits AvailableOperation.DNS, AvailableOperation.Plesk {

    static AvailableOperation valueOf(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected format: TYPE.VALUE, got: " + qualifiedName);
        }

        return switch (parts[0].toUpperCase()) {
            case "DNS" -> DNS.valueOf(parts[1].toUpperCase());
            case "PLESK" -> Plesk.valueOf(parts[1].toUpperCase());
            default -> throw new IllegalArgumentException("Unknown enum type: " + parts[0]);
        };
    }

    enum DNS implements AvailableOperation {
        REMOVE_ZONE,
        GET_ZONE_MASTER
    }

    enum Plesk implements AvailableOperation {
        GET_LOGIN_LINK,
        FETCH_SUBSCRIPTION_INFO,
        GET_TESTMAIL_CREDENTIALS,
        RESTART_DNS_SERVICE,
        GET_SUBSCRIPTION_ID_BY_DOMAIN
    }
}