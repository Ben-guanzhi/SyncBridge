-- ============================================================
-- 配置化数据同步平台 - 数据库表结构
-- ============================================================

-- 1. 数据源配置表
CREATE TABLE `sync_datasource` (
    `id`                BIGINT UNSIGNED       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(128)          NOT NULL COMMENT '数据源名称（业务语义，如：OMS_主库）',
    `type`              TINYINT UNSIGNED      NOT NULL COMMENT '类型：1=数据库, 2=HTTP API',
    -- DB类型字段
    `host`              VARCHAR(256)          DEFAULT NULL COMMENT 'DB主机地址',
    `port`              INT UNSIGNED          DEFAULT NULL COMMENT 'DB端口',
    `database_name`     VARCHAR(128)          DEFAULT NULL COMMENT '数据库名',
    `username`          VARCHAR(128)          DEFAULT NULL COMMENT '用户名（加密存储）',
    `password`          VARCHAR(512)          DEFAULT NULL COMMENT '密码（加密存储）',
    `extra_config`      JSON                  DEFAULT NULL COMMENT '额外连接参数（如 charset、sslMode）',
    -- HTTP API类型字段
    `base_url`          VARCHAR(1024)         DEFAULT NULL COMMENT 'API Base URL',
    `auth_method`       VARCHAR(32)           DEFAULT NULL COMMENT '认证方式：bearer / basic / apikey',
    `auth_token`        VARCHAR(512)          DEFAULT NULL COMMENT 'Token / API Key（加密存储）',
    `headers`           JSON                  DEFAULT NULL COMMENT '默认请求头',
    -- 通用
    `max_connections`   INT UNSIGNED          DEFAULT 10 COMMENT '最大连接数（仅DB类型）',
    `timeout_ms`        INT UNSIGNED          DEFAULT 10000 COMMENT '超时毫秒数',
    `enabled`           TINYINT(1)            NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置';


-- 2. 同步任务表（顶层容器，一个任务定义一次端到端同步）
CREATE TABLE `sync_job` (
    `id`                BIGINT UNSIGNED       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`              VARCHAR(256)          NOT NULL COMMENT '任务名称',
    `description`       VARCHAR(512)          DEFAULT NULL COMMENT '任务描述',
    `source_ds_id`      BIGINT UNSIGNED       NOT NULL COMMENT '源数据源ID',
    `target_ds_id`      BIGINT UNSIGNED       NOT NULL COMMENT '目标数据源ID',
    `sync_mode`         TINYINT UNSIGNED      NOT NULL COMMENT '同步模式：1=DB→DB, 2=DB→API, 3=API→DB, 4=API→API',
    `schedule_cron`     VARCHAR(64)           DEFAULT NULL COMMENT 'Cron表达式，NULL=手动触发',
    `incremental_key`   VARCHAR(128)          DEFAULT NULL COMMENT '增量同步字段（如 updated_at）',
    `status`            TINYINT UNSIGNED      NOT NULL DEFAULT 0 COMMENT '状态：0=停用, 1=启用',
    `created_at`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_source_ds` (`source_ds_id`),
    KEY `idx_target_ds` (`target_ds_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步任务';


-- 3. 同步任务配置表（每个 task_item 对应一条"源 → 目标映射"关系）
CREATE TABLE `sync_job_config` (
    `id`                        BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id`                    BIGINT UNSIGNED   NOT NULL COMMENT '所属任务ID',
    `item_order`                INT UNSIGNED      NOT NULL DEFAULT 0 COMMENT '执行顺序（支持多段 pipeline）',
    -- 源侧
    `source_table`              VARCHAR(128)      DEFAULT NULL COMMENT '源表名 / API路径（首段必填）',
    `source_condition`          TEXT              DEFAULT NULL COMMENT 'SQL WHERE / Aviator过滤表达式',
    `source_columns`            JSON              DEFAULT NULL COMMENT '读取列列表，null=SELECT *',
    -- 目标侧
    `target_table`              VARCHAR(128)      DEFAULT NULL COMMENT '目标表名 / API路径',
    `target_write_mode`         TINYINT UNSIGNED  DEFAULT 1 COMMENT '写入模式：1=INSERT, 2=UPSERT, 3=UPDATE, 4=DELETE',
    `upsert_keys`               JSON              DEFAULT NULL COMMENT 'UPSERT唯一键列表',
    -- 字段映射
    `field_mappings`            JSON              NOT NULL COMMENT '字段映射数组 [{source_field, target_field, transform}]',
    -- 主明细配置（当 source_table 是主表时）
    `is_master`                 TINYINT(1)        NOT NULL DEFAULT 0 COMMENT '是否主节点',
    `master_detail_relations`   JSON              DEFAULT NULL COMMENT '[{detail_table, detail_condition, detail_columns}]',
    `nested_json_path`          VARCHAR(512)      DEFAULT NULL COMMENT '嵌套JSON输出路径（API目标用）',
    -- 分页配置
    `page_size`                 INT UNSIGNED      DEFAULT 100 COMMENT '每页条数',
    `pagination_key`            VARCHAR(64)       DEFAULT NULL COMMENT '分页键名（offset / cursor）',
    `max_pages`                 INT UNSIGNED      DEFAULT 50 COMMENT '最大拉取页数（API多页拉取用）',
    -- 增量
    `last_sync_value`           VARCHAR(512)      DEFAULT NULL COMMENT '上次同步增量位点',
    `incremental_window_days`   INT UNSIGNED      DEFAULT 7 COMMENT '增量查询回溯天数',
    -- 运行时
    `enabled`                   TINYINT(1)        NOT NULL DEFAULT 1 COMMENT '是否启用本段配置',
    `created_at`                DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步任务配置';


-- 4. 同步执行日志表
CREATE TABLE `sync_exec_log` (
    `id`                BIGINT UNSIGNED       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `job_id`            BIGINT UNSIGNED       NOT NULL COMMENT '所属任务ID',
    `config_id`         BIGINT UNSIGNED       DEFAULT NULL COMMENT '关联的配置项ID（可为空，表示任务级别日志）',
    `stage`             VARCHAR(32)           DEFAULT NULL COMMENT '阶段：READ / FILTER / MAP / WRITE',
    `batch_no`          VARCHAR(64)           DEFAULT NULL COMMENT '批次编号（UUID），用于关联同一批次的多条日志',
    `status`            TINYINT UNSIGNED      NOT NULL COMMENT '状态：0=进行中, 1=成功, 2=失败',
    `rows_read`         BIGINT UNSIGNED       DEFAULT 0 COMMENT '读取行数',
    `rows_written`      BIGINT UNSIGNED       DEFAULT 0 COMMENT '写入行数',
    `rows_filtered`     BIGINT UNSIGNED       DEFAULT 0 COMMENT '被过滤的行数',
    `error_message`     TEXT                  DEFAULT NULL COMMENT '错误信息',
    `start_time`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time`          DATETIME              DEFAULT NULL,
    `duration_ms`       BIGINT UNSIGNED       DEFAULT NULL COMMENT '耗时（毫秒）',
    `created_at`        DATETIME              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_job_id` (`job_id`),
    KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='同步执行日志';
