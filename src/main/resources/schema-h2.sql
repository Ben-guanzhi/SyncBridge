-- H2 兼容版建表语句（用于开发/测试）

CREATE TABLE IF NOT EXISTS sync_datasource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    type TINYINT NOT NULL,
    host VARCHAR(256),
    port INTEGER,
    database_name VARCHAR(128),
    username VARCHAR(128),
    password VARCHAR(512),
    extra_config VARCHAR(1024),
    base_url VARCHAR(1024),
    auth_method VARCHAR(32),
    auth_token VARCHAR(512),
    headers VARCHAR(1024),
    max_connections INTEGER DEFAULT 10,
    timeout_ms INTEGER DEFAULT 10000,
    enabled TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(512),
    source_ds_id BIGINT NOT NULL,
    target_ds_id BIGINT NOT NULL,
    sync_mode TINYINT NOT NULL,
    schedule_cron VARCHAR(64),
    incremental_key VARCHAR(128),
    status TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_job_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    item_order INTEGER DEFAULT 0,
    source_table VARCHAR(128),
    source_condition VARCHAR(1024),
    source_columns VARCHAR(1024),
    target_table VARCHAR(128),
    target_write_mode TINYINT DEFAULT 1,
    upsert_keys VARCHAR(1024),
    field_mappings VARCHAR(4096) NOT NULL,
    is_master TINYINT DEFAULT 0,
    master_detail_relations VARCHAR(4096),
    nested_json_path VARCHAR(512),
    page_size INTEGER DEFAULT 100,
    pagination_key VARCHAR(64),
    max_pages INTEGER DEFAULT 50,
    last_sync_value VARCHAR(512),
    incremental_window_days INTEGER DEFAULT 7,
    enabled TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_exec_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    config_id BIGINT,
    stage VARCHAR(32),
    batch_no VARCHAR(64),
    status TINYINT NOT NULL,
    rows_read BIGINT DEFAULT 0,
    rows_written BIGINT DEFAULT 0,
    rows_filtered BIGINT DEFAULT 0,
    error_message VARCHAR(2048),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms BIGINT
);
