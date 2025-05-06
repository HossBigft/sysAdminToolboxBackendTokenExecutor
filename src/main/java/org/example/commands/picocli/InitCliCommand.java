package org.example.commands.picocli;

import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.config.core.AppConfiguration;
import org.example.config.key_ed25519.KeyManager;
import picocli.CommandLine;
import java.net.URI;

@CommandLine.Command(
        name = "init",
        description = "Initialises and repairs environment setup. Optionally accepts a key link to fetch and save a public key."
)
public class InitCliCommand extends AbstractCliCommand {

    @CommandLine.Parameters(
            index = "0",
            description = "Optional link to fetch public key",
            arity = "0..1"  // This makes the parameter optional (0 or 1 arguments)
    )
    private String keyLink;

    public InitCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            if (keyLink != null && !keyLink.isEmpty()) {
                System.out.println("Fetching public key from: " + keyLink);
                KeyManager km = new KeyManager();
                km.savePublicKey(km.fetchPublicKey(new URI(keyLink)));
                System.out.println("Public key fetched and saved successfully");
            }
            AppConfiguration.getInstance().initialize();
            return 0;
        } catch (Exception e) {
            System.out.println("Failed to setup environment");
            e.printStackTrace();
            return 1;
        }
    }
}