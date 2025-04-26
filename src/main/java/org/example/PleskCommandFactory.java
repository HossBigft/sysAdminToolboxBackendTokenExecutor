package org.example;

import org.example.Commands.Plesk.PleskFetchSubscriptionInfoCommand;
import org.example.Commands.Plesk.PleskGetLoginLinkCommand;
import org.example.Commands.Plesk.PleskGetTestMailboxCommand;
import org.example.Interfaces.Command;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;

import java.util.Map;

public class PleskCommandFactory implements CommandFactory {
    private static final Map<ServiceCommand, CommandBuilder> COMMANDS = Map.of(
            ServiceCommand.Plesk.GET_LOGIN_LINK, args -> new PleskGetLoginLinkCommand(
                    Integer.parseInt(args[0]),
                    new LinuxUsername(args[1])
            ),
            ServiceCommand.Plesk.GET_TESTMAIL_CREDENTIALS, args -> new PleskGetTestMailboxCommand(
                    new DomainName(args[0])
            ),
            ServiceCommand.Plesk.FETCH_SUBSCRIPTION_INFO, args -> new PleskFetchSubscriptionInfoCommand(new DomainName(args[0]))
    );

    @Override
    public Command<?> build(CommandInput parsed) {
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
