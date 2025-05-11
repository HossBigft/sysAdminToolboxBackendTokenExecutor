package org.example.commands.dns;

import org.example.commands.AvailableOperation;
import org.example.commands.Operation;
import org.example.commands.OperationRequest;
import org.example.commands.core.CommandBuilderFactory;
import org.example.value_types.DomainName;

import java.util.Map;

public class NsOperationFactory implements CommandBuilderFactory {

    private static final Map<AvailableOperation, CommandBuilder> COMMANDS = Map.of(AvailableOperation.NS.GET_ZONE_MASTER,
            args -> new GetZoneMaster(new DomainName(args[0])), AvailableOperation.NS.REMOVE_ZONE,
            args -> new RemoveZone(new DomainName(args[0])));

    @Override
    public Operation<?> build(OperationRequest parsed) {
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
