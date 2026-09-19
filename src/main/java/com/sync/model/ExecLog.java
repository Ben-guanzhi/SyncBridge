package com.sync.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 执行日志实体 */
@TableName("sync_exec_log")
public class ExecLog {
    private Long id;
    private Long jobId;
    private Long configId;
    private String stage;
    private String batchNo;
    private Integer status;
    private Long rowsRead;
    private Long rowsWritten;
    private Long rowsFiltered;
    private String errorMessage;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    private Long durationMs;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getRowsRead() { return rowsRead; }
    public void setRowsRead(Long rowsRead) { this.rowsRead = rowsRead; }
    public Long getRowsWritten() { return rowsWritten; }
    public void setRowsWritten(Long rowsWritten) { this.rowsWritten = rowsWritten; }
    public Long getRowsFiltered() { return rowsFiltered; }
    public void setRowsFiltered(Long rowsFiltered) { this.rowsFiltered = rowsFiltered; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
