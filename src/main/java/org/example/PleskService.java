package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.Commands.Plesk.PleskFetchSubscriptionInfoCommand;
import org.example.Commands.Plesk.PleskGetLoginLinkCommand;
import org.example.Commands.Plesk.PleskGetTestMailboxCommand;
import org.example.Exceptions.CommandFailedException;
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
        return new PleskGetLoginLinkCommand(subscriptionId, username).execute();
    }

    private String pleskGetUserLoginLink(String username) throws CommandFailedException {
        return ShellUtils.runCommand(PLESK_CLI_EXECUTABLE, "login", username).getFirst();
    }

    public Optional<List<String>> fetchSubscriptionInfo(DomainName domain) throws
            SQLException {
        return new PleskFetchSubscriptionInfoCommand(domain).execute();
    }

    public Optional<ObjectNode> getTestMailbox(DomainName testMailDomain) throws
            CommandFailedException {
        return new PleskGetTestMailboxCommand(testMailDomain).execute();
    }


}