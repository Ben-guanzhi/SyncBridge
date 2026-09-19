package com.sync.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sync.enums.DataSourceType;
import com.sync.mapper.DatasourceMapper;
import com.sync.model.DatasourceConfig;
import com.sync.reader.impl.DataSourceManager;
import com.sync.service.DatasourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据源服务实现
 */
@Service
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, DatasourceConfig>
        implements DatasourceService {

    private static final Logger log = LoggerFactory.getLogger(DatasourceServiceImpl.class);

    @Override
    public DatasourceConfig findByName(String name) {
        return getBaseMapper().findByName(name);
    }

    @Override
    public List<DatasourceConfig> findAllEnabled() {
        return getBaseMapper().findAllEnabled();
    }

    @Override
    public void invalidatePool(Long datasourceId) {
        // 仅当数据源类型为 DATABASE 时才关闭连接池
        DatasourceConfig ds = getById(datasourceId);
        if (ds != null && DataSourceType.fromCode(ds.getType()).isDatabase()) {
            DataSourceManager.closePool(datasourceId);
            log.info("Invalidated datasource pool: id={}", datasourceId);
        }
    }
}
