package com.sync.engine;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sync.reader.impl.ApiHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * API分页循环拉取器
 * 职责：
 *   1. 循环调用API直到无下一页数据
 *   2. 支持两种分页策略：offset-based / cursor-based
 *   3. JSONPath提取嵌套数组
 */
public class ApiPaginationFetcher {

    private static final Logger log = LoggerFactory.getLogger(ApiPaginationFetcher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ApiHttpClient httpClient;

    public ApiPaginationFetcher(ApiHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> fetchAll(String url, Map<String, String> headers,
                                               com.sync.model.JobConfig config, int maxPages) {
        List<Map<String, Object>> allRecords = new ArrayList<>();
        int page = 0;
        String nextCursor = null;

        while (page < maxPages) {
            Map<String, String> params = buildPaginationParams(config, page, nextCursor);
            Map<String, Object> response = httpClient.get(url, headers, params);

            if (StrUtil.isNotBlank(config.getNestedJsonPath())) {
                response = parseJsonPath(response, config.getNestedJsonPath());
            }

            List<Map<String, Object>> pageRecords = extractRecords(response);
            if (pageRecords.isEmpty()) {
                log.info("No more records at page {}, stopping", page + 1);
                break;
            }

            allRecords.addAll(pageRecords);
            log.debug("Fetched {} records (total: {})", pageRecords.size(), allRecords.size());

            nextCursor = extractNextCursor(response, config);
            if (StrUtil.isBlank(nextCursor)) {
                int pageSize = Optional.ofNullable(config.getPageSize()).orElse(100);
                if (pageRecords.size() < pageSize) {
                    log.info("Page {} returned fewer than page_size({}), no more pages", page + 1, pageSize);
                    break;
                }
            }
            page++;
        }

        log.info("ApiPaginationFetcher finished: {} total records fetched", allRecords.size());
        return allRecords;
    }

    private Map<String, String> buildPaginationParams(com.sync.model.JobConfig config, int currentPage, String cursor) {
        Map<String, String> params = new HashMap<>();
        String paginationKey = Optional.ofNullable(config.getPaginationKey()).orElse("offset");

        if ("cursor".equalsIgnoreCase(paginationKey) && cursor != null) {
            params.put(paginationKey, cursor);
        } else {
            int pageSize = Optional.ofNullable(config.getPageSize()).orElse(100);
            params.put(paginationKey, String.valueOf(currentPage * pageSize));
            params.put("pageSize", String.valueOf(pageSize));
        }
        return params;
    }

    private String extractNextCursor(Map<String, Object> response, com.sync.model.JobConfig config) {
        for (String key : new String[]{"next_cursor", "cursor", "pageToken", "token"}) {
            Object val = response.get(key);
            if (val != null && StrUtil.isNotBlank(val.toString())) {
                return val.toString();
            }
        }
        return "";
    }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonPath(Map<String, Object> source, String jsonPath) {
        try {
            JsonNode node = MAPPER.valueToTree(source);
            String[] parts = jsonPath.split("\\.");
            JsonNode result = node;
            for (String part : parts) {
                if (part.isEmpty()) continue;
                result = result.path(part);
                if (result.isMissingNode()) {
                    throw new IllegalArgumentException("JSON path not found: " + jsonPath);
                }
            }
            return MAPPER.convertValue(result, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSONPath parse failed: " + jsonPath, e);
        }
    }
}
