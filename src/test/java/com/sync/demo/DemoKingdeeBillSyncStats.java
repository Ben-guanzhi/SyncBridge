package com.sync.demo;

import cn.hutool.json.JSONUtil;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.*;

import java.util.*;

/**
 * 测试Demo：金蝶云星空单据同步统计
 *
 * 场景描述：
 *   - 从金蝶云星空 API 拉取单据数据
 *   - 统计同步结果（成功/失败/跳过）
 *   - 支持增量同步
 *
 * 对应参考项目：D:\OLP\jindie_api\金蝶云星空单据同步统计.md
 */
public class DemoKingdeeBillSyncStats {

    public static void main(String[] args) {
        System.out.println("=== 金蝶云星空单据同步统计 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(10L);
        job.setName("Kingdee-Bill-Sync-Stats");
        job.setDescription("从金蝶云星空同步单据并统计");
        job.setSyncMode(2); // DB→API
        job.setIncrementalKey("FModifyTime");

        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(1000L);
        sourceDs.setName("Kingdee-Cloud-API");
        sourceDs.setType(2);
        sourceDs.setBaseUrl("https://api.kingdee.com/cloud/api");
        sourceDs.setAuthMethod("bearer");
        sourceDs.setAuthToken("encrypted_kingdee_token");
        sourceDs.setTimeoutMs(30000);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("x-kd-auth-token", "encrypted_token");
        sourceDs.setHeaders(headers);
        job.setSourceDatasource(sourceDs);

        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(1100L);
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
        config.setSourceTable("FSaleOrder");
        config.setPageSize(50);
        config.setPaginationKey("page");
        config.setMaxPages(20);
        config.setIncrementalKey("FModifyTime");
        config.setSourceCondition("Aviator:$FStatus == 'RELEASED'");
        config.setTargetTable("sync_kingdee_sale_order");
        config.setTargetWriteMode(2); // UPSERT
        config.setUpsertKeys(Arrays.asList("FBillNo"));

        config.setFieldMappings(Arrays.asList(
                entry("FBillNo", "FBillNo"),
                entry("FDocumentDate", "FDocumentDate"),
                entry("FCustomerId", "FCustomerId"),
                entry("FAmount", "FAmount"),
                entry("FStatus", "FStatus"),
                entry("FModifyTime", "FModifyTime")
        ));

        configs.add(config);
        job.setConfigs(configs);

        // ==========================
        // 3. 模拟API响应数据
        // ==========================
        System.out.println("--- [READ] 模拟金蝶API响应数据 ---");
        List<Map<String, Object>> mockRecords = buildMockKingdeeData();
        for (Map<String, Object> r : mockRecords) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 4. 统计同步结果
        // ==========================
        System.out.println("\n--- [STATS] 同步统计 ---");
        SyncStats stats = calculateStats(mockRecords);
        System.out.println("  总记录数: " + stats.total);
        System.out.println("  成功: " + stats.success);
        System.out.println("  失败: " + stats.failed);
        System.out.println("  跳过: " + stats.skipped);
        System.out.println("  成功率: " + String.format("%.2f%%", stats.successRate));

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
            System.out.println("  Engine error (expected — no real API): " + e.getMessage());
        }

        System.out.println("\n=== Demo 完成 ===");
    }

    /**
     * 计算同步统计
     */
    private static SyncStats calculateStats(List<Map<String, Object>> records) {
        SyncStats stats = new SyncStats();
        stats.total = records.size();

        for (Map<String, Object> record : records) {
            String status = (String) record.get("FStatus");
            if ("RELEASED".equals(status)) {
                stats.success++;
            } else if ("CANCELLED".equals(status)) {
                stats.skipped++;
            } else {
                stats.failed++;
            }
        }

        stats.successRate = stats.total > 0 ? (stats.success * 100.0 / stats.total) : 0.0;
        return stats;
    }

    private static List<Map<String, Object>> buildMockKingdeeData() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("FBillNo", "SO20250720001");
        r1.put("FDocumentDate", "2025-07-20");
        r1.put("FCustomerId", "CUST001");
        r1.put("FAmount", 15000.00);
        r1.put("FStatus", "RELEASED");
        r1.put("FModifyTime", "2025-07-20T10:00:00");
        records.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("FBillNo", "SO20250721002");
        r2.put("FDocumentDate", "2025-07-21");
        r2.put("FCustomerId", "CUST002");
        r2.put("FAmount", 8500.50);
        r2.put("FStatus", "RELEASED");
        r2.put("FModifyTime", "2025-07-21T14:30:00");
        records.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("FBillNo", "SO20250722003");
        r3.put("FDocumentDate", "2025-07-22");
        r3.put("FCustomerId", "CUST001");
        r3.put("FAmount", 23000.00);
        r3.put("FStatus", "DRAFT");
        r3.put("FModifyTime", "2025-07-22T09:15:00");
        records.add(r3);

        Map<String, Object> r4 = new HashMap<>();
        r4.put("FBillNo", "SO20250723004");
        r4.put("FDocumentDate", "2025-07-23");
        r4.put("FCustomerId", "CUST003");
        r4.put("FAmount", 5600.00);
        r4.put("FStatus", "CANCELLED");
        r4.put("FModifyTime", "2025-07-23T11:00:00");
        records.add(r4);

        return records;
    }

    private static Map<String, String> entry(String src, String tgt) {
        Map<String, String> e = new HashMap<>();
        e.put("source_field", src);
        e.put("target_field", tgt);
        return e;
    }

    static class SyncStats {
        int total;
        int success;
        int failed;
        int skipped;
        double successRate;
    }
}
