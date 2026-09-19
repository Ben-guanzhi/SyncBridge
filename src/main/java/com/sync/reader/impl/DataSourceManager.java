package com.sync.reader.impl;

import com.sync.model.DatasourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源动态管理器 - HikariCP 连接池生命周期管理
 */
public class DataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(DataSourceManager.class);
    private static final Map<Long, HikariDataSource> POOL_CACHE = new ConcurrentHashMap<>();

    public static DataSource getDataSource(DatasourceConfig config) {
        if (config.getType() != 1) {
            throw new IllegalStateException("DataSourceManager only supports DATABASE type");
        }
        return POOL_CACHE.computeIfAbsent(config.getId(), id -> createPool(config));
    }

    public static void closePool(Long datasourceId) {
        HikariDataSource pool = POOL_CACHE.remove(datasourceId);
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("DataSourcePool closed for id={}", datasourceId);
        }
    }

    public static void closeAllPools() {
        for (Map.Entry<Long, HikariDataSource> entry : POOL_CACHE.entrySet()) {
            try {
                if (!entry.getValue().isClosed()) {
                    entry.getValue().close();
                }
            } catch (Exception e) {
                log.warn("Failed to close pool for id={}", entry.getKey(), e);
            }
        }
        POOL_CACHE.clear();
    }

    public static boolean isLoaded(Long datasourceId) {
        return POOL_CACHE.containsKey(datasourceId);
    }

    private static HikariDataSource createPool(DatasourceConfig config) {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("sync-pool-" + config.getName());
        hc.setJdbcUrl(buildJdbcUrl(config));
        hc.setUsername(config.getUsername());
        hc.setPassword(config.getPassword());
        hc.setMaximumPoolSize(Optional.ofNullable(config.getMaxConnections()).orElse(10));
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout((long) Optional.ofNullable(config.getTimeoutMs()).orElse(10_000));
        hc.setIdleTimeout(300_000L);
        hc.setMaxLifetime(600_000L);
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        if (config.getExtraConfig() != null) {
            for (Map.Entry<String, Object> entry : config.getExtraConfig().entrySet()) {
                hc.addDataSourceProperty(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        log.info("Creating DataSourcePool: name={} | maxPoolSize={}", config.getName(), hc.getMaximumPoolSize());
        return new HikariDataSource(hc);
    }

    private static String buildJdbcUrl(DatasourceConfig config) {
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(config.getHost()).append(":")
                .append(Optional.ofNullable(config.getPort()).orElse(3306)).append("/")
                .append(config.getDatabaseName())
                .append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai");

        if (config.getExtraConfig() != null) {
            for (Map.Entry<String, Object> entry : config.getExtraConfig().entrySet()) {
                String key = entry.getKey();
                if (!key.equals("useSSL") && !key.equals("allowPublicKeyRetrieval") && !key.equals("serverTimezone")) {
                    url.append("&").append(key).append("=").append(entry.getValue());
                }
            }
        }
        return url.toString();
    }
}
