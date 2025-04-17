package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.ShellUtils;

import java.util.List;

public class DatabasePermissionManager {

    private static final String databaseUser = ConfigManager.getDatabaseUser();

    void ensureUserIsReadOnly() {
        if (!isDbUserReadOnly()) {
            setReadOnly();
        } else {
            System.out.printf("User %s is already read-only.%n", databaseUser);
        }
    }

    private boolean isDbUserReadOnly() {
        try {
            List<String>
                    output =
                    ShellUtils.runCommand(ShellUtils.getSqlCliName(),
                            "-u",
                            DatabaseProvisioner.ADMIN_USER,
                            "--skip-column-names",
                            "-e",
                            String.format("SHOW GRANTS FOR '%s'@'localhost'", databaseUser));

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

    private void setReadOnly() {
        if (databaseUser.equalsIgnoreCase(DatabaseProvisioner.ADMIN_USER)) {
            System.err.println(
                    "WARNING: Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            String commands = String.format(
                    "REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'localhost'; " +
                            "GRANT SELECT ON *.* TO '%s'@'localhost'; " +
                            "FLUSH PRIVILEGES;",
                    databaseUser, databaseUser);

            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", DatabaseProvisioner.ADMIN_USER, "-e", commands);

        } catch (CommandFailedException e) {
            System.err.println("Error setting user as read-only: " + e.getMessage());
        }
    }
}