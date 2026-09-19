package com.sync.enums;

/**
 * 写入模式枚举
 * 1=INSERT, 2=UPSERT, 3=UPDATE, 4=DELETE
 */
public enum WriteMode {

    INSERT(1, "全量插入"),
    UPSERT(2, "存在则更新，不存在则插入"),
    UPDATE(3, "按条件更新"),
    DELETE(4, "按条件删除");

    private final int code;
    private final String description;

    WriteMode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static WriteMode fromCode(int code) {
        for (WriteMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown write mode code: " + code);
    }
}
