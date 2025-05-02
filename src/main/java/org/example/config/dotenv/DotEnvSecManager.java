package org.example.config.dotenv;

import org.example.config.core.EnvironmentConfig;
import org.example.config.security.FileAccessPolicy;

import java.io.File;
import java.io.IOException;

public class DotEnvSecManager {

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


    public void ensureDotEnvPermissions() throws IOException {
        if (!dotenvFilePolicy.isSecured()) {
            dotenvFilePolicy.enforce();
        }
    }


}
