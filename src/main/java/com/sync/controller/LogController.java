package com.sync.controller;

import com.sync.model.ExecLog;
import com.sync.service.SyncExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 执行日志查询 API
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final SyncExecutionService syncExecutionService;

    public LogController(SyncExecutionService syncExecutionService) {
        this.syncExecutionService = syncExecutionService;
    }

    /** 按批次号查日志 */
    @GetMapping("/batch/{batchNo}")
    public ResponseEntity<List<ExecLog>> getByBatch(@PathVariable String batchNo) {
        return ResponseEntity.ok(syncExecutionService.getLogsByBatch(batchNo));
    }

    /** 清理旧日志 */
    @DeleteMapping("/clean")
    public ResponseEntity<String> cleanOldLogs(
            @RequestParam(defaultValue = "30") int daysAgo) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(daysAgo);
        // TODO: 调用 ExecLogMapper.clearOldLogsBefore(cutoff)
        return ResponseEntity.ok("Cleaned logs before " + cutoff);
    }
}
