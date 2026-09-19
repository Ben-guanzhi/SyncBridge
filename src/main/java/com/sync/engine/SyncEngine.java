package com.sync.engine;

import cn.hutool.core.util.StrUtil;
import com.sync.enums.DataSourceType;
import com.sync.model.*;
import com.sync.reader.DataReader;
import com.sync.reader.impl.ApiReader;
import com.sync.reader.impl.DbReader;
import com.sync.writer.DataWriter;
import com.sync.writer.impl.ApiWriter;
import com.sync.writer.impl.DbWriter;
import com.sync.writer.impl.FileDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 同步引擎核心 - 编排完整数据流链路
 * 标准链路：读取源数据 → 输入端过滤/主明细组装 → 字段映射转换 → 目标写入
 */
public class SyncEngine {

    private static final Logger log = LoggerFactory.getLogger(SyncEngine.class);

    private final List<DataReader> readers = new ArrayList<>();
    private final List<DataWriter> writers = new ArrayList<>();

    public SyncEngine() {
        readers.add(new DbReader());
        readers.add(new ApiReader());
        readers.add(new FileDataReader());
        writers.add(new DbWriter());
        writers.add(new ApiWriter());
        writers.add(new FileDataWriter());
    }

    /** 执行单次同步任务 */
    public SyncResult execute(Job job) {
        long start = System.currentTimeMillis();
        String batchNo = UUID.randomUUID().toString().replace("-", "");
        SyncResult result = new SyncResult(job.getName(), batchNo);

        if (job.getConfigs() == null || job.getConfigs().isEmpty()) {
            log.warn("Job={} has no configs, skipping", job.getName());
            result.status = "SKIPPED";
            return result;
        }

        // 按 itemOrder 排序
        job.getConfigs().sort(Comparator.comparingInt(JobConfig::getItemOrder));

        try {
            for (JobConfig config : job.getConfigs()) {
                if (!Boolean.TRUE.equals(config.getEnabled())) continue;
                result.segmentResults.add(executeSegment(job, config, batchNo));
            }

            result.successCount += result.segmentResults.stream().filter(r -> r.success).count();
            result.failureCount += result.segmentResults.stream().filter(r -> !r.success && r.error != null).count();
            result.status = result.failureCount > 0 ? "PARTIAL_SUCCESS" : "SUCCESS";

        } catch (Exception e) {
            log.error("SyncEngine failed for job={}", job.getName(), e);
            result.status = "FAILED";
            result.errorMessage = e.getMessage();
        } finally {
            result.durationMs = System.currentTimeMillis() - start;
        }

        return result;
    }

    private SegmentResult executeSegment(Job job, JobConfig config, String batchNo) {
        SegmentResult seg = new SegmentResult(config.getId(), batchNo);

        // 1. 路由 Reader/Writer
        DataReader reader = findReader(job, config);
        DataWriter writer = findWriter(job, config);

        if (reader == null) {
            seg.status = "NO_READER";
            seg.error = "No reader supports source type " + (job.getSourceDatasource() != null ? job.getSourceDatasource().getType() : "null");
            return seg;
        }
        if (writer == null) {
            seg.status = "NO_WRITER";
            seg.error = "No writer supports target type " + (job.getTargetDatasource() != null ? job.getTargetDatasource().getType() : "null");
            return seg;
        }

        // 2. READ
        String lastSyncValue = resolveLastSyncValue(config);
        List<Map<String, Object>> rawRecords = reader.readPage(job, config, lastSyncValue);
        seg.rowsRead = rawRecords.size();
        log.debug("[READ] {} records from segment {}", rawRecords.size(), config.getId());

        // 3. 主明细组装
        if (Boolean.TRUE.equals(config.getIsMaster()) && config.getMasterDetailRelations() != null) {
            rawRecords = MasterDetailAssembler.assemble(job, config, rawRecords);
            seg.rowsAfterAssemble = rawRecords.size();
        }

        // 4. FILTER (Aviator)
        String filterExpr = config.getSourceCondition();
        if (AviatorFilter.isAviatorExpression(filterExpr)) {
            List<Map<String, Object>> filtered = AviatorFilter.filter(rawRecords, filterExpr);
            seg.rowsFiltered = rawRecords.size() - filtered.size();
            rawRecords = filtered;
            log.debug("[FILTER] {} remain after filtering", rawRecords.size());
        }

        // 5. MAP & TRANSFORM
        List<Map<String, Object>> mapped = FieldMapper.map(rawRecords, config.getFieldMappings());
        log.debug("[MAP] {} records after field mapping", mapped.size());

        // 【关键】：全部被过滤时，禁止推进增量位点
        if (mapped.isEmpty()) {
            seg.status = "EMPTY_AFTER_FILTER";
            seg.success = true;
            return seg;
        }

        // 6. WRITE
        seg.rowsWritten = writer.batchWrite(job, config, mapped);

        // 仅写入成功后才推进位点
        String latestValue = extractLatestIncrementalValue(mapped, config);
        if (latestValue != null && !Objects.equals(latestValue, lastSyncValue)) {
            config.setLastSyncValue(latestValue);
            seg.lastSyncValueUpdated = latestValue;
        }

        seg.status = "SUCCESS";
        seg.success = true;
        return seg;
    }

