package org.example.utils;

import org.example.config.AppConfigException;
import org.example.config.core.AppConfiguration;
import org.example.value_types.DomainName;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbUtils {
    private DbUtils() {
    }

    public static Optional<String> fetchSubscriptionNameById(int id) throws SQLException {
        return executeSqlQueryJDBC(prepareFetchSubscriptionNameById(id)).flatMap(list -> list.stream().findFirst());

    }


    private static Optional<List<String>> executeSqlQueryJDBC(Query query) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query.sql())) {

            for (int i = 0; i < query.params().length; i++) {
                stmt.setObject(i + 1, query.params()[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> results = new ArrayList<>();

                while (rs.next()) {
                    StringBuilder row = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) row.append("\t");
                        row.append(rs.getString(i));
                    }
                    results.add(row.toString());
                }

                return results.isEmpty() ? Optional.empty() : Optional.of(results);
            }

        }
    }

    private static Query prepareFetchSubscriptionNameById(int id) {
        String sql = "SELECT name FROM domains WHERE webspace_id=0 AND id=?";
        return new Query(sql, id);
    }

    private static Connection getConnection() {
        AppConfiguration config = AppConfiguration.getInstance();
        String dbUrl = "jdbc:mysql://127.0.0.1/psa";
        String dbUser = config.getDatabaseUser();
        String dbPassword = config.getDatabasePassword();

        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException firstEx) {
            config.getBootstrapper().ensureDatabaseSetup();
            try {
                return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            } catch (SQLException secondEx) {
                SQLException combined = new SQLException("Failed to connect to database after setup", secondEx);
                combined.addSuppressed(firstEx);
                throw new AppConfigException("Database connection failed", combined);
            }
        }
    }

    public static Optional<List<String>> fetchSubscriptionInfoByDomain(String domain) throws SQLException {
        return executeSqlQueryJDBC(prepareFetchSubscriptionInfoByDomain(domain));

    }

    private static Query prepareFetchSubscriptionInfoByDomain(String domain) {
        String sql = """
                SELECT
                    base.subscription_id AS result,
                    (SELECT name FROM domains WHERE id = base.subscription_id) AS name,
                    (SELECT pname FROM clients WHERE id = base.cl_id) AS username,
                    (SELECT login FROM clients WHERE id = base.cl_id) AS userlogin,
                    (SELECT GROUP_CONCAT(CONCAT(d2.name, ':', d2.status) SEPARATOR ',')
                        FROM domains d2
                        WHERE base.subscription_id IN (d2.id, d2.webspace_id)) AS domains,
                    (SELECT overuse FROM domains WHERE id = base.subscription_id) as is_space_overused,
                    (
                        SELECT ROUND(SUM(real_size)/1024/1024)
                        FROM domains d3
                        WHERE d3.webspace_id = base.subscription_id
                           OR d3.id = base.subscription_id
                    ) AS subscription_size_mb,
                    (SELECT status FROM domains WHERE id = base.subscription_id) as subscription_status
                FROM (
                    SELECT
                        CASE
                            WHEN webspace_id = 0 THEN id
                            ELSE webspace_id
                        END AS subscription_id,
                        cl_id,
                        name
                    FROM domains
                    WHERE name LIKE ?
                ) AS base;
                """;
        return new Query(sql, domain);
    }

    public static Optional<List<String>> fetchSubscriptionIdByDomain(DomainName domain) throws SQLException {
        return executeSqlQueryJDBC(prepareGetSubscriptionIdByDomain(domain.name()));
    }

    private static Query prepareGetSubscriptionIdByDomain(String domain) {
        String sql = """
                SELECT CASE WHEN webspace_id = 0 THEN id ELSE webspace_id END AS result FROM domains WHERE name LIKE ?
                """;
        return new Query(sql, domain);
    }

    public record Query(String sql, Object... params) {
    }
}