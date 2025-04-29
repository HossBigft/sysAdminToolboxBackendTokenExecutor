package org.example.config.database;

import org.example.constants.EnvironmentConstants;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;
import org.example.utils.Utils;
import org.example.config.core.ConfigManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserSetup {
    private final String databaseUser = ConfigManager.getDatabaseUser();

    private String databasePassword = ConfigManager.getDatabasePassword();

    public void setupDatabaseUser() throws IOException {
        try {
            if (!doesUserExist()) {
                createUser();
            } else {
                if (!isDbUserAbleToConnect()) {
                    regenerateDbUserPassword();
                    setDbUserPassword();
                    ConfigManager.updateDotEnv();
                }
            }
        } catch (CommandFailedException e) {
            getLogger().
                    error("Failed to ensure database user" + databaseUser, e);
        }
    }

    boolean doesUserExist() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                databaseUser);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "--skip-column-names",
                "-e", query
        };

        try {
            List<String> output = ShellUtils.runCommand(command);
            boolean exists = output.getFirst().trim().equals("1");

            if (exists) {
                getLogger().
                        debug("Database user " + databaseUser + " is present.");
            } else {
                getLogger().
                        info("Database user " + databaseUser + " does not exist.");
            }

            return exists;
        } catch (CommandFailedException e) {
            getLogger().
                    errorEntry().command(command).exception(e).log();
            throw e;
        }
    }

    void createUser() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                databaseUser, databasePassword);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            getLogger().
                    info("Created database user " + databaseUser);
        } catch (CommandFailedException e) {
            getLogger().
                    errorEntry().command(command).exception(e).log();
            throw e;
        }
    }

    private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DatabaseSetupCoordinator.DB_URL, databaseUser,
                databasePassword)) {
            getLogger().
                    debug(databaseUser + " can connec to to database.");
            return true;
        } catch (SQLException e) {
            getLogger().
                    error(databaseUser + " can't connect to to database.", e);
            return false;
        }
    }

    private void regenerateDbUserPassword() {
        databasePassword = Utils.generatePassword(ConfigManager.getDatabasePasswordLength());
        ConfigManager.values.put("DATABASE_PASSWORD", databasePassword
        );
        getLogger().
                info("Regenerate password for database user " + databaseUser);
    }

    void setDbUserPassword() throws CommandFailedException {
        if (databaseUser.equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            getLogger().warn(
                    "WARNING: Refusing to modify the root database user password. Please configure a different database user");
            return;
        }

        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                databaseUser, databasePassword);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            getLogger().
                    info("Set database user password " + databaseUser);
        } catch (CommandFailedException e) {
            getLogger().
                    errorEntry().command(command).exception(e).log();
            throw e;
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }
}