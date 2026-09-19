package com.sync.reader.impl;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp客户端封装 - 统一处理HTTP请求、JSON解析、JSONPath取值
 */
public class ApiHttpClient {

    private static final Logger log = LoggerFactory.getLogger(ApiHttpClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final OkHttpClient client;

    public ApiHttpClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** GET请求，返回Map结构 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String url, Map<String, String> headers, Map<String, String> params) {
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) throw new IllegalArgumentException("Invalid URL: " + url);
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return executeGet(requestBuilder.build());
    }

    /** POST请求，返回Map结构 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> post(String url, Map<String, String> headers, Map<String, Object> body) {
        RequestBody requestBody = RequestBody.create(
                JSONUtil.toJsonStr(body),
                MediaType.get("application/json; charset=utf-8"));

        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return executePost(requestBuilder.build());
    }

    private Map<String, Object> executeGet(Request request) {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String text = body != null ? body.string() : "{}";
            log.debug("GET {} → {} | body: {}", request.url(), response.code(), text);
            JsonNode node = MAPPER.readTree(text);
            return convertToMap(node);
        } catch (IOException e) {
            throw new RuntimeException("GET request failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> executePost(Request request) {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            String text = body != null ? body.string() : "{}";
            log.debug("POST {} → {} | body: {}", request.url(), response.code(), text);
            JsonNode node = MAPPER.readTree(text);
            return convertToMap(node);
        } catch (IOException e) {
            throw new RuntimeException("POST request failed: " + e.getMessage(), e);
        }
    }

    /** JSONPath解析：取嵌套节点的Map形式 */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJsonPath(Map<String, Object> source, String jsonPath) {
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
            throw new RuntimeException("JSONPath parse failed: " + e.getMessage(), e);
        }
    }

    /** JsonNode → Map 转换 */
    private Map<String, Object> convertToMap(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> map.put(e.getKey(), convertNode(e.getValue())));
            return map;
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(n -> list.add(convertNode(n)));
            return Map.of("__array__", list);
        }
        return Map.of("value", convertNode(node));
    }

    private Object convertNode(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble() || node.isFloatingPointNumber()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNull()) return null;
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            node.fields().forEachRemaining(e -> map.put(e.getKey(), convertNode(e.getValue())));
            return map;
        }
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            node.forEach(n -> list.add(convertNode(n)));
            return list;
        }
        return node.asText();
    }
}
