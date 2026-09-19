package com.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sync.model.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 同步任务 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {

    /**
     * 查询启用的定时任务（schedule_cron != null）
     */
    List<Job> findEnabledScheduledJobs();

    /**
     * 按 ID 获取任务及其所有配置
     */
    Job selectWithConfigs(@Param("id") Long id);
}
