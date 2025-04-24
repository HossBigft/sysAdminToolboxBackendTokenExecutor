package org.example.Config;

import org.example.Logging.facade.LogManager;
import org.example.Logging.implementations.DefaultCliLogger;

import java.io.IOException;

public class DatabaseProvisioner {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    static final String ADMIN_USER = "root";
    private static final DefaultCliLogger logger = LogManager.getLogger();

    static public void ensureDatabaseSetup() {
        try {
            new DataBaseUserManager().ensureDatabaseUser();
            new DatabasePermissionManager().ensureUserIsReadOnly();
        } catch (IOException e) {
            logger.error("Database setup failed: ", e);
        }
    }


}