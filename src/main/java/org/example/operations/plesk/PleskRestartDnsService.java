package org.example.operations.plesk;

import org.example.config.constants.Executables;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.utils.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

public class PleskRestartDnsService implements Operation {
    private final DomainName domain;

    public PleskRestartDnsService(DomainName domain) {
        this.domain = domain;
    }

    @Override
    public OperationResult execute() {

        try {
            ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--off", domain.name());
        } catch (CommandFailedException e) {
            getLogger().errorEntry().message("Operation stop DNS service for domain " + domain + " failed.")
                    .exception(e).log();
            return OperationResult.internalError("Operation stop DNS service for domain " + domain + " failed.");
        }

        try {
            ShellUtils.execute(Executables.PLESK_CLI_EXECUTABLE, "bin", "dns", "--on", domain.name());
        } catch (CommandFailedException e) {
            getLogger().errorEntry().message("Operation start DNS service for domain " + domain + " failed.")
                    .exception(e).log();
            return OperationResult.internalError("Operation start DNS service for domain " + domain + " failed.");
        }
        return OperationResult.success();
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }
}
