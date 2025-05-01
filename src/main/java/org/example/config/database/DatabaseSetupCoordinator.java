package org.example.config.database;

import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;

public class DatabaseSetupCoordinator {
    static final String DB_URL = "jdbc:mysql://localhost:3306";

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    public void ensureDatabaseSetup() {
        new DataBaseUserSetup().setupDatabaseUser();
        new DatabasePrivilegeManager().enforceReadOnlyAccess();
    }


}