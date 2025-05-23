package org.example.config.database;

import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;

public class DatabaseSetupCoordinator {
    static final String DB_URL = "jdbc:mysql://localhost:3306";

    public void ensureDatabaseSetup() {
        if (ShellUtils.isDatabaseInstalled()) {
            getLogger().infoEntry().message("Database is present. Proceeding to database user setup.").log();
            new DataBaseUserSetup().setupDatabaseUser();
            new DatabasePrivilegeManager().enforceReadOnlyAccess();
        } else {
            getLogger().infoEntry().message("Database is not found. Skipping database user setup.").log();
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }


}