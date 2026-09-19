-- 示例数据：插入测试用数据源和任务（MySQL）
-- 执行前请确保表结构已创建

-- 1. 源数据源：本地 MySQL
INSERT INTO sync_datasource (name, type, host, port, database_name, username, password, max_connections, timeout_ms)
VALUES ('ERP-Source-MySQL', 1, '127.0.0.1', 3306, 'erp_source', 'sync_user', ENCRYPT('xyz123'), 5, 5000);

-- 2. 目标数据源：金蝶云星空 API
INSERT INTO sync_datasource (name, type, base_url, auth_method, auth_token, headers, timeout_ms)
VALUES ('Kingdee-Cloud-API', 2, 'https://api.kingdee.com/cloud/api', 'bearer', 
        ENCRYPT('token_abc'), '{"Content-Type":"application/json","accept":"application/json"}', 30000);

-- 获取上面两行 ID
SET @source_id = LAST_INSERT_ID() - 1;
SET @target_id = LAST_INSERT_ID();

-- 3. 同步任务：采购申请单 DB→API
INSERT INTO sync_job (name, description, source_ds_id, target_ds_id, sync_mode, incremental_key, status)
VALUES ('PUR-REQ-Sync-to-Kingdee', '本地采购申请单 → 金蝶云星空', @source_id, @target_id, 2, 'FReqDate', 1);

SET @job_id = LAST_INSERT_ID();

-- 4. 任务配置段
INSERT INTO sync_job_config (
    job_id, item_order, source_table, source_condition, incremental_key,
    target_table, target_write_mode, field_mappings,
    is_master, master_detail_relations, nested_json_path, page_size
) VALUES (
    @job_id, 1,
    'PUR_REQ_HEAD',
    "Aviator:$FCancelled == false && $FStatus == 'RELEASED'",
    'FReqDate',
    'vap/bos/CDR/KDCKF9040/PUR_REQ_CREATE',
    1, -- INSERT
    '[{"source_field":"FReqDate","target_field":"Header.FReqDate"},{"source_field":"FReqOrgId","target_field":"Header.FReqOrgId"},{"source_field":"FID","target_field":"_localBillId"}]',
    1, -- isMaster
    '[{"detail_table":"PUR_REQ_ENTRY","detail_condition":"entry.FEntryHeadId = {FID}","detail_columns":["FMaterialId","FPurchaseQuantity","FEntryUnitPrice"]}]',
    'Entries',
    100
);
