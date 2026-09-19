package com.sync.writer.impl;

import com.sync.enums.DataSourceType;
import com.sync.enums.WriteMode;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.writer.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;

/**
 * 数据库写入器 - INSERT / UPSERT / UPDATE / DELETE
 */
public class DbWriter implements DataWriter {

    private static final Logger log = LoggerFactory.getLogger(DbWriter.class);

    @Override
    public int batchWrite(Job job, JobConfig config, List<Map<String, Object>> records) {
        if (job.getTargetDatasource() == null) {
            throw new IllegalStateException("目标数据源未加载");
        }
        DataSource ds = com.sync.reader.impl.DataSourceManager.getDataSource(job.getTargetDatasource());

        WriteMode mode = WriteMode.fromCode(config.getTargetWriteMode());
        String table = config.getTargetTable();
        int count = 0;

        try {
            switch (mode) {
                case INSERT -> count = doInsert(ds, table, records);
                case UPSERT -> count = doUpsert(ds, table, records, config.getUpsertKeys());
                case UPDATE -> count = doUpdate(ds, table, records, config.getUpsertKeys());
                case DELETE -> count = doDelete(ds, table, records, config.getUpsertKeys());
                default -> throw new UnsupportedOperationException("Unsupported write mode: " + mode.getCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("DbWriter failed on " + mode + ": " + e.getMessage(), e);
        }

        log.info("DbWriter wrote {} records to table={} mode={}", count, table, mode);
        return count;
    }

    private int doInsert(DataSource ds, String table, List<Map<String, Object>> records) throws Exception {
        if (records.isEmpty()) return 0;
        Map<String, Object> first = records.get(0);
        String sql = buildInsertSql(table, first.keySet());

        try (var conn = ds.getConnection()) {
            try (var ps = conn.prepareStatement(sql)) {
                for (Map<String, Object> rec : records) {
                    int idx = 1;
                    for (Object val : rec.values()) {
                        ps.setObject(idx++, val);
                    }
                    ps.addBatch();
                }
                return Arrays.stream(ps.executeBatch()).filter(r -> r > 0).sum();
            }
        }
    }

    private int doUpsert(DataSource ds, String table, List<Map<String, Object>> records, List<String> upsertKeys) throws Exception {
        if (upsertKeys == null || upsertKeys.isEmpty()) {
            throw new IllegalArgumentException("UPSERT requires upsert_keys");
        }
        int total = 0;
        try (var conn = ds.getConnection()) {
            for (Map<String, Object> rec : records) {
                if (exists(conn, table, upsertKeys, rec)) {
                    total += updateRecord(conn, table, rec, upsertKeys);
                } else {
                    total += insertRecord(conn, table, rec);
                }
            }
        }
        return total;
    }

    private boolean exists(java.sql.Connection conn, String table, List<String> keys, Map<String, Object> rec) throws Exception {
        String where = buildWhere(keys);
        String sql = "SELECT COUNT(*) FROM `" + table + "` WHERE " + where;
        try (var ps = conn.prepareStatement(sql)) {
            setParams(ps, keys, rec);
            try (var rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int updateRecord(java.sql.Connection conn, String table, Map<String, Object> rec, List<String> keys) throws Exception {
        Set<String> updateCols = new HashSet<>(rec.keySet());
        updateCols.removeAll(keys);
        if (updateCols.isEmpty()) return 0;

        String setClause = String.join(" = ?, ", updateCols) + " = ?";
        String whereClause = buildWhere(keys);
        String sql = "UPDATE `" + table + "` SET " + setClause + " WHERE " + whereClause;

        try (var ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String col : updateCols) {
                ps.setObject(idx++, rec.get(col));
            }
            setParams(ps, keys, rec);
            return ps.executeUpdate();
        }
    }

    private int insertRecord(java.sql.Connection conn, String table, Map<String, Object> rec) throws Exception {
        String sql = buildInsertSql(table, rec.keySet());
        try (var ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Object val : rec.values()) {
                ps.setObject(idx++, val);
            }
            return ps.executeUpdate();
        }
    }

    private int doUpdate(DataSource ds, String table, List<Map<String, Object>> records, List<String> conditionKeys) throws Exception {
        if (conditionKeys == null || conditionKeys.isEmpty()) {
            throw new IllegalArgumentException("UPDATE requires condition keys");
        }
        int total = 0;
        try (var conn = ds.getConnection()) {
            for (Map<String, Object> rec : records) {
                total += updateRecord(conn, table, rec, conditionKeys);
            }
        }
        return total;
    }

    private int doDelete(DataSource ds, String table, List<Map<String, Object>> records, List<String> conditionKeys) throws Exception {
        if (conditionKeys == null || conditionKeys.isEmpty()) {
            throw new IllegalArgumentException("DELETE requires condition keys");
        }
        int total = 0;
        try (var conn = ds.getConnection()) {
            String where = "WHERE " + conditionKeys.get(0) + " = ?";
            String sql = "DELETE FROM `" + table + "` " + where;
            try (var ps = conn.prepareStatement(sql)) {
                for (Map<String, Object> rec : records) {
                    ps.setObject(1, rec.get(conditionKeys.get(0)));
                    ps.addBatch();
                }
                for (int r : ps.executeBatch()) total += r;
            }
        }
        return total;
    }

    private String buildInsertSql(String table, Set<String> columns) {
        List<String> cols = new ArrayList<>(columns);
        String colsStr = String.join(", ", cols);
        String placeholders = String.join(", ", Collections.nCopies(cols.size(), "?"));
        return "INSERT INTO `" + table + "` (" + colsStr + ") VALUES (" + placeholders + ")";
    }

    private String buildWhere(List<String> keys) {
        return String.join(" AND ", keys.stream().map(k -> "`" + k + "` = ?").toList());
    }

    private void setParams(java.sql.PreparedStatement ps, List<String> keys, Map<String, Object> rec) throws Exception {
        int i = 1;
        for (String key : keys) {
            ps.setObject(i++, rec.get(key));
        }
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getTargetDatasource() == null) return false;
        return DataSourceType.fromCode(job.getTargetDatasource().getType()).isDatabase();
    }
}
