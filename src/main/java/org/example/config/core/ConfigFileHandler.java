package org.example.config.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigFileHandler {
    private static final CliLogger logger = LogManager.getInstance().getLogger();
    private static final ConfigProvider cprovider = new ConfigProvider();

    public void ensureConfigDirExists() throws IOException {
        File configDir = cprovider.getConfigDir();
        if (!configDir.isDirectory()) {
            logger.warnEntry().message("Config directory is not present").field("Directory", configDir.toPath()).log();
            Files.createDirectories(configDir.toPath());
            logger.infoEntry().message("Created config directory").field("Directory", configDir.toPath()).log();
        }
    }

    public Map<String, String> loadConfig(File envFile) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> envValues = new HashMap<>();
        try {
            envValues = mapper.readValue(envFile, new TypeReference<>() {
            });
            logger.debug("Loaded dotenv " + envFile);
        } catch (IOException e) {
            logger.infoEntry().message("Dotenv file not found or invalid.").field("File", envFile).log();
            return envValues;
        }
        return envValues;
    }

    public void saveConfig(File envFile, Map<String,String> values) throws IOException {
        ensureConfigDirExists();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        if (!envFile.isFile()) {
            logger.info("Creating dotenv " + envFile);
        } else {
            logger.info("Writing to dotenv " + envFile);
        }
        mapper.writeValue(envFile, values);

        logger.info("New data written to " + envFile);
    }

    public void saveConfig() throws IOException {
        ensureConfigDirExists();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File envFile =cprovider.getEnvFile();
        if (!envFile.isFile()) {
            logger.info("Creating dotenv " + envFile);
        } else {
            logger.info("Writing to dotenv " + envFile);
        }
        mapper.writeValue(envFile, cprovider.getConfigMap());

        logger.info("New data written to " + envFile);
    }
}
