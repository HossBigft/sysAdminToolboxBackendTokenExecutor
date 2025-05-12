package org.example.operations.dns;

import org.example.constants.Executables;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.utils.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NsRemoveZone implements Operation {
    private final DomainName domainNameToDelete;

    public NsRemoveZone(DomainName domainName) {
        this.domainNameToDelete = domainName;
    }

    @Override
    public OperationResult execute() {
        Path removeZoneExecutable;

        if (findRemoveZoneExecutable().isPresent()) {
            removeZoneExecutable = findRemoveZoneExecutable().get();
        } else {
            getLogger().errorEntry().message("Bind executable not found.")
                    .field("Executable", Executables.BIND_REMOVE_ZONE_EXECUTABLE).log();
            return OperationResult.internalError();
        }

        ShellUtils.ExecutionResult result;

        try {
            result = ShellUtils.execute(
                    removeZoneExecutable.toString(),
                    "delzone",
                    "-clean",
                    domainNameToDelete.name()
            );
        } catch (CommandFailedException e) {
            getLogger().errorEntry().message("Remove DNS zone operation failed with").exception(e).log();
            return OperationResult.internalError("Remove DNS zone operation failed.");
        }

        if (!result.isSuccessful()) {
            if (!result.stderrString().contains("not found")) {
                getLogger().errorEntry().message(result.getFormattedErrorMessage()).log();
                return OperationResult.notFound(
                        String.format("DNS zone for domain %s was not found.", domainNameToDelete));
            }

        }
        return OperationResult.success();
    }

    private Optional<Path> findRemoveZoneExecutable() {
        Path primaryPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE);
        Path fallbackPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE_FALLBACK);

        if (Files.isExecutable(primaryPath)) {
            return Optional.of(primaryPath);
        } else if (Files.isExecutable(fallbackPath)) {
            return Optional.of(fallbackPath);
        } else {
            return Optional.empty();
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

}
