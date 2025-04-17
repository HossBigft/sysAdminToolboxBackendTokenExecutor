package org.example.Config;

import java.io.IOException;

public class DatabaseProvisioner {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    static final String ADMIN_USER = "root";

    static public void ensureDatabaseSetup() {
        try {
            new DataBaseUserManager().ensureDatabaseUser();
            new DatabasePermissionManager().ensureUserIsReadOnly();
        } catch (IOException e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }


}