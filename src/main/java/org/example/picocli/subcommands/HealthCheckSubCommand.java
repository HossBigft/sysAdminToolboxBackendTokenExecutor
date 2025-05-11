package org.example.picocli.subcommands;

import org.example.main;
import picocli.CommandLine;

@CommandLine.Command(
        name = "status",
        description = "Checks if the system is online."
)
public class HealthCheckSubCommand extends AbstractSubCommand {

    public HealthCheckSubCommand(main parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        System.out.println("online");
        return 0;
    }
}

