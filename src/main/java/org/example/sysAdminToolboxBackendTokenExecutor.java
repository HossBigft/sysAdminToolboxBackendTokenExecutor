package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Pattern;


@Command(name = "sysadmintoolbox", description = "Executes sudo commands on server", mixinStandardHelpOptions = true)
public class sysAdminToolboxBackendTokenExecutor implements Callable<Integer> {
    private static final String TEST_MAIL_LOGIN = "testsupportmail";
    private static final String TEST_MAIL_DESCRIPTION = "throwaway mail for troubleshooting purposes. You may delete it at will.";
    private static final int TEST_MAIL_PASSWORD_LENGTH = 15;
    private static final String PLESK_CLI_EXECUTABLE = "/usr/sbin/plesk";
    private static final String PLESK_CLI_GET_MAIL_USERS_CREDENTIALS = "/usr/local/psa/admin/bin/mail_auth_view";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$");


    Predicate<String> isDomain = DOMAIN_PATTERN.asMatchPredicate();
    @Parameters(index = "0", description = "The domain to check.")
    private String domain;


    public static void main(String[] args) {
        int exitCode = new CommandLine(new sysAdminToolboxBackendTokenExecutor()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (!isDomain.test(domain)) {
            System.err.println("Error: Invalid domain format.");
            return 1;
        }
        Optional<List<String>> mailCredentials;
        try {
            mailCredentials = plesk_fetch_subscription_info_by_domain(domain);
        } catch (CommandFailedException e) {
            System.out.println("Test mail creation failed with " + e);
            return 1;
        }
        mailCredentials.ifPresentOrElse(creds -> System.out.println(String.join("", creds)),
                () -> System.out.println("Email for " + domain + " was not found"));

        return 0;
    }

    private Optional<List<String>> plesk_fetch_subscription_info_by_domain(String domain) throws
            CommandFailedException {
        Optional<List<String>> result = Optional.empty();
        if (isDomain.test(domain)) {
            try {
                result = executeSqlQueryJDBC(prepareSubscriptionInfoStatement(domain));
            } catch (SQLException e) {
                System.out.println("Subscription info fetch failed with " + e);
            }
        }
        return result;
    }

    private Optional<List<String>> executeSqlQueryJDBC(PreparedStatement stmt) throws CommandFailedException {

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

    private PreparedStatement prepareSubscriptionInfoStatement(String domain) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement stmt = connection.prepareStatement(buildSqlQuery());
        stmt.setString(1, domain);
        return stmt;
    }

    private Connection getConnection() throws SQLException {
        String dbHost = "localhost";
        String dbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();

        String dbUrl = String.format("jdbc:mysql://%s/%s", dbHost, dbName);
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private String buildSqlQuery() {
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

    private String buildSqlQueryCLI(String domain) {
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

    private Optional<List<String>> executeSqlCommandCLI(String cmd) throws CommandFailedException {

        String dbHost = "localhost";
        String dbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();
        String mysqlCliName = getSqlCliName();


        ProcessBuilder pb = new ProcessBuilder(mysqlCliName, "--host", dbHost, "--user=" + dbUser,
                "--password=" + dbPassword, "--database", dbName, "--batch", "--skip-column-names", "--raw", "-e", cmd);

        try {
            Process process = pb.start();
            int exitStatus = process.waitFor();

            if (exitStatus != 0) {
                throw new CommandFailedException("SQL command execution failed with status " + exitStatus);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = reader.lines().toList();
                return lines.isEmpty() ? Optional.empty() : Optional.of(lines);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandFailedException("SQL command interrupted", e);
        } catch (IOException e) {
            throw new CommandFailedException("SQL cli executable " + mysqlCliName + " is not found.");
        }
    }

    private String getSqlCliName() throws CommandFailedException {
        if (isCommandAvailable("mariadb")) {
            return "mariadb";
        } else if (isCommandAvailable("mysql")) {
            return "mysql";
        } else {
            throw new CommandFailedException("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
        }
    }

    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }


    private Optional<ObjectNode> plesk_get_testmail_credentials(String testMailDomain) throws CommandFailedException {
        ObjectMapper om = new ObjectMapper();
        ObjectNode mailCredentials = om.createObjectNode();
        if (isDomain.test(testMailDomain)) {
            String password;
            URI login_link = URI.create("https://webmail." + domain + "/roundcube/index.php?_user=" + URLEncoder.encode(
                    TEST_MAIL_LOGIN + "@" + domain, StandardCharsets.UTF_8));
            Optional<String> existing_password = Optional.empty();
            try {
                existing_password = getEmailPassword(TEST_MAIL_LOGIN, testMailDomain);
            } catch (IOException e) {
                System.out.println(PLESK_CLI_GET_MAIL_USERS_CREDENTIALS + " is not found");
            }
            if (existing_password.isPresent()) {
                password = existing_password.get();
            } else {
                password = generatePassword(TEST_MAIL_PASSWORD_LENGTH);
                try {
                    createMail(TEST_MAIL_LOGIN, domain, password, TEST_MAIL_DESCRIPTION);
                } catch (CommandFailedException e) {
                    System.err.println("Email creation for " + domain + " failed with " + e);
                    throw new CommandFailedException("Email creation for " + domain + " failed with " + e);
                }
            }
            mailCredentials.put("email", TEST_MAIL_LOGIN + "@" + testMailDomain);
            mailCredentials.put("password", password);
            mailCredentials.put("login_link", login_link.toString());


        }
        return mailCredentials.isEmpty() ? Optional.empty() : Optional.of(mailCredentials);
    }

    private Optional<String> getEmailPassword(String login,
                                              String mailDomain) throws IOException {
        String emailPassword = "";
        if (isDomain.test(mailDomain)) {
            ProcessBuilder builder = new ProcessBuilder("/usr/local/psa/admin/bin/mail_auth_view");
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> result = reader.lines().filter(line -> line.contains(login + "@" + mailDomain))
                        .map(line -> line.replaceAll("\\s", "")) // remove all whitespace
                        .map(line -> {
                            int index = line.indexOf('|');
                            return index >= 0 ? line.split("\\|")[3] : "";
                        }).toList();

                if (!result.isEmpty()) {
                    emailPassword = result.get(0);
                }
            }

        }
        return emailPassword.isEmpty() ? Optional.empty() : Optional.of(emailPassword);
    }

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

    private void createMail(String login,
                            String mailDomain,
                            String password,
                            String description) throws CommandFailedException {
        if (isDomain.test(mailDomain)) {
            ProcessBuilder builder = new ProcessBuilder(PLESK_CLI_EXECUTABLE, "bin", "mail", "--create", login, "@",
                    mailDomain, "-passwd", password, "-mailbox", "true", "-description", description);
            try {
                Process process = builder.start();
                int exitCode = -1;
                try {
                    exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new CommandFailedException("Mail creation failed with exit code " + exitCode);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CommandFailedException("Mail creation interrupted with exit code " + exitCode, e);
                }
            } catch (IOException e) {
                throw new CommandFailedException(PLESK_CLI_EXECUTABLE + " is not found");
            }

        }

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
