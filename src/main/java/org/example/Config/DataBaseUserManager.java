package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.Logging.LogManager;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserManager {
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
            LogManager.log().action("Failed to ensure database user", databaseUser).error(e);
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
                LogManager.log().debug("Database user " + databaseUser + " is present.");
            } else {
                LogManager.log().info("Database user " + databaseUser + " does not exist.");
            }

            return exists;
        } catch (CommandFailedException e) {
            LogManager.log().command(command).error(e);
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
            LogManager.log().info("Created database user " + databaseUser);
        } catch (CommandFailedException e) {
            LogManager.log().command(command).error(e);
            throw e;
        }
    }


    private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DatabaseProvisioner.DB_URL, databaseUser,
                databasePassword)) {
            LogManager.log().action("Check if database user can connect", databaseUser, true).debug();
            return true;
        } catch (SQLException e) {
            LogManager.log().action("Check if database user can connect", databaseUser, true).error(e);
            return false;
        }
    }

    private void regenerateDbUserPassword() {
        databasePassword = Utils.generatePassword(ConfigManager.DB_USER_PASSWORD_LENGTH);
        ConfigManager.values.put("DATABASE_PASSWORD", databasePassword
        );
        LogManager.log().info("Regenerate password for database user " + databaseUser);
    }

    void setDbUserPassword() throws CommandFailedException {
        if (databaseUser.equalsIgnoreCase(DatabaseProvisioner.ADMIN_USER)) {
            LogManager.log()
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
            LogManager.log().info("Set database user password " + databaseUser);
        } catch (CommandFailedException e) {
            LogManager.log().command(command).error(e);
            throw e;
        }
    }
}