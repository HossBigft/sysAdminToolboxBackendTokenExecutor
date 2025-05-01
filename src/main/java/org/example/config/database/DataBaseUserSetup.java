package org.example.config.database;

import org.example.config.core.ConfigFileHandler;
import org.example.config.core.ConfigProvider;
import org.example.constants.EnvironmentConstants;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserSetup {
    private static final ConfigProvider cprovider = new ConfigProvider();
    private static final ConfigFileHandler chandler = new ConfigFileHandler();


    public void setupDatabaseUser() throws IOException {
        try {
            if (!doesUserExist()) {
                createUser();
            } else {
                if (!isDbUserAbleToConnect()) {
                    cprovider.regenerateDbUserPassword();
                    setDbUserPassword();
                    chandler.saveConfig();
                }
            }
        } catch (CommandFailedException e) {
            getLogger().
                    error("Failed to ensure database user" + cprovider.getDatabaseUser(), e);
        }
    }

    boolean doesUserExist() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                cprovider.getDatabaseUser());

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
                        debug("Database user " + cprovider.getDatabaseUser() + " is present.");
            } else {
                getLogger().
                        info("Database user " + cprovider.getDatabaseUser() + " does not exist.");
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
                cprovider.getDatabaseUser(), cprovider.getDatabasePassword());

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            getLogger().
                    info("Created database user " + cprovider.getDatabaseUser());
        } catch (CommandFailedException e) {
            getLogger().
                    errorEntry().command(command).exception(e).log();
            throw e;
        }
    }

    private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DatabaseSetupCoordinator.DB_URL, cprovider.getDatabaseUser(),
                cprovider.getDatabasePassword())) {
            getLogger().
                    debug(cprovider.getDatabaseUser() + " can connect to to database.");
            return true;
        } catch (SQLException e) {
            getLogger().
                    error(cprovider.getDatabaseUser() + " can't connect to to database.", e);
            return false;
        }
    }


    void setDbUserPassword() throws CommandFailedException {
        if (cprovider.getDatabaseUser().equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            getLogger().warn(
                    "WARNING: Refusing to modify the root database user password. Please configure a different database user");
            return;
        }

        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                cprovider.getDatabaseUser(), cprovider.getDatabasePassword());

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            getLogger().
                    info("Set database user password " + cprovider.getDatabaseUser());
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