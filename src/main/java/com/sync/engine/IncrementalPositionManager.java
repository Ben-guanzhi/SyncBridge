package com.sync.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 增量位点管理器
 *
 * 核心规则：
 *   - 记录全部被过滤时，禁止推进 last_sync_value
 *   - 仅当有数据真正写入后才更新位点
 *   - 支持 ISO时间戳、Long时间戳、字符串字典序三种比较方式
 */
public class IncrementalPositionManager {

    private static final Logger log = LoggerFactory.getLogger(IncrementalPositionManager.class);

    /**
     * 尝试推进增量位点
     * @param currentLastSyncValue 当前已记录的位点
     * @param newCandidate 新的候选位点值
     * @param hasSuccessfulWrite 是否成功写入了数据
     * @return 新位点（若未推进则返回原值）
     */
    public String tryAdvance(String currentLastSyncValue, String newCandidate, boolean hasSuccessfulWrite) {
        if (!hasSuccessfulWrite) {
            log.debug("Skip incremental position advance: no successful write. current={}, candidate={}",
                    currentLastSyncValue, newCandidate);
            return currentLastSyncValue;
        }

        if (currentLastSyncValue == null || currentLastSyncValue.isEmpty()) {
            log.info("First sync, set initial position to {}", newCandidate);
            return newCandidate;
        }

        if (isGreaterThan(newCandidate, currentLastSyncValue)) {
            log.info("Incremental position advanced: {} → {}", currentLastSyncValue, newCandidate);
            return newCandidate;
        }

        log.debug("No position change needed. current={}, candidate={}", currentLastSyncValue, newCandidate);
        return currentLastSyncValue;
    }

    /** 从记录列表中提取最新的增量字段值 */
    public String extractLatestFromRecords(Map<String, Object> latestRecord, String incrementKey) {
        if (latestRecord == null || incrementKey == null) return null;
        Object val = latestRecord.get(incrementKey);
        return val != null ? formatForStorage(val) : null;
    }

    /**
     * 判断 candidate > target（自动类型推断）
     */
    public boolean isGreaterThan(String candidate, String target) {
        if (candidate == null || target == null) return false;
        if (candidate.equals(target)) return false;

        // 1) 尝试按 LocalDateTime 比较
        try {
            LocalDateTime c = LocalDateTime.parse(candidate);
            LocalDateTime t = LocalDateTime.parse(target);
            return c.isAfter(t);
        } catch (DateTimeParseException ignored) {
        }

        // 2) 尝试按 Long（时间戳）比较
        try {
            long c = Long.parseLong(candidate);
            long t = Long.parseLong(target);
            return c > t;
        } catch (NumberFormatException ignored) {
        }

        // 3) 兜底：字符串字典序
        return candidate.compareTo(target) > 0;
    }

    /** 统一输出格式 */
    private String formatForStorage(Object value) {
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        if (value instanceof Long) {
            return LocalDateTime.ofEpochSecond((Long) value / 1000, 0, ZoneOffset.ofHours(8))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.toString();
    }

    /** 获取当前时间作为位点 */
    public String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
