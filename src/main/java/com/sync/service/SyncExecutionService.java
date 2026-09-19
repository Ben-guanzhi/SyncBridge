package com.sync.service;

import com.sync.engine.SyncEngine;
import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.Job;
import com.sync.model.ExecLog;

/**
 * 同步执行服务接口
 */
public interface SyncExecutionService {

    /**
     * 手动触发一次同步
     * @param jobId 任务ID
     * @return 执行结果
     */
    SyncResult executeSync(Long jobId);

    /**
     * 触发全部启用的定时任务
     */
    void executeAllScheduledJobs();

    /**
     * 按批次号查询日志
     */
    java.util.List<ExecLog> getLogsByBatch(String batchNo);
}
