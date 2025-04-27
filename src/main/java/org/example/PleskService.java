package org.example;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.commands.plesk.PleskFetchSubscriptionInfoCommand;
import org.example.commands.plesk.PleskGetLoginLinkCommand;
import org.example.commands.plesk.PleskGetTestMailboxCommand;
import org.example.exceptions.CommandFailedException;
import org.example.utils.ShellUtils;
import org.example.value_types.DomainName;
import org.example.value_types.LinuxUsername;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.example.constants.PleskConstants.PLESK_CLI_EXECUTABLE;

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