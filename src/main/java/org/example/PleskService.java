package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PleskService {
    static final String TEST_MAIL_LOGIN = "testsupportmail";
    static final String
            TEST_MAIL_DESCRIPTION =
            "throwaway mail for troubleshooting purposes. You may delete it at will.";
    static final int TEST_MAIL_PASSWORD_LENGTH = 15;
    static final String PLESK_CLI_EXECUTABLE = "/usr/sbin/plesk";
    static final String PLESK_CLI_GET_MAIL_USERS_CREDENTIALS = "/usr/local/psa/admin/bin/mail_auth_view";
    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.(?!-)[A-Za-z0-9.-]{2,}$");


    Predicate<String> isDomain = DOMAIN_PATTERN.asMatchPredicate();

    public PleskService() {
    }

    public Optional<String> pleskGetSubscriptionLoginLinkBySubscriptionId(int subscriptionId,
                                                                          String username) throws
            ShellUtils.CommandFailedException {
        final String REDIRECTION_HEADER = "&success_redirect_url=%2Fadmin%2Fsubscription%2Foverview%2Fid%2F";

        Optional<List<String>>
                result =
                DbUtils.executeSqlQueryJDBC(DbUtils.prepareFetchSubscriptionNameById(subscriptionId));

        if (result.isPresent()) {
            String link = pleskGetUserLoginLink(username);
            return Optional.of(link + REDIRECTION_HEADER + subscriptionId);
        } else {
            throw new ShellUtils.CommandFailedException("Subscription with ID " + subscriptionId + " doesn't exist.");
        }
    }

    private String pleskGetUserLoginLink(String username) throws ShellUtils.CommandFailedException {
        return ShellUtils.runCommand(PLESK_CLI_EXECUTABLE, "login", username).getFirst();
    }

    public Optional<List<String>> plesk_fetch_subscription_info_by_domain(String domain) throws
            ShellUtils.CommandFailedException {
        Optional<List<String>> result = Optional.empty();
        if (isDomain.test(domain)) {
            try {
                result = DbUtils.executeSqlQueryJDBC(DbUtils.prepareFetchSubscriptionInfoSql(domain));
            } catch (SQLException e) {
                System.out.println("Subscription info fetch failed with " + e);
            }
        }
        return result;
    }

    public Optional<ObjectNode> plesk_get_testmail_credentials(String testMailDomain) throws
            ShellUtils.CommandFailedException {
        ObjectMapper om = new ObjectMapper();
        ObjectNode mailCredentials = om.createObjectNode();
        if (isDomain.test(testMailDomain)) {
            String password;
            URI login_link = URI.create(
                    "https://webmail." + testMailDomain + "/roundcube/index.php?_user=" + URLEncoder.encode(
                            TEST_MAIL_LOGIN + "@" + testMailDomain,
                            StandardCharsets.UTF_8));
            Optional<String> existing_password;
            existing_password = getEmailPassword(TEST_MAIL_LOGIN, testMailDomain);
            if (existing_password.isPresent()) {
                password = existing_password.get();
            } else {
                password = Utils.generatePassword(TEST_MAIL_PASSWORD_LENGTH);
                try {
                    createMail(TEST_MAIL_LOGIN, testMailDomain, password,
                            TEST_MAIL_DESCRIPTION);
                } catch (ShellUtils.CommandFailedException e) {
                    System.err.println(
                            "Email creation for " + testMailDomain + " failed with " + e);
                    throw new ShellUtils.CommandFailedException(
                            "Email creation for " + testMailDomain + " failed with " + e);
                }
            }
            mailCredentials.put("email", TEST_MAIL_LOGIN + "@" + testMailDomain);
            mailCredentials.put("password", password);
            mailCredentials.put("login_link", login_link.toString());


        }
        return mailCredentials.isEmpty() ? Optional.empty() : Optional.of(mailCredentials);
    }

    private Optional<String> getEmailPassword(String login,
                                              String mailDomain) throws ShellUtils.CommandFailedException {
        String emailPassword = "";
        if (isDomain.test(mailDomain)) {
            List<String> result = ShellUtils.runCommand(PLESK_CLI_GET_MAIL_USERS_CREDENTIALS);

            result = result.stream()
                    .filter(line -> line.contains(login + "@" + mailDomain))
                    .map(line -> line.replaceAll("\\s", ""))
                    .map(line -> {
                        int index = line.indexOf('|');
                        return index >= 0 ? line.split("\\|")[3] : "";
                    })
                    .toList();

            if (!result.isEmpty()) {
                emailPassword = result.get(0);
            }

        }
        return emailPassword.isEmpty() ? Optional.empty() : Optional.of(emailPassword);
    }

    private void createMail(String login,
                            String mailDomain,
                            String password,
                            String description) throws ShellUtils.CommandFailedException {
        if (isDomain.test(mailDomain)) {
            ShellUtils.runCommand(PLESK_CLI_EXECUTABLE,
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
        }

    }
}