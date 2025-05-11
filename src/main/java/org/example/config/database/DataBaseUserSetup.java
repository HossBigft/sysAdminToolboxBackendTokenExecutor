package org.example.config.database;

import org.example.config.AppConfigException;
import org.example.config.core.AppConfiguration;
import org.example.constants.EnvironmentConstants;
import org.example.operations.OperationFailedException;
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
        String dbUser = getDatabaseUser();
        try {
            if (!doesUserExist()) {
                createUser();
            } else {
                if (!isDbUserAbleToConnect()) {
                    regenerateAndSavePassword();
                }
            }
        } catch (Exception e) {
            logger.errorEntry().message("Failed to ensure database user.").field("User", dbUser).log();
            throw new AppConfigException("Database user setup failed", e);
        }
    }

    private String getDatabaseUser() {
        return appConfiguration.getDatabaseUser();
    }

    boolean doesUserExist() throws OperationFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String dbUser = getDatabaseUser();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                dbUser);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "--skip-column-names",
                "-e", query
        };

        try {
            ShellUtils.ShellCommandResult result = ShellUtils.execute(command);
            List<String> output = result.stdout();
            if (!result.isSuccessful()) {
                throw new OperationFailedException(result.getFormattedErrorMessage());
            }
            boolean exists = output.getFirst().trim().equals("1");

            if (exists) {
                logger.debugEntry().message("Database user is present.").field("User", dbUser).log();
            } else {
                logger.infoEntry().message("Database user does not exist.").field("User", dbUser).log();
            }

            return exists;
        } catch (OperationFailedException e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to check if database user exists", e);
        }
    }

    void createUser() throws OperationFailedException {
        String dbUser = getDatabaseUser();
        String dbPass = getDatabasePassword();
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                dbUser, dbPass);

        String[] command = new String[]{
                mysqlCliName,
                "-u", EnvironmentConstants.SUPERADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.execute(command);
            logger.infoEntry().message("Created database user").field("User", dbUser).log();
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to create database user", e);
        }
    }

    private boolean isDbUserAbleToConnect() {
        String dbUser = getDatabaseUser();
        String dbPass = getDatabasePassword();
        try (Connection conn = DriverManager.getConnection(
                DatabaseSetupCoordinator.DB_URL,
                dbUser, dbPass)) {
            logger.debugEntry().message("User can connect to database.").field("User", dbUser).log();
            return true;
        } catch (SQLException e) {
            logger.error(getDatabaseUser() + " can't connect to database.", e);
            return false;
        }
    }

    private void regenerateAndSavePassword() throws OperationFailedException {

        setDbUserPassword(appConfiguration.regenerateDatabasePassword());


    }

    private String getDatabasePassword() {
        return appConfiguration.getDatabasePassword();
    }

    void setDbUserPassword(String password) throws OperationFailedException {
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
            ShellUtils.execute(command);
            logger.infoEntry().message("Set database user password").field("User", getDatabaseUser())
                    .field("Password", getDatabasePassword()).log();
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to set database user password", e);
        }
    }

}