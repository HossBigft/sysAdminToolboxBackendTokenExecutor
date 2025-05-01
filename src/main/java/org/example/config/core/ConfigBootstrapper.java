package org.example.config.core;

import org.example.config.AppConfigException;
import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.json_config.JsonConfigStore;
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

    private boolean isDbSetup = false;
    private boolean isSudoConfigured = false;
    private boolean isConfigLoaded = false;

    public ConfigBootstrapper(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
        this.fileHandler = new JsonConfigStore(environmentConfig);
        this.logger = LogManager.getInstance().getLogger();
    }

    public void initializeLazily() {
        checkPrerequisites();
        try {
            loadConfigIfNeeded();
            ensureSudoPrivilegesConfigured();
            logger.debug("Config file " + environmentConfig.getEnvFilePath() + " is loaded.");
        } catch (Exception e) {
            logger.error("Failed to initialize configuration", e);
            throw new AppConfigException("Configuration initialization failed", e);
        }
    }
    public void initialize() {
        checkPrerequisites();
        try {
            fileHandler.loadConfig();
            logger.debug("Config file " + environmentConfig.getEnvFilePath() + " is loaded.");
            ensureSudoPrivilegesConfigured();
            new DatabaseSetupCoordinator().ensureDatabaseSetup();
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

    private void loadConfigIfNeeded() {
        if (!isConfigLoaded) {
            fileHandler.loadConfig();
            setDefaultIfMissing(environmentConfig.getEnvDbPassFieldName(),
                    () -> environmentConfig.generatePassword(environmentConfig.getDbUserPasswordLength()));
            isConfigLoaded = true;
        }
    }

    public void ensureSudoPrivilegesConfigured() throws IOException, CommandFailedException, URISyntaxException {
        if (!isSudoConfigured) {
            new SudoPrivilegeManager().setupSudoPrivileges();
            isSudoConfigured = true;
        }
    }

    private void setDefaultIfMissing(String key,
                                     Supplier<String> defaultValueSupplier) {
        String value = environmentConfig.getValue(key);
        if (value == null || value.isBlank()) {
            environmentConfig.setValue(key, defaultValueSupplier.get());
            fileHandler.saveConfig();
        }
    }

    public void ensureDatabaseSetup() {
        if (!isDbSetup) {
            new DatabaseSetupCoordinator().ensureDatabaseSetup();
            isDbSetup = true;
        }
    }
}

