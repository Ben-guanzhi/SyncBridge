package com.sync.controller;

import com.sync.model.DatasourceConfig;
import com.sync.service.DatasourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理 API
 */
@RestController
@RequestMapping("/api/datasources")
public class DatasourceController {

    private final DatasourceService datasourceService;

    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    /** 分页查询（可用 MyBatis-Plus 分页插件） */
    @GetMapping
    public ResponseEntity<List<DatasourceConfig>> list() {
        return ResponseEntity.ok(datasourceService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatasourceConfig> get(@PathVariable Long id) {
        return ResponseEntity.of(java.util.Optional.ofNullable(
                datasourceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<DatasourceConfig> create(@RequestBody DatasourceConfig ds) {
        datasourceService.save(ds);
        return ResponseEntity.status(201).body(ds);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody DatasourceConfig ds) {
        // 修改配置后使旧连接池失效
        datasourceService.invalidatePool(id);
        ds.setId(id);
        datasourceService.updateById(ds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        datasourceService.invalidatePool(id);
        datasourceService.removeById(id);
        return ResponseEntity.noContent().build();
    }

    /** 手动测试连接 */
    @PostMapping("/{id}/test")
    public ResponseEntity<String> testConnection(@PathVariable Long id) {
        DatasourceConfig ds = datasourceService.getById(id);
        if (ds == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            if (ds.getType() == 1) {
                // DB：尝试获取连接
                var conn = com.sync.reader.impl.DataSourceManager
                        .getDataSource(ds).getConnection();
                conn.close();
                return ResponseEntity.ok("Connection OK");
            } else if (ds.getType() == 3 || ds.getType() == 4) {
                // 文件类型：检查文件是否存在
                java.io.File file = new java.io.File(ds.getFilePath());
                if (file.exists()) {
                    return ResponseEntity.ok("File exists: " + file.getAbsolutePath());
                } else {
                    return ResponseEntity.badRequest().body("File not found: " + ds.getFilePath());
                }
            } else {
                return ResponseEntity.ok("HTTP API - cannot test DB-style connection");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Connection failed: " + e.getMessage());
        }
    }
}
