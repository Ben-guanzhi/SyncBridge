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
 * 测试Demo：API→DB 供应商数据同步场景
 *
 * 场景描述：
 *   - 源：第三方供应商API（分页拉取）
 *   - 目标：本地MySQL数据库
 *   - 增量同步：基于更新时间字段
 */
public class DemoSupplierApiToDbSync {

    public static void main(String[] args) {
        System.out.println("=== 供应商数据 API→DB 同步 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(3L);
        job.setName("Supplier-API-to-DB");
        job.setDescription("从第三方供应商API同步数据到本地数据库");
        job.setSyncMode(3); // API→DB
        job.setIncrementalKey("FUpdateTime");

        // 源数据源：第三方API
        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(500L);
        sourceDs.setName("Supplier-API");
        sourceDs.setType(2);
        sourceDs.setBaseUrl("https://api.supplier.com/v1");
        sourceDs.setAuthMethod("bearer");
        sourceDs.setAuthToken("encrypted_api_token");
        sourceDs.setTimeoutMs(30000);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        sourceDs.setHeaders(headers);
        job.setSourceDatasource(sourceDs);

        // 目标数据源：本地MySQL
        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(600L);
        targetDs.setName("Local-MySQL");
        targetDs.setType(1);
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
        config.setSourceTable("suppliers");
        config.setPageSize(100);
        config.setPaginationKey("offset");
        config.setMaxPages(50);
        config.setIncrementalKey("FUpdateTime");
        config.setTargetTable("sync_supplier");
        config.setTargetWriteMode(2); // UPSERT
        config.setUpsertKeys(Arrays.asList("FSupplierCode"));

        // 字段映射
        config.setFieldMappings(Arrays.asList(
                entry("supplierCode",  "FSupplierCode"),
                entry("supplierName",  "FSupplierName"),
                entry("contactName",   "FContactName"),
                entry("contactPhone",  "FContactPhone"),
                entry("email",         "FEmail"),
                entry("updateTime",    "FUpdateTime")
        ));

        configs.add(config);
        job.setConfigs(configs);

        // ==========================
        // 3. 模拟API响应数据
        // ==========================
        System.out.println("--- [READ] 模拟API响应数据 ---");
        List<Map<String, Object>> mockRecords = buildMockApiData();
        for (Map<String, Object> r : mockRecords) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 4. 字段映射
        // ==========================
        System.out.println("\n--- [MAP] 字段映射 ---");
        List<Map<String, Object>> mapped = FieldMapper.map(mockRecords, config.getFieldMappings());
        for (Map<String, Object> r : mapped) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
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
            System.out.println("  Engine error (expected — no real API/DB): " + e.getMessage());
        }

        System.out.println("\n=== Demo 完成 ===");
    }

    private static List<Map<String, Object>> buildMockApiData() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("supplierCode", "SUP001");
        r1.put("supplierName", "华为技术有限公司");
        r1.put("contactName", "张经理");
        r1.put("contactPhone", "13800138001");
        r1.put("email", "zhang@huawei.com");
        r1.put("updateTime", "2025-07-20T10:00:00");
        records.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("supplierCode", "SUP002");
        r2.put("supplierName", "阿里巴巴集团");
        r2.put("contactName", "李主管");
        r2.put("contactPhone", "13900139002");
        r2.put("email", "li@alibaba.com");
        r2.put("updateTime", "2025-07-21T15:30:00");
        records.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("supplierCode", "SUP003");
        r3.put("supplierName", "腾讯科技有限公司");
        r3.put("contactName", "王总监");
        r3.put("contactPhone", "13700137003");
        r3.put("email", "wang@tencent.com");
        r3.put("updateTime", "2025-07-22T09:15:00");
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
