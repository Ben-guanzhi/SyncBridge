package com.sync.reader.impl;

import cn.hutool.core.util.StrUtil;
import com.sync.engine.ApiPaginationFetcher;
import com.sync.enums.DataSourceType;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.reader.DataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP API读取器 - 通过OkHttp分页拉取远程API数据
 * 支持：Bearer/Basic认证、JSONPath解析、分页循环拉取
 */
public class ApiReader implements DataReader {

    private static final Logger log = LoggerFactory.getLogger(ApiReader.class);
    private final ApiHttpClient httpClient;

    public ApiReader() {
        this.httpClient = new ApiHttpClient();
    }

    @Override
    public List<Map<String, Object>> readPage(Job job, JobConfig config, String lastSyncValue) {
        if (job.getSourceDatasource() == null) {
            throw new IllegalStateException("源数据源未加载");
        }

        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>(
                Optional.ofNullable(job.getSourceDatasource().getHeaders()).orElse(new HashMap<>()));

        // 增量条件 → 查询参数
        if (StrUtil.isNotBlank(config.getIncrementalKey()) && StrUtil.isNotBlank(lastSyncValue)) {
            queryParams.put(config.getIncrementalKey(), lastSyncValue);
        }

        // 分页参数
        int pageSize = Optional.ofNullable(config.getPageSize()).orElse(100);
        String paginationKey = Optional.ofNullable(config.getPaginationKey()).orElse("offset");
        queryParams.put(paginationKey, "0");
        queryParams.put("pageSize", String.valueOf(pageSize));

        String url = job.getSourceDatasource().getBaseUrl() + "/" + config.getSourceTable();

        log.info("ApiReader fetching: {} | params: {}", url, queryParams);

        // 检查是否需要多页拉取（page_size < total 或配置了 maxPages）
        Integer maxPages = Optional.ofNullable(config.getMaxPages()).orElse(50);
        boolean multiPageFetch = pageSize < 1000 || "cursor".equalsIgnoreCase(paginationKey);

        List<Map<String, Object>> allRecords;
        if (multiPageFetch) {
            // 使用分页循环拉取器
            ApiPaginationFetcher fetcher = new ApiPaginationFetcher(httpClient);
            Map<String, String> multiPageParams = buildMultiPageParams(config, queryParams);
            Map<String, Object> response = httpClient.get(url, headers, multiPageParams);
            if (StrUtil.isNotBlank(config.getNestedJsonPath())) {
                response = httpClient.parseJsonPath(response, config.getNestedJsonPath());
            }
            allRecords = fetcher.fetchAll(url, headers, config, maxPages);
        } else {
            // 单页拉取（兼容原有逻辑）
            Map<String, Object> response = httpClient.get(url, headers, queryParams);
            if (StrUtil.isNotBlank(config.getNestedJsonPath())) {
                response = httpClient.parseJsonPath(response, config.getNestedJsonPath());
            }
            allRecords = extractRecords(response);
        }

        log.info("ApiReader read {} records", allRecords.size());
        return allRecords;
    }

    /** 为多页拉取构建初始查询参数 */
    private Map<String, String> buildMultiPageParams(JobConfig config, Map<String, String> baseParams) {
        Map<String, String> params = new HashMap<>(baseParams);
        // 移除 offset=0，由 ApiPaginationFetcher 自己管理分页
        String paginationKey = Optional.ofNullable(config.getPaginationKey()).orElse("offset");
        if (!"cursor".equalsIgnoreCase(paginationKey)) {
            params.remove(paginationKey);
            params.remove("pageSize");
        }
        return params;
    }

    /** 从通用响应中提取记录列表 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecords(Map<String, Object> response) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (String key : new String[]{"data", "items", "records", "list", "rows"}) {
            Object val = response.get(key);
            if (val instanceof List) {
                for (Object item : (List<?>) val) {
                    if (item instanceof Map) {
                        result.add((Map<String, Object>) item);
                    }
                }
            }
        }

        // 兜底：只有一层且值是数组
        if (result.isEmpty() && response.size() == 1) {
            Object onlyVal = response.values().iterator().next();
            if (onlyVal instanceof List) {
                for (Object item : (List<?>) onlyVal) {
                    if (item instanceof Map) {
                        result.add((Map<String, Object>) item);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getSourceDatasource() == null) return false;
        return DataSourceType.fromCode(job.getSourceDatasource().getType()).isApi();
    }
}
