package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;


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

    private static String generatePassword(int length) {

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

    private static class CommandFailedException extends Exception {
        public CommandFailedException(String message) {
            super(message);
        }

        public CommandFailedException(String message,
                                      Throwable cause) {
            super(message, cause);
        }
    }

    private static class Config {
        private static final String ENV_PATH = ".env.json";
        private static final String DOTENV_PERMISSIONS = "rw-------";
        private static final String DOTENV_OWNER = "root";
        private static final String DOTENV_GROUP = "root";
        private static final int DB_USER_PASSWORD_LENGTH = 15;
        private static Map<String, String> values = new HashMap<>();

        static {
            try {
                loadConfig();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config", e);
            }
        }

        private static void loadConfig() throws IOException {
            File envFile = new File(ENV_PATH);
            ObjectMapper mapper = new ObjectMapper();

            try {
                values = mapper.readValue(envFile, new TypeReference<Map<String, String>>() {
                });
            } catch (IOException e) {
                values = new HashMap<>();
            }

            boolean updated = false;
            updated |= computeIfAbsentOrBlank(values, "DATABASE_USER", Config::resolveUser);
            updated |= computeIfAbsentOrBlank(values, "DATABASE_PASSWORD",
                    () -> generatePassword(DB_USER_PASSWORD_LENGTH));
            if (updated) {
                updateDotEnv();
            }
            if (!isEnvPermissionsSecure(envFile)) {
                setEnvPermissionsOwner(envFile);
            }
            DatabaseSetup.ensureDatabaseSetup();

        }

        private static boolean computeIfAbsentOrBlank(Map<String, String> map,
                                                      String key,
                                                      Supplier<String> supplier) {
            String val = map.get(key);
            if (val == null || val.isBlank()) {
                map.put(key, supplier.get());
                return true;
            }
            return false;
        }

        public static String resolveUser() {
            return Stream.of(getSudoUser(), getSystemUser(), getUserFromPath()).flatMap(Optional::stream)
                    .filter(Config::isValidUser).findFirst().orElseThrow(
                            () -> new IllegalStateException("Could not determine valid user for running executable."));
        }

        private static void updateDotEnv() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            File envFile = new File(ENV_PATH);
            mapper.writeValue(envFile, values);
        }

        private static boolean isEnvPermissionsSecure(File envFile) throws IOException {
            Path envPath = envFile.toPath();
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(envPath);

            String permString = PosixFilePermissions.toString(permissions);
            boolean permsOk = DOTENV_PERMISSIONS.equals(permString);

            UserPrincipal owner = Files.getOwner(envPath);
            boolean ownerOk = DOTENV_OWNER.equals(owner.getName());

            PosixFileAttributes attrs = Files.readAttributes(envPath, PosixFileAttributes.class);
            boolean groupOk = DOTENV_GROUP.equals(attrs.group().getName());

            return permsOk && ownerOk && groupOk;

        }

        private static void setEnvPermissionsOwner(File envFile) throws IOException {
            Path filePath = envFile.toPath();
            Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString(DOTENV_PERMISSIONS));
            UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
            UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(DOTENV_OWNER);
            GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(DOTENV_GROUP);

            Files.setAttribute(filePath, "posix:owner", userPrincipal, LinkOption.NOFOLLOW_LINKS);
            Files.setAttribute(filePath, "posix:group", groupPrincipal, LinkOption.NOFOLLOW_LINKS);
        }

        private static Optional<String> getSudoUser() {
            return Optional.ofNullable(System.getenv("SUDO_USER"));
        }

        private static Optional<String> getSystemUser() {
            String systemUser = System.getProperty("user.name");
            return Optional.ofNullable(systemUser);
        }

        private static Optional<String> getUserFromPath() {
            String cwd = System.getProperty("user.dir");
            if (cwd == null) return Optional.empty();

            Path path = Paths.get(cwd).toAbsolutePath();
            for (int i = 0; i < path.getNameCount() - 1; i++) {
                if ("home".equals(path.getName(i).toString())) {
                    String username = path.getName(i + 1).toString();
                    return Optional.of(username);
                }
            }
            return Optional.empty();
        }

        private static boolean isValidUser(String user) {
            return !"root".equals(user);
        }

        static String getDatabaseUser() {
            return values.get("DATABASE_USER");
        }

        static String getDatabasePassword() {
            return values.get("DATABASE_PASSWORD");
        }

        private static class DatabaseSetup {
            static final String DB_URL = "jdbc:mysql://localhost:3306";
            static final String ADMIN_USER = "root";

            private static void ensureDatabaseSetup() {
                try {
                    ensureDatabaseUser();
                    ensureUserIsReadOnly();
                } catch (Exception e) {
                    System.err.println("Database setup failed: " + e.getMessage());
                }
            }

            private static void ensureUserIsReadOnly() {
                if (!isDbUserReadOnly()) {
                    setReadOnly();
                } else {
                    System.out.printf("User %s is already read-only.%n", getDatabaseUser());
                }
            }

            private static boolean isDbUserReadOnly() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "--skip-column-names",
                            "-e", String.format("SHOW GRANTS FOR '%s'@'localhost'", getDatabaseUser()));

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

            private static void ensureDatabaseUser() throws IOException {
                if (!isDbUserExists()) {
                    createUser();
                    System.out.printf("Created database user %s.%n", getDatabaseUser());
                } else {
                    System.out.printf("Database user %s already exists.%n", getDatabaseUser());
                    if (!isDbUserAbleToConnect()) {
                        regenerateDbUserPassword();
                        setDbUserPassword();
                        updateDotEnv();
                        System.out.printf("Updated password for user %s.%n", getDatabaseUser());
                    }
                }
            }

            private static void regenerateDbUserPassword() {
                values.put("DATABASE_PASSWORD", generatePassword(DB_USER_PASSWORD_LENGTH));
            }

            private static void setDbUserPassword() {
                if (getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
                    System.err.println(
                            "WARNING: Refusing to modify the root user. Please configure a different database user.");
                    return;
                }
                try {
                    ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e",
                            String.format("ALTER USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                                    getDatabaseUser(), getDatabasePassword()));

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

            private static boolean isDbUserAbleToConnect() {
                try (Connection conn = DriverManager.getConnection(DB_URL, getDatabaseUser(), getDatabasePassword())) {
                    return true;
                } catch (SQLException e) {
                    System.err.println("User unable to connect: " + e.getMessage());
                    return false;
                }
            }

            private static boolean isDbUserExists() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "--skip-column-names",
                            "-e",
                            String.format("SELECT EXISTS(SELECT 1 FROM mysql.user WHERE user = '%s') AS user_exists;",
                                    getDatabaseUser()));

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

            private static void createUser() {
                if (getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
                    System.err.println(
                            "WARNING: Refusing to modify the root user. Please configure a different database user.");
                    return;
                }
                try {
                    ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e",
                            String.format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'; FLUSH PRIVILEGES;",
                                    getDatabaseUser(), getDatabasePassword()));

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

            private static void setReadOnly() {
                if (getDatabaseUser().equalsIgnoreCase(ADMIN_USER)) {
                    System.err.println(
                            "WARNING: Refusing to modify the root user. Please configure a different database user.");
                    return;
                }
                try {
                    String commands = String.format(
                            "REVOKE ALL PRIVILEGES, GRANT OPTION FROM '%s'@'localhost'; " + "GRANT SELECT ON *.* TO '%s'@'localhost'; " + "FLUSH PRIVILEGES;",
                            getDatabaseUser(), getDatabaseUser());

                    ProcessBuilder pb = new ProcessBuilder(getSqlCliName(), "-u", ADMIN_USER, "-e", commands);

                    Process process = pb.start();
                    String error = readProcessOutput(process.getErrorStream());
                    int exitCode = process.waitFor();

                    if (exitCode != 0) {
                        System.err.println("Failed to set user as read-only: " + error);
                    } else {
                        System.out.printf("Set user %s to read-only.%n", getDatabaseUser());
                    }
                } catch (Exception e) {
                    System.err.println("Error setting user as read-only: " + e.getMessage());
                }
            }

            private static String readProcessOutput(InputStream inputStream) throws IOException {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    return output.toString();
                }
            }


            private static String getSqlCliName() throws CommandFailedException {
                if (isCommandAvailable("mariadb")) {
                    return "mariadb";
                } else if (isCommandAvailable("mysql")) {
                    return "mysql";
                } else {
                    System.err.println("Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
                    throw new CommandFailedException(
                            "Neither 'mariadb' nor 'mysql' is installed or available in PATH.");
                }
            }


            private static boolean isCommandAvailable(String command) {
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
    }
}
