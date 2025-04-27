package org.example.commands.picocli;

import org.example.exceptions.CommandFailedException;
import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.value_types.LinuxUsername;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.sql.SQLException;
import java.util.Optional;

@Command(
        name = "get-login-link",
        description = "Get a Plesk login link for a subscription"
)
public class GetLoginLinkCliCommand extends AbstractCliCommand {
    @Parameters(index = "0", description = "The subscription ID")
    private int subscriptionId;

    @Parameters(index = "1", description = "The Linux username")
    private String username;

    public GetLoginLinkCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            Optional<String> loginLink = getPleskService()
                    .pleskGetSubscriptionLoginLinkBySubscriptionId(
                            subscriptionId, new LinuxUsername(username));

            if (loginLink.isPresent()) {
                return success(loginLink.get());
            } else {
                return error("No login link found for subscription ID: " + subscriptionId);
            }
        } catch (CommandFailedException | SQLException e) {
            return error("Error getting login link: " + e.getMessage());
        }
    }
}