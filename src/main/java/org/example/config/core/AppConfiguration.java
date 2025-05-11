package org.example.config.core;

import org.example.config.AppConfigException;
import org.example.config.key_ed25519.KeyManager;
import org.example.config.key_ed25519.KeyManagerException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.net.URI;
import java.security.PublicKey;
import java.util.Optional;

import static org.example.utils.Utils.generatePassword;

public class AppConfiguration {
    private static AppConfiguration instance;
    private final EnvironmentConfig environmentConfig;
    private final ConfigBootstrapper bootstrapper;
    private final KeyManager keyManager;


    private AppConfiguration() {
        this.environmentConfig = new EnvironmentConfig();
        this.bootstrapper = new ConfigBootstrapper(environmentConfig);
        this.keyManager = new KeyManager(environmentConfig.getPublicKeyPath());
    }


    public static AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    public void initializeLazily() {
        try {
            new ConfigBootstrapper(environmentConfig).initializeLazily();
        } catch (AppConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new AppConfigException("Failed to initialize configuration", e);
        }
    }

    public void initialize() {
        try {
            new ConfigBootstrapper(environmentConfig).initialize();
        } catch (AppConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new AppConfigException("Failed to initialize configuration", e);
        }
    }

    public String getDatabaseUser() {
        return environmentConfig.getDatabaseUser();
    }

    public String regenerateDatabasePassword() {
        environmentConfig.setDatabasePassword(generatePassword(environmentConfig.getDbUserPasswordLength()));
        return getDatabasePassword();
    }


    public String getDatabasePassword() {
        return environmentConfig.getDatabasePassword();
    }


    public ConfigBootstrapper getBootstrapper() {
        return bootstrapper;
    }

    public PublicKey getPublicKey() {
        try {
            Optional<String> fileKey = keyManager.readKeyFromFile();
            if (fileKey.isPresent()) {
                getLogger().infoEntry()
                        .message("Using existing public key from file")
                        .log();

                try {
                    return keyManager.convertToPublicKey(fileKey.get());
                } catch (KeyManagerException e) {
                    getLogger().errorEntry()
                            .message("Failed to process existing key file")
                            .exception(e)
                            .log();
                }
            }

            URI uri = getPublicKeyURI();
            if (uri == null) {
                getLogger().errorEntry()
                        .message("No public key file and URI is not set")
                        .log();
                throw new AppConfigException("No public key available: file not found and URI not configured");
            }

            return keyManager.getPublicKeyOrFetch(uri);
        } catch (KeyManagerException e) {
            throw new AppConfigException("Failed to retrieve public key", e);
        }
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private URI getPublicKeyURI() {
        return environmentConfig.getPublicKeyURI();
    }

    public void setPublicKeyURI(String keyURI) throws KeyManagerException {
        environmentConfig.setPublicKeyURI(keyURI);
        keyManager.fetchKeyAndSave(getPublicKeyURI());
    }

}