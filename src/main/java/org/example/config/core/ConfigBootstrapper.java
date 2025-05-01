package org.example.config.core;

import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.dotenv.DotEnvSecManager;
import org.example.config.security.SudoPrivilegeManager;
import org.example.constants.Executables;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class ConfigBootstrapper {
    private final EnvironmentConfig environmentConfig;
    private final JsonConfigStore fileHandler;
    private final CliLogger logger;

    public ConfigBootstrapper(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
        this.fileHandler = new JsonConfigStore(environmentConfig);
        this.logger = LogManager.getInstance().getLogger();
    }

    public void initialize() {
        checkPrerequisites();

        try {
            loadConfig();
            ensureSetup();
            logger.debug("Config file " + environmentConfig.getEnvFilePath() + " is loaded.");
        } catch (Exception e) {
            logger.error("Failed to initialize configuration", e);
            throw new AppConfigException("Configuration initialization failed", e);
        }
    }

    private void checkPrerequisites() {
        boolean pleskExists = Files.isExecutable(Paths.get(Executables.PLESK_CLI_EXECUTABLE));
        boolean bindExists = Files.isExecutable(Paths.get(Executables.BIND_REMOVE_ZONE_EXECUTABLE));

        if (!pleskExists && !bindExists) {
            logger.error("CRITICAL ERROR: Neither Plesk nor Bind are installed");
            throw new AppConfigException("Missing required system components");
        }
    }

    private void loadConfig() {

        fileHandler.loadConfig();
        setDefaultIfMissing(environmentConfig.getEnvDbPassFieldName(),
                () -> environmentConfig.generatePassword(environmentConfig.getDbUserPasswordLength()));
    }

    private void setDefaultIfMissing(String key, Supplier<String> defaultValueSupplier) {
        String value = environmentConfig.getValue(key);
        if (value == null || value.isBlank()) {
            environmentConfig.setValue(key, defaultValueSupplier.get());
            fileHandler.saveConfig();
        }
    }

    private void ensureSetup() throws IOException, URISyntaxException, CommandFailedException {
        new DotEnvSecManager().ensureDotEnvPermissions();
        new DatabaseSetupCoordinator().ensureDatabaseSetup();
        new SudoPrivilegeManager().setupSudoPrivileges();
    }
}
