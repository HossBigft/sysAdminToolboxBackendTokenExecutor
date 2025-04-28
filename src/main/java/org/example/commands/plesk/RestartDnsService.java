package org.example.commands.plesk;

import org.example.commands.Command;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

public class RestartDnsService implements Command<Void> {
    private final DomainName domain;

    public RestartDnsService(DomainName domain){
        this.domain=domain;
    }

    @Override
    public Void execute() throws CommandFailedException {
        ShellUtils.runCommand(Executables.PLESK_CLI_EXECUTABLE, "dns", "--off", domain.name());
        ShellUtils.runCommand(Executables.PLESK_CLI_EXECUTABLE, "dns", "--on", domain.name());
        return null;
    }
}
