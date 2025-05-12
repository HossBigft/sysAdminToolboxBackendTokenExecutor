package org.example.operations.plesk;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.utils.CommandFailedException;
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


public class PleskGetTestMailbox implements Operation {

    static final String TEST_MAIL_LOGIN = "testsupportmail";
    static final String
            TEST_MAIL_DESCRIPTION =
            "This is a throwaway mail for troubleshooting purposes. You may delete it will.";
    static final int TEST_MAIL_PASSWORD_LENGTH = 15;
    final DomainName testMailDomain;

    public PleskGetTestMailbox(DomainName testmailDomain) {
        this.testMailDomain = testmailDomain;
    }

    @Override
    public OperationResult execute() {
        ObjectMapper om = new ObjectMapper();
        ObjectNode mailCredentials = om.createObjectNode();
        URI loginLink = URI.create(
                "https://webmail." + testMailDomain + "/roundcube/index.php?_user=" +
                        URLEncoder.encode(TEST_MAIL_LOGIN + "@" + testMailDomain, StandardCharsets.UTF_8));

        boolean newEmailCreated = false;
        String password;

        Optional<String> existingPassword = getEmailPassword(TEST_MAIL_LOGIN, testMailDomain);

        if (existingPassword.isPresent()) {
            password = existingPassword.get();
        } else {
            password = Utils.generatePassword(TEST_MAIL_PASSWORD_LENGTH);
            try {
                createMail(TEST_MAIL_LOGIN, testMailDomain, password, TEST_MAIL_DESCRIPTION);
                newEmailCreated = true;
            } catch (CommandFailedException e) {
                getLogger().errorEntry().message("Failed to create test mail").field("Domain", testMailDomain)
                        .exception(e).log();
                return OperationResult.internalError("Could not create test mail account.");
            }
        }


        mailCredentials.put("email", TEST_MAIL_LOGIN + "@" + testMailDomain);
        mailCredentials.put("password", password);
        mailCredentials.put("login_link", loginLink.toString());
        mailCredentials.put("new_email_created", String.valueOf(newEmailCreated));

        if (mailCredentials.isEmpty()) {
            return OperationResult.notFound(String.format("Email domain %s not found.", testMailDomain));
        }

        return newEmailCreated
                ? OperationResult.successCreated("New test email created.", Optional.of(mailCredentials))
                : OperationResult.success("Test email credentials fetched.", Optional.of(mailCredentials));
    }

    private Optional<String> getEmailPassword(String login,
                                              DomainName mailDomain) {
        String emailPassword = "";
        List<String> result;
        try {
            result = ShellUtils.execute(PLESK_CLI_GET_MAIL_USERS_CREDENTIALS).stdout();
        } catch (CommandFailedException e) {
            return Optional.empty();
        }

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
        ShellUtils.execute(PLESK_CLI_EXECUTABLE,
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

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }
}
