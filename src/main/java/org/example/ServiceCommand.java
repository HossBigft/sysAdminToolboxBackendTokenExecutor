package org.example;

public sealed interface ServiceCommand permits ServiceCommand.Ns, ServiceCommand.Plesk {

    public enum Ns implements ServiceCommand {
        REMOVE_DNS_ZONE;
    }

    public enum Plesk implements ServiceCommand {
        GET_LOGIN_LINK,
        FETCH_SUBSCRIPTION_INFO,
        GET_TESTMAIL_CREDENTIALS;
    }

    static ServiceCommand valueOf(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected format: TYPE.VALUE, got: " + qualifiedName);
        }

        return switch (parts[0].toUpperCase()) {
            case "NS" -> Ns.valueOf(parts[1].toUpperCase());
            case "PLESK" -> Plesk.valueOf(parts[1].toUpperCase());
            default -> throw new IllegalArgumentException("Unknown enum type: " + parts[0]);
        };
    }
}