package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Commands.Plesk.PleskFetchSubscriptionInfoByDomainCommand;
import org.example.Commands.Plesk.PleskLoginLinkCommand;
import org.example.Exceptions.CommandFailedException;
import org.example.Utils.DbUtils;
import org.example.Utils.ShellUtils;
import org.example.ValueTypes.DomainName;
import org.example.ValueTypes.LinuxUsername;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.example.Constants.PleskConstants.PLESK_CLI_EXECUTABLE;

public class PleskService {


    public PleskService() {
    }


    public Optional<String> pleskGetSubscriptionLoginLinkBySubscriptionId(int subscriptionId,
                                                                          LinuxUsername username) throws
            CommandFailedException, SQLException {
        return new PleskLoginLinkCommand(subscriptionId, username).execute();
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


}