package com.sync.demo;

import cn.hutool.json.JSONUtil;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.*;

import java.util.*;

/**
 * 测试Demo：模拟 CloudDataSyncJob 模式
 *
 * 场景描述：
 *   - 模拟用友ERP → MES Cloud 的数据同步
 *   - 支持热更新配置（从配置文件读取）
 *   - 支持增量同步（基于时间游标）
 *   - 支持手动触发
 *
 * 对应参考项目：D:\OLP\olp_cloud\olp-module-yonyou\CloudDataSyncJob.java
 */
public class DemoCloudDataSyncJob {

    // ==================== 配置常量 ====================
    private static final String CRON_PREFIX = "CLOUD_SYNC_CRON_";
    private static final String ENABLED_PREFIX = "CLOUD_SYNC_ENABLED_";
    private static final String LAST_TIME_PREFIX = "sync.";
    private static final String LAST_TIME_SUFFIX = ".last_time";

    // ==================== 同步任务定义 ====================
    static class SyncTaskDef {
        final String name;
        final String cronKey;
        final String enabledKey;
        final String defaultCron;
        final String cursorKey;
        final java.util.function.Function<Job, SyncResult> executor;

        SyncTaskDef(String name, String cronKey, String enabledKey, String defaultCron,
                    String cursorKey, java.util.function.Function<Job, SyncResult> executor) {
            this.name = name;
            this.cronKey = cronKey;
            this.enabledKey = enabledKey;
            this.defaultCron = defaultCron;
            this.cursorKey = cursorKey;
            this.executor = executor;
        }
    }

    // ==================== 模拟配置存储 ====================
    // 实际项目中应从数据库 sys_config 表读取
    private static final Map<String, String> configStore = new HashMap<>();

    static {
        // 初始化默认配置
        configStore.put("CLOUD_SYNC_CRON_UNIT_MEASURE", "120");
        configStore.put("CLOUD_SYNC_ENABLED_UNIT_MEASURE", "true");
        configStore.put("sync.unit_measure.last_time", "1990-01-01 00:00:00");

        configStore.put("CLOUD_SYNC_CRON_ITEM", "60");
        configStore.put("CLOUD_SYNC_ENABLED_ITEM", "true");
        configStore.put("sync.mes_md_item.last_time", "1990-01-01 00:00:00");

        configStore.put("CLOUD_SYNC_CRON_SALE_ORDER", "60");
        configStore.put("CLOUD_SYNC_ENABLED_SALE_ORDER", "true");
        configStore.put("sync.erp_sale_order.last_time", "1990-01-01 00:00:00");
    }

    public static void main(String[] args) {
        System.out.println("=== Cloud Data Sync Job Demo ===\n");

        // ==========================
        // 1. 构建同步任务列表
        // ==========================
        List<SyncTaskDef> tasks = buildSyncTasks();
        System.out.println("[Cloud同步] 定时任务共注册 " + tasks.size() + " 个，独立开关，支持热更新\n");

        // ==========================
        // 2. 执行同步任务
        // ==========================
        for (SyncTaskDef task : tasks) {
            executeTask(task);
        }

        // ==========================
        // 3. 手动触发示例
        // ==========================
        System.out.println("\n--- [MANUAL] 手动触发销售订单同步 ---");
        manualSyncSaleOrder();

        System.out.println("\n=== Demo 完成 ===");
    }

    /**
     * 构建同步任务列表（对应参考项目的 buildSyncTasks()）
     */
    private static List<SyncTaskDef> buildSyncTasks() {
        List<SyncTaskDef> tasks = new ArrayList<>();

        tasks.add(new SyncTaskDef(
                "Cloud计量单位同步",
                "CLOUD_SYNC_CRON_UNIT_MEASURE",
                "CLOUD_SYNC_ENABLED_UNIT_MEASURE",
                "120",
                "sync.unit_measure.last_time",
                DemoCloudDataSyncJob::syncUnitMeasure
        ));

        tasks.add(new SyncTaskDef(
                "Cloud物料档案同步",
                "CLOUD_SYNC_CRON_ITEM",
                "CLOUD_SYNC_ENABLED_ITEM",
                "60",
                "sync.mes_md_item.last_time",
                DemoCloudDataSyncJob::syncItem
        ));

        tasks.add(new SyncTaskDef(
                "Cloud销售订单同步",
                "CLOUD_SYNC_CRON_SALE_ORDER",
                "CLOUD_SYNC_ENABLED_SALE_ORDER",
                "60",
                "sync.erp_sale_order.last_time",
                DemoCloudDataSyncJob::syncSaleOrder
        ));

        return tasks;
    }

    /**
     * 执行单个同步任务（对应参考项目的 executeTask()）
     */
    private static void executeTask(SyncTaskDef task) {
        // 1. 读取开关配置（热更新）
        String enabledValue = configStore.getOrDefault(task.enabledKey, "true");
        if (isDisabled(enabledValue)) {
            System.out.println("[Cloud同步] 任务 [" + task.name + "] 已在配置中被禁用（" + task.enabledKey + "=" + enabledValue + "），跳过本次执行");
            return;
        }

        // 2. 读取调度配置（热更新）
        String cronValue = configStore.getOrDefault(task.cronKey, task.defaultCron);
        if (isStopped(cronValue)) {
            System.out.println("[Cloud同步] 任务 [" + task.name + "] 的调度配置为停止标记（" + cronValue + "），不再调度");
            return;
        }

        // 3. 读取增量游标
        String lastTime = configStore.getOrDefault(task.cursorKey, "1990-01-01 00:00:00");
        System.out.println("[Cloud同步] 开始同步 [" + task.name + "] | 游标: " + lastTime);

        // 4. 执行同步
        long start = System.currentTimeMillis();
        try {
            SyncResult result = task.executor.apply(buildMockJob(task.name));
            long duration = System.currentTimeMillis() - start;
            System.out.println("[Cloud同步] " + task.name + " 同步完成，耗时 " + duration + "ms | 状态: " + result.status);
        } catch (Exception e) {
            System.out.println("[Cloud同步] " + task.name + " 同步失败: " + e.getMessage());
        }
    }

