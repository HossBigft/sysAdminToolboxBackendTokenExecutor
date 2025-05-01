package org.example.utils;

import org.example.config.core.AppConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbUtils {
    private DbUtils() {
    }

    public static Optional<String> fetchSubscriptionNameById(int id) throws SQLException {
        return executeSqlQueryJDBC(prepareFetchSubscriptionNameById(id))
                .flatMap(list -> list.stream().findFirst());

    }


    private static Optional<List<String>> executeSqlQueryJDBC(Query query) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.sql())) {

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

    private static Connection getConnection() throws SQLException {
        String pleskDbName = "psa";
        AppConfiguration cfmng = AppConfiguration.getInstance();
        String dbUser = cfmng.getDatabaseUser();
        String dbPassword = cfmng.getDatabasePassword();
        String dbUrl = String.format("jdbc:mysql://localhost/%s", pleskDbName);
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
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
                    (SELECT ROUND(real_size/1024/1024) FROM domains WHERE id = base.subscription_id) as subscription_size_mb,
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

    public record Query(String sql, Object... params) {
    }
}