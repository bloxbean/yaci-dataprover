package com.bloxbean.cardano.dataprover.polyglot.helpers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;

/**
 * Database helper exposed to polyglot scripts.
 * Provides connection pooling and query execution.
 */
public class DatabaseHelper {
    private static final Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

    @HostAccess.Export
    public DataSource createPool(Value config) {
        String jdbcUrl = config.getMember("jdbcUrl").asString();
        String username = config.getMember("username").asString();
        String password = config.getMember("password").asString();
        int maxPoolSize = config.hasMember("maxPoolSize") ? config.getMember("maxPoolSize").asInt() : 5;

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        return new HikariDataSource(hikariConfig);
    }

    @HostAccess.Export
    public List<Map<String, Object>> query(DataSource dataSource, String sql, Object[] params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        // Convert BigDecimal to BigInteger for better JavaScript compatibility
                        // BigDecimal host objects don't serialize properly through polyglot
                        if (value instanceof BigDecimal bd) {
                            value = bd.toBigInteger();
                        }
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }

        return results;
    }

    @HostAccess.Export
    public List<Map<String, Object>> query(DataSource dataSource, String sql, Value params) {
        return query(dataSource, sql, convertParams(params));
    }

    @HostAccess.Export
    public Object queryScalar(DataSource dataSource, String sql, Object[] params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Scalar query failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public Object queryScalar(DataSource dataSource, String sql, Value params) {
        return queryScalar(dataSource, sql, convertParams(params));
    }

    @HostAccess.Export
    public int execute(DataSource dataSource, String sql, Object[] params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Execute failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public int execute(DataSource dataSource, String sql, Value params) {
        return execute(dataSource, sql, convertParams(params));
    }

    @HostAccess.Export
    public Connection testConnection(String jdbcUrl, String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
            log.info("Test connection successful to: {}", jdbcUrl);
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public void closePool(DataSource dataSource) {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }

    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof Value v) {
                    param = convertValue(v);
                }
                stmt.setObject(i + 1, param);
            }
        }
    }

    private Object[] convertParams(Value params) {
        if (params == null || params.isNull()) {
            return new Object[0];
        }
        if (params.hasArrayElements()) {
            int size = (int) params.getArraySize();
            Object[] result = new Object[size];
            for (int i = 0; i < size; i++) {
                result[i] = convertValue(params.getArrayElement(i));
            }
            return result;
        }
        return new Object[0];
    }

    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        return value.toString();
    }
}
