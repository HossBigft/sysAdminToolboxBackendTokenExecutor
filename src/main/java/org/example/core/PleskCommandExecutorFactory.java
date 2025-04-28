package org.example.core;

import org.example.commands.Command;
import org.example.commands.CommandRequest;
import org.example.commands.plesk.PleskFetchSubscriptionInfoCommand;
import org.example.commands.plesk.PleskGetLoginLinkCommand;
import org.example.commands.plesk.PleskGetTestMailboxCommand;
import org.example.commands.plesk.RestartDnsService;
import org.example.value_types.DomainName;
import org.example.value_types.LinuxUsername;

import java.util.Map;

public class PleskCommandExecutorFactory implements CommandBuilderFactory {
    private static final Map<AvailableCommand, CommandBuilder> COMMANDS = Map.of(
            AvailableCommand.Plesk.GET_LOGIN_LINK, args -> new PleskGetLoginLinkCommand(
                    Integer.parseInt(args[0]),
                    new LinuxUsername(args[1])
            ),
            AvailableCommand.Plesk.GET_TESTMAIL_CREDENTIALS, args -> new PleskGetTestMailboxCommand(
                    new DomainName(args[0])
            ),
            AvailableCommand.Plesk.FETCH_SUBSCRIPTION_INFO,
            args -> new PleskFetchSubscriptionInfoCommand(new DomainName(args[0])),
            AvailableCommand.Plesk.RESTART_DNS_SERVICE,
            args -> new RestartDnsService(new DomainName(args[0]))
    );

    @Override
    public Command<?> build(CommandRequest parsed) {
        CommandBuilder builder = COMMANDS.get(parsed.commandName());
        if (builder == null) {
            throw new IllegalArgumentException("Unknown command: " + parsed.commandName());
        }
        return builder.build(parsed.commandArgs());
    }

    private interface CommandBuilder {
        Command<?> build(String[] args);
    }

}
