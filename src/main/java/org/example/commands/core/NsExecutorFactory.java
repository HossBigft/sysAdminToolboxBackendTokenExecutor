package org.example.commands.core;

import org.example.commands.Command;
import org.example.commands.CommandRequest;
import org.example.commands.dns.GetZoneMaster;
import org.example.value_types.DomainName;

import java.util.Map;

public class NsExecutorFactory implements CommandBuilderFactory {

    private static final Map<AvailableCommand, CommandBuilder> COMMANDS = Map.of(
            AvailableCommand.NS.GET_ZONE_MASTER, args -> new GetZoneMaster(
                    new DomainName(args[0])
            )
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
