package com.sync.service.impl;

import com.sync.engine.IncrementalPositionManager;
import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.mapper.ExecLogMapper;
import com.sync.mapper.JobConfigMapper;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.model.ExecLog;
import com.sync.service.DatasourceService;
import com.sync.service.JobService;
import com.sync.service.SyncExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步执行服务实现
 */
@Service
public class SyncExecutionServiceImpl implements SyncExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SyncExecutionServiceImpl.class);

    private final JobService jobService;
    private final DatasourceService datasourceService;
    private final ExecLogMapper execLogMapper;
    private final JobConfigMapper jobConfigMapper;
    private final IncrementalPositionManager positionManager = new IncrementalPositionManager();
    private final SyncEngine syncEngine = new SyncEngine();

    public SyncExecutionServiceImpl(JobService jobService,
                                    DatasourceService datasourceService,
                                    ExecLogMapper execLogMapper,
                                    JobConfigMapper jobConfigMapper) {
        this.jobService = jobService;
        this.datasourceService = datasourceService;
        this.execLogMapper = execLogMapper;
        this.jobConfigMapper = jobConfigMapper;
    }

    @Override
    public SyncResult executeSync(Long jobId) {
        Job job = jobService.loadFullJob(jobId);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobId);
        }

        log.info("=== Starting sync job: {} (id={}) ===", job.getName(), jobId);
        long start = System.currentTimeMillis();

        try {
            SyncResult result = syncEngine.execute(job);

            // 持久化日志
            saveExecLog(result, jobId);

            // 更新位点到数据库
            updatePositionsFromResult(result, job.getConfigs());

            long duration = System.currentTimeMillis() - start;
            log.info("=== Sync job {} completed in {}ms: {} ===",
                    job.getName(), duration, result.getStatus());
            return result;

        } catch (Exception e) {
            log.error("Sync failed for job id={}, name={}", jobId, job.getName(), e);
            throw new RuntimeException("Sync failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Scheduled(cron = "${sync.scheduler.cron:0 */5 * * * ?}")
    public void executeAllScheduledJobs() {
        try {
            List<Job> jobs = jobService.findEnabledScheduledJobs();
            log.info("Cron trigger: {} scheduled jobs to execute", jobs.size());

            for (Job job : jobs) {
                try {
                    executeSync(job.getId());
                } catch (Exception e) {
                    log.error("Cron execution failed for job id={}, name={}",
                            job.getId(), job.getName(), e);
                }
            }
        } catch (Exception e) {
            log.error("Scheduled job execution failed", e);
        }
    }

    @Override
    public List<ExecLog> getLogsByBatch(String batchNo) {
        return execLogMapper.findByBatchNo(batchNo);
    }

    public SyncEngine getSyncEngine() {
        return syncEngine;
    }

    /** 将 SegmentResult 持久化为 ExecLog */
    private void saveExecLog(SyncResult result, Long jobId) {
        for (SyncEngine.SegmentResult seg : result.getSegmentResults()) {
            ExecLog entry = new ExecLog();
            entry.setJobId(jobId);
            entry.setConfigId(seg.configId);
            entry.setBatchNo(seg.batchNo);
            entry.setStage(seg.status);
            entry.setStatus(1);
            entry.setRowsRead((long) seg.rowsRead);
            entry.setRowsWritten((long) seg.rowsWritten);
            entry.setRowsFiltered(seg.rowsFiltered);
            entry.setStartTime(java.time.LocalDateTime.now());
            entry.setEndTime(java.time.LocalDateTime.now());
            execLogMapper.insert(entry);
        }
    }

    /** 将成功的位点持久化到数据库 */
    private void updatePositionsFromResult(SyncResult result, List<JobConfig> allConfigs) {
        if (allConfigs == null || allConfigs.isEmpty()) return;

        Map<Long, String> updates = new HashMap<>();
        for (SyncEngine.SegmentResult seg : result.getSegmentResults()) {
            if (seg.lastSyncValueUpdated != null && seg.success) {
                updates.put(seg.configId, seg.lastSyncValueUpdated);
            }
        }

        if (!updates.isEmpty()) {
            log.info("Persisting {} incremental position updates", updates.size());
            for (Map.Entry<Long, String> entry : updates.entrySet()) {
                jobConfigMapper.batchUpdateLastSyncValue(
                        Collections.singletonList(entry.getKey()),
                        entry.getValue()
                );
            }
            log.info("Incremental positions persisted successfully");
        }
    }
}
