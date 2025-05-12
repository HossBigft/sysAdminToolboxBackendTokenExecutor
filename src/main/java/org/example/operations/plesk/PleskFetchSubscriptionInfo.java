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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PleskFetchSubscriptionInfo implements Operation {
    final DomainName domain;

    public PleskFetchSubscriptionInfo(DomainName domain) {
        this.domain = domain;
    }

    public OperationResult execute() {
        return runSafely(() -> {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode resultArray = objectMapper.createArrayNode();

            Optional<List<String>> subscriptionInfoRaw = DbUtils.fetchSubscriptionInfoByDomain(domain.name());

            if (subscriptionInfoRaw.isEmpty()) {
                return OperationResult.notFound(
                        "No subscription information found for domain: " + domain.name()
                );
            }

            List<String> infoList = subscriptionInfoRaw.get();
            processSubscriptionInfo(infoList, resultArray, objectMapper);

            return OperationResult.success(Optional.of(resultArray));
        });
    }

    private OperationResult runSafely(ThrowingSupplier<OperationResult> operation) {
        try {
            return operation.get();
        } catch (SQLException e) {
            getLogger().error("Database error while fetching subscription info: {}", e);
            return OperationResult.internalError(
                    "Database error while fetching subscription information: " + e
            );
        } catch (Exception e) {
            getLogger().errorEntry().message("Unexpected error: {}").exception(e).log();
            return OperationResult.internalError(
                    "Unexpected error while fetching subscription information: " + e
            );
        }
    }

    private void processSubscriptionInfo(List<String> infoList,
                                         ArrayNode resultArray,
                                         ObjectMapper objectMapper) {
        infoList.stream()
                .filter(info -> info != null && !info.trim().isEmpty())
                .forEach(info -> processSubscriptionEntry(info, resultArray, objectMapper));
    }

    private CliLogger getLogger() {
        return LogManager.getInstance().getLogger();
    }

    private void processSubscriptionEntry(String info,
                                          ArrayNode resultArray,
                                          ObjectMapper objectMapper) {
        try {
            SubscriptionDetails details = SubscriptionDetails.parse(info);
            resultArray.add(details.toJsonNode(objectMapper));
        } catch (Exception e) {
            getLogger().error("Error processing subscription info: {}", e);
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public record SubscriptionDetails(String id, String name, String username, String userlogin,
                                      List<DomainState> domainStates, boolean isSpaceOverused, int subscriptionSizeMb,
                                      String subscriptionStatus) {
        public static SubscriptionDetails parse(String subscriptionInfo) {
            String[] resultLines = subscriptionInfo.split("\t");

            String id = resultLines[0];
            String name = resultLines[1];
            String username = resultLines[2];
            String userlogin = resultLines[3];

            List<DomainState> domainStates = parseDomainStates(resultLines[4]);
            boolean isSpaceOverused = resultLines[5].equalsIgnoreCase("true");
            int subscriptionSizeMb = Integer.parseInt(resultLines[6]);

            int statusCode = Integer.parseInt(resultLines[7]);
            String subscriptionStatus = DomainStatus.getStatusString(statusCode);


            return new SubscriptionDetails(id, name, username, userlogin, domainStates, isSpaceOverused,
                    subscriptionSizeMb, subscriptionStatus);
        }

        private static List<DomainState> parseDomainStates(String domainStatesStr) {
            List<DomainState> domainStates = new ArrayList<>();
            if (domainStatesStr == null || domainStatesStr.trim().isEmpty()) {
                return domainStates;
            }

            for (String domainStatus : domainStatesStr.split(",")) {
                try {
                    String[] parts = domainStatus.split(":");
                    if (parts.length == 2) {
                        String domain = parts[0];
                        int statusCode = Integer.parseInt(parts[1]);
                        String status = DomainStatus.getStatusString(statusCode);
                        domainStates.add(new DomainState(domain, status));
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing domain state: " + domainStatus + " - " + e);
                }
            }

            return domainStates;
        }

        public ObjectNode toJsonNode(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", this.id);
            node.put("name", this.name);
            node.put("username", this.username);
            node.put("userlogin", this.userlogin);

            ArrayNode domainStatesArray = node.putArray("domain_states");
            ArrayNode domainsArray = node.putArray("domains");
            for (DomainState domainState : this.domainStates) {
                ObjectNode stateNode = mapper.createObjectNode();
                stateNode.put("domain", domainState.domain());
                stateNode.put("status", domainState.status());
                domainStatesArray.add(stateNode);
                
                domainsArray.add(domainState.domain());
            }

            node.put("is_space_overused", this.isSpaceOverused);
            node.put("subscription_size_mb", this.subscriptionSizeMb);
            node.put("subscription_status", this.subscriptionStatus);

            return node;
        }

        private enum DomainStatus {
            ONLINE(0, "online"), SUBSCRIPTION_DISABLED(2, "subscription_is_disabled"), DISABLED_BY_ADMIN(16,
                    "domain_disabled_by_admin"), DISABLED_BY_CLIENT(64, "domain_disabled_by_client");

            private final int code;
            private final String status;

            DomainStatus(int code,
                         String status) {
                this.code = code;
                this.status = status;
            }

            public static String getStatusString(int statusCode) {
                for (DomainStatus status : DomainStatus.values()) {
                    if (status.getCode() == statusCode) {
                        return status.getStatus();
                    }
                }
                return "unknown_status";
            }

            public int getCode() {
                return code;
            }

            public String getStatus() {
                return status;
            }
        }
    }

    public record DomainState(String domain, String status) {
    }
}