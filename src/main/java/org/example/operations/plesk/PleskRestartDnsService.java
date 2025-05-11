package org.example.operations.plesk;

import org.example.constants.Executables;
import org.example.operations.Operation;
import org.example.operations.OperationFailedException;
import org.example.utils.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.util.Optional;

public class PleskRestartDnsService implements Operation<Void> {
    private final DomainName domain;

    public PleskRestartDnsService(DomainName domain) {
        this.domain = domain;
    }

    @Override
    public Optional<Void> execute() throws OperationFailedException {
        try {
            ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--off", domain.name());
            ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--on", domain.name());
        } catch (CommandFailedException e) {
            throw new OperationFailedException("Operation restart DNS service for domain " + domain + " failed with",
                    e);
        }
        return Optional.empty();
    }
}
