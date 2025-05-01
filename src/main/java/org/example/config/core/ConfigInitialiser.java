package org.example.config.core;

import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.dotenv.DotEnvSecManager;
import org.example.config.security.SudoPrivilegeManager;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.Utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigInitialiser {
    private static final ConfigProvider cprovider = new ConfigProvider();
    private static final ConfigFileHandler chandler = new ConfigFileHandler();
    private static Map<String, String> values = new HashMap<>();


    public void initialise() {
        checkPrerequisites();
        try {
            loadConfig();
            ensureSetup();
            getLogger().debug("Config file " + getEnvFilePath() + " is loaded.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config", e);
        } catch (CommandFailedException | URISyntaxException e) {
            getLogger().error("Failed to initialize", e);
            throw new RuntimeException(e);
        }
    }

    private void checkPrerequisites() {
        boolean pleskExists = Files.isExecutable(Paths.get(Executables.PLESK_CLI_EXECUTABLE));
        boolean bindExists = Files.isExecutable(Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE));

        if (!pleskExists && !bindExists) {
            System.err.println("CRITICAL ERROR: Neither Plesk nor Bind are installed. Exiting immediately!");
            System.exit(1);
        }

    }

    private void loadConfig() throws IOException, CommandFailedException, URISyntaxException {
        values = chandler.loadConfig(cprovider.getEnvFile());

        boolean
                updated =
                computeIfAbsentOrBlank(values,
                        cprovider.getEnvDbPassFieldName(),
                        () -> Utils.generatePassword(cprovider.getDbUserPasswordLength()));
        if (updated) {
            chandler.saveConfig();
        }

    }

    private void ensureSetup() throws IOException, URISyntaxException, CommandFailedException {
        new DotEnvSecManager().ensureDotEnvPermissions();
        new DatabaseSetupCoordinator().ensureDatabaseSetup();
        new SudoPrivilegeManager().setupSudoPrivileges();
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private String getEnvFilePath() {
        return cprovider.getEnvFile().getPath();
    }


    private boolean computeIfAbsentOrBlank(Map<String, String> map, String key, Supplier<String> supplier) {
        String val = map.get(key);
        if (val == null || val.isBlank()) {
            map.put(key, supplier.get());
            return true;
        }
        return false;
    }

}
