package com.sync.reader.impl;

import cn.hutool.core.util.StrUtil;
import com.sync.enums.DataSourceType;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.reader.DataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** 数据库读取器 */
public class DbReader implements DataReader {

    private static final Logger log = LoggerFactory.getLogger(DbReader.class);

    @Override
    public List<Map<String, Object>> readPage(Job job, JobConfig config, String lastSyncValue) {
        if (job.getSourceDatasource() == null) throw new IllegalStateException("源数据源未加载");
        DataSource ds = DataSourceManager.getDataSource(job.getSourceDatasource());

        String table = config.getSourceTable();
        int pageSize = Optional.ofNullable(config.getPageSize()).orElse(100);
        String columns = config.getSourceColumns() != null && !config.getSourceColumns().isEmpty()
                ? String.join(", ", config.getSourceColumns()) : "*";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(columns).append(" FROM `").append(table).append("` ");
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        // 增量条件
        if (StrUtil.isNotBlank(config.getIncrementalKey()) && StrUtil.isNotBlank(lastSyncValue)) {
            conditions.add("`" + config.getIncrementalKey() + "` > ?");
            params.add(toTimestamp(lastSyncValue));
        }

        // SQL WHERE
        if (StrUtil.isNotBlank(config.getSourceCondition())) {
            if (!config.getSourceCondition().startsWith("Aviator")) {
                conditions.add("(" + config.getSourceCondition() + ")");
            }
        }
        if (!conditions.isEmpty()) sql.append("WHERE ").append(String.join(" AND ", conditions));

        sql.append(" ORDER BY `").append(config.getIncrementalKey()).append("` ASC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(0);

        log.debug("DbReader: {} | params: {}", sql, params);

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    List<Map<String, Object>> records = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.put(meta.getColumnName(i), toSafe(rs.getObject(i)));
                        }
                        records.add(row);
                    }
                    log.info("DbReader read {} records from {}", records.size(), table);
                    return records;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("DbReader failed: " + e.getMessage(), e);
        }
    }

    private Timestamp toTimestamp(String v) {
        try { return Timestamp.valueOf(LocalDateTime.parse(v)); } catch (Exception e) {
            return new Timestamp(new BigDecimal(v).longValue());
        }
    }

    private Object toSafe(Object val) {
        return val instanceof byte[] ? Arrays.toString((byte[]) val) : (val != null ? val : "");
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getSourceDatasource() == null) return false;
        return DataSourceType.fromCode(job.getSourceDatasource().getType()).isDatabase();
    }
}
