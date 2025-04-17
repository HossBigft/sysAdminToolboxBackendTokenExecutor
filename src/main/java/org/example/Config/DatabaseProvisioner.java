package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class DatabaseProvisioner {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    static final String ADMIN_USER = "root";

    static public void ensureDatabaseSetup() {
        try {
            ensureDatabaseUser();
            ensureUserIsReadOnly();
        } catch (IOException e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }

    static private void ensureDatabaseUser() throws IOException {
        if (!isDbUserExists()) {
            createUser();
            System.out.printf("Created database user %s.%n", ConfigManager.getDatabaseUser());
        } else {
            System.out.printf("Database user %s already exists.%n", ConfigManager.getDatabaseUser());
            if (!isDbUserAbleToConnect()) {
                regenerateDbUserPassword();
                setDbUserPassword();
                ConfigManager.updateDotEnv();
                System.out.printf("Updated password for user %s.%n", ConfigManager.getDatabaseUser());
            }
        }
    }

    static private void ensureUserIsReadOnly() {
        if (!isDbUserReadOnly()) {
            setReadOnly();
        } else {
            System.out.printf("User %s is already read-only.%n", ConfigManager.getDatabaseUser());
        }
    }

    static private boolean isDbUserExists() {
        try {
            List<String> output = ShellUtils.runCommand(ShellUtils.getSqlCliName(),
                    "-u", ADMIN_USER,
                    "--skip-column-names",
                    "-e",
                    String.format("SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",

                            ConfigManager.getDatabaseUser())
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

    static private void createUser() {
        try {
            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", ADMIN_USER, "-e",
                    String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            ConfigManager.getDatabaseUser(), ConfigManager.getDatabasePassword()));


        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
    }

    static private boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DB_URL, ConfigManager.getDatabaseUser(),
                ConfigManager.getDatabasePassword())) {
            return true;
        } catch (SQLException e) {
            System.err.println("User unable to connect: " + e.getMessage());
            return false;
        }
    }

    static private void regenerateDbUserPassword() {
        ConfigManager.values.put("DATABASE_PASSWORD",
                Utils.generatePassword(ConfigManager.DB_USER_PASSWORD_LENGTH));
    }

    static private void setDbUserPassword() {
        if (ConfigManager.getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", ADMIN_USER, "-e",
                    String.format("ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            ConfigManager.getDatabaseUser(), ConfigManager.getDatabasePassword()));


        } catch (CommandFailedException e) {
            System.err.println("Error updating user password: " + e.getMessage());
        }
    }

    static private boolean isDbUserReadOnly() {
        try {
            List<String>
                    output =
                    ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", ADMIN_USER, "--skip-column-names",
                            "-e", String.format("SHOW GRANTS FOR '%s'@'localhost'", ConfigManager.getDatabaseUser()));

            boolean hasOnlySelectPrivileges = true;
            for (String line : output) {
                line = line.toUpperCase();
                if (!line.contains("SHOW GRANTS") && !line.contains("GRANTS FOR")) {
                    if (!line.contains("SELECT")) {
                        hasOnlySelectPrivileges = false;
                        break;
                    }

                    if (line.matches(
                            ".*\\b(INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|INDEX|EXECUTE|ALL PRIVILEGES)\\b.*")) {
                        hasOnlySelectPrivileges = false;
                        break;
                    }
                }
            }

            return hasOnlySelectPrivileges;
        } catch (CommandFailedException e) {
            System.err.println("Error checking user permissions: " + e.getMessage());
            return false;
        }
    }

    static private void setReadOnly() {
        if (ConfigManager.getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            String commands = String.format(
                    "REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'localhost'; " +
                            "GRANT SELECT ON *.* TO '%s'@'localhost'; " +
                            "FLUSH PRIVILEGES;",
                    ConfigManager.getDatabaseUser(), ConfigManager.getDatabaseUser());

            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", ADMIN_USER, "-e", commands);

        } catch (CommandFailedException e) {
            System.err.println("Error setting user as read-only: " + e.getMessage());
        }
    }


}