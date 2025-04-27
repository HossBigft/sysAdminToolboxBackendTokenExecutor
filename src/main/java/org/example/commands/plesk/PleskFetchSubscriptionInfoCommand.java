package org.example.commands.plesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.commands.Command;
import org.example.utils.DbUtils;
import org.example.value_types.DomainName;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PleskFetchSubscriptionInfoCommand implements Command<Optional<ArrayNode>> {
    final DomainName domain;

    public PleskFetchSubscriptionInfoCommand(DomainName domain) {
        this.domain = domain;
    }

    public Optional<ArrayNode> execute() throws SQLException {
        Optional<List<String>> subscriptionInfoList = DbUtils.fetchSubscriptionInfoByDomain(domain.name());
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode resultArray = objectMapper.createArrayNode();

        if (subscriptionInfoList.isPresent() && !subscriptionInfoList.get().isEmpty()) {
            for (String info : subscriptionInfoList.get()) {
                if (info != null && !info.trim().isEmpty()) {
                    try {
                        SubscriptionDetails details = SubscriptionDetails.parse(info);

                        ObjectNode subscriptionNode = objectMapper.createObjectNode();
                        subscriptionNode.put("host", details.host());
                        subscriptionNode.put("id", details.id());
                        subscriptionNode.put("name", details.name());
                        subscriptionNode.put("username", details.username());
                        subscriptionNode.put("userlogin", details.userlogin());


                        ArrayNode domainsArray = subscriptionNode.putArray("domains");
                        for (String domain : details.domains()) {
                            domainsArray.add(domain);
                        }

                        subscriptionNode.put("is_space_overused", details.isSpaceOverused());
                        subscriptionNode.put("subscription_size_mb", details.subscriptionSizeMb());
                        subscriptionNode.put("subscription_status", details.subscriptionStatus());

                        resultArray.add(subscriptionNode);
                    } catch (Exception e) {
                        System.err.println("Error processing subscription info: " + e.getMessage());
                    }
                }
            }
        } else {
            System.out.println("No subscription information found for domain: " + domain.name());
        }

        try {
            String jsonString = objectMapper.writeValueAsString(resultArray);
        } catch (Exception e) {
            System.err.println("Error serializing to JSON: " + e.getMessage());
        }

        return Optional.of(resultArray);
    }


    private record SubscriptionDetails(
            String host,
            String id,
            String name,
            String username,
            String userlogin,
            List<String> domains,
            boolean isSpaceOverused,
            int subscriptionSizeMb,
            String subscriptionStatus
    ) {
        public static SubscriptionDetails parse(String subscriptionInfo) {
            String[] resultLines = subscriptionInfo.split("\t");

            List<String> domains = List.of(resultLines[4].split(","));
            boolean isSpaceOverused = resultLines[5].equalsIgnoreCase("true");
            int subscriptionSizeMb = Integer.parseInt(resultLines[6]);

            int statusCode = Integer.parseInt(resultLines[7]);
            String subscriptionStatus = DomainStatus.getStatusString(statusCode);


            return new SubscriptionDetails(
                    resultLines[0],  // host
                    resultLines[1],  // id
                    resultLines[2],  // name
                    resultLines[3],  // username
                    resultLines[4],  // userlogin
                    domains,
                    isSpaceOverused,
                    subscriptionSizeMb,
                    subscriptionStatus
            );
        }

        private enum DomainStatus {
            ONLINE(0, "online"),
            SUBSCRIPTION_DISABLED(2, "subscription_is_disabled"),
            DISABLED_BY_ADMIN(16, "domain_disabled_by_admin"),
            DISABLED_BY_CLIENT(64, "domain_disabled_by_client");

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
}