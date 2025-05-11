package org.example.picocli.subcommands;

import org.example.SysAdminToolboxBackendTokenExecutor;
import picocli.CommandLine;

@CommandLine.Command(
        name = "status",
        description = "Checks if the system is online."
)
public class HealthCheckCliCommand extends AbstractCliCommand {

    public HealthCheckCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        System.out.println("online");
        return 0;
    }
}

