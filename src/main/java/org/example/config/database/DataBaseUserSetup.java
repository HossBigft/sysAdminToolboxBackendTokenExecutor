package org.example.config.database;

import org.example.config.AppConfigException;
import org.example.config.constants.EnvironmentConstants;
import org.example.config.constants.Executables;
import org.example.config.core.AppConfiguration;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.CommandFailedException;
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

    boolean doesUserExist() {
        String dbUser = getDatabaseUser();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s' AND host='127.0.0.1') AS user_exists;",
                dbUser);

        String[] command = new String[]{Executables.PLESK_CLI_EXECUTABLE, "db", "-Ne", query};

        try {
            ShellUtils.ExecutionResult result = ShellUtils.execute(command);
            List<String> output = result.stdout();
            if (!result.isSuccessful()) {
                throw new CommandFailedException(result.getFormattedErrorMessage());
            }
            boolean exists = output.getFirst().trim().equals("1");

            if (exists) {
                logger.debugEntry().message("Database user is present.").field("User", dbUser).log();
            } else {
                logger.infoEntry().message("Database user does not exist.").field("User", dbUser).log();
            }

            return exists;
        } catch (CommandFailedException e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to check if database user exists", e);
        }
    }

    void createUser() {
        String dbUser = getDatabaseUser();
        String dbPass = getDatabasePassword();
        String query = String.format("CREATE USER '%s'@'127.0.0.1' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;", dbUser,
                dbPass);

        String[] command = new String[]{Executables.PLESK_CLI_EXECUTABLE, "db", query};

        try {
            ShellUtils.execute(command);
            logger.infoEntry().message("Created database user").field("User", dbUser).log();
        } catch (Exception e) {
            logger.errorEntry().command(command).exception(e).log();
            throw new AppConfigException("Failed to create database user", e);
        }
    }

    public boolean isDbUserAbleToConnect() {
        String dbUser = getDatabaseUser();
        String dbPass = getDatabasePassword();
        try (Connection conn = DriverManager.getConnection(DatabaseSetupCoordinator.DB_URL, dbUser, dbPass)) {
            logger.debugEntry().message("User can connect to database.").field("User", dbUser).log();
            return true;
        } catch (SQLException e) {
            logger.errorEntry().message("User can't connect to database.").field("User", dbUser).log();
            logger.debugEntry().message(getDatabaseUser() + " can't connect to database.").exception(e).log();
            return false;
        }
    }

    private void regenerateAndSavePassword() throws CommandFailedException {

        setDbUserPassword(appConfiguration.regenerateDatabasePassword());


    }

    private String getDatabasePassword() {
        return appConfiguration.getDatabasePassword();
    }

    void setDbUserPassword(String password) {
        if (getDatabaseUser().equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            logger.warn(
                    "WARNING: Refusing to modify the root database user password. Please configure a different database user");
            return;
        }

        String query = String.format("SET PASSWORD FOR '%s'@'127.0.0.1'= PASSWORD('%s'); FLUSH PRIVILEGES;",
                getDatabaseUser(), password);

        String[] command = new String[]{Executables.PLESK_CLI_EXECUTABLE, "db", query};

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