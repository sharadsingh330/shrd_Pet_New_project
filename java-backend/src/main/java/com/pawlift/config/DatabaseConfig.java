package com.pawlift.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * HikariCP connection pool. Configuration comes from -D system properties
 * or environment variables: DB_URL, DB_USER, DB_PASSWORD.
 */
public final class DatabaseConfig {
    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) dataSource = create();
            }
        }
        return dataSource;
    }

    private static HikariDataSource create() {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        cfg.setJdbcUrl(env("DB_URL",
                "jdbc:mysql://localhost:3306/pawlift?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"));
        cfg.setUsername(env("DB_USER", "root"));
        cfg.setPassword(env("DB_PASSWORD", ""));
        cfg.setPoolName("pawlift-pool");
        cfg.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_SIZE", "10")));
        cfg.setMinimumIdle(1);
        cfg.setConnectionTimeout(10_000);
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        return new HikariDataSource(cfg);
    }

    public static String env(String key, String defaultValue) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}
