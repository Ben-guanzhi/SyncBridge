package com.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sync.model.JobConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务配置 Mapper
 */
@Mapper
public interface JobConfigMapper extends BaseMapper<JobConfig> {

    /** 按任务ID查询所有配置项（按 order 排序） */
    List<JobConfig> findByJobId(@Param("jobId") Long jobId);

    /** 批量更新增量位点 */
    int batchUpdateLastSyncValue(@Param("ids") List<Long> ids,
                                 @Param("lastSyncValue") String lastSyncValue);
}
