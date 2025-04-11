package org.example;

import org.json.JSONObject;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.regex.Pattern;


@Command(name = "sysadmintoolbox",
        description = "Executes sudo commands on server",
        mixinStandardHelpOptions = true
)
public class sysAdminToolboxBackendTokenExecutor implements Callable<Integer> {
    private static final String TEST_MAIL_LOGIN = "testsupportmail";
    private static final String TEST_MAIL_DESCRIPTION =
            "throwaway mail for troubleshooting purposes. You may delete it at will.";
    private static final int TEST_MAIL_PASSWORD_LENGTH = 15;
    private static final String PLESK_CLI_EXECUTABLE = "/usr/sbin/plesk";
    private static final String PLESK_CLI_GET_MAIL_USERS_CREDENTIALS = "/usr/local/psa/admin/bin/mail_auth_view";
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$");

    Predicate<String> isDomain = DOMAIN_PATTERN.asMatchPredicate();
    @Parameters(index = "0", description = "The domain to check.")
    private String domain;

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

        List<CharacterRule> rules = Arrays.asList(
                lowerCaseRule,
                upperCaseRule,
                digitRule,
                specialRule
        );

        return generator.generatePassword(length, rules);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new sysAdminToolboxBackendTokenExecutor()).execute(args);
        System.exit(exitCode);
    }

    private Optional<String> getEmailPassword(String login,
                                              String mailDomain) throws IOException {
        String emailPassword = "";
        if (isDomain.test(mailDomain)) {
            ProcessBuilder builder = new ProcessBuilder("/usr/local/psa/admin/bin/mail_auth_view");
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> result = reader.lines()
                        .filter(line -> line.contains(login + "@" + mailDomain))
                        .map(line -> line.replaceAll("\\s",
                                "")) // remove all whitespace
                        .map(line -> {
                            int index = line.indexOf('|');
                            return index >= 0 ? line.split("\\|")[3] : "";
                        })
                        .toList();

                if (!result.isEmpty()) {
                    emailPassword = result.get(0);
                }
            }

        }
        return emailPassword.isEmpty() ? Optional.empty() : Optional.of(emailPassword);
    }

    private void createMail(String login,
                            String mailDomain,
                            String password,
                            String description) throws CommandFailedException {
        if (isDomain.test(mailDomain)) {
            ProcessBuilder builder = new ProcessBuilder(PLESK_CLI_EXECUTABLE,
                    "bin",
                    "mail",
                    "--create",
                    login,
                    "@",
                    mailDomain,
                    "-passwd",
                    password,
                    "-mailbox",
                    "true",
                    "-description",
                    description);
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

    private Optional<JSONObject> plesk_get_testmail_credentials(String testMailDomain) throws CommandFailedException {
        JSONObject mailCredentials = new JSONObject();
        if (isDomain.test(testMailDomain)) {
            String password;
            URI login_link = URI.create("https://webmail." + domain + "/roundcube/index.php?_user=" +
                    URLEncoder.encode(TEST_MAIL_LOGIN + "@" + domain, StandardCharsets.UTF_8));
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
                    createMail(TEST_MAIL_LOGIN, domain, password,
                            TEST_MAIL_DESCRIPTION);
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

    @Override
    public Integer call() throws CommandFailedException {
        if (!isDomain.test(domain)) {
            System.err.println("Error: Invalid domain format.");
            return 1;
        }
        Optional<JSONObject> mailCredentials;
        try {
            mailCredentials = plesk_get_testmail_credentials(domain);
        } catch (CommandFailedException e) {
            throw new CommandFailedException("Test mail creation failed with " + e);
        }
        mailCredentials.ifPresentOrElse(System.out::println, () -> System.out.println
                ("Email for " + domain + " was not found"));

        return 0;
    }

    private static class CommandFailedException extends Exception {
        public CommandFailedException(String message) {
            super(message);
        }

        public CommandFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
