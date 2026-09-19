-- SQLite 兼容版建表语句（用于生产环境）
-- 适用于 SQLite 3.x

CREATE TABLE IF NOT EXISTS sync_datasource (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(128) NOT NULL UNIQUE,
    type INTEGER NOT NULL,
    host VARCHAR(256),
    port INTEGER,
    database_name VARCHAR(128),
    username VARCHAR(128),
    password VARCHAR(512),
    extra_config TEXT,
    base_url VARCHAR(1024),
    auth_method VARCHAR(32),
    auth_token VARCHAR(512),
    headers TEXT,
    max_connections INTEGER DEFAULT 10,
    timeout_ms INTEGER DEFAULT 10000,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_job (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(512),
    source_ds_id INTEGER NOT NULL,
    target_ds_id INTEGER NOT NULL,
    sync_mode INTEGER NOT NULL,
    schedule_cron VARCHAR(64),
    incremental_key VARCHAR(128),
    status INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_source_ds ON sync_job(source_ds_id);
CREATE INDEX IF NOT EXISTS idx_job_target_ds ON sync_job(target_ds_id);

CREATE TABLE IF NOT EXISTS sync_job_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    item_order INTEGER DEFAULT 0,
    source_table VARCHAR(128),
    source_condition TEXT,
    source_columns TEXT,
    target_table VARCHAR(128),
    target_write_mode INTEGER DEFAULT 1,
    upsert_keys TEXT,
    field_mappings TEXT NOT NULL,
    is_master INTEGER DEFAULT 0,
    master_detail_relations TEXT,
    nested_json_path VARCHAR(512),
    page_size INTEGER DEFAULT 100,
    pagination_key VARCHAR(64),
    max_pages INTEGER DEFAULT 50,
    last_sync_value VARCHAR(512),
    incremental_window_days INTEGER DEFAULT 7,
    enabled INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_config_job_id ON sync_job_config(job_id);

CREATE TABLE IF NOT EXISTS sync_exec_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    config_id INTEGER,
    stage VARCHAR(32),
    batch_no VARCHAR(64),
    status INTEGER NOT NULL,
    rows_read INTEGER DEFAULT 0,
    rows_written INTEGER DEFAULT 0,
    rows_filtered INTEGER DEFAULT 0,
    error_message TEXT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    duration_ms INTEGER
);

CREATE INDEX IF NOT EXISTS idx_exec_log_job_id ON sync_exec_log(job_id);
CREATE INDEX IF NOT EXISTS idx_exec_log_batch_no ON sync_exec_log(batch_no);
