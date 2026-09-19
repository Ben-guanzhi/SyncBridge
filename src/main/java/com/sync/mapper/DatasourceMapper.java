package com.sync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sync.model.DatasourceConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据源 Mapper
 */
@Mapper
public interface DatasourceMapper extends BaseMapper<DatasourceConfig> {

    /** 按名称查找（唯一索引） */
    DatasourceConfig findByName(@Param("name") String name);

    /** 批量查询启用的数据源 */
    List<DatasourceConfig> findAllEnabled();
}
