package org.example.config.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.config.AppConfigException;
import org.example.config.security.FileAccessPolicy;
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
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    private static final File dotEnvFile = new EnvironmentConfig().getEnvFile();
    private static final FileAccessPolicy
            dotenvFilePolicy =
            new FileAccessPolicy(dotEnvFile)
                    .permissions(DOTENV_PERMISSIONS)
                    .owner(DOTENV_OWNER)
                    .group(DOTENV_GROUP);

    private Map<String, String> configMap = new HashMap<>();

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
        updateValue(EnvironmentConstants.ENV_DB_PASS_FIELD, password);
    }

    private void updateValue(String key,
                             String value) {
        loadConfig();
        configMap.put(key, value);
        saveConfig();
    }

    private void saveConfig() {
        ensureConfigDirExists();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File envFile = getEnvFile();
        String action = envFile.exists() ? "Updated" : "Created";

        try {
            mapper.writeValue(envFile, getConfigMap());
            getConfigMap().forEach(
                    (k, v) -> getLogger().debugEntry().field(k, v).field("Action", action).field("File", envFile)
                            .log());

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

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = new HashMap<>(configMap);
    }

    public Path getPublicKeyPath() {
        return Paths.get(
                new EnvironmentConfig().getConfigDir().toString() + "/" + EnvironmentConstants.PUBLIC_KEY_FILENAME);
    }

    public File getConfigDir() {
        final String appUser = ShellUtils.resolveAppUser();
        return Paths.get("/home/" + appUser + "/." + EnvironmentConstants.APP_NAME).toFile();
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
            dotenvFilePolicy.enforce();
        } catch (IOException e) {
            getLogger().warnEntry().message("Failed to load config file").field("File", envFile).exception(e).log();
        }
    }

    private void setDefaultIfMissing(String key,
                                     Supplier<String> defaultValueSupplier) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            updateValue(key, defaultValueSupplier.get());
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
        if (uriStr == null) {
            getLogger().errorEntry().message("Public key URI is not set or config is nto readable")
                    .field("Field", EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD)
                    .field("File", getEnvFilePath()).log();
            throw new AppConfigException("Failed to read public key URI from config file.");
        }
        try {
            return new URI(uriStr);
        } catch (URISyntaxException e) {
            getLogger().errorEntry().message("Failed to read public key URI")
                    .field("Field", EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD).field("Value", uriStr).exception(e)
                    .log();
            throw new AppConfigException("Failed to read public key URI", e);
        }
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public String getEnvFilePath() {
        return getEnvFile().getPath();
    }

    public File getEnvFile() {
        return Paths.get(getConfigDir() + "/" + EnvironmentConstants.ENV_FILENAME).toFile();
    }

    public void setPublicKeyURI(String uri) {
        updateValue(EnvironmentConstants.ENV_PUBLIC_KEY_URI_FIELD, uri);
    }
}

