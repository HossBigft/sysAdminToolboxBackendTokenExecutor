package org.example.Config;

import org.example.Utils.ShellUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class PermissionManager {
    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    private static File dotEnv = new File(ConfigManager.ENV_PATH);

    public void ensureDotEnvPermissions() throws IOException {

        if (isEnvPermissionsSecureNot(dotEnv)) {
            secureDotEnvPermissionsOwnerGroup(dotEnv);
        }
    }

    private boolean isEnvPermissionsSecureNot(File envFile) throws IOException {
        Path path = envFile.toPath();

        return !ShellUtils.hasCorrectPermissions(path, DOTENV_PERMISSIONS)
                || !ShellUtils.hasCorrectOwner(path, DOTENV_OWNER)
                || !ShellUtils.hasCorrectGroup(path, DOTENV_GROUP);
    }

    private void secureDotEnvPermissionsOwnerGroup(File envFile) throws IOException {
        Path path = envFile.toPath();

        ShellUtils.setPermissions(path, DOTENV_PERMISSIONS);
        ShellUtils.setOwner(path, DOTENV_OWNER);
        ShellUtils.setGroup(path, DOTENV_GROUP);
    }
}
