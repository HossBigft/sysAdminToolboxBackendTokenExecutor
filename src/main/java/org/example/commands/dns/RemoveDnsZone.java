package org.example.commands.dns;

import org.example.commands.Command;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.util.Optional;

public class RemoveDnsZone implements Command {
    private DomainName domainNameToDelete;

    public RemoveDnsZone(DomainName domainName) {
        this.domainNameToDelete = domainName;
    }

    @Override
    public Optional<Void> execute() throws CommandFailedException {
        ShellUtils.runCommand(Executables.BIND_REMOVE_ZONE_EXECUTABLE, "delzone", "-clean", domainNameToDelete.name());
        return Optional.empty();
    }
}
