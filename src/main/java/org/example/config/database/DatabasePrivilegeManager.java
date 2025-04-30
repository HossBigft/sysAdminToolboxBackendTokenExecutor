package org.example.config.database;

import org.example.config.core.ConfigProvider;
import org.example.constants.EnvironmentConstants;
import org.example.exceptions.CommandFailedException;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.utils.ShellUtils;
import org.example.config.core.ConfigManager;

import java.util.List;

public class DatabasePrivilegeManager {

    private static final String databaseUser = new ConfigProvider().getDatabaseUser();

    public void enforceReadOnlyAccess() {
        getLogger().
                debug("Checking if user " + databaseUser + " is read-only.");
        if (!isDbUserReadOnly()) {
            setReadOnly();
        } else {
            getLogger().
                    debug("User " + databaseUser + " is already read-only.");
        }
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private boolean isDbUserReadOnly() {
        try {
            List<String>
                    output =
                    ShellUtils.runCommand(ShellUtils.getSqlCliName(),
                            "-u",
                            EnvironmentConstants.SUPERADMIN_USER,
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
            getLogger().
                    error("Error checking user permissions for " + databaseUser, e);
            return false;
        }
    }

    private void setReadOnly() {
        if (databaseUser.equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            getLogger().
                    warn("Refusing to modify the root user. Please configure a different database user.");
            return;
        }
        try {
            String
                    commands =
                    String.format("REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'localhost'; " +
                            "GRANT SELECT ON *.* TO '%s'@'localhost'; " +
                            "FLUSH PRIVILEGES;", databaseUser, databaseUser);

            ShellUtils.runCommand(ShellUtils.getSqlCliName(), "-u", EnvironmentConstants.SUPERADMIN_USER, "-e",
                    commands);
            getLogger().
                    info(String.format("Set user %s as read-only successfully.", databaseUser));
        } catch (CommandFailedException e) {
            getLogger().
                    error("Error setting database user " + databaseUser + " as read-only.", e);
        }
    }
}