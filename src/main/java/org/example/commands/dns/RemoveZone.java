package org.example.commands.dns;

import org.example.commands.Command;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class RemoveZone implements Command<Void> {
    private final DomainName domainNameToDelete;

    public RemoveZone(DomainName domainName) {
        this.domainNameToDelete = domainName;
    }

    @Override
    public Optional<Void> execute() throws CommandFailedException {
        Path removeZoneExecutable = findRemoveZoneExecutable();

        ShellUtils.ShellCommandResult result = ShellUtils.execute(
                removeZoneExecutable.toString(),
                "delzone",
                "-clean",
                domainNameToDelete.name()
        );
        if (!result.isSuccessful()) {
            if (!result.stderrString().contains("not found")) {
                throw new CommandFailedException(result.getFormattedErrorMessage());
            }

        }
        return Optional.empty();
    }

    private Path findRemoveZoneExecutable() throws CommandFailedException {
        Path primaryPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE);
        Path fallbackPath = Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE_FALLBACK);

        if (Files.isExecutable(primaryPath)) {
            return primaryPath;
        } else if (Files.isExecutable(fallbackPath)) {
            return fallbackPath;
        } else {
            throw new CommandFailedException("Cannot find executable remove zone script");
        }
    }

}
