package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Logging.facade.LogManager;
import org.example.Logging.implementations.DefaultCliLogger;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserManager {
    private static final DefaultCliLogger logger = LogManager.getLogger();
    private final String databaseUser = ConfigManager.getDatabaseUser();
    private String databasePassword = ConfigManager.getDatabasePassword();

    public void ensureDatabaseUser() throws IOException {
        try {
            if (!isDbUserExists()) {
                createUser();
            } else {
                if (!isDbUserAbleToConnect()) {
                    regenerateDbUserPassword();
                    setDbUserPassword();
                    ConfigManager.updateDotEnv();
                }
            }
        } catch (CommandFailedException e) {
            LogManager.error("Failed to ensure database user" + databaseUser, e);
        }
    }


    boolean isDbUserExists() throws CommandFailedException {
        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                databaseUser);

        String[] command = new String[]{
                mysqlCliName,
                "-u", DatabaseProvisioner.ADMIN_USER,
                "--skip-column-names",
                "-e", query
        };

        try {
            List<String> output = ShellUtils.runCommand(command);
            boolean exists = output.getFirst().trim().equals("1");

            if (exists) {
                logger.debug("Database user " + databaseUser + " is present.");
            } else {
                logger.info("Database user " + databaseUser + " does not exist.");
            }

            return exists;
        } catch (CommandFailedException e) {
            logger.errorEntry().command(command).exception(e).log();
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
                "-u", DatabaseProvisioner.ADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            logger.info("Created database user " + databaseUser);
        } catch (CommandFailedException e) {
            logger.errorEntry().command(command).exception(e).log();
            throw e;
        }
    }


    private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DatabaseProvisioner.DB_URL, databaseUser,
                databasePassword)) {
            LogManager.debug(databaseUser + " can connec to to database.");
            return true;
        } catch (SQLException e) {
            LogManager.error(databaseUser + " can't connect to to database.", e);
            return false;
        }
    }

    private void regenerateDbUserPassword() {
        databasePassword = Utils.generatePassword(ConfigManager.DB_USER_PASSWORD_LENGTH);
        ConfigManager.values.put("DATABASE_PASSWORD", databasePassword
        );
        logger.info("Regenerate password for database user " + databaseUser);
    }

    void setDbUserPassword() throws CommandFailedException {
        if (databaseUser.equalsIgnoreCase(DatabaseProvisioner.ADMIN_USER)) {
            logger
                    .warn("WARNING: Refusing to modify the root database user password. Please configure a different database user");
            return;
        }

        String mysqlCliName = ShellUtils.getSqlCliName();
        String query = String.format(
                "ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                databaseUser, databasePassword);

        String[] command = new String[]{
                mysqlCliName,
                "-u", DatabaseProvisioner.ADMIN_USER,
                "-e", query
        };

        try {
            ShellUtils.runCommand(command);
            logger.info("Set database user password " + databaseUser);
        } catch (CommandFailedException e) {
            logger.errorEntry().command(command).exception(e).log();
            throw e;
        }
    }
}