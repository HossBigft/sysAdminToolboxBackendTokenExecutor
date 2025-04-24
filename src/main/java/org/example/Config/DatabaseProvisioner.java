package org.example.Config;

import org.example.Utils.Logging.facade.LogManager;

import java.io.IOException;

public class DatabaseProvisioner {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    static final String ADMIN_USER = "root";

    static public void ensureDatabaseSetup() {
        try {
            new DataBaseUserManager().ensureDatabaseUser();
            new DatabasePermissionManager().ensureUserIsReadOnly();
        } catch (IOException e) {
            LogManager.error("Database setup failed: ", e);
        }
    }


}