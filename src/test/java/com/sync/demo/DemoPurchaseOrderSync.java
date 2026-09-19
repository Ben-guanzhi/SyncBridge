package com.sync.demo;

import cn.hutool.json.JSONUtil;
import com.sync.engine.AviatorFilter;
import com.sync.engine.FieldMapper;
import com.sync.engine.MasterDetailAssembler;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试Demo：DB→API 采购申请单同步场景
 *
 * 场景描述：
 *   - 源：本地MySQL，读取采购申请单主表 PUR_REQ_HEAD + 明细表 PUR_REQ_ENTRY
 *   - 目标：金蝶云星空 API（创建采购申请单）
 *   - 行过滤：仅同步已审核且未作废的单据
 */
public class DemoPurchaseOrderSync {

    public static void main(String[] args) {
        System.out.println("=== 采购申请单 DB→API 同步 Demo ===\n");

        // ==========================
        // 1. 构建Job
        // ==========================
        Job job = new Job();
        job.setId(1L);
        job.setName("PUR-REQ-Sync-to-Kingdee");
        job.setDescription("将本地采购申请单同步到金蝶云星空");
        job.setSyncMode(2);
        job.setIncrementalKey("FReqDate");

        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(100L);
        sourceDs.setName("Local-MySQL");
        sourceDs.setType(1);
        sourceDs.setHost("127.0.0.1");
        sourceDs.setPort(3306);
        sourceDs.setDatabaseName("erp_source");
        sourceDs.setUsername("sync_user");
        sourceDs.setPassword("encrypted_xyz");
        sourceDs.setMaxConnections(5);
        job.setSourceDatasource(sourceDs);

        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(200L);
        targetDs.setName("Kingdee-Cloud-API");
        targetDs.setType(2);
        targetDs.setBaseUrl("https://api.kingdee.com/cloud/api");
        targetDs.setAuthMethod("bearer");
        targetDs.setAuthToken("encrypted_token_xyz");
        targetDs.setTimeoutMs(30000);
        Map<String, String> apiHeaders = new HashMap<>();
        apiHeaders.put("Content-Type", "application/json");
        targetDs.setHeaders(apiHeaders);
        job.setTargetDatasource(targetDs);

        // ==========================
        // 2. 构建JobConfig
        // ==========================
        List<JobConfig> configs = new ArrayList<>();
        JobConfig config = new JobConfig();
        config.setJobId(job.getId());
        config.setItemOrder(1);
        config.setEnabled(true);
        config.setSourceTable("PUR_REQ_HEAD");
        config.setPageSize(10);
        config.setIncrementalKey("FReqDate");
        config.setSourceCondition("Aviator:$FCancelled == false && $FStatus == 'RELEASED'");
        config.setTargetTable("vap/bos/CDR/KDCKF9040/PUR_REQ_CREATE");
        config.setTargetWriteMode(1);
        config.setUpsertKeys(Collections.singletonList("_localBillId"));
        config.setIsMaster(true);
        config.setNestedJsonPath("Entries");

        // 主明细关联
        List<Map<String, Object>> relations = new ArrayList<>();
        Map<String, Object> detailRelation = new LinkedHashMap<>();
        detailRelation.put("detail_table", "PUR_REQ_ENTRY");
        detailRelation.put("detail_condition", "entry.FEntryHeadId = {FID}");
        detailRelation.put("detail_columns", Arrays.asList("FMaterialId", "FPurchaseQuantity", "FEntryUnitPrice"));
        relations.add(detailRelation);
        config.setMasterDetailRelations(relations);

        // 字段映射
        config.setFieldMappings(Arrays.asList(
                entry("FReqDate",     "Header.FReqDate"),
                entry("FReqOrgId",    "Header.FReqOrgId"),
                entry("FReqDeptId",   "Header.FReqDeptId"),
                entry("FNote",        "Header.FNote"),
                entry("FID",          "_localBillId")
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
        // 5. 主明细组装
        // ==========================
        System.out.println("\n--- [ASSEMBLE] 主明细展开 ---");
        List<Map<String, Object>> assembled = MasterDetailAssembler.assemble(job, config, filtered);
        for (Map<String, Object> r : assembled) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 6. 字段映射
        // ==========================
        System.out.println("\n--- [MAP] 字段映射 ---");
        List<Map<String, Object>> mapped = FieldMapper.map(assembled, config.getFieldMappings());
        for (Map<String, Object> r : mapped) {
            System.out.println("  " + JSONUtil.toJsonStr(r));
        }

        // ==========================
        // 7. 构建金蝶嵌套报文
        // ==========================
        System.out.println("\n--- [WRITE] 金蝶嵌套报文 ---");
        if (!mapped.isEmpty()) {
            Map<String, Object> kingdeePayload = buildKingdeePayload(mapped.get(0), config);
            System.out.println("  " + JSONUtil.toJsonPrettyStr(kingdeePayload));
        }

        // ==========================
        // 8. 完整引擎执行（会因无真实DB连接报错，正常）
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

        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("FID", 1001);
        m1.put("FReqDate", "2025-07-20T10:00:00");
        m1.put("FReqOrgId", "ORG001");
        m1.put("FReqDeptId", "DEPT001");
        m1.put("FNote", "Q2季度生产物料采购");
        m1.put("FCancelled", false);
        m1.put("FStatus", "RELEASED");
        records.add(m1);

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("FID", 1002);
        m2.put("FReqDate", "2025-07-21T10:00:00");
        m2.put("FReqOrgId", "ORG001");
        m2.put("FReqDeptId", "DEPT002");
        m2.put("FNote", "已作废的单据");
        m2.put("FCancelled", true);
        m2.put("FStatus", "CANCELLED");
        records.add(m2);

        Map<String, Object> m3 = new LinkedHashMap<>();
        m3.put("FID", 1003);
        m3.put("FReqDate", "2025-07-22T10:00:00");
        m3.put("FReqOrgId", "ORG002");
        m3.put("FReqDeptId", "DEPT001");
        m3.put("FNote", "紧急补货申请");
        m3.put("FCancelled", false);
        m3.put("FStatus", "RELEASED");
        records.add(m3);

        return records;
    }

    private static Map<String, String> entry(String src, String tgt) {
        Map<String, String> e = new HashMap<>();
        e.put("source_field", src);
        e.put("target_field", tgt);
        return e;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> buildKingdeePayload(Map<String, Object> record, JobConfig config) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("FReqDate", record.get("FReqDate"));
        header.put("FReqOrgId", record.get("FReqOrgId"));
        header.put("FReqDeptId", record.get("FReqDeptId"));
        header.put("FNote", record.get("FNote"));
        header.put("_localBillId", record.get("FID"));
        header.put("_empty_detail_", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("Header", header);
        body.put("Entries", Collections.emptyList());
        return body;
    }
}
