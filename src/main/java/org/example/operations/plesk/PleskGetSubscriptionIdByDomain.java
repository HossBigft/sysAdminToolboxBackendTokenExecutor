package org.example.operations.plesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.logging.core.CliLogger;
import org.example.logging.facade.LogManager;
import org.example.operations.Operation;
import org.example.operations.OperationResult;
import org.example.utils.DbUtils;
import org.example.value_types.DomainName;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PleskGetSubscriptionIdByDomain implements Operation {
    private final DomainName domain;

    public PleskGetSubscriptionIdByDomain(DomainName domain) {
        this.domain = domain;
    }

    public OperationResult execute() {
        Optional<List<String>> subscriptionIdList;
        try {
            subscriptionIdList = DbUtils.fetchSubscriptionIdByDomain(domain);
        } catch (SQLException e) {
            getLogger().errorEntry().message("Failed to fetch subscription ID by domain.").field("Domain", domain)
                    .log();
            return OperationResult.internalError();
        }
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
        return OperationResult.success(Optional.of(resultJson));
    }

    private static CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }
}





