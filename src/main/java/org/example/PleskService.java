package org.example;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.commands.plesk.PleskFetchSubscriptionInfoCommand;
import org.example.commands.plesk.PleskGetLoginLinkCommand;
import org.example.commands.plesk.PleskGetTestMailboxCommand;
import org.example.exceptions.CommandFailedException;
import org.example.value_types.DomainName;
import org.example.value_types.LinuxUsername;

import java.sql.SQLException;
import java.util.Optional;

public class PleskService {


    public PleskService() {
    }


    public Optional<String> pleskGetSubscriptionLoginLinkBySubscriptionId(int subscriptionId,
                                                                          LinuxUsername username) throws
            CommandFailedException, SQLException {
        return new PleskGetLoginLinkCommand(subscriptionId, username).execute();
    }


    public Optional<ArrayNode> fetchSubscriptionInfo(DomainName domain) throws
            SQLException {
        return new PleskFetchSubscriptionInfoCommand(domain).execute();
    }

    public Optional<ObjectNode> getTestMailbox(DomainName testMailDomain) throws
            CommandFailedException {
        return new PleskGetTestMailboxCommand(testMailDomain).execute();
    }


}