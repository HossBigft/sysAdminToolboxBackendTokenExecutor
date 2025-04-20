package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Commands.Plesk.PleskFetchSubscriptionInfoByDomainCommand;
import org.example.Exceptions.CommandFailedException;
import org.example.Utils.DbUtils;
import org.example.Utils.ShellUtils;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.example.Constants.PleskConstants.PLESK_CLI_EXECUTABLE;
import static org.example.Constants.PleskConstants.PLESK_CLI_GET_MAIL_USERS_CREDENTIALS;

public class PleskService {



    public PleskService() {
    }

    public Optional<String> pleskGetSubscriptionLoginLinkBySubscriptionId(int subscriptionId,
                                                                          LinuxUsername username) throws
            CommandFailedException, SQLException {
        final String REDIRECTION_HEADER = "&success_redirect_url=%2Fadmin%2Fsubscription%2Foverview%2Fid%2F";
        Optional<String> result;


        result = DbUtils.fetchSubscriptionNameById(subscriptionId);

        if (result.isPresent()) {
            String link = pleskGetUserLoginLink(username.value());
            return Optional.of(link + REDIRECTION_HEADER + subscriptionId);
        } else {
            throw new CommandFailedException("Subscription with ID " + subscriptionId + " doesn't exist.");
        }
    }

    private String pleskGetUserLoginLink(String username) throws CommandFailedException {
        return ShellUtils.runCommand(PLESK_CLI_EXECUTABLE, "login", username).getFirst();
    }

    public Optional<List<String>> plesk_fetch_subscription_info_by_domain(DomainName domain) throws
            SQLException {
        Optional<List<String>> result;
        result = DbUtils.fetchSubscriptionInfoByDomain(domain.name());
        return result;
    }

    public Optional<ObjectNode> plesk_get_testmail_credentials(DomainName testMailDomain) throws
            CommandFailedException {
        return new PleskFetchSubscriptionInfoByDomainCommand(testMailDomain).execute();
    }

    private Optional<String> getEmailPassword(String login,
                                              DomainName mailDomain) throws
            CommandFailedException {
        String emailPassword = "";
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

        return emailPassword.isEmpty() ? Optional.empty() : Optional.of(emailPassword);
    }



}