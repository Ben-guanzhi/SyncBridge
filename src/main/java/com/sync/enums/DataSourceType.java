package com.sync.enums;

/**
 * 数据源类型枚举
 * 1=数据库, 2=HTTP API, 3=Excel文件, 4=SQL文件
 */
public enum DataSourceType {

    DATABASE(1, "数据库"),
    HTTP_API(2, "HTTP API"),
    EXCEL_FILE(3, "Excel文件"),
    SQL_FILE(4, "SQL文件");

    private final int code;
    private final String description;

    DataSourceType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataSourceType fromCode(int code) {
        for (DataSourceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown datasource type code: " + code);
    }

    /** 是否为API类型 */
    public boolean isApi() {
        return this == HTTP_API;
    }

    /** 是否为数据库类型 */
    public boolean isDatabase() {
        return this == DATABASE;
    }

    /** 是否为文件类型 */
    public boolean isFile() {
        return this == EXCEL_FILE || this == SQL_FILE;
    }
}
