package org.example.commands.plesk;

import org.example.commands.Command;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.util.Optional;

public class RestartDnsService implements Command<Void> {
    private final DomainName domain;

    public RestartDnsService(DomainName domain) {
        this.domain = domain;
    }

    @Override
    public Optional<Void> execute() throws CommandFailedException {
        ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--off", domain.name());
        ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--on", domain.name());
        return Optional.empty();
    }
}
