package com.sync.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sync.model.DatasourceConfig;

import java.util.List;

/**
 * 数据源服务接口
 */
public interface DatasourceService extends IService<DatasourceConfig> {

    /** 按名称查找 */
    DatasourceConfig findByName(String name);

    /** 获取所有启用的数据源 */
    List<DatasourceConfig> findAllEnabled();

    /** 销毁指定连接池（重新加载 DS 配置后调用） */
    void invalidatePool(Long datasourceId);
}
