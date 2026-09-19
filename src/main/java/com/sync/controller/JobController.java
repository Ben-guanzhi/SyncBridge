package com.sync.controller;

import com.sync.engine.SyncEngine.SyncResult;
import com.sync.model.Job;
import com.sync.service.JobService;
import com.sync.service.SyncExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同步任务管理 API
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final SyncExecutionService syncExecutionService;

    public JobController(JobService jobService, SyncExecutionService syncExecutionService) {
        this.jobService = jobService;
        this.syncExecutionService = syncExecutionService;
    }

    @GetMapping
    public ResponseEntity<List<Job>> list() {
        return ResponseEntity.ok(jobService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> get(@PathVariable Long id) {
        // 返回完整结构（含配置项和数据源信息）
        Job job = jobService.loadFullJob(id);
        return ResponseEntity.of(java.util.Optional.ofNullable(job));
    }

    @PostMapping
    public ResponseEntity<Job> create(@RequestBody Job job) {
        jobService.save(job);
        return ResponseEntity.status(201).body(job);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Job job) {
        job.setId(id);
        jobService.updateById(job);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 手动触发同步
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<SyncResult> execute(@PathVariable Long id) {
        SyncResult result = syncExecutionService.executeSync(id);
        return ResponseEntity.ok(result);
    }

    /**
     * 触发所有定时任务
     */
    @PostMapping("/execute-all")
    public ResponseEntity<String> executeAll() {
        syncExecutionService.executeAllScheduledJobs();
        return ResponseEntity.ok("All scheduled jobs triggered");
    }
}
