package org.example.config.core;

import org.example.constants.EnvironmentConstants;
import org.example.utils.ShellUtils;

import java.io.File;
import java.nio.file.Paths;

public class ConfigProvider {
    public static final int DB_USER_PASSWORD_LENGTH = 15;
    private static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";

    public File getConfigDir() {
        final String appUser = ShellUtils.resolveAppUser();
        return Paths.get("/home/" + appUser + "/." + EnvironmentConstants.APP_NAME).toFile();
    }
    public File getEnvFile() {
        return Paths.get(getConfigDir() + "/" + EnvironmentConstants.ENV_FILENAME).toFile();
    }
    public String getEnvDbPassFieldName(){
        return ENV_DB_PASS_FIELD;
    }
    public int getDbUserPasswordLength(){
        return DB_USER_PASSWORD_LENGTH;
    }

}
