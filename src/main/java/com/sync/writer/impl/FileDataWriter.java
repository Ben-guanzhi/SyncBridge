package com.sync.writer.impl;

import com.sync.enums.DataSourceType;
import com.sync.enums.WriteMode;
import com.sync.model.DatasourceConfig;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.writer.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文件数据写入器 - 支持写入 SQL 文件
 * 用于 DB→文件 和 文件→文件 同步模式
 */
public class FileDataWriter implements DataWriter {

    private static final Logger log = LoggerFactory.getLogger(FileDataWriter.class);

    @Override
    public int batchWrite(Job job, JobConfig config, List<Map<String, Object>> records) {
        if (job.getTargetDatasource() == null) {
            throw new IllegalStateException("目标数据源未加载");
        }

        DatasourceConfig targetDs = job.getTargetDatasource();
        String filePath = targetDs.getFilePath();
        String sqlContent = targetDs.getSqlContent();
        String table = config.getTargetTable();

        if (filePath != null && !filePath.isEmpty()) {
            return writeToFile(filePath, records, config.getTargetWriteMode(), table);
        } else if (sqlContent != null && !sqlContent.isEmpty()) {
            // 内联 SQL 模式，直接输出到控制台
            System.out.println("=== SQL 输出 (内联模式) ===");
            for (Map<String, Object> rec : records) {
                System.out.println(buildInsertSql(table, rec));
            }
            return records.size();
        } else {
            throw new IllegalArgumentException("文件路径或SQL内容不能同时为空");
        }
    }

    private int writeToFile(String filePath, List<Map<String, Object>> records, int writeMode, String table) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            boolean append = writeMode == WriteMode.INSERT.getCode();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8))) {
                for (Map<String, Object> rec : records) {
                    String sql = buildInsertSql(table, rec);
                    writer.write(sql + ";\n");
                }
            }

            log.info("FileDataWriter wrote {} records to {}", records.size(), filePath);
            return records.size();
        } catch (Exception e) {
            throw new RuntimeException("FileDataWriter failed: " + e.getMessage(), e);
        }
    }

    private String buildInsertSql(String table, Map<String, Object> record) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(table).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        int i = 0;
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (i > 0) {
                sb.append(", ");
                values.append(", ");
            }
            sb.append(entry.getKey());
            values.append(formatValue(entry.getValue()));
            i++;
        }
        sb.append(")").append(values).append(")");
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof String) {
            String str = ((String) value).replace("'", "\\'");
            return "'" + str + "'";
        }
        if (value instanceof java.sql.Timestamp) {
            return "'" + value.toString() + "'";
        }
        return value.toString();
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getTargetDatasource() == null) return false;
        DataSourceType type = DataSourceType.fromCode(job.getTargetDatasource().getType());
        return type.isFile();
    }
}
