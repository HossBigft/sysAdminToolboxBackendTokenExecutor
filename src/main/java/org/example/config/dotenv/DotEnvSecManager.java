package org.example.config.dotenv;

import org.example.config.core.ConfigManager;
import org.example.config.core.ConfigProvider;
import org.example.config.security.FileAccessPolicy;
import org.example.config.security.FileSecurityManager;

import java.io.File;
import java.io.IOException;

public class DotEnvSecManager {

    private static final String DOTENV_PERMISSIONS = "rw-------";
    private static final String DOTENV_OWNER = "root";
    private static final String DOTENV_GROUP = "root";
    private static final FileAccessPolicy
            dotenvFilePolicy =
            new FileAccessPolicy(DOTENV_PERMISSIONS, DOTENV_OWNER, DOTENV_GROUP);
    private static final File dotEnvFile = new ConfigProvider().getEnvFile();

    public void ensureDotEnvPermissions() throws IOException {
        FileSecurityManager fileSecurityManager = new FileSecurityManager();
        if (!fileSecurityManager.isFilePermissionsSecure(dotEnvFile, dotenvFilePolicy)) {
              fileSecurityManager.enforceFileAccessPolicy(dotEnvFile, dotenvFilePolicy);
        }
    }


}
