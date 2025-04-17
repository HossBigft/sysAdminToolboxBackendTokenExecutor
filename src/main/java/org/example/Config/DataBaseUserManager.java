package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DataBaseUserManager {
    private final String databaseUser = ConfigManager.getDatabaseUser();
    private final String databasePassword = ConfigManager.getDatabasePassword();

    public void ensureDatabaseUser() throws IOException {
        if (!isDbUserExists()) {
            createUser();
            System.out.printf("Created database user %s.%n", databaseUser);
        } else {
            System.out.printf("Database user %s already exists.%n", databaseUser);
            if (!isDbUserAbleToConnect()) {
                regenerateDbUserPassword();
                setDbUserPassword();
                ConfigManager.updateDotEnv();
                System.out.printf("Updated password for user %s.%n", databaseUser);
            }
        }
    }

    private boolean isDbUserExists() {
        try {
            List<String> output = ShellUtils.runCommand(ShellUtils.getSqlCliName(),
                    "-u", DatabaseProvisioner.ADMIN_USER,
                    "--skip-column-names",
                    "-e",
                    String.format("SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",

                            databaseUser)
            );


            for (String line : output.getFirst().split("\n")) {
                if (line.trim().equals("1")) {
                    return true;
                }
            }

            return false;
        } catch (CommandFailedException e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            return false;
        }
    }

    private void createUser() {
        try {
            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", DatabaseProvisioner.ADMIN_USER, "-e",
                    String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            databaseUser, databasePassword));


        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
    }

    private boolean isDbUserAbleToConnect() {
        System.out.println(databasePassword);
        try (Connection conn = DriverManager.getConnection(DatabaseProvisioner.DB_URL, databaseUser,
                databasePassword)) {
            return true;
        } catch (SQLException e) {
            System.err.println("User unable to connect: " + e.getMessage());
            return false;
        }
    }

    private void regenerateDbUserPassword() {
        ConfigManager.values.put("DATABASE_PASSWORD",
                Utils.generatePassword(ConfigManager.DB_USER_PASSWORD_LENGTH));
    }

    private void setDbUserPassword() {
        if (databaseUser.equalsIgnoreCase(DatabaseProvisioner.ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", DatabaseProvisioner.ADMIN_USER, "-e",
                    String.format("ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            databaseUser, databasePassword));


        } catch (CommandFailedException e) {
            System.err.println("Error updating user password: " + e.getMessage());
        }
    }
}