package org.example.config.core;

import org.example.constants.EnvironmentConstants;
import org.example.utils.ShellUtils;
import org.example.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvironmentConfig {
    private static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    private static final Path PUBLIC_KEY_FILENAME = Paths.get("pub.key");

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

    public int getDbUserPasswordLength() {
        return DB_USER_PASSWORD_LENGTH;
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
        return ENV_DB_PASS_FIELD;
    }

    public void setValue(String key,
                         String value) {
        configMap.put(key, value);
    }

    public Map<String, String> getConfigMap() {
        return configMap;
    }

    public void setConfigMap(Map<String, String> configMap) {
        this.configMap = new HashMap<>(configMap);
    }


    public String generatePassword(int length) {
        return Utils.generatePassword(length);
    }
    public Path getPublicKeyPath(){
        return Paths.get(new EnvironmentConfig().getConfigDir().toString() + "/" + PUBLIC_KEY_FILENAME);
    }
}

