package org.example.commands.plesk;

import org.example.exceptions.CommandFailedException;
import org.example.commands.Command;
import org.example.utils.DbUtils;
import org.example.utils.ShellUtils;
import org.example.value_types.LinuxUsername;

import java.sql.SQLException;
import java.util.Optional;

import static org.example.constants.Executables.PLESK_CLI_EXECUTABLE;

public class PleskGetLoginLinkCommand implements Command<String> {
    final int subscriptionId;
    final LinuxUsername username;


    public PleskGetLoginLinkCommand(int subscriptionId,
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
