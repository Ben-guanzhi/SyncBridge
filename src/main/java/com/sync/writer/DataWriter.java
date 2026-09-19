package com.sync.writer;

import com.sync.model.JobConfig;
import com.sync.model.Job;

import java.util.List;
import java.util.Map;

/**
 * 数据写入器抽象接口
 * 所有写入器实现此接口，由 SyncEngine 统一调度
 */
public interface DataWriter {

    /**
     * 批量写入目标
     * @param job 同步任务上下文
     * @param config 当前段配置
     * @param records 待写入的数据
     * @return 实际写入行数
     */
    int batchWrite(Job job, JobConfig config, List<Map<String, Object>> records);

    /**
     * 判断是否支持该任务的目标类型
     */
    boolean supports(Job job, JobConfig config);
}
