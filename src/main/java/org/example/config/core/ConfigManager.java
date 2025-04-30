package org.example.config.core;

public class ConfigManager {


    private ConfigManager() {
    }

    public static void initialize() {
        new ConfigInitialiser().initialise();
    }


}