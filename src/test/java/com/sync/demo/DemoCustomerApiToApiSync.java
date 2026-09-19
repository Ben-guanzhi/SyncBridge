package com.sync.demo;

import cn.hutool.json.JSONUtil;
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
 * 测试Demo：API→API 客户数据同步场景
 *
 * 场景描述：
 *   - 源：CRM系统API
 *   - 目标：ERP系统API
 *   - 实时同步客户主数据
 */
public class DemoCustomerApiToApiSync {

    public static void main(String[] args) {
        System.out.println("=== 客户数据 API→API 同步 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(4L);
        job.setName("Customer-CRM-to-ERP");
        job.setDescription("将CRM系统客户数据同步到ERP系统");
        job.setSyncMode(4); // API→API
        job.setIncrementalKey("FModifyTime");

        // 源数据源：CRM API
        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(700L);
        sourceDs.setName("CRM-API");
        sourceDs.setType(2);
        sourceDs.setBaseUrl("https://crm.example.com/api");
        sourceDs.setAuthMethod("bearer");
        sourceDs.setAuthToken("encrypted_crm_token");
        sourceDs.setTimeoutMs(30000);
        Map<String, String> srcHeaders = new HashMap<>();
        srcHeaders.put("Content-Type", "application/json");
        srcHeaders.put("X-API-Version", "v2");
        sourceDs.setHeaders(srcHeaders);
        job.setSourceDatasource(sourceDs);

        // 目标数据源：ERP API
        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(800L);
        targetDs.setName("ERP-API");
        targetDs.setType(2);
        targetDs.setBaseUrl("https://erp.example.com/api");
        targetDs.setAuthMethod("basic");
        targetDs.setUsername("sync_user");
        targetDs.setPassword("encrypted_erp_pwd");
        targetDs.setTimeoutMs(30000);
        Map<String, String> tgtHeaders = new HashMap<>();
        tgtHeaders.put("Content-Type", "application/json");
        targetDs.setHeaders(tgtHeaders);
        job.setTargetDatasource(targetDs);

        // ==========================
        // 2. 构建JobConfig
        // ==========================
        List<JobConfig> configs = new ArrayList<>();
        JobConfig config = new JobConfig();
        config.setJobId(job.getId());
        config.setItemOrder(1);
        config.setEnabled(true);
        config.setSourceTable("customers");
        config.setPageSize(50);
        config.setPaginationKey("cursor");
        config.setMaxPages(20);
        config.setIncrementalKey("FModifyTime");
        config.setSourceCondition("Aviator:$FStatus == 'ACTIVE'");
        config.setTargetTable("customers");
        config.setTargetWriteMode(2); // UPSERT
        config.setUpsertKeys(Arrays.asList("FCustomerCode"));

        // 字段映射
        config.setFieldMappings(Arrays.asList(
                entry("customerCode",    "FCustomerCode"),
                entry("customerName",    "FCustomerName"),
                entry("contactPerson",   "FContactPerson"),
                entry("phone",          "FPhone"),
                entry("email",          "FEmail"),
                entry("address",        "FAddress"),
                entry("modifyTime",     "FModifyTime")
        ));

        configs.add(config);
        job.setConfigs(configs);

        // ==========================
        // 3. 模拟API响应数据
        // ==========================
        System.out.println("--- [READ] 模拟CRM API响应数据 ---");
        List<Map<String, Object>> mockRecords = buildMockData();
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
            System.out.println("  Engine error (expected — no real API): " + e.getMessage());
        }

        System.out.println("\n=== Demo 完成 ===");
    }

    private static List<Map<String, Object>> buildMockData() {
        List<Map<String, Object>> records = new ArrayList<>();

        Map<String, Object> r1 = new HashMap<>();
        r1.put("customerCode", "CUST001");
        r1.put("customerName", "深圳科技有限公司");
        r1.put("contactPerson", "陈总");
        r1.put("phone", "0755-12345678");
        r1.put("email", "chen@tech.com");
        r1.put("address", "深圳市南山区科技园");
        r1.put("FStatus", "ACTIVE");
        r1.put("modifyTime", "2025-07-20T10:00:00");
        records.add(r1);

        Map<String, Object> r2 = new HashMap<>();
        r2.put("customerCode", "CUST002");
        r2.put("customerName", "广州贸易有限公司");
        r2.put("contactPerson", "林经理");
        r2.put("phone", "020-87654321");
        r2.put("email", "lin@gztrade.com");
        r2.put("address", "广州市天河区珠江新城");
        r2.put("FStatus", "ACTIVE");
        r2.put("modifyTime", "2025-07-21T14:30:00");
        records.add(r2);

        Map<String, Object> r3 = new HashMap<>();
        r3.put("customerCode", "CUST003");
        r3.put("customerName", "杭州互联网公司");
        r3.put("contactPerson", "杨主管");
        r3.put("phone", "0571-12345678");
        r3.put("email", "yang@hztech.com");
        r3.put("address", "杭州市西湖区文三路");
        r3.put("FStatus", "INACTIVE");
        r3.put("modifyTime", "2025-07-19T09:00:00");
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
