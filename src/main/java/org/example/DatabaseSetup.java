package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseSetup {
    static final String DB_URL = "jdbc:mysql://localhost:3306";
    static final String ADMIN_USER = "root";

    static void ensureDatabaseSetup() {
        try {
            ensureDatabaseUser();
            ensureUserIsReadOnly();
        } catch (Exception e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }

    static void ensureUserIsReadOnly() {
        if (!isDbUserReadOnly()) {
            setReadOnly();
        } else {
            System.out.printf("User %s is already read-only.%n", Config.getDatabaseUser());
        }
    }

    static boolean isDbUserReadOnly() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "--skip-column-names",
                    "-e", String.format("SHOW GRANTS FOR '%s'@'localhost'", Config.getDatabaseUser()));

            Process process = pb.start();
            String output = readProcessOutput(process.getInputStream());
            String error = readProcessOutput(process.getErrorStream());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Failed to check user permissions: " + error);
                return false;
            }

            boolean hasOnlySelectPrivileges = true;
            for (String line : output.split("\n")) {
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
        } catch (Exception e) {
            System.err.println("Error checking user permissions: " + e.getMessage());
            return false;
        }
    }

    static void ensureDatabaseUser() throws IOException {
        if (!isDbUserExists()) {
            createUser();
            System.out.printf("Created database user %s.%n", Config.getDatabaseUser());
        } else {
            System.out.printf("Database user %s already exists.%n", Config.getDatabaseUser());
            if (!isDbUserAbleToConnect()) {
                regenerateDbUserPassword();
                setDbUserPassword();
                Config.updateDotEnv();
                System.out.printf("Updated password for user %s.%n", Config.getDatabaseUser());
            }
        }
    }

    static void regenerateDbUserPassword() {
        Config.values.put("DATABASE_PASSWORD",
                Utils.generatePassword(Config.DB_USER_PASSWORD_LENGTH));
    }

    static void setDbUserPassword() {
        if (Config.getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e",
                    String.format("ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            Config.getDatabaseUser(), Config.getDatabasePassword()));

            Process process = pb.start();
            String error = readProcessOutput(process.getErrorStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Failed to update user password: " + error);
            }
        } catch (Exception e) {
            System.err.println("Error updating user password: " + e.getMessage());
        }
    }

    static boolean isDbUserAbleToConnect() {
        try (Connection conn = DriverManager.getConnection(DB_URL, Config.getDatabaseUser(),
                Config.getDatabasePassword())) {
            return true;
        } catch (SQLException e) {
            System.err.println("User unable to connect: " + e.getMessage());
            return false;
        }
    }

    static boolean isDbUserExists() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "--skip-column-names",
                    "-e",
                    String.format("SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                            Config.getDatabaseUser()));

            Process process = pb.start();
            String output = readProcessOutput(process.getInputStream());
            String error = readProcessOutput(process.getErrorStream());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Failed to check if user exists: " + error);
                return false;
            }

            for (String line : output.split("\n")) {
                if (line.trim().equals("1")) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("Error checking if user exists: " + e.getMessage());
            return false;
        }
    }

    static void createUser() {
        if (Config.getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e",
                    String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            Config.getDatabaseUser(), Config.getDatabasePassword()));

            Process process = pb.start();
            String error = readProcessOutput(process.getErrorStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Failed to create user: " + error);
            }
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
    }

    static void setReadOnly() {
        if (Config.getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            String commands = String.format(
                    "REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'localhost'; " + "GRANT SELECT ON *.* TO '%s'@'localhost'; " + "FLUSH PRIVILEGES;",
                    Config.getDatabaseUser(), Config.getDatabaseUser());

            ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e", commands);

            Process process = pb.start();
            String error = readProcessOutput(process.getErrorStream());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Failed to set user as read-only: " + error);
            } else {
                System.out.printf("Set user %s to read-only.%n", Config.getDatabaseUser());
            }
        } catch (Exception e) {
            System.err.println("Error setting user as read-only: " + e.getMessage());
        }
    }

    static String readProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        }
    }

    static String getSqlCliName() throws sysAdminToolboxBackendTokenExecutor.CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            System.err.println("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
            throw new sysAdminToolboxBackendTokenExecutor.CommandFailedException(
                    "Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}