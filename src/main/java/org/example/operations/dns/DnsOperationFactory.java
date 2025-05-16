package org.example.operations.dns;

import org.example.operations.AvailableOperation;
import org.example.operations.Operation;
import org.example.operations.OperationRequest;
import org.example.operations.core.OperationFactory;
import org.example.value_types.DomainName;

import java.util.Map;

public class DnsOperationFactory implements OperationFactory {

    private static final Map<AvailableOperation, CommandBuilder> COMMANDS = Map.of(
            AvailableOperation.DNS.GET_ZONE_MASTER,
            args -> new DnsGetZoneMaster(new DomainName(args[0])), AvailableOperation.DNS.REMOVE_ZONE,
            args -> new DnsRemoveZone(new DomainName(args[0])));

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
