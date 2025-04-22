package org.example.Config;

import org.example.Exceptions.CommandFailedException;
import org.example.Utils.Logging.LogManager;
import org.example.Utils.ShellUtils;
import org.example.Utils.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DataBaseUserManager {
    private final String databaseUser = ConfigManager.getDatabaseUser();
    private String databasePassword = ConfigManager.getDatabasePassword();

    public void ensureDatabaseUser() throws IOException {
        if (!isDbUserExists()) {
            createUser();
        } else {
            if (!isDbUserAbleToConnect()) {
                regenerateDbUserPassword();
                setDbUserPassword();
                ConfigManager.updateDotEnv();
            }
        }
    }

    private boolean isDbUserExists() {
        String mysqlCliName;
        try {
            mysqlCliName = ShellUtils.getSqlCliName();
            String[] command = new String[]{mysqlCliName,
                    "-u", DatabaseProvisioner.ADMIN_USER,
                    "--skip-column-names",
                    "-e",
                    String.format("SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                            databaseUser)};
            try {

                List<String> output = ShellUtils.runCommand(command);

                for (String line : output.getFirst().split("\n")) {
                    if (line.trim().equals("1")) {
                        LogManager.log().action("Check if database user is present", databaseUser, true).debug();
                        return true;
                    }
                }
                LogManager.log().action("Check if database user is present", databaseUser, false).debug();
                return false;
            } catch (CommandFailedException e) {
                LogManager.log()
                        .command(command[0], Arrays.copyOfRange(command, 1, command.length))
                        .error(e);
                return false;
            }
        } catch (CommandFailedException e) {
            LogManager.log()
                    .action("Get MySQL cli executable name to check database user", databaseUser, false)
                    .error(e);
        }
        return false;
    }

    private void createUser() {
        String mysqlCliName;
        try {
            mysqlCliName = ShellUtils.getSqlCliName();
            String[] command = new String[]{mysqlCliName, "-u", DatabaseProvisioner.ADMIN_USER, "-e",
                    String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            databaseUser, databasePassword)};
            try {

                ShellUtils.runCommand(command);

                LogManager.log().action("Create database user", databaseUser, true).info();
            } catch (CommandFailedException e) {
                LogManager.log()
                        .command(command[0], Arrays.copyOfRange(command, 1, command.length))
                        .error(e);
            }
        } catch (CommandFailedException e) {
            LogManager.log()
                    .action("Create database user", databaseUser, false)
                    .error(e);
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
        LogManager.log().action("Regenerate password for database user", databaseUser).info();
    }

    private void setDbUserPassword() {
        if (databaseUser.equalsIgnoreCase(DatabaseProvisioner.ADMIN_USER)) {
            LogManager.log()
                    .warn("WARNING: Refusing to modify the root database user. Please configure a different database user");
            return;
        }
        try {
            String[] command = new String[]{ShellUtils.getSqlCliName(), "-u", DatabaseProvisioner.ADMIN_USER, "-e",
                    String.format("ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                            databaseUser, databasePassword)};
            try {
                ShellUtils.runCommand(command);

                LogManager.log().action("Set database user password", databaseUser, true).info();
            } catch (CommandFailedException e) {

                LogManager.log()
                        .command(command[0], Arrays.copyOfRange(command, 1, command.length))
                        .error(e);
            }
        } catch (CommandFailedException e) {
            LogManager.log()
                    .action("Create database user", databaseUser, false)
                    .error(e);
        }
    }
}