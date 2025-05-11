package org.example.commands.plesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.commands.Operation;
import org.example.utils.DbUtils;
import org.example.value_types.DomainName;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PleskGetSubscriptionIdByDomain implements Operation<ObjectNode> {
    private final DomainName domain;

    public PleskGetSubscriptionIdByDomain(DomainName domain) {
        this.domain = domain;
    }

    public Optional<ObjectNode> execute() throws SQLException {
        Optional<List<String>> subscriptionIdList = DbUtils.fetchSubscriptionIdByDomain(domain);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode resultJson = mapper.createObjectNode();
        ArrayNode idArray = mapper.createArrayNode();

        subscriptionIdList.ifPresentOrElse(
                infoList -> infoList.stream()
                        .filter(info -> info != null && !info.trim().isEmpty())
                        .forEach(idArray::add),
                () -> System.out.println("No subscription found with domain: " + domain.name())
        );

        resultJson.set("id", idArray);
        return Optional.of(resultJson);
    }
}





