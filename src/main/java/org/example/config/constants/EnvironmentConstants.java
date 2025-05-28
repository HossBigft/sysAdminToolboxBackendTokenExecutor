package org.example.config.constants;

import java.nio.file.Path;
import java.nio.file.Paths;

public class EnvironmentConstants {
    public static final String APP_NAME = "secOpsDispatcher";
    public static final String ENV_FILENAME = ".env.json";
    public static final String SUPERADMIN_USER = "root";
    public static final int DB_USER_PASSWORD_LENGTH = 15;
    public static final String ENV_DB_PASS_FIELD = "DATABASE_PASSWORD";
    public static final Path PUBLIC_KEY_FILENAME = Paths.get("pub.key");
    public static final String ENV_PUBLIC_KEY_URI_FIELD = "PUBLIC_KEY_URI";
}
