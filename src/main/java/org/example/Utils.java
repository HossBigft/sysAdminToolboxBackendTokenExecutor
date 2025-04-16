package org.example;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Utils {
    static String generatePassword(int length) {

        PasswordGenerator generator = new PasswordGenerator();

        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase, 1);
        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase, 1);
        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, 1);

        CharacterData safeSpecials = new CharacterData() {
            public String getErrorCode() {
                return "SHELL_QUOTE_CHARS_PROHIBITED";
            }

            public String getCharacters() {
                return "!#$%&()*+,-./:;<=>?@[\\]^_{|}~";
            }
        };
        CharacterRule specialRule = new CharacterRule(safeSpecials, 1);

        List<CharacterRule> rules = Arrays.asList(lowerCaseRule, upperCaseRule, digitRule, specialRule);

        return generator.generatePassword(length, rules);
    }

    static List<String> runCommand(String... args) throws CommandFailedException {
        try {
            Process process = new ProcessBuilder(args).start();


            List<String> outputLines = new ArrayList<>();
            StringBuilder errorBuilder = new StringBuilder();

            try (
                    BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            ) {
                CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(() ->
                        stdOutput.lines().forEach(outputLines::add));

                CompletableFuture<Void> errorFuture = CompletableFuture.runAsync(() ->
                        stdError.lines().forEach(line -> errorBuilder.append(line).append("\n")));

                boolean completed = process.waitFor(30, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    throw new CommandFailedException("Command execution timed out: " + String.join(" ", args));
                }


                CompletableFuture.allOf(outputFuture, errorFuture).join();

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String errorOutput = errorBuilder.toString().trim();
                    throw new CommandFailedException(
                            String.format("Command failed with exit code %d: %s\nCommand: %s",
                                    exitCode, errorOutput, String.join(" ", args))
                    );
                }

                return Collections.unmodifiableList(outputLines);
            }
        } catch (IOException e) {
            throw new CommandFailedException("Failed to execute command: " + String.join(" ", args), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new CommandFailedException("Command execution was interrupted: " + String.join(" ", args), e);
        }
    }

    static String getSqlCliName() throws CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            throw new CommandFailedException("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    private static boolean isCommandAvailable(String command) {
        try {
            runCommand("which", command);
        } catch (CommandFailedException e) {
            return false;
        }
        return true;
    }

    static Optional<List<String>> executeSqlQueryJDBC(PreparedStatement stmt) throws CommandFailedException {

        try (ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> results = new ArrayList<>();

            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) row.append("\t");
                    row.append(rs.getString(i));
                }
                results.add(row.toString());
            }

            return results.isEmpty() ? Optional.empty() : Optional.of(results);
        } catch (SQLException e) {
            throw new CommandFailedException("SQL command execution failed: " + e.getMessage(), e);
        }
    }

    static PreparedStatement prepareSubscriptionInfoStatement(String domain) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement stmt = connection.prepareStatement(buildSqlQuery());
        stmt.setString(1, domain);
        return stmt;
    }

    private static Connection getConnection() throws SQLException {
        String dbHost = "localhost";
        String dbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();

        String dbUrl = String.format("jdbc:mysql://%s/%s", dbHost, dbName);
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static String buildSqlQuery() {
        return """
                SELECT
                    base.subscription_id AS result,
                    (SELECT name FROM domains WHERE id = base.subscription_id) AS name,
                    (SELECT pname FROM clients WHERE id = base.cl_id) AS username,
                    (SELECT login FROM clients WHERE id = base.cl_id) AS userlogin,
                    (SELECT GROUP_CONCAT(CONCAT(d2.name, ':', d2.status) SEPARATOR ',')
                        FROM domains d2
                        WHERE base.subscription_id IN (d2.id, d2.webspace_id)) AS domains,
                    (SELECT overuse FROM domains WHERE id = base.subscription_id) as is_space_overused,
                    (SELECT ROUND(real_size/1024/1024) FROM domains WHERE id = base.subscription_id) as subscription_size_mb,
                    (SELECT status FROM domains WHERE id = base.subscription_id) as subscription_status
                FROM (
                    SELECT
                        CASE
                            WHEN webspace_id = 0 THEN id
                            ELSE webspace_id
                        END AS subscription_id,
                        cl_id,
                        name
                    FROM domains
                    WHERE name LIKE ?
                ) AS base;
                """;
    }

    private static String buildSqlQueryCLI(String domain) {
        return """
                SELECT\s
                    base.subscription_id AS result,
                    (SELECT name FROM domains WHERE id = base.subscription_id) AS name,
                    (SELECT pname FROM clients WHERE id = base.cl_id) AS username,
                    (SELECT login FROM clients WHERE id = base.cl_id) AS userlogin,
                    (SELECT GROUP_CONCAT(CONCAT(d2.name, ':', d2.status) SEPARATOR ',')
                        FROM domains d2\s
                        WHERE base.subscription_id IN (d2.id, d2.webspace_id)) AS domains,
                    (SELECT overuse FROM domains WHERE id = base.subscription_id) as is_space_overused,
                    (SELECT ROUND(real_size/1024/1024) FROM domains WHERE id = base.subscription_id) as subscription_size_mb,
                    (SELECT status FROM domains WHERE id = base.subscription_id) as subscription_status
                FROM (
                    SELECT\s
                        CASE\s
                            WHEN webspace_id = 0 THEN id\s
                            ELSE webspace_id\s
                        END AS subscription_id,
                        cl_id,
                        name
                    FROM domains\s
                    WHERE name LIKE '%s'
                ) AS base;
                """.formatted(domain);
    }

    private static Optional<List<String>> executeSqlCommandCLI(String cmd) throws CommandFailedException {
        String dbHost = "localhost";
        String dbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();
        String mysqlCliName = getSqlCliName();

        List<String> lines = runCommand(mysqlCliName,
                "--host",
                dbHost,
                "--user=" + dbUser,
                "--password=" + dbPassword,
                "--database",
                dbName,
                "--batch",
                "--skip-column-names",
                "--raw",
                "-e",
                cmd);
        return lines.isEmpty() ? Optional.empty() : Optional.of(lines);

    }

    static class CommandFailedException extends Exception {
        public CommandFailedException(String message) {
            super(message);
        }

        public CommandFailedException(String message,
                                      Throwable cause) {
            super(message, cause);
        }
    }
}