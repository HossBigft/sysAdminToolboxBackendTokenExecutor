package org.example.operations.plesk;

import org.example.operations.Operation;
import org.example.constants.Executables;
import org.example.operations.OperationFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.util.Optional;

public class RestartDnsService implements Operation<Void> {
    private final DomainName domain;

    public RestartDnsService(DomainName domain) {
        this.domain = domain;
    }

    @Override
    public Optional<Void> execute() throws OperationFailedException {
        ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--off", domain.name());
        ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--on", domain.name());
        return Optional.empty();
    }
}
