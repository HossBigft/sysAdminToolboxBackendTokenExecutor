package org.example.commands.picocli;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.example.SysAdminToolboxBackendTokenExecutor;
import org.example.value_types.DomainName;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Command(
        name = "get-subscription-info",
        description = "Fetch subscription information for a domain"
)
public class GetSubscriptionInfoCliCommand extends AbstractCliCommand {
    @Parameters(index = "0", description = "The domain to check")
    private String domain;


    public GetSubscriptionInfoCliCommand(SysAdminToolboxBackendTokenExecutor parent) {
        super(parent);
    }

    @Override
    public Integer call() {
        try {
            Optional<ArrayNode> info = getPleskService().fetchSubscriptionInfo(new DomainName(domain));

            if (info.isPresent()) {;
                return success(String.valueOf(info));
            } else {
                return error("No subscription information found for domain: " + domain);
            }
        } catch (SQLException e) {
            return error("Database error while fetching subscription info: " + e.getMessage());
        }
    }
}