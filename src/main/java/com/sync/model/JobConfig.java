package com.sync.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 同步任务配置实体 - 对应 sync_job_config 表 */
@TableName("sync_job_config")
public class JobConfig {
    private Long id;
    private Long jobId;

    @TableField("item_order")
    private Integer itemOrder;

    // 源侧
    private String sourceTable;
    private String sourceCondition;

    @TableField(value = "source_columns", typeHandler = com.sync.config.JsonTypeHandler.class)
    private List<String> sourceColumns;

    // 目标侧
    private String targetTable;

    @TableField("target_write_mode")
    private Integer targetWriteMode;

    @TableField(value = "upsert_keys", typeHandler = com.sync.config.JsonTypeHandler.class)
    private List<String> upsertKeys;

    // 字段映射
    @TableField(value = "field_mappings", typeHandler = com.sync.config.JsonTypeHandler.class)
    private List<Map<String, String>> fieldMappings;

    // 主明细
    @TableField("is_master")
    private Boolean isMaster;

    @TableField(value = "master_detail_relations", typeHandler = com.sync.config.JsonTypeHandler.class)
    private List<Map<String, Object>> masterDetailRelations;

    @TableField("nested_json_path")
    private String nestedJsonPath;

    // 分页
    @TableField("page_size")
    private Integer pageSize;

    @TableField("pagination_key")
    private String paginationKey;

    @TableField("max_pages")
    private Integer maxPages;

    // 增量
    private String incrementalKey;

    @TableField("last_sync_value")
    private String lastSyncValue;

    @TableField("incremental_window_days")
    private Integer incrementalWindowDays;

    // 运行时
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Integer getItemOrder() { return itemOrder; }
    public void setItemOrder(Integer itemOrder) { this.itemOrder = itemOrder; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getSourceCondition() { return sourceCondition; }
    public void setSourceCondition(String sourceCondition) { this.sourceCondition = sourceCondition; }
    public List<String> getSourceColumns() { return sourceColumns; }
    public void setSourceColumns(List<String> sourceColumns) { this.sourceColumns = sourceColumns; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
    public Integer getTargetWriteMode() { return targetWriteMode; }
    public void setTargetWriteMode(Integer targetWriteMode) { this.targetWriteMode = targetWriteMode; }
    public List<String> getUpsertKeys() { return upsertKeys; }
    public void setUpsertKeys(List<String> upsertKeys) { this.upsertKeys = upsertKeys; }
    public List<Map<String, String>> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(List<Map<String, String>> fieldMappings) { this.fieldMappings = fieldMappings; }
    public Boolean getIsMaster() { return isMaster; }
    public void setIsMaster(Boolean isMaster) { this.isMaster = isMaster; }
    public List<Map<String, Object>> getMasterDetailRelations() { return masterDetailRelations; }
    public void setMasterDetailRelations(List<Map<String, Object>> masterDetailRelations) { this.masterDetailRelations = masterDetailRelations; }
    public String getNestedJsonPath() { return nestedJsonPath; }
    public void setNestedJsonPath(String nestedJsonPath) { this.nestedJsonPath = nestedJsonPath; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public String getPaginationKey() { return paginationKey; }
    public void setPaginationKey(String paginationKey) { this.paginationKey = paginationKey; }
    public Integer getMaxPages() { return maxPages; }
    public void setMaxPages(Integer maxPages) { this.maxPages = maxPages; }
    public String getIncrementalKey() { return incrementalKey; }
    public void setIncrementalKey(String incrementalKey) { this.incrementalKey = incrementalKey; }
    public String getLastSyncValue() { return lastSyncValue; }
    public void setLastSyncValue(String lastSyncValue) { this.lastSyncValue = lastSyncValue; }
    public Integer getIncrementalWindowDays() { return incrementalWindowDays; }
    public void setIncrementalWindowDays(Integer incrementalWindowDays) { this.incrementalWindowDays = incrementalWindowDays; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
