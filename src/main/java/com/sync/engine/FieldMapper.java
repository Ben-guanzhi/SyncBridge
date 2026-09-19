package com.sync.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 字段映射转换器
 * 根据 field_mappings 配置，将源字段重命名为目标字段并可选地应用 Aviator 转换表达式
 */
public class FieldMapper {

    private static final Logger log = LoggerFactory.getLogger(FieldMapper.class);

    /**
     * 批量映射转换
     * @param records 输入记录
     * @param mappings 映射配置 [{source_field, target_field, transform}]
     * @return 转换后的记录
     */
    public static List<Map<String, Object>> map(List<Map<String, Object>> records,
                                                 List<Map<String, String>> mappings) {
        if (records.isEmpty() || mappings == null || mappings.isEmpty()) {
            return records;
        }

        List<Map<String, Object>> result = new ArrayList<>(records.size());

        for (Map<String, Object> record : records) {
            Map<String, Object> mapped = new LinkedHashMap<>();

            for (Map<String, String> mapping : mappings) {
                String srcField = mapping.get("source_field");
                String tgtField = mapping.get("target_field");
                String transform = mapping.getOrDefault("transform", "");

                if (srcField == null || !record.containsKey(srcField)) {
                    mapped.put(tgtField != null ? tgtField : srcField, null);
                    continue;
                }

                Object value = record.get(srcField);

                // 有 Aviator 转换表达式则执行
                if (AviatorFilter.hasTransform(transform)) {
                    // 构造上下文：注入全量字段 + _value 快捷引用
                    Map<String, Object> context = new HashMap<>(record);
                    context.put("_value", value);
                    context.put("$value", value);

                    try {
                        value = AviatorFilter.transform(context, transform);
                    } catch (Exception e) {
                        log.warn("Field transform failed for field={}, expr={}: {}", srcField, transform, e.getMessage());
                        value = record.get(srcField);
                    }
                }

                mapped.put(tgtField != null ? tgtField : srcField, value);
            }

            result.add(mapped);
        }

        log.debug("FieldMapper: {} records × {} mappings", records.size(), mappings.size());
        return result;
    }
}
