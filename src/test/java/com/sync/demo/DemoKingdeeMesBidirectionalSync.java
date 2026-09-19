package com.sync.demo;

import cn.hutool.json.JSONUtil;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.*;

import java.util.*;

/**
 * 金蝶云星空 ↔ MES 双向同步 Demo
 *
 * 对应参考项目：
 * - D:\OLP\jindie_api\src\main\java\com\olp\kingdee\mes
 * - D:\OLP\jindie_api\金蝶云星空单据同步统计.md
 *
 * 功能：
 * 1. MES → 金蝶：8种业务单据同步（生产订单、生产领料、产品入库等）
 * 2. 金蝶 → MES：14种主数据同步（客户、供应商、物料、BOM等）
 * 3. 单据状态同步：提交、审核、删除、结案
 * 4. 统计功能：成功/失败/跳过数量和成功率
 */
public class DemoKingdeeMesBidirectionalSync {

    // ==================== 枚举定义 ====================

    /** 金蝶单据类型枚举 */
    enum BillTypeEnum {
        SCDD("生产订单", "PRD_MO"),
        FBDC("分步式调出单", "STK_TRANSFEROUT"),
        SCLL("生产领料", "PRD_PickMtrl"),
        CPRK("产品入库", "PRD_INSTOCK"),
        SCTL("生产退料", "PRD_ReturnMtrl"),
        ZJDB("直接调拨单", "STK_TransferDirect"),
        SCBL("生产补料", "PRD_FeedMtrl"),
        CGSQ("采购申请单", "PUR_Requisition");

        final String name;
        final String formId;

        BillTypeEnum(String name, String formId) {
            this.name = name;
            this.formId = formId;
        }
    }

    /** 主数据同步任务枚举 */
    enum MasterDataSyncEnum {
        BD_Customer("客户", "BD_Customer", "mes_md_client"),
        BD_Supplier("供应商", "BD_Supplier", "mes_md_vendor"),
        BD_CustomerType("客户分类", "BD_CustomerType", "mes_md_client_category"),
        BD_SupplierType("供应商分类", "BD_SupplierType", "mes_md_vendor_category"),
        BD_Material("物料档案", "BD_Material", "mes_md_item"),
        BD_MaterialType("物料分类", "BD_MaterialType", "mes_md_item_type"),
        BOM_Bom("BOM", "BOM_Bom", "mes_md_product_bom"),
        BD_UnitMeasure("计量单位", "BD_UnitMeasure", "mes_md_unit_measure"),
        STK_Stock("仓库", "STK_Stock", "mes_wm_warehouse"),
        STK_StockType("仓库分类", "STK_StockType", "mes_wm_warehouse_class"),
        BD_BatchMainFile("批号", "BD_BatchMainFile", "mes_wm_batch"),
        STK_Inventory("即时库存", "STK_Inventory", "mes_wm_material_stock"),
        PUR_PurchaseOrder("采购订单", "PUR_PurchaseOrder", "mes_purchase_order"),
        BD_Emp("员工账号", "BD_Emp", "mes_employee_account");

        final String name;
        final String kingdeeFormId;
        final String targetTable;

        MasterDataSyncEnum(String name, String kingdeeFormId, String targetTable) {
            this.name = name;
            this.kingdeeFormId = kingdeeFormId;
            this.targetTable = targetTable;
        }
    }

    /** 金蝶 API 配置 */
    static class KingdeeApiConfig {
        String baseUrl = "https://api.kingdee.com/cloud/api";
        String authMethod = "bearer";
        String authToken = "encrypted_kingdee_token";
        int timeoutMs = 30000;
        Map<String, String> headers = new HashMap<>();
    }

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  金蝶云星空 ↔ MES 双向同步 Demo");
        System.out.println("========================================\n");

        // ==========================
        // 1. 金蝶 → MES 主数据同步
        // ==========================
        System.out.println("【第一部分】金蝶 → MES 主数据同步");
        System.out.println("----------------------------------------");
        syncMasterDataFromKingdee();

        // ==========================
        // 2. MES → 金蝶 业务单据同步
        // ==========================
        System.out.println("\n【第二部分】MES → 金蝶 业务单据同步");
        System.out.println("----------------------------------------");
        syncBusinessBillsToKingdee();

        // ==========================
        // 3. 单据状态同步
        // ==========================
        System.out.println("\n【第三部分】单据状态同步");
        System.out.println("----------------------------------------");
        syncBillStatus();

        // ==========================
        // 4. 同步统计
        // ==========================
        System.out.println("\n【第四部分】同步统计报告");
        System.out.println("----------------------------------------");
        printSyncStats();

        System.out.println("\n========================================");
        System.out.println("  Demo 完成！");
        System.out.println("========================================");
    }

    // ==================== 金蝶 → MES 主数据同步 ====================

    /**
     * 执行所有主数据同步任务
     * 对应参考项目：MesSyncScheduler
     */
    private static void syncMasterDataFromKingdee() {
        KingdeeApiConfig kingdeeApi = buildKingdeeApiConfig();
        DatasourceConfig kingdeeDs = buildKingdeeDatasource(kingdeeApi);
        DatasourceConfig mesDs = buildMesDatasource();

        int totalSuccess = 0;
        int totalFailed = 0;

        for (MasterDataSyncEnum task : MasterDataSyncEnum.values()) {
            System.out.println("\n[主数据同步] " + task.name + " (来源: " + task.kingdeeFormId + ")");

            try {
                // 模拟从金蝶 API 拉取数据
                List<Map<String, Object>> records = fetchMasterDataFromKingdee(task);
                System.out.println("  [READ] 从金蝶读取 " + records.size() + " 条记录");

                // 模拟写入 MES 数据库
                int written = writeToMesDatabase(task.targetTable, records);
                System.out.println("  [WRITE] 写入 MES 表 " + task.targetTable + " " + written + " 条记录");

                totalSuccess++;
                System.out.println("  ✓ 同步成功");
            } catch (Exception e) {
                totalFailed++;
                System.out.println("  ✗ 同步失败: " + e.getMessage());
            }
        }

        System.out.println("\n[主数据同步] 完成: 成功 " + totalSuccess + ", 失败 " + totalFailed);
    }

    /**
     * 从金蝶 API 拉取主数据
     */
    private static List<Map<String, Object>> fetchMasterDataFromKingdee(MasterDataSyncEnum task) {
        List<Map<String, Object>> records = new ArrayList<>();

        switch (task) {
            case BD_Customer:
                records.add(createMockCustomer("CUST001", "深圳科技有限公司"));
                records.add(createMockCustomer("CUST002", "广州贸易有限公司"));
                break;
            case BD_Supplier:
                records.add(createMockSupplier("SUP001", "华为技术有限公司"));
                records.add(createMockSupplier("SUP002", "阿里巴巴集团"));
                break;
            case BD_Material:
                records.add(createMockMaterial("MAT001", "原材料A"));
                records.add(createMockMaterial("MAT002", "原材料B"));
                records.add(createMockMaterial("MAT003", "成品C"));
                break;
            case BD_UnitMeasure:
                records.add(createMockUnit("UNIT001", "个"));
                records.add(createMockUnit("UNIT002", "件"));
                break;
            case STK_Stock:
                records.add(createMockWarehouse("WH001", "深圳仓库"));
                records.add(createMockWarehouse("WH002", "广州仓库"));
                break;
            default:
                records.add(new HashMap<>()); // 空记录表示模拟成功
        }

        return records;
    }

    /**
     * 写入 MES 数据库
     */
    private static int writeToMesDatabase(String targetTable, List<Map<String, Object>> records) {
        // 模拟写入数据库
        return records.size();
    }

    // ==================== MES → 金蝶 业务单据同步 ====================

    /**
     * 执行所有业务单据同步任务
     * 对应参考项目：MesToKingdeeSyncScheduler
     */
    private static void syncBusinessBillsToKingdee() {
        KingdeeApiConfig kingdeeApi = buildKingdeeApiConfig();
        DatasourceConfig kingdeeDs = buildKingdeeDatasource(kingdeeApi);
        DatasourceConfig mesDs = buildMesDatasource();

        int totalSuccess = 0;
        int totalFailed = 0;
        List<BillSyncStats> billStats = new ArrayList<>();

        for (BillTypeEnum billType : BillTypeEnum.values()) {
            System.out.println("\n[业务单据同步] " + billType.name + " (表单ID: " + billType.formId + ")");

            try {
                // 模拟从 MES 读取单据
                List<Map<String, Object>> records = fetchBillsFromMes(billType);
                System.out.println("  [READ] 从 MES 读取 " + records.size() + " 条单据");

                // 模拟推送到金蝶 API
                int sent = sendToKingdee(billType.formId, records);
                System.out.println("  [WRITE] 推送到金蝶 " + sent + " 条单据");

                totalSuccess++;
                billStats.add(new BillSyncStats(billType.name, records.size(), sent, 0));
                System.out.println("  ✓ 同步成功");
            } catch (Exception e) {
                totalFailed++;
                billStats.add(new BillSyncStats(billType.name, 0, 0, 1));
                System.out.println("  ✗ 同步失败: " + e.getMessage());
            }
        }

        System.out.println("\n[业务单据同步] 完成: 成功 " + totalSuccess + ", 失败 " + totalFailed);
    }

    /**
     * 从 MES 读取业务单据
     */
    private static List<Map<String, Object>> fetchBillsFromMes(BillTypeEnum billType) {
        List<Map<String, Object>> records = new ArrayList<>();

        switch (billType) {
            case SCDD:
                records.add(createMockSaleOrder("SCDD001", "生产订单001", 15000.00));
                records.add(createMockSaleOrder("SCDD002", "生产订单002", 8500.50));
                break;
            case SCLL:
                records.add(createMockMaterialIssue("SCLL001", "领料单001", 100));
                break;
            case CPRK:
                records.add(createMockProductReceipt("CPRK001", "入库单001", 500));
                break;
            default:
                records.add(new HashMap<>());
        }

        return records;
    }

    /**
     * 推送到金蝶 API
     */
    private static int sendToKingdee(String formId, List<Map<String, Object>> records) {
        // 模拟推送到金蝶 API
        return records.size();
    }

    // ==================== 单据状态同步 ====================

    /**
     * 执行单据状态同步任务
     */
    private static void syncBillStatus() {
        System.out.println("\n[状态同步] 单据提交状态同步...");
        syncBillStatusTask("提交", 20);

        System.out.println("\n[状态同步] 单据审核状态同步...");
        syncBillStatusTask("审核", 20);

        System.out.println("\n[状态同步] 单据删除同步...");
        syncBillStatusTask("删除", 60);

        System.out.println("\n[状态同步] 生产订单结案/完工同步...");
        syncBillStatusTask("结案", 120);
    }

    /**
     * 执行单据状态同步任务
     */
    private static void syncBillStatusTask(String actionType, int intervalSeconds) {
        try {
            // 模拟同步状态
            List<Map<String, Object>> statusUpdates = fetchStatusUpdates(actionType);
            System.out.println("  [READ] 读取 " + statusUpdates.size() + " 条状态变更");

            int updated = updateBillStatus(actionType, statusUpdates);
            System.out.println("  [WRITE] 更新 " + updated + " 条状态");

            System.out.println("  ✓ " + actionType + " 状态同步完成 (间隔: " + intervalSeconds + "s)");
        } catch (Exception e) {
            System.out.println("  ✗ " + actionType + " 状态同步失败: " + e.getMessage());
        }
    }

    /**
     * 获取状态更新列表
     */
    private static List<Map<String, Object>> fetchStatusUpdates(String actionType) {
        List<Map<String, Object>> updates = new ArrayList<>();

        if ("提交".equals(actionType)) {
            updates.add(createStatusUpdate("SCDD001", "待提交", "已提交"));
            updates.add(createStatusUpdate("SCDD002", "待提交", "已提交"));
        } else if ("审核".equals(actionType)) {
            updates.add(createStatusUpdate("SCDD001", "已提交", "已审核"));
        } else if ("删除".equals(actionType)) {
            updates.add(createStatusUpdate("SCDD003", "已审核", "已删除"));
        } else if ("结案".equals(actionType)) {
            updates.add(createStatusUpdate("SCDD001", "已审核", "已结案"));
        }

        return updates;
    }

    /**
     * 更新单据状态
     */
    private static int updateBillStatus(String actionType, List<Map<String, Object>> updates) {
        return updates.size();
    }

    // ==================== 统计报告 ====================

    /**
     * 打印同步统计报告
     */
    private static void printSyncStats() {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                      同步统计报告                           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│ 【金蝶 → MES 主数据同步】                                   │");
        System.out.println("│   客户同步:    ✓ 成功                                       │");
        System.out.println("│   供应商同步:  ✓ 成功                                       │");
        System.out.println("│   物料档案:    ✓ 成功 (3条)                                 │");
        System.out.println("│   BOM同步:     ✓ 成功                                       │");
        System.out.println("│   仓库同步:    ✓ 成功 (2条)                                 │");
        System.out.println("│   计量单位:    ✓ 成功 (2条)                                 │");
        System.out.println("│   ... (共14个主数据任务)                                    │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│ 【MES → 金蝶 业务单据同步】                                 │");
        System.out.println("│   生产订单 (SCDD):   ✓ 成功 (2条)                          │");
        System.out.println("│   分步式调出单 (FBDC): ✓ 成功                               │");
        System.out.println("│   生产领料 (SCLL):   ✓ 成功 (1条)                          │");
        System.out.println("│   产品入库 (CPRK):   ✓ 成功 (1条)                          │");
        System.out.println("│   生产退料 (SCTL):   ✓ 成功                               │");
        System.out.println("│   直接调拨单 (ZJDB): ✓ 成功                               │");
        System.out.println("│   生产补料 (SCBL):   ✓ 成功                               │");
        System.out.println("│   采购申请单 (CGSQ): ✓ 成功                               │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│ 【单据状态同步】                                             │");
        System.out.println("│   提交状态同步:  ✓ 成功 (2条)                              │");
        System.out.println("│   审核状态同步:  ✓ 成功 (1条)                              │");
        System.out.println("│   删除同步:      ✓ 成功 (1条)                              │");
        System.out.println("│   结案同步:      ✓ 成功 (1条)                              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│ 【统计汇总】                                                 │");
        System.out.println("│   主数据同步:  14/14 成功 (100%)                          │");
        System.out.println("│   业务单据:    8/8 成功 (100%)                            │");
        System.out.println("│   状态同步:    4/4 成功 (100%)                            │");
        System.out.println("│   总计:       26/26 成功 (100%)                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建金蝶 API 数据源配置
     */
    private static DatasourceConfig buildKingdeeDatasource(KingdeeApiConfig config) {
        DatasourceConfig ds = new DatasourceConfig();
        ds.setId(2000L);
        ds.setName("Kingdee-Cloud-API");
        ds.setType(2);
        ds.setBaseUrl(config.baseUrl);
        ds.setAuthMethod(config.authMethod);
        ds.setAuthToken(config.authToken);
        ds.setTimeoutMs(config.timeoutMs);
        ds.setHeaders(config.headers);
        return ds;
    }

    /**
     * 构建 MES 数据库数据源配置
     */
    private static DatasourceConfig buildMesDatasource() {
        DatasourceConfig ds = new DatasourceConfig();
        ds.setId(2100L);
        ds.setName("MES-Database");
        ds.setType(1);
        ds.setHost("127.0.0.1");
        ds.setPort(3306);
        ds.setDatabaseName("mes_db");
        ds.setUsername("mes_user");
        ds.setPassword("encrypted_mes_pwd");
        ds.setMaxConnections(10);
        return ds;
    }

    /**
     * 构建金蝶 API 配置
     */
    private static KingdeeApiConfig buildKingdeeApiConfig() {
        KingdeeApiConfig config = new KingdeeApiConfig();
        config.headers.put("Content-Type", "application/json");
        config.headers.put("Accept", "application/json");
        return config;
    }

    // ==================== 模拟数据创建方法 ====================

    private static Map<String, Object> createMockCustomer(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("FNumber", code);
        record.put("FName", name);
        record.put("FCreatorId", "admin");
        record.put("FCreateDate", "2025-07-20");
        return record;
    }

    private static Map<String, Object> createMockSupplier(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("FNumber", code);
        record.put("FName", name);
        record.put("FCreatorId", "admin");
        record.put("FCreateDate", "2025-07-20");
        return record;
    }

    private static Map<String, Object> createMockMaterial(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("FNumber", code);
        record.put("FName", name);
        record.put("FModel", "MODEL-" + code);
        record.put("FUnitId", "UNIT001");
        return record;
    }

    private static Map<String, Object> createMockUnit(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("FNumber", code);
        record.put("FName", name);
        return record;
    }

    private static Map<String, Object> createMockWarehouse(String code, String name) {
        Map<String, Object> record = new HashMap<>();
        record.put("FNumber", code);
        record.put("FName", name);
        record.put("FAddress", "地址" + code);
        return record;
    }

    private static Map<String, Object> createMockSaleOrder(String billNo, String name, double amount) {
        Map<String, Object> record = new HashMap<>();
        record.put("FBillNo", billNo);
        record.put("FName", name);
        record.put("FAmount", amount);
        record.put("FStatus", "RELEASED");
        record.put("FCreateTime", "2025-07-20T10:00:00");
        return record;
    }

    private static Map<String, Object> createMockMaterialIssue(String billNo, String name, int quantity) {
        Map<String, Object> record = new HashMap<>();
        record.put("FBillNo", billNo);
        record.put("FName", name);
        record.put("FQuantity", quantity);
        record.put("FStatus", "APPROVED");
        return record;
    }

    private static Map<String, Object> createMockProductReceipt(String billNo, String name, int quantity) {
        Map<String, Object> record = new HashMap<>();
        record.put("FBillNo", billNo);
        record.put("FName", name);
        record.put("FQuantity", quantity);
        record.put("FStatus", "APPROVED");
        return record;
    }

    private static Map<String, Object> createStatusUpdate(String billNo, String oldStatus, String newStatus) {
        Map<String, Object> update = new HashMap<>();
        update.put("FBillNo", billNo);
        update.put("FOldStatus", oldStatus);
        update.put("FNewStatus", newStatus);
        update.put("FUpdateTime", "2025-07-22T15:00:00");
        return update;
    }

    /** 单据同步统计 */
    static class BillSyncStats {
        final String billType;
        final int readCount;
        final int writeCount;
        final int failed;

        BillSyncStats(String billType, int readCount, int writeCount, int failed) {
            this.billType = billType;
            this.readCount = readCount;
            this.writeCount = writeCount;
            this.failed = failed;
        }
    }
}
