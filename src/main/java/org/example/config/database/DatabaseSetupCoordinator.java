package org.example.config.database;

import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;


import java.io.IOException;

public class DatabaseSetupCoordinator {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    private static final CliLogger logger = LogManager.getInstance().getLogger();

    static public void ensureDatabaseSetup() {
        try {
            new DataBaseUserSetup().ensureDatabaseUser();
            new DatabasePrivilegeManager().ensureUserIsReadOnly();
        } catch (IOException e) {
            logger.error("Database setup failed: ", e);
        }
    }


}