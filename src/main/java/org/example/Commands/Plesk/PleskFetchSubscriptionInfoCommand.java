package org.example.Commands.Plesk;

import org.example.Interfaces.Command;
import org.example.Utils.DbUtils;
import org.example.ValueTypes.DomainName;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PleskFetchSubscriptionInfoCommand implements Command<Optional<List<String>>> {
    final DomainName domain;

    public PleskFetchSubscriptionInfoCommand(DomainName domain) {
        this.domain = domain;
    }

    public Optional<List<String>> execute() throws SQLException {
        return DbUtils.fetchSubscriptionInfoByDomain(domain.name());
    }
}
