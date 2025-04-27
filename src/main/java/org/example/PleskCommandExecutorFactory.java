package org.example;

import org.example.Commands.Plesk.PleskFetchSubscriptionInfoCommand;
import org.example.Commands.Plesk.PleskGetLoginLinkCommand;
import org.example.Commands.Plesk.PleskGetTestMailboxCommand;
import org.example.Interfaces.Command;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;

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
            args -> new PleskFetchSubscriptionInfoCommand(new DomainName(args[0]))
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
