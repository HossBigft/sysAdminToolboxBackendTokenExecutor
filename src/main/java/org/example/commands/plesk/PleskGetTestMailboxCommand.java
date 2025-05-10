package org.example.commands.plesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.commands.Command;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.utils.Utils;
import org.example.value_types.DomainName;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.example.constants.Executables.PLESK_CLI_EXECUTABLE;
import static org.example.constants.Executables.PLESK_CLI_GET_MAIL_USERS_CREDENTIALS;


public class PleskGetTestMailboxCommand implements Command<ObjectNode> {

    static final String TEST_MAIL_LOGIN = "testsupportmail";
    static final String
            TEST_MAIL_DESCRIPTION =
            "This is a throwaway mail for troubleshooting purposes. You may delete it will.";
    static final int TEST_MAIL_PASSWORD_LENGTH = 15;
    final DomainName testMailDomain;

    public PleskGetTestMailboxCommand(DomainName testmailDomain) {
        this.testMailDomain = testmailDomain;
    }

    @Override
    public Optional<ObjectNode> execute() throws CommandFailedException {
        ObjectMapper om = new ObjectMapper();
        ObjectNode mailCredentials = om.createObjectNode();
        String password;
        URI login_link = URI.create(
                "https://webmail." + testMailDomain + "/roundcube/index.php?_user=" + URLEncoder.encode(
                        TEST_MAIL_LOGIN + "@" + testMailDomain,
                        StandardCharsets.UTF_8));
        Optional<String> existing_password;
        existing_password = getEmailPassword(TEST_MAIL_LOGIN, testMailDomain);
        password = existing_password.orElseGet(() -> Utils.generatePassword(TEST_MAIL_PASSWORD_LENGTH));
        if (existing_password.isEmpty()) {
            try {
                createMail(TEST_MAIL_LOGIN, testMailDomain, password,
                        TEST_MAIL_DESCRIPTION);
                mailCredentials.put("new_email_created","true");
            } catch (CommandFailedException e) {
                System.err.println(
                        "Email creation for " + testMailDomain + " failed with " + e);
                throw new CommandFailedException(
                        "Email creation for " + testMailDomain + " failed with " + e);
            }
        }
        mailCredentials.put("email", TEST_MAIL_LOGIN + "@" + testMailDomain);
        mailCredentials.put("password", password);
        mailCredentials.put("login_link", login_link.toString());
        mailCredentials.put("new_email_created","false");
        return mailCredentials.isEmpty() ? Optional.empty() : Optional.of(mailCredentials);
    }

    private Optional<String> getEmailPassword(String login,
                                              DomainName mailDomain) throws
            CommandFailedException {
        String emailPassword = "";
        List<String> result = ShellUtils.runCommand(PLESK_CLI_GET_MAIL_USERS_CREDENTIALS);
        String email = login + "@" + mailDomain;
        result = result.stream()
                .filter(line -> line.contains(email))
                .map(line -> line.split("\\|"))
                .filter(parts -> parts.length >= 4)
                .map(parts -> parts[3].trim())
                .toList();

        if (!result.isEmpty()) {
            emailPassword = result.get(0);
        }

        return emailPassword.isEmpty() ? Optional.empty() : Optional.of(emailPassword);
    }

    private void createMail(String login,
                            DomainName mailDomain,
                            String password,
                            String description) throws CommandFailedException {
        String email = login + "@" + mailDomain;
        ShellUtils.runCommand(PLESK_CLI_EXECUTABLE,
                "bin",
                "mail",
                "--create",
                email,
                "-passwd",
                password,
                "-mailbox",
                "true",
                "-description",
                description);
    }
}
