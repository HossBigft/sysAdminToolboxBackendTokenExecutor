package org.example.operations.dns;

import org.example.operations.Operation;
import org.example.constants.Executables;
import org.example.operations.OperationFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class NsRemoveZone implements Operation<Void> {
    private final DomainName domainNameToDelete;

    public NsRemoveZone(DomainName domainName) {
        this.domainNameToDelete = domainName;
    }

    @Override
    public Optional<Void> execute() throws OperationFailedException {
        Path removeZoneExecutable = findRemoveZoneExecutable();

        ShellUtils.ShellCommandResult result = ShellUtils.execute(
                removeZoneExecutable.toString(),
                "delzone",
                "-clean",
                domainNameToDelete.name()
        );
        if (!result.isSuccessful()) {
            if (!result.stderrString().contains("not found")) {
                throw new OperationFailedException(result.getFormattedErrorMessage());
            }

        }
        return Optional.empty();
    }

    private Path findRemoveZoneExecutable() throws OperationFailedException {
        Path primaryPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE);
        Path fallbackPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE_FALLBACK);

        if (Files.isExecutable(primaryPath)) {
            return primaryPath;
        } else if (Files.isExecutable(fallbackPath)) {
            return fallbackPath;
        } else {
            throw new OperationFailedException("Cannot find executable remove zone script");
        }
    }

}
