package org.example;

import org.example.Commands.Plesk.PleskFetchSubscriptionInfoCommand;
import org.example.Commands.Plesk.PleskGetLoginLinkCommand;
import org.example.Commands.Plesk.PleskGetTestMailboxCommand;
import org.example.Interfaces.Command;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;

import java.util.Arrays;
import java.util.Map;

public class PleskCommandFactory implements CommandFactory {
    private static final Map<ServiceCommand, CommandFactory> COMMANDS = Map.of(
            PleskCommand.GET_LOGIN_LINK, args -> new PleskGetLoginLinkCommand(
                    Integer.parseInt(args[0]),
                    new LinuxUsername(args[1])
            ),
            PleskCommand.GET_TESTMAIL_CREDENTIALS, args -> new PleskGetTestMailboxCommand(
                    new DomainName(args[0])
            ),
            PleskCommand.GET_SUBSCRIPTION_INFO, args -> new PleskFetchSubscriptionInfoCommand(new DomainName(args[0]))
    );

    @Override
    public Command<?> build(String... args) {
        ParsedCommand parsed = parseCommandAndArgs(args);
        CommandFactory builder = COMMANDS.get(parsed.commandName());
        if (builder == null) {
            throw new IllegalArgumentException("Unknown command: " + parsed.commandName());
        }
        return builder.build(parsed.commandArgs());
    }

    private ParsedCommand parseCommandAndArgs(String... args) {
        if (args.length == 0 || args[0].isBlank()) {
            throw new IllegalArgumentException("No command provided");
        }
        String[] split = args[0].split(" ");
        if (split.length == 0) {
            throw new IllegalArgumentException("Invalid command input");
        }
        ServiceCommand commandName = PleskCommand.valueOf(split[0]);
        String[] commandArgs = Arrays.copyOfRange(split, 1, split.length);
        return new ParsedCommand(commandName, commandArgs);
    }

    private record ParsedCommand(ServiceCommand commandName, String[] commandArgs) {}
}
