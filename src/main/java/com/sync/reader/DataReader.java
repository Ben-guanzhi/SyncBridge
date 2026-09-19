package com.sync.reader;

import com.sync.model.JobConfig;
import com.sync.model.Job;

import java.util.List;
import java.util.Map;

/**
 * 数据读取器抽象接口
 * 所有读取器实现此接口，由 SyncEngine 统一调度
 */
public interface DataReader {

    /**
     * 分页拉取源数据
     * @param job 同步任务上下文（包含job-level配置）
     * @param config 当前段配置（source_table, condition, pagination等）
     * @param lastSyncValue 上次的增量位点，用于增量同步
     * @return 单页数据列表
     */
    List<Map<String, Object>> readPage(Job job, JobConfig config, String lastSyncValue);

    /**
     * 判断是否支持该任务的源类型
     */
    boolean supports(Job job, JobConfig config);

    /**
     * 是否需要执行增量拼接（由具体实现决定何时推定位点）
     * true = 引擎负责推进 last_sync_value；false = 内部自己处理
     */
    default boolean manageIncrementalPosition() {
        return true;
    }
}
