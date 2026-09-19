package com.sync.reader.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sync.enums.DataSourceType;
import com.sync.model.Job;
import com.sync.model.JobConfig;
import com.sync.reader.DataReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * 文件数据读取器 - 支持 Excel 和 SQL 文件
 */
public class FileDataReader implements DataReader {

    private static final Logger log = LoggerFactory.getLogger(FileDataReader.class);

    @Override
    public List<Map<String, Object>> readPage(Job job, JobConfig config, String lastSyncValue) {
        if (job.getSourceDatasource() == null) {
            throw new IllegalStateException("源数据源未加载");
        }

        DatasourceConfig sourceDs = job.getSourceDatasource();
        String filePath = sourceDs.getFilePath();

        if (StrUtil.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        String ext = FileUtil.extName(filePath).toLowerCase();
        List<Map<String, Object>> records;

        if ("xlsx".equals(ext) || "xls".equals(ext)) {
            records = readExcel(file, sourceDs.getSheetName());
        } else if ("sql".equals(ext)) {
            records = readSqlFile(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }

        log.info("FileDataReader read {} records from {}", records.size(), filePath);
        return records;
    }

    /**
     * 读取 Excel 文件
     */
    private List<Map<String, Object>> readExcel(File file, String sheetName) {
        List<Map<String, Object>> records = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = createWorkbook(fis, file.getName())) {

            Sheet sheet = StrUtil.isNotBlank(sheetName)
                    ? workbook.getSheet(sheetName)
                    : workbook.getSheetAt(0);

            if (sheet == null) {
                throw new IllegalArgumentException("工作表不存在: " + sheetName);
            }

            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return records;
            }

            List<String> columns = new ArrayList<>();
            for (Cell cell : headerRow) {
                columns.add(getCellValueAsString(cell));
            }

            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> record = new LinkedHashMap<>();
                for (int j = 0; j < columns.size(); j++) {
                    Cell cell = row.getCell(j);
                    record.put(columns.get(j), getCellValue(cell));
                }
                records.add(record);
            }

        } catch (Exception e) {
            throw new RuntimeException("读取 Excel 文件失败: " + e.getMessage(), e);
        }

        return records;
    }

    /**
     * 读取 SQL 文件（解析 INSERT 语句）
     */
    private List<Map<String, Object>> readSqlFile(File file) {
        List<Map<String, Object>> records = new ArrayList<>();

        try {
            String content = FileUtil.readString(file, "UTF-8");
            String[] lines = content.split(";");

            for (String line : lines) {
                line = line.trim();
                if (!line.toUpperCase().startsWith("INSERT")) continue;

                // 提取列名和值
                int parenStart = line.indexOf("(");
                int parenEnd = line.lastIndexOf(")");
                if (parenStart < 0 || parenEnd < 0) continue;

                String colsStr = line.substring(parenStart + 1, parenEnd);
                String[] cols = colsStr.split(",");

                // 找 VALUES 部分
                int valuesIdx = line.toUpperCase().indexOf("VALUES");
                if (valuesIdx < 0) continue;

                String valuesStr = line.substring(valuesIdx + 6).trim();
                // 去掉括号
                if (valuesStr.startsWith("(")) valuesStr = valuesStr.substring(1);
                if (valuesStr.endsWith(")")) valuesStr = valuesStr.substring(0, valuesStr.length() - 1);

                String[] values = valuesStr.split(",");

                if (cols.length == values.length) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    for (int i = 0; i < cols.length; i++) {
                        record.put(cols[i].trim(), cleanSqlValue(values[i].trim()));
                    }
                    records.add(record);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取 SQL 文件失败: " + e.getMessage(), e);
        }

        return records;
    }

    /**
     * 创建 Workbook（支持 xls 和 xlsx）
     */
    private Workbook createWorkbook(FileInputStream fis, String filename) throws Exception {
        if (filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filename.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        }
        return new XSSFWorkbook(fis);
    }

    /**
     * 获取单元格值（转换为字符串）
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return "";
        }
    }

    /**
     * 获取单元格值（转换为对象）
     */
    public static Object getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                }
                double num = cell.getNumericCellValue();
                return num == (long) num ? (long) num : num;
            case BOOLEAN: return cell.getBooleanCellValue();
            case FORMULA: return null;
            default: return null;
        }
    }

    /**
     * 清理 SQL 值
     */
    private Object cleanSqlValue(String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        try {
            if (value.matches("-?\\d+")) {
                return Long.parseLong(value);
            }
            if (value.matches("-?\\d+\\.\\d+")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return value;
    }

    @Override
    public boolean supports(Job job, JobConfig config) {
        if (job.getSourceDatasource() == null) return false;
        DataSourceType type = DataSourceType.fromCode(job.getSourceDatasource().getType());
        return type.isFile();
    }
}
