package org.example.operations.plesk;

import org.example.operations.Operation;
import org.example.operations.OperationFailedException;
import org.example.utils.DbUtils;
import org.example.utils.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.LinuxUsername;

import java.sql.SQLException;
import java.util.Optional;

import static org.example.constants.Executables.PLESK_CLI_EXECUTABLE;

public class PleskGetLoginLink implements Operation<String> {
    final int subscriptionId;
    final LinuxUsername username;


    public PleskGetLoginLink(int subscriptionId,
                             LinuxUsername username) {
        this.subscriptionId = subscriptionId;
        this.username = username;
    }


    public Optional<String> execute() throws
            OperationFailedException, SQLException {
        final String REDIRECTION_HEADER = "&success_redirect_url=%2Fadmin%2Fsubscription%2Foverview%2Fid%2F";
        Optional<String> result;


        result = DbUtils.fetchSubscriptionNameById(subscriptionId);

        if (result.isPresent()) {
            String link;
            try {
                link = pleskGetUserLoginLink(username.value());
            } catch (CommandFailedException e) {
                throw new OperationFailedException(
                        "Operation get subscription login link for subscription with ID " + subscriptionId + " for user " + username + " failed.");
            }
            return Optional.of(link + REDIRECTION_HEADER + subscriptionId);
        } else {
            throw new OperationFailedException("Subscription with ID " + subscriptionId + " doesn't exist.");
        }
    }

    private String pleskGetUserLoginLink(String username) throws CommandFailedException {
        ShellUtils.ShellCommandResult result = ShellUtils.execute(PLESK_CLI_EXECUTABLE, "login", username);

        return result.stdout().getFirst();
    }

}
