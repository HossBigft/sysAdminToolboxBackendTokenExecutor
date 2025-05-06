package org.example.config.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.config.AppConfigException;
import org.example.config.dotenv.DotEnvSecManager;
import org.example.constants.EnvironmentConstants;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;
import org.example.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EnvironmentConfig {

    private Map<String, String> configMap = new HashMap<>();


    public String getEnvFilePath() {
        return getEnvFile().getPath();
    }

    public File getEnvFile() {
        return Paths.get(getConfigDir() + "/" + EnvironmentConstants.ENV_FILENAME).toFile();
    }

    public File getConfigDir() {
        final String appUser = ShellUtils.resolveAppUser();
        return Paths.get("/home/" + appUser + "/." + EnvironmentConstants.APP_NAME).toFile();
    }

    public String getDatabaseUser() {
        return EnvironmentConstants.APP_NAME;
    }

    public String getDatabasePassword() {
        return getValue(getEnvDbPassFieldName());
    }

    public String getValue(String key) {
        return configMap.get(key);
    }

    public String getEnvDbPassFieldName() {
        return EnvironmentConstants.ENV_DB_PASS_FIELD;
    }

    public void setDatabasePassword(String password) {
        setValue(EnvironmentConstants.ENV_DB_PASS_FIELD, password);
    }

    private void setValue(String key,
                          String value) {
        getLogger().debugEntry().message("Put key, value pair to in memory config.").field(key, value).log();
        configMap.put(key, value);
        saveConfig();
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private void saveConfig() {
        ensureConfigDirExists();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File envFile = getEnvFile();
        String action = envFile.exists() ? "Updated" : "Created";

        try {
            mapper.writeValue(envFile, getConfigMap());
            getLogger().info(action + " config file " + envFile);

        } catch (IOException e) {
            throw new AppConfigException("Failed to save config file", e);
        }
    }

    private void ensureConfigDirExists() {
        File configDir = getConfigDir();
        if (!configDir.isDirectory()) {
            try {
                getLogger().warnEntry().message("Config directory is not present")
                        .field("Directory", configDir.toPath())
                        .log();
                Files.createDirectories(configDir.toPath());
                getLogger().infoEntry().message("Created config directory").field("Directory", configDir.toPath())
                        .log();
            } catch (IOException e) {
                throw new AppConfigException("Failed to create config directory", e);
            }
        }
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public Path getPublicKeyPath() {
        return Paths.get(
                new EnvironmentConfig().getConfigDir().toString() + "/" + EnvironmentConstants.PUBLIC_KEY_FILENAME);
    }

    public void loadConfig() {
        ObjectMapper mapper = new ObjectMapper();
        File envFile = getEnvFile();

        if (!envFile.exists()) {
            getLogger().infoEntry().message("Config file does not exist, will create on save").field("File", envFile)
                    .log();
            return;
        }

        try {
            Map<String, String> values = mapper.readValue(envFile, new TypeReference<>() {
            });
            setConfigMap(values);

            setDefaultIfMissing(getEnvDbPassFieldName(),
                    () -> generatePassword(getDbUserPasswordLength()));


            getLogger().debug("Loaded config from " + envFile);
            new DotEnvSecManager().ensureDotEnvPermissions();
        } catch (IOException e) {
            getLogger().warnEntry().message("Failed to load config file").field("File", envFile).exception(e).log();
        }
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = new HashMap<>(configMap);
    }

    private void setDefaultIfMissing(String key,
                                     Supplier<String> defaultValueSupplier) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            setValue(key, defaultValueSupplier.get());
        }
    }

    public String generatePassword(int length) {
        return Utils.generatePassword(length);
    }

    public int getDbUserPasswordLength() {
        return EnvironmentConstants.DB_USER_PASSWORD_LENGTH;
    }

    public URI getPublicKeyURI() {
        String uriStr = getValue(EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD);
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            getLogger().errorEntry().message("Failed to read public key URI")
                    .field("Field", EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD).field("Value", uriStr).exception(e)
                    .log();
            throw new AppConfigException("Failed to read public key URI", e);
        }
    }

    public void setPublicKeyURI(String uri) {
        setValue(EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD, uri);
    }
}

