package com.sync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sync.model.Job;
import com.sync.model.JobConfig;

import java.util.List;

/**
 * 同步任务服务接口
 */
public interface JobService extends IService<Job> {

    /** 查询启用的定时任务 */
    List<Job> findEnabledScheduledJobs();

    /** 加载完整任务（含配置 + 关联数据源） */
    Job loadFullJob(Long jobId);

    /** 获取任务的所有配置项 */
    List<JobConfig> getConfigsByJobId(Long jobId);
}
