package org.example.config.core;

import static org.example.utils.Utils.generatePassword;

public class AppConfiguration {
    private static AppConfiguration instance;
    private final EnvironmentConfig environmentConfig;

    // Private constructor for singleton pattern
    private AppConfiguration() {
        this.environmentConfig = new EnvironmentConfig();
    }

    /**
     * Get singleton instance
     */
    public static synchronized AppConfiguration getInstance() {
        if (instance == null) {
            instance = new AppConfiguration();
        }
        return instance;
    }

    /**
     * Initialize configuration system
     */
    public void initialize() {
        try {
            new ConfigBootstrapper(environmentConfig).initialize();
        } catch (AppConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new AppConfigException("Failed to initialize configuration", e);
        }
    }

    /**
     * Get configuration value
     */
    private String getConfigValue(String key) {
        return environmentConfig.getValue(key);
    }

    public String getDatabaseUser(){
        return environmentConfig.getDatabaseUser();
    }
    public String getDatabasePassword(){
        return  environmentConfig.getDatabasePassword();
    }
    /**
     * Update configuration value
     */
    private void setConfigValue(String key, String value) {
        environmentConfig.setValue(key, value);
    }


    public String regenerateDatabasePassword() {
        setConfigValue("DATABASE_PASSWORD", generatePassword(environmentConfig.getDbUserPasswordLength()));
        saveConfig();
        return getDatabasePassword();
    }

    /**
     * Save current configuration
     */
    public void saveConfig() {
        try {
            new JsonConfigStore(environmentConfig).saveConfig();
        } catch (Exception e) {
            throw new AppConfigException("Failed to save configuration", e);
        }
    }
}