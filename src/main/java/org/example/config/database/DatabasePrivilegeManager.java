package org.example.config.database;

import org.example.config.core.ConfigManager;
import org.example.Constants.EnvironmentConstants;
import org.example.Exceptions.CommandFailedException;
import org.example.Logging.core.CliLogger;
import org.example.Logging.facade.LogManager;
import org.example.Utils.ShellUtils;

import java.util.List;

public class DatabasePrivilegeManager {

    private static final String databaseUser = ConfigManager.getDatabaseUser();
    private static final CliLogger logger = LogManager.getInstance().getLogger();

    public void ensureUserIsReadOnly() {
        logger.debug("Checking if user " + databaseUser + " is read-only.");
        if (!isDbUserReadOnly()) {
            setReadOnly();
        } else {
            logger.debug("User " + databaseUser + " is already read-only.");
        }
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
            logger.error("Error checking user permissions for " + databaseUser, e);
            return false;
        }
    }

    private void setReadOnly() {
        if (databaseUser.equalsIgnoreCase(EnvironmentConstants.SUPERADMIN_USER)) {
            logger.warn("Refusing to modify the root user. Please configure a different database user.");
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
            logger.info(String.format("Set user %s as read-only successfully.", databaseUser));
        } catch (CommandFailedException e) {
            logger.error("Error setting database user " + databaseUser + " as read-only.", e);
        }
    }
}