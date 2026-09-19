package com.sync.enums;

/**
 * 同步模式枚举
 * 1=DB→DB, 2=DB→API, 3=API→DB, 4=API→API
 * 5=文件→DB, 6=文件→API, 7=DB→文件, 8=文件→文件
 */
public enum SyncMode {

    DB_TO_DB(1, "数据库到数据库"),
    DB_TO_API(2, "数据库到HTTP API"),
    API_TO_DB(3, "HTTP API到数据库"),
    API_TO_API(4, "HTTP API到HTTP API"),
    FILE_TO_DB(5, "文件到数据库"),
    FILE_TO_API(6, "文件到HTTP API"),
    DB_TO_FILE(7, "数据库到文件"),
    FILE_TO_FILE(8, "文件到文件");

    private final int code;
    private final String description;

    SyncMode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SyncMode fromCode(int code) {
        for (SyncMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown sync mode code: " + code);
    }

    /** 源是否为数据源（HTTP API） */
    public boolean sourceIsApi() {
        return this == DB_TO_API || this == API_TO_DB || this == API_TO_API;
    }

    /** 目标是否为数据源（HTTP API） */
    public boolean targetIsApi() {
        return this == DB_TO_API || this == API_TO_DB || this == API_TO_API;
    }
}
