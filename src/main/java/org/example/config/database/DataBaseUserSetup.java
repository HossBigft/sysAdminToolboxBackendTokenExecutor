package org.example.config.database;

import org.example.config.AppConfigException;
import org.example.config.core.AppConfiguration;
import org.example.constants.EnvironmentConstants;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserSetup {
    private final AppConfiguration appConfiguration;
    private final CliLogger logger;

    public DataBaseUserSetup() {
        this.appConfiguration = AppConfiguration.getInstance();
        this.logger = LogManager.getInstance().getLogger();
    }

    public void setupDatabaseUser() {
        try {
            if (!doesUserExist()) {
                createUser();
            } else {
                if (!isDbUserAbleToConnect()) {
                    regenerateAndSavePassword();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to ensure database user " + getDatabaseUser(), e);
            throw new AppConfigException("Database user setup failed", e);
        }
    }

    boolean doesUserExist() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                getDatabaseUser());

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
                logger.debug("Database user " + getDatabaseUser() + " is present.");
            } else {
                logger.info("Database user " + getDatabaseUser() + " does not exist.");
            }

            return exists;
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to check if database user exists", e);
        }
    }

    void createUser() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                getDatabaseUser(), getDatabasePassword());

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            logger.info("Created database user " + getDatabaseUser());
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to create database user", e);
        }
    }

    private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(
                DatabaseSetupCoordinator.DB_URL,
                getDatabaseUser(),
                getDatabasePassword())) {
            logger.debug(getDatabaseUser() + " can connect to database.");
            return true;
        } catch (SQLException e) {
            logger.error(getDatabaseUser() + " can't connect to database.", e);
            return false;
        }
    }

    private void regenerateAndSavePassword() throws CommandFailedException {

        setDbUserPassword(appConfiguration.regenerateDatabasePassword());


    }

    private String getDatabaseUser() {
        return appConfiguration.getDatabaseUser();
    }

    private String getDatabasePassword() {
        return appConfiguration.getDatabasePassword();
    }

    void setDbUserPassword(String password) throws CommandFailedException {
        if (getDatabaseUser().equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            logger.warn(
                    "WARNING: Refusing to modify the root database user password. Please configure a different database user");
            return;
        }

        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                getDatabaseUser(), password);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            logger.info("Set database user password for " + getDatabaseUser());
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to set database user password", e);
        }
    }

}