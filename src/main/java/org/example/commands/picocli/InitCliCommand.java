package org.example.commands.picocli;

import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.config.core.ConfigManager;
import picocli.CommandLine;

@CommandLine.Command(
        name = "init",
        description = "Initialises and repairs environment setup"
)
public class InitCliCommand extends AbstractCliCommand {

    public InitCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            ConfigManager.loadConfig();
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to setup environment");
            e.printStackTrace();
            return 1;
        }
    }
}
