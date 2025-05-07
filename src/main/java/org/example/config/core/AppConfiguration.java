package org.example.config.core;

import org.example.config.AppConfigException;
import org.example.config.key_ed25519.KeyManager;
import org.example.exceptions.KeyManagerException;

import java.net.URI;
import java.security.PublicKey;

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
            return keyManager.getPublicKeyOrFetch(getPublicKeyURI());
        } catch (KeyManagerException e) {
            throw new AppConfigException("Failed to retrieve public key", e);
        }
    }    private URI getPublicKeyURI() {
        return environmentConfig.getPublicKeyURI();

    }

    public void setPublicKeyURI(String keyURI) throws KeyManagerException {
        environmentConfig.setPublicKeyURI(keyURI);
        keyManager.fetchKeyAndSave(getPublicKeyURI());
    }


}