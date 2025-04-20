package org.example.Commands.Plesk;

import org.example.Exceptions.CommandFailedException;
import org.example.Interfaces.Command;
import org.example.Utils.DbUtils;
import org.example.Utils.ShellUtils;
import org.example.ValueTypes.LinuxUsername;

import java.sql.SQLException;
import java.util.Optional;

import static org.example.Constants.PleskConstants.PLESK_CLI_EXECUTABLE;

public class PleskGetSubscriptionLoginLinkBySubscriptionIdCommand implements Command<Optional<String>> {
    final int subscriptionId;
    final LinuxUsername username;


    public PleskGetSubscriptionLoginLinkBySubscriptionIdCommand(int subscriptionId,
                                                                LinuxUsername username) {
        this.subscriptionId = subscriptionId;
        this.username = username;
    }

    public Optional<String> execute() throws
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

}
