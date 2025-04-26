package org.example;

import java.util.Arrays;
import java.util.stream.Stream;

public sealed interface ServiceCommand permits ServiceCommand.Ns, ServiceCommand.Plesk {

    public enum Ns implements ServiceCommand {
        REMOVE_DNS_ZONE;
    }

    public enum Plesk implements ServiceCommand {
        GET_LOGIN_LINK,
        FETCH_SUBSCRIPTION_INFO,
        GET_TESTMAIL_CREDENTIALS;
    }

    static ServiceCommand valueOf(String name) {
        String upperName = name.toUpperCase();

        return Stream.of(Ns.values(), Plesk.values())
                .flatMap(Arrays::stream)
                .filter(cmd -> cmd.name().equals(upperName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No enum constant matching: " + name));
    }
}