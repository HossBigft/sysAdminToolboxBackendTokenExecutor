package org.example.commands.dns;

import org.example.commands.AvailableCommand;
import org.example.commands.Operation;
import org.example.commands.CommandRequest;
import org.example.commands.core.CommandBuilderFactory;
import org.example.value_types.DomainName;

import java.util.Map;

public class NsExecutorFactory implements CommandBuilderFactory {

    private static final Map<AvailableCommand, CommandBuilder> COMMANDS = Map.of(AvailableCommand.NS.GET_ZONE_MASTER,
            args -> new GetZoneMaster(new DomainName(args[0])), AvailableCommand.NS.REMOVE_ZONE,
            args -> new RemoveZone(new DomainName(args[0])));

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
