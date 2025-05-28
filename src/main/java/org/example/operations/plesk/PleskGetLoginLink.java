package org.example.operations.plesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.utils.CommandFailedException;
import org.example.utils.DbUtils;
import org.example.utils.ShellUtils;
import org.example.value_types.LinuxUsername;

import java.sql.SQLException;
import java.util.Optional;

import static org.example.config.constants.Executables.PLESK_CLI_EXECUTABLE;

public class PleskGetLoginLink implements Operation {
    final int subscriptionId;
    final LinuxUsername username;


    public PleskGetLoginLink(int subscriptionId,
                             LinuxUsername username) {
        this.subscriptionId = subscriptionId;
        this.username = username;
    }


    public OperationResult execute() {
        ObjectMapper om = new ObjectMapper();
        ObjectNode jsonArr = om.createObjectNode();

        final String REDIRECTION_HEADER = "&success_redirect_url=%2Fadmin%2Fsubscription%2Foverview%2Fid%2F";
        Optional<String> subscriptionName;

        try {
            subscriptionName = DbUtils.fetchSubscriptionNameById(subscriptionId);
        } catch (SQLException e) {
            return OperationResult.internalError("Failed to fetch subscription name by ID " + subscriptionId);
        }


        if (subscriptionName.isPresent()) {
            String link;
            try {
                link = pleskGetUserLoginLink(username.value());
            } catch (CommandFailedException e) {
                return OperationResult.internalError(
                        "Operation get subscription login link for subscription with ID " + subscriptionId + " for user " + username + " failed.");
            }
            jsonArr.put("subscription_name", subscriptionName.get());
            jsonArr.put("login_link", link + REDIRECTION_HEADER + subscriptionId);

            return OperationResult.success("Subscription login link generated.", Optional.of(jsonArr));
        } else {
            return OperationResult.notFound("Subscription with ID " + subscriptionId + " doesn't exist.");
        }
    }

    private String pleskGetUserLoginLink(String username) throws CommandFailedException {
        ShellUtils.ExecutionResult result = ShellUtils.execute(PLESK_CLI_EXECUTABLE, "login", username);
        return result.stdout().getFirst();
    }

}
