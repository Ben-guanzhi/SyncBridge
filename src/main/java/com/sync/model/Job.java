package com.sync.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.List;

/** 同步任务实体 */
@TableName("sync_job")
public class Job {
    private Long id;
    private String name;
    private String description;
    private Long sourceDsId;
    private Long targetDsId;
    private Integer syncMode;
    private String scheduleCron;
    private String incrementalKey;
    private Integer status;

    @TableField(exist = false)
    private List<JobConfig> configs;

    @TableField(exist = false)
    private DatasourceConfig sourceDatasource;

    @TableField(exist = false)
    private DatasourceConfig targetDatasource;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getSourceDsId() { return sourceDsId; }
    public void setSourceDsId(Long sourceDsId) { this.sourceDsId = sourceDsId; }
    public Long getTargetDsId() { return targetDsId; }
    public void setTargetDsId(Long targetDsId) { this.targetDsId = targetDsId; }
    public Integer getSyncMode() { return syncMode; }
    public void setSyncMode(Integer syncMode) { this.syncMode = syncMode; }
    public String getScheduleCron() { return scheduleCron; }
    public void setScheduleCron(String scheduleCron) { this.scheduleCron = scheduleCron; }
    public String getIncrementalKey() { return incrementalKey; }
    public void setIncrementalKey(String incrementalKey) { this.incrementalKey = incrementalKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<JobConfig> getConfigs() { return configs; }
    public void setConfigs(List<JobConfig> configs) { this.configs = configs; }
    public DatasourceConfig getSourceDatasource() { return sourceDatasource; }
    public void setSourceDatasource(DatasourceConfig sourceDatasource) { this.sourceDatasource = sourceDatasource; }
    public DatasourceConfig getTargetDatasource() { return targetDatasource; }
    public void setTargetDatasource(DatasourceConfig targetDatasource) { this.targetDatasource = targetDatasource; }
}
