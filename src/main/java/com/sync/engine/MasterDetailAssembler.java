package com.sync.engine;

import cn.hutool.core.util.StrUtil;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 主表关联明细查询 & 树形组装
 * 输出扁平记录（主表字段 + _前缀明细字段），ApiWriter 自动归入嵌套 JSON
 */
public class MasterDetailAssembler {

    private static final Logger log = LoggerFactory.getLogger(MasterDetailAssembler.class);

    public static List<Map<String, Object>> assemble(Job job, JobConfig config, List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) return Collections.emptyList();

        boolean useDbQuery = job.getSourceDatasource() != null && job.getSourceDatasource().getType() == 1;
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> master : records) {
            List<Map<String, Object>> details = fetchDetails(job, config, master, useDbQuery);

            if (details.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (var e : master.entrySet()) {
                    if (!e.getKey().startsWith("_")) row.put(e.getKey(), e.getValue());
                }
                row.put("_empty_", "true");
                result.add(row);
            } else {
                for (Map<String, Object> d : details) {
                    Map<String, Object> merged = new LinkedHashMap<>(master);
                    for (Map.Entry<String, Object> e : d.entrySet()) {
                        merged.put("_" + e.getKey(), e.getValue());
                    }
                    result.add(merged);
                }
            }
        }

        log.info("MasterDetailAssembler: {} master → {} flat rows", records.size(), result.size());
        return result;
    }

    private static List<Map<String, Object>> fetchDetails(Job job, JobConfig config,
                                                           Map<String, Object> master, boolean useDbQuery) {
        List<Map<String, Object>> allDetails = new ArrayList<>();
        List<Map<String, Object>> relations = config.getMasterDetailRelations();
        if (relations == null || relations.isEmpty()) return allDetails;

        for (Map<String, Object> rel : relations) {
            String detailTable = asStr(rel.get("detail_table"));
            String detailCondition = asStr(rel.get("detail_condition"));
            @SuppressWarnings("unchecked")
            List<String> detailColumns = (List<String>) rel.get("detail_columns");

            if (StrUtil.isBlank(detailTable) || StrUtil.isBlank(detailCondition)) continue;

            String[] parsed = parseRelationKey(detailCondition, master);
            String keyField = parsed[0];
            String keyValue = parsed[1];
            if (StrUtil.isBlank(keyValue)) continue;

            if (useDbQuery) {
                try {
                    DataSource ds = com.sync.reader.impl.DataSourceManager.getDataSource(job.getSourceDatasource());
                    try (Connection conn = ds.getConnection()) {
                        allDetails.addAll(queryDetails(conn, detailTable, keyField, keyValue, detailColumns));
                    }
                } catch (Exception e) {
                    log.warn("Failed to query detail table {}: {}", detailTable, e.getMessage());
                }
            }
        }
        return allDetails;
    }

    private static String[] parseRelationKey(String condition, Map<String, Object> master) {
        int s = condition.indexOf('{'), e = condition.indexOf('}', s);
        if (s < 0 || e <= s) return new String[]{null, null};
        String field = condition.substring(s + 1, e).trim();
        Object val = master.get(field);
        return new String[]{field, val != null ? val.toString() : null};
    }

    private static List<Map<String, Object>> queryDetails(Connection conn, String table,
                                                            String keyField, String keyValue, List<String> columns) throws SQLException {
        List<String> cols = (columns != null && !columns.isEmpty()) ? columns : Collections.singletonList("*");
        String sql = String.format("SELECT %s FROM `%s` WHERE `%s` = ?", String.join(", ", cols), table, keyField);
        List<Map<String, Object>> results = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyValue);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }

    private static String asStr(Object obj) { return obj != null ? obj.toString() : null; }
}
