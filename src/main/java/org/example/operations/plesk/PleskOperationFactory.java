package org.example.operations.plesk;

import org.example.operations.AvailableOperation;
import org.example.operations.Operation;
import org.example.operations.OperationRequest;
import org.example.operations.core.OperationFactory;
import org.example.value_types.DomainName;
import org.example.value_types.LinuxUsername;

import java.util.Map;

public class PleskOperationFactory implements OperationFactory {
    private static final Map<AvailableOperation, CommandBuilder> COMMANDS = Map.of(
            AvailableOperation.Plesk.GET_LOGIN_LINK, args -> new PleskGetLoginLink(
                    Integer.parseInt(args[0]),
                    new LinuxUsername(args[1])
            ),
            AvailableOperation.Plesk.GET_TESTMAIL_CREDENTIALS, args -> new PleskGetTestMailbox(
                    new DomainName(args[0])
            ),
            AvailableOperation.Plesk.FETCH_SUBSCRIPTION_INFO,
            args -> new PleskFetchSubscriptionInfo(new DomainName(args[0])),
            AvailableOperation.Plesk.RESTART_DNS_SERVICE,
            args -> new PleskRestartDnsService(new DomainName(args[0])),
            AvailableOperation.Plesk.GET_SUBSCRIPTION_ID_BY_DOMAIN,
            args -> new PleskGetSubscriptionIdByDomain(new DomainName(args[0]))
    );

    @Override
    public Operation build(OperationRequest parsed) {
        CommandBuilder builder = COMMANDS.get(parsed.commandName());
        if (builder == null) {
            throw new IllegalArgumentException("Unknown command: " + parsed.commandName());
        }
        return builder.build(parsed.commandArgs());
    }

    private interface CommandBuilder {
        Operation build(String[] args);
    }

}
