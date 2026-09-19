package com.sync.demo;

import cn.hutool.json.JSONUtil;
import com.sync.engine.AviatorFilter;
import com.sync.engine.FieldMapper;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试Demo：Excel/SQL 文件数据同步
 *
 * 场景描述：
 *   - 支持从 Excel 文件读取源数据
 *   - 支持从 SQL 文件读取源数据
 *   - 支持数据预览和转换
 */
public class DemoFileDataSync {

    public static void main(String[] args) {
        System.out.println("=== Excel/SQL 文件数据同步 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(5L);
        job.setName("File-Data-Sync-Demo");
        job.setDescription("从Excel/SQL文件同步数据");
        job.setSyncMode(2); // DB→API

        // 源数据源：Excel 文件
        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(500L);
        sourceDs.setName("Excel-Source");
        sourceDs.setType(3); // EXCEL_FILE
        sourceDs.setFilePath("D:/data/sync_source.xlsx");
        sourceDs.setSheetName("Sheet1");
        job.setSourceDatasource(sourceDs);

        // 目标数据源：MySQL
        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(600L);
        targetDs.setName("Target-MySQL");
        targetDs.setType(1); // DATABASE
        targetDs.setHost("127.0.0.1");
        targetDs.setPort(3306);
        targetDs.setDatabaseName("sync_platform");
        targetDs.setUsername("admin");
        targetDs.setPassword("encrypted_pwd");
        targetDs.setMaxConnections(5);
        job.setTargetDatasource(targetDs);

        // ==========================
        // 2. 构建JobConfig
        // ==========================
        List<JobConfig> configs = new ArrayList<>();
        JobConfig config = new JobConfig();
        config.setJobId(job.getId());
        config.setItemOrder(1);
        config.setEnabled(true);
        config.setSourceTable("data"); // Excel 工作表名或 SQL 表名
        config.setPageSize(100);
        config.setTargetTable("sync_target_table");
        config.setTargetWriteMode(1); // INSERT
        config.setUpsertKeys(Arrays.asList("id"));

        // 字段映射
        config.setFieldMappings(Arrays.asList(
                entry("name", "name"),
                entry("value", "value"),
                entry("date", "createdTime"),
                entry("status", "status")
        ));

        configs.add(config);
        job.setConfigs(configs);

        // ==========================
        // 3. 演示文件预览功能
        // ==========================
        System.out.println("--- [PREVIEW] 文件数据预览 ---");
        previewFileData(sourceDs.getFilePath(), sourceDs.getSheetName());

        // ==========================
        // 4. 模拟数据转换
        // ==========================
        System.out.println("\n--- [TRANSFORM] 数据转换演示 ---");
        List<Map<String, Object>> mockRecords = buildMockExcelData();
        for (Map<String, Object> r : mockRecords) {
            System.out.println("  原始: " + JSONUtil.toJsonStr(r));
        }

        // 应用字段映射
        List<Map<String, Object>> mapped = FieldMapper.map(mockRecords, config.getFieldMappings());
        for (Map<String, Object> r : mapped) {
            System.out.println("  映射: " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 5. 完整引擎执行
        // ==========================
        System.out.println("\n--- [ENGINE] SyncEngine执行 ---");
        try {
            SyncEngine engine = new SyncEngine();
            SyncResult result = engine.execute(job);
            System.out.println("  " + result);
            engine.shutdown();
        } catch (Exception e) {
            System.out.println("  Engine error (expected — no real file): " + e.getMessage());
        }

        System.out.println("\n=== Demo 完成 ===");
    }

    /**
     * 预览文件数据
     */
    private static void previewFileData(String filePath, String sheetName) {
        System.out.println("  文件路径: " + filePath);
        System.out.println("  工作表: " + (sheetName != null ? sheetName : "默认"));
        System.out.println("  预览限制: 100 行");
        System.out.println("  提示: 实际预览请使用 API: GET /api/file-preview/preview?path=" + filePath);
    }

    /**
     * 构建模拟 Excel 数据
     */
    private static List<Map<String, Object>> buildMockExcelData() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("name", "产品A");
        r1.put("value", 100);
        r1.put("date", "2025-07-20");
        r1.put("status", "active");
        records.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("name", "产品B");
        r2.put("value", 200);
        r2.put("date", "2025-07-21");
        r2.put("status", "inactive");
        records.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("name", "产品C");
        r3.put("value", 150);
        r3.put("date", "2025-07-22");
        r3.put("status", "active");
        records.add(r3);

        return records;
    }

    private static Map<String, String> entry(String src, String tgt) {
        Map<String, String> e = new HashMap<>();
        e.put("source_field", src);
        e.put("target_field", tgt);
        return e;
    }
}
