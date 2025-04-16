package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbUtils {
    static Query prepareFetchSubscriptionInfoSql(String domain) throws SQLException {
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

    static String buildSqlQueryCLI(String domain) {
        return """
                SELECT\s
                    base.subscription_id AS result,
                    (SELECT name FROM domains WHERE id = base.subscription_id) AS name,
                    (SELECT pname FROM clients WHERE id = base.cl_id) AS username,
                    (SELECT login FROM clients WHERE id = base.cl_id) AS userlogin,
                    (SELECT GROUP_CONCAT(CONCAT(d2.name, ':', d2.status) SEPARATOR ',')
                        FROM domains d2\s
                        WHERE base.subscription_id IN (d2.id, d2.webspace_id)) AS domains,
                    (SELECT overuse FROM domains WHERE id = base.subscription_id) as is_space_overused,
                    (SELECT ROUND(real_size/1024/1024) FROM domains WHERE id = base.subscription_id) as subscription_size_mb,
                    (SELECT status FROM domains WHERE id = base.subscription_id) as subscription_status
                FROM (
                    SELECT\s
                        CASE\s
                            WHEN webspace_id = 0 THEN id\s
                            ELSE webspace_id\s
                        END AS subscription_id,
                        cl_id,
                        name
                    FROM domains\s
                    WHERE name LIKE '%s'
                ) AS base;
                """.formatted(domain);
    }

    static Optional<List<String>> executeSqlCommandCLI(String cmd) throws ShellUtils.CommandFailedException {
        String dbHost = "localhost";
        String dbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();
        String mysqlCliName = ShellUtils.getSqlCliName();

        List<String> lines = ShellUtils.runCommand(mysqlCliName,
                "--host",
                dbHost,
                "--user=" + dbUser,
                "--password=" + dbPassword,
                "--database",
                dbName,
                "--batch",
                "--skip-column-names",
                "--raw",
                "-e",
                cmd);
        return lines.isEmpty() ? Optional.empty() : Optional.of(lines);

    }

    static Optional<List<String>> executeSqlQueryJDBC(Query query) throws ShellUtils.CommandFailedException {
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

        } catch (SQLException e) {
            throw new ShellUtils.CommandFailedException("SQL command execution failed: " + e.getMessage(), e);
        }
    }


    static Connection getConnection() throws SQLException {
        String pleskDbName = "psa";
        String dbUser = Config.getDatabaseUser();
        String dbPassword = Config.getDatabasePassword();

        String dbUrl = String.format("jdbc:mysql://localhost/%s", pleskDbName);
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    static Query prepareFetchSubscriptionNameById(int id) {
        String sql = "SELECT name FROM domains WHERE webspace_id=0 AND id=?";
        return new Query(sql, id);
    }

    record Query(String sql, Object... params) {
    }
}