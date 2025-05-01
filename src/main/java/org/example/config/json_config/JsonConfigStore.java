package org.example.config.json_config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.config.AppConfigException;
import org.example.config.core.EnvironmentConfig;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class JsonConfigStore {
    private final CliLogger logger;
    private final EnvironmentConfig environmentConfig;

    public JsonConfigStore(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
        this.logger = LogManager.getInstance().getLogger();
    }

    public void loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        File envFile = environmentConfig.getEnvFile();

        if (!envFile.exists()) {
            logger.infoEntry().message("Config file does not exist, will create on save").field("File", envFile).log();
            return;
        }

        try {
            Map<String, String> values = mapper.readValue(envFile, new TypeReference<>() {
            });
            environmentConfig.setConfigMap(values);
            logger.debug("Loaded config from " + envFile);
        } catch (IOException e) {
            logger.warnEntry().message("Failed to load config file").field("File", envFile).exception(e).log();
        }
    }

    public void saveConfig() {
        ensureConfigDirExists();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File envFile = environmentConfig.getEnvFile();
        String action = envFile.exists() ? "Updated" : "Created";

        try {
            mapper.writeValue(envFile, environmentConfig.getConfigMap());
            logger.info(action + " config file " + envFile);
        } catch (IOException e) {
            throw new AppConfigException("Failed to save config file", e);
        }
    }

    public void ensureConfigDirExists() {
        File configDir = environmentConfig.getConfigDir();
        if (!configDir.isDirectory()) {
            try {
                logger.warnEntry().message("Config directory is not present").field("Directory", configDir.toPath())
                        .log();
                Files.createDirectories(configDir.toPath());
                logger.infoEntry().message("Created config directory").field("Directory", configDir.toPath()).log();
            } catch (IOException e) {
                throw new AppConfigException("Failed to create config directory", e);
            }
        }
    }
}
