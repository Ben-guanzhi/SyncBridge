package com.sync.engine;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aviator表达式引擎封装
 * 承载行过滤与字段转换两大职责
 */
public class AviatorFilter {

    /** 前缀标识，区分 Aviator表达式和纯SQL条件 */
    private static final String AVIATOR_PREFIX = "Aviator:";

    /** 表达式缓存（避免每次重复编译） */
    private static final Map<String, Expression> EXPRESSION_CACHE = new HashMap<>();

    /** 判断是否包含Aviator表达式 */
    public static boolean isAviatorExpression(String sourceCondition) {
        return sourceCondition != null && sourceCondition.startsWith(AVIATOR_PREFIX);
    }

    /** 判断是否是需要Aviator转换的映射值 */
    public static boolean hasTransform(String transformExpr) {
        return transformExpr != null && !transformExpr.isEmpty()
                && !transformExpr.equals("identity"); // identity表示不做转换
    }

    /**
     * 根据Aviator表达式过滤行
     * @param records 记录列表
     * @param filterExpr Aviator表达式，如 "Aviator:$status == 'VALID' && $qty > 0"
     * @return 过滤后的记录
     */
    public static List<Map<String, Object>> filter(List<Map<String, Object>> records, String filterExpr) {
        if (!isAviatorExpression(filterExpr)) {
            return records; // 非Aviator条件，跳过（可能在DB层处理）
        }

        String expr = filterExpr.substring(AVIATOR_PREFIX.length()).trim();
        Expression compiled = compile(expr);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> env = buildEnv(record);
            Boolean match = (Boolean) compiled.execute(env);
            if (Boolean.TRUE.equals(match)) {
                result.add(record);
            }
        }

        return result;
    }

    /**
     * 对单条记录的单个字段执行Aviator转换
     * @param record 记录
     * @param transformExpr Aviator表达式，如 "$amount * 1.13" 或 "'KD-' + $billNo"
     * @return 转换后的值
     */
    public static Object transform(Map<String, Object> record, String transformExpr) {
        if (!hasTransform(transformExpr)) {
            return record.get("value") != null ? record.get("value") : "";
        }

        Expression compiled = compile(transformExpr.trim());
        Map<String, Object> env = buildEnv(record);
        Object result = compiled.execute(env);

        // 类型规范化：返回字符串或null
        if (result instanceof Number) {
            Number n = (Number) result;
            // 整数类型返回Integer/Long，小数返回Double
            return n;
        }
        return result != null ? result.toString() : null;
    }

    /** 编译并缓存表达式 */
    private static Expression compile(String expr) {
        return EXPRESSION_CACHE.computeIfAbsent(expr, e ->
                AviatorEvaluator.compile(e, true)); // opt=true 启用JIT编译
    }

    /** 将记录转为Aviator环境Map */
    private static Map<String, Object> buildEnv(Map<String, Object> record) {
        Map<String, Object> env = new HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        return env;
    }
}
