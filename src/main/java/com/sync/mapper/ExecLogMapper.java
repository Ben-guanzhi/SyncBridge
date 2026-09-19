package com.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sync.model.ExecLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行日志 Mapper
 */
@Mapper
public interface ExecLogMapper extends BaseMapper<ExecLog> {

    /** 按批次号查询 */
    List<ExecLog> findByBatchNo(@Param("batchNo") String batchNo);

    /** 按时间段统计失败日志 */
    long countFailedBetween(@Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    /** 清理 N 天前的历史日志 */
    int clearOldLogsBefore(@Param("before") LocalDateTime before);
}
