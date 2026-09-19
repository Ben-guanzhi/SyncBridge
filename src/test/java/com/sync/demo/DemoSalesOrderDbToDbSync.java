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
 * 测试Demo：DB→DB 销售订单同步场景
 *
 * 场景描述：
 *   - 源：金蝶云星空数据库（erp_source）
 *   - 目标：用友ERP数据库（erp_target）
 *   - 同步销售订单主表及明细
 */
public class DemoSalesOrderDbToDbSync {

    public static void main(String[] args) {
        System.out.println("=== 销售订单 DB→DB 同步 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(2L);
        job.setName("SalesOrder-DB-to-DB");
        job.setDescription("将金蝶销售订单同步到用友ERP数据库");
        job.setSyncMode(1); // DB→DB
        job.setIncrementalKey("FSaleDate");

        // 源数据源：金蝶数据库
        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(300L);
        sourceDs.setName("Kingdee-DB");
        sourceDs.setType(1);
        sourceDs.setHost("127.0.0.1");
        sourceDs.setPort(3306);
        sourceDs.setDatabaseName("kingdee_erp");
        sourceDs.setUsername("sync_user");
        sourceDs.setPassword("encrypted_src");
        sourceDs.setMaxConnections(5);
        job.setSourceDatasource(sourceDs);

        // 目标数据源：用友数据库
        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(400L);
        targetDs.setName("Yonyou-DB");
        targetDs.setType(1);
        targetDs.setHost("127.0.0.1");
        targetDs.setPort(3306);
        targetDs.setDatabaseName("yonyou_erp");
        targetDs.setUsername("sync_user");
        targetDs.setPassword("encrypted_tgt");
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
        config.setSourceTable("SO_SaleOrder");
        config.setPageSize(50);
        config.setIncrementalKey("FSaleDate");
        config.setSourceCondition("Aviator:$FStatus == 'APPROVED' && $FCancelled == false");
        config.setTargetTable("YD_SALE_ORDER");
        config.setTargetWriteMode(2); // UPSERT
        config.setUpsertKeys(Arrays.asList("FSourceBillNo"));

        // 字段映射（金蝶 → 用友）
        config.setFieldMappings(Arrays.asList(
                entry("FBillNo",       "FSourceBillNo"),
                entry("FSaleDate",     "FSaleDate"),
                entry("FCustomerId",   "FCustomerId"),
                entry("FSaleOrgId",    "FSaleOrgId"),
                entry("FAmount",       "FAmount"),
                entry("FRemark",       "FRemark")
        ));

        configs.add(config);
        job.setConfigs(configs);

        // ==========================
        // 3. 模拟源数据
        // ==========================
        System.out.println("--- [READ] 模拟源数据 ---");
        List<Map<String, Object>> mockRecords = buildMockData();
        for (Map<String, Object> r : mockRecords) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 4. Aviator行过滤
        // ==========================
        System.out.println("\n--- [FILTER] Aviator过滤 ---");
        List<Map<String, Object>> filtered = AviatorFilter.filter(mockRecords, config.getSourceCondition());
        for (Map<String, Object> r : filtered) {
            System.out.println("  ✓ PASS: " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 5. 字段映射
        // ==========================
        System.out.println("\n--- [MAP] 字段映射 ---");
        List<Map<String, Object>> mapped = FieldMapper.map(filtered, config.getFieldMappings());
        for (Map<String, Object> r : mapped) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 6. 完整引擎执行
        // ==========================
        System.out.println("\n--- [ENGINE] SyncEngine执行 ---");
        try {
            SyncEngine engine = new SyncEngine();
            SyncResult result = engine.execute(job);
            System.out.println("  " + result);
            engine.shutdown();
        } catch (Exception e) {
            System.out.println("  Engine error (expected — no real DB): " + e.getMessage());
        }

        System.out.println("\n=== Demo 完成 ===");
    }

    private static List<Map<String, Object>> buildMockData() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("FID", 5001);
        r1.put("FBillNo", "SO20250720001");
        r1.put("FSaleDate", "2025-07-20T09:00:00");
        r1.put("FCustomerId", "CUST001");
        r1.put("FSaleOrgId", "ORG001");
        r1.put("FAmount", 15000.00);
        r1.put("FStatus", "APPROVED");
        r1.put("FCancelled", false);
        r1.put("FRemark", "Q3季度订单");
        records.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("FID", 5002);
        r2.put("FBillNo", "SO20250721002");
        r2.put("FSaleDate", "2025-07-21T14:30:00");
        r2.put("FCustomerId", "CUST002");
        r2.put("FSaleOrgId", "ORG001");
        r2.put("FAmount", 8500.50);
        r2.put("FStatus", "DRAFT");
        r2.put("FCancelled", false);
        r2.put("FRemark", "待审核订单");
        records.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("FID", 5003);
        r3.put("FBillNo", "SO20250722003");
        r3.put("FSaleDate", "2025-07-22T11:15:00");
        r3.put("FCustomerId", "CUST001");
        r3.put("FSaleOrgId", "ORG002");
        r3.put("FAmount", 23000.00);
        r3.put("FStatus", "APPROVED");
        r3.put("FCancelled", false);
        r3.put("FRemark", "大客户订单");
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
