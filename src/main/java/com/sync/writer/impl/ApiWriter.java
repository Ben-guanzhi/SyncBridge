package com.sync.writer.impl;

import com.sync.enums.DataSourceType;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.reader.impl.ApiHttpClient;
import com.sync.writer.DataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/** HTTP API写入器 - 通过OkHttp推送到远程API */
public class ApiWriter implements DataWriter {

    private static final Logger log = LoggerFactory.getLogger(ApiWriter.class);
    private final ApiHttpClient httpClient = new ApiHttpClient();

    @Override
    public int batchWrite(Job job, JobConfig config, List<Map<String, Object>> records) {
        if (job.getTargetDatasource() == null) {
            throw new IllegalStateException("目标数据源未加载");
        }

        String url = job.getTargetDatasource().getBaseUrl() + "/" + config.getTargetTable();
        Map<String, String> headers = Optional.ofNullable(job.getTargetDatasource().getHeaders())
                .orElse(new HashMap<>());

        applyAuth(job.getTargetDatasource(), headers);

        Map<String, Object> body = config.getNestedJsonPath() != null && !config.getNestedJsonPath().isEmpty()
                ? buildNestedBody(records, config)
                : new HashMap<>(Map.of("data", records));

        log.info("ApiWriter POST: {} | recordCount: {}", url, records.size());
        httpClient.post(url, headers, body);

        return records.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildNestedBody(List<Map<String, Object>> records, JobConfig config) {
        if (records.isEmpty()) return Collections.emptyMap();

        Map<String, Object> master = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);

            if (i == 0) {
                for (Map.Entry<String, Object> e : record.entrySet()) {
                    if (!e.getKey().startsWith("_")) master.put(e.getKey(), e.getValue());
                }
            }

            Map<String, Object> detailRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : record.entrySet()) {
                if (e.getKey().startsWith("_")) {
                    detailRow.put(e.getKey().substring(1), e.getValue());
                }
            }
            if (!detailRow.isEmpty()) details.add(detailRow);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.putAll(master);
        body.put(config.getNestedJsonPath(), details);
        return body;
    }

    private void applyAuth(com.sync.model.DatasourceConfig ds, Map<String, String> headers) {
        String method = ds.getAuthMethod();
        if ("bearer".equalsIgnoreCase(method) && ds.getAuthToken() != null) {
            headers.put("Authorization", "Bearer " + ds.getAuthToken());
        } else if ("basic".equalsIgnoreCase(method)) {
            String auth = ds.getUsername() + ":" + ds.getPassword();
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + encoded);
        } else if ("apikey".equalsIgnoreCase(method) && ds.getAuthToken() != null) {
            headers.put("x-api-key", ds.getAuthToken());
        }
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getTargetDatasource() == null) return false;
        return DataSourceType.fromCode(job.getTargetDatasource().getType()).isApi();
    }
}