    /**
     * 判断配置是否禁用
     */
    private static boolean isDisabled(String value) {
        if (value == null || value.isEmpty()) return false;
        String lower = value.toLowerCase().trim();
        return lower.equals("false") || lower.equals("0") || lower.equals("no") ||
               lower.equals("off") || lower.equals("disabled") || lower.equals("stop") ||
               lower.equals("pause") || lower.equals("paused");
    }

    /**
     * 判断配置是否为停止标记
     */
    private static boolean isStopped(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase().trim();
        return lower.equals("-") || lower.equals("off") || lower.equals("disabled");
    }

    /**
     * 模拟计量单位同步
     */
    private static SyncResult syncUnitMeasure(Job job) {
        System.out.println("  [READ] 读取计量单位...");
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(createMockRecord("UNIT001", "个", "2025-07-20 10:00:00"));
        records.add(createMockRecord("UNIT002", "件", "2025-07-21 11:00:00"));
        System.out.println("  [READ] 读取 " + records.size() + " 条记录");

        System.out.println("  [WRITE] 写入计量单位...");
        System.out.println("  [WRITE] 写入 " + records.size() + " 条记录");

        // 更新游标
        updateCursor("sync.unit_measure.last_time", "2025-07-21 11:00:00");

        return new SyncResult(job.getName(), UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 模拟物料档案同步
     */
    private static SyncResult syncItem(Job job) {
        System.out.println("  [READ] 读取物料档案...");
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(createMockRecord("ITEM001", "物料A", "2025-07-20 09:00:00"));
        records.add(createMockRecord("ITEM002", "物料B", "2025-07-21 10:30:00"));
        records.add(createMockRecord("ITEM003", "物料C", "2025-07-22 14:15:00"));
        System.out.println("  [READ] 读取 " + records.size() + " 条记录");

        System.out.println("  [WRITE] 写入物料档案...");
        System.out.println("  [WRITE] 写入 " + records.size() + " 条记录");

        // 更新游标
        updateCursor("sync.mes_md_item.last_time", "2025-07-22 14:15:00");

        return new SyncResult(job.getName(), UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 模拟销售订单同步
     */
    private static SyncResult syncSaleOrder(Job job) {
        System.out.println("  [READ] 读取销售订单...");
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(createMockRecord("SO001", "销售订单001", "2025-07-20 08:00:00"));
        records.add(createMockRecord("SO002", "销售订单002", "2025-07-21 09:30:00"));
        System.out.println("  [READ] 读取 " + records.size() + " 条记录");

        System.out.println("  [WRITE] 写入销售订单...");
        System.out.println("  [WRITE] 写入 " + records.size() + " 条记录");

        // 更新游标
        updateCursor("sync.erp_sale_order.last_time", "2025-07-21 09:30:00");

        return new SyncResult(job.getName(), UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 手动触发销售订单同步
     */
    public static void manualSyncSaleOrder() {
        System.out.println("  [MANUAL] 手动触发销售订单同步...");
        Job job = buildMockJob("手动触发-销售订单同步");

        // 重置游标以强制全量同步
        updateCursor("sync.erp_sale_order.last_time", "1990-01-01 00:00:00");

        SyncResult result = syncSaleOrder(job);
        System.out.println("  [MANUAL] 同步完成: " + result.status);
    }

    /**
     * 构建模拟Job
     */
    private static Job buildMockJob(String jobName) {
        Job job = new Job();
        job.setId(UUID.randomUUID().getMostSignificantBits());
        job.setName(jobName);
        job.setDescription("Demo同步任务");
        job.setSyncMode(1); // DB→DB

        DatasourceConfig sourceDs = new DatasourceConfig();
        sourceDs.setId(100L);
        sourceDs.setName("Yonyou-ERP");
        sourceDs.setType(1);
        sourceDs.setHost("127.0.0.1");
        sourceDs.setPort(1433);
        sourceDs.setDatabaseName("YONYOU_ERP");
        job.setSourceDatasource(sourceDs);

        DatasourceConfig targetDs = new DatasourceConfig();
        targetDs.setId(200L);
        targetDs.setName("MES-Cloud");
        targetDs.setType(1);
        targetDs.setHost("127.0.0.1");
        targetDs.setPort(3306);
        targetDs.setDatabaseName("MES_CLOUD");
        job.setTargetDatasource(targetDs);

        return job;
    }

    /**
     * 创建模拟记录
     */
    private static Map<String, Object> createMockRecord(String id, String name, String updateTime) {
        Map<String, Object> record = new HashMap<>();
        record.put("FID", id);
        record.put("FName", name);
        record.put("FUpdateTime", updateTime);
        return record;
    }

    /**
     * 更新游标
     */
    private static void updateCursor(String cursorKey, String newValue) {
        configStore.put(cursorKey, newValue);
        System.out.println("  [CURSOR] 更新游标 " + cursorKey + " = " + newValue);
    }
}
