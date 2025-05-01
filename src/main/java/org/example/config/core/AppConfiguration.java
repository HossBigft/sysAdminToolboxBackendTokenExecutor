package org.example.config.core;

import org.example.config.AppConfigException;
import org.example.config.json_config.JsonConfigStore;

import static org.example.utils.Utils.generatePassword;

public class AppConfiguration {
    private static AppConfiguration instance;
    private final EnvironmentConfig environmentConfig;
    private final ConfigBootstrapper bootstrapper;


    private AppConfiguration() {
        this.environmentConfig = new EnvironmentConfig();
        this.bootstrapper = new ConfigBootstrapper(environmentConfig);
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
        setConfigValue("DATABASE_PASSWORD", generatePassword(environmentConfig.getDbUserPasswordLength()));
        saveConfig();
        return getDatabasePassword();
    }


    private void setConfigValue(String key,
                                String value) {
        environmentConfig.setValue(key, value);
    }


    public void saveConfig() {
        try {
            new JsonConfigStore(environmentConfig).saveConfig();
        } catch (Exception e) {
            throw new AppConfigException("Failed to save configuration", e);
        }
    }

    public String getDatabasePassword() {
        return environmentConfig.getDatabasePassword();
    }


    public ConfigBootstrapper getBootstrapper() {
        return bootstrapper;
    }
}