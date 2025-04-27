package org.example.commands.plesk;

import org.example.commands.Command;
import org.example.utils.DbUtils;
import org.example.value_types.DomainName;

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
