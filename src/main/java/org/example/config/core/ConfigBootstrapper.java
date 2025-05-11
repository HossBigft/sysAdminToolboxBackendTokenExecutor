package org.example.config.core;

import org.example.config.AppConfigException;
import org.example.config.database.DatabaseSetupCoordinator;
import org.example.config.key_ed25519.KeyManager;
import org.example.config.key_ed25519.KeyManagerException;
import org.example.config.security.SudoPrivilegeManager;
import org.example.constants.Executables;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ProcessFailedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ConfigBootstrapper {
    private final EnvironmentConfig environmentConfig;
    private final CliLogger logger;

    private boolean isDbSetup = false;
    private boolean isSudoConfigured = false;
    private boolean isConfigLoaded = false;

    public ConfigBootstrapper(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
        this.logger = LogManager.getInstance().getLogger();
    }

    public void initializeLazily() {
        checkPrerequisites();
        try {
            logger.debugEntry().message("Starting lazy initialisation...").log();
            loadConfigIfNeeded();
            ensureSudoPrivilegesConfigured();
            logger.debugEntry().message("Lazy initialization finished successfully.").log();
        } catch (Exception e) {
            logger.errorEntry().message("Lazy initialization failed.").exception(e).log();
            throw new AppConfigException("Lazy configuration initialization failed", e);
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
            environmentConfig.loadConfig();
            isConfigLoaded = true;
        }
    }

    public void ensureSudoPrivilegesConfigured() throws IOException, ProcessFailedException {
        if (!isSudoConfigured) {
            new SudoPrivilegeManager().setupSudoPrivileges();
            isSudoConfigured = true;
        }
    }

    public void initialize() {
        checkPrerequisites();
        try {
            logger.debugEntry().message("Starting initialisation...").log();
            environmentConfig.loadConfig();
            logger.debug("Config file " + environmentConfig.getEnvFilePath() + " is loaded.");
            ensureSudoPrivilegesConfigured();
            new DatabaseSetupCoordinator().ensureDatabaseSetup();
            ensurePublicKey();
        } catch (Exception e) {
            logger.error("Failed to initialize configuration", e);
            throw new AppConfigException("Configuration initialization failed", e);
        }
    }

    private void ensurePublicKey() {
        logger.debugEntry().message("Ensuring public key is present.").log();
        KeyManager km = new KeyManager(environmentConfig.getPublicKeyPath());
        if (km.readKeyFromFile().isEmpty()) {
            try {
                km.fetchKeyAndSave(environmentConfig.getPublicKeyURI());
            } catch (AppConfigException e) {
                System.err.println("Public key link is not set. Set it with 'init LINK' command");
            } catch (KeyManagerException e) {

            }
        }
    }

    public void ensureDatabaseSetup() {
        if (!isDbSetup) {
            new DatabaseSetupCoordinator().ensureDatabaseSetup();
            isDbSetup = true;
        }
    }
}

