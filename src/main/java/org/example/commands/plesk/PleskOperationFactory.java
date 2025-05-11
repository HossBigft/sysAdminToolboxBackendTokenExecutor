package org.example.commands.plesk;

import org.example.commands.AvailableCommand;
import org.example.commands.Operation;
import org.example.commands.CommandRequest;
import org.example.commands.core.CommandBuilderFactory;
import org.example.value_types.DomainName;
import org.example.value_types.LinuxUsername;

import java.util.Map;

public class PleskOperationFactory implements CommandBuilderFactory {
    private static final Map<AvailableCommand, CommandBuilder> COMMANDS = Map.of(
            AvailableCommand.Plesk.GET_LOGIN_LINK, args -> new PleskGetLoginLink(
                    Integer.parseInt(args[0]),
                    new LinuxUsername(args[1])
            ),
            AvailableCommand.Plesk.GET_TESTMAIL_CREDENTIALS, args -> new PleskGetTestMailbox(
                    new DomainName(args[0])
            ),
            AvailableCommand.Plesk.FETCH_SUBSCRIPTION_INFO,
            args -> new PleskFetchSubscriptionInfo(new DomainName(args[0])),
            AvailableCommand.Plesk.RESTART_DNS_SERVICE,
            args -> new RestartDnsService(new DomainName(args[0])),
            AvailableCommand.Plesk.GET_SUBSCRIPTION_ID_BY_DOMAIN,
            args -> new PleskGetSubscriptionIdByDomain(new DomainName(args[0]))
    );

    @Override
    public Operation<?> build(CommandRequest parsed) {
        CommandBuilder builder = COMMANDS.get(parsed.commandName());
        if (builder == null) {
            throw new IllegalArgumentException("Unknown command: " + parsed.commandName());
        }
        return builder.build(parsed.commandArgs());
    }

    private interface CommandBuilder {
        Operation<?> build(String[] args);
    }

}