    private String resolveLastSyncValue(JobConfig config) {
        if (config.getLastSyncValue() != null) return config.getLastSyncValue();
        if (StrUtil.isNotBlank(config.getIncrementalKey())) return "1970-01-01T00:00:00";
        return "";
    }

    private String extractLatestIncrementalValue(List<Map<String, Object>> records, JobConfig config) {
        String key = config.getIncrementalKey();
        if (key == null) return null;

        Object latest = null;
        for (Map<String, Object> rec : records) {
            Object val = rec.get(key);
            if (val != null && (latest == null || val.toString().compareTo(latest.toString()) > 0)) {
                latest = val;
            }
        }
        return latest != null ? latest.toString() : null;
    }

    private DataReader findReader(Job job, JobConfig config) {
        return readers.stream().filter(r -> r.supports(job, config)).findFirst().orElse(null);
    }

    private DataWriter findWriter(Job job, JobConfig config) {
        return writers.stream().filter(w -> w.supports(job, config)).findFirst().orElse(null);
    }

    public void shutdown() {
        com.sync.reader.impl.DataSourceManager.closeAllPools();
        log.info("SyncEngine shutdown complete");
    }

    // ===== 内部结果类 =====

    public static class SyncResult {
        public final String jobName;
        public final String batchNo;
        public String status = "UNKNOWN";
        public long durationMs;
        public long successCount;
        public long failureCount;
        public String errorMessage;
        public ExecLog execLog;
        public final List<SegmentResult> segmentResults = new ArrayList<>();

        SyncResult(String jobName, String batchNo) {
            this.jobName = jobName;
            this.batchNo = batchNo;
        }

        public String getStatus() { return status; }
        public long getDurationMs() { return durationMs; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public List<SegmentResult> getSegmentResults() { return segmentResults; }

        @Override
        public String toString() {
            return String.format("SyncResult{job=%s, batch=%s, status=%s, duration=%dms, segments=%d}",
                    jobName, batchNo, status, durationMs, segmentResults.size());
        }
    }

    public static class SegmentResult {
        public final Long configId;
        public final String batchNo;
        public String status;
        public boolean success = false;
        public int rowsRead;
        public int rowsAfterAssemble;
        public long rowsFiltered;
        public int rowsWritten;
        public String lastSyncValueUpdated;
        public String error;

        SegmentResult(Long configId, String batchNo) {
            this.configId = configId;
            this.batchNo = batchNo;
        }

        public int getRowsRead() { return rowsRead; }
        public int getRowsWritten() { return rowsWritten; }
        public long getRowsFiltered() { return rowsFiltered; }
        public String getLastSyncValueUpdated() { return lastSyncValueUpdated; }
    }
}
