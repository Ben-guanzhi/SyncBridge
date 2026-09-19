package com.sync.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * 文件数据预览控制器
 * 支持 Excel 和 SQL 文件的临时读取和预览
 */
@RestController
@RequestMapping("/api/file-preview")
public class FileDataPreviewController {

    private static final Logger log = LoggerFactory.getLogger(FileDataPreviewController.class);

    @Value("${spring.sync.file.upload-dir:~/.syncbridge/uploads}")
    private String uploadDir;

    @Value("${spring.sync.file.preview-limit:100}")
    private int previewLimit;

    /**
     * 上传文件并预览
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }

        try {
            // 创建上传目录
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            // 保存文件
            String originalName = file.getOriginalFilename();
            String ext = FileUtil.extName(originalName);
            String savedFileName = UUID.randomUUID().toString() + "." + ext;
            File savedFile = new File(uploadDirFile, savedFileName);
            file.transferTo(savedFile);

            log.info("文件已上传: {} -> {}", originalName, savedFile.getAbsolutePath());

            // 读取数据
            List<Map<String, Object>> records = readFromFile(savedFile, sheetName, limit);

            // 获取列名
            List<String> columns = new ArrayList<>();
            if (!records.isEmpty()) {
                columns.addAll(records.get(0).keySet());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", originalName);
            result.put("savedPath", savedFile.getAbsolutePath());
            result.put("columns", columns);
            result.put("totalRows", records.size());
            result.put("previewRows", Math.min(records.size(), limit));
            result.put("records", records);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "文件上传失败: " + e.getMessage()));
        }
    }

    /**
     * 预览已有文件
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewFile(
            @RequestParam("path") String filePath,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {

        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Map<String, Object>> records = readFromFile(file, sheetName, limit);

            List<String> columns = new ArrayList<>();
            if (!records.isEmpty()) {
                columns.addAll(records.get(0).keySet());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("fileName", file.getName());
            result.put("filePath", filePath);
            result.put("columns", columns);
            result.put("totalRows", records.size());
            result.put("previewRows", Math.min(records.size(), limit));
            result.put("records", records);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("预览文件失败", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "预览失败: " + e.getMessage()));
        }
    }

    /**
     * 从文件中读取数据
     */
    private List<Map<String, Object>> readFromFile(File file, String sheetName, int limit) {
        String ext = FileUtil.extName(file.getName()).toLowerCase();
        List<Map<String, Object>> records;

        if ("xlsx".equals(ext) || "xls".equals(ext)) {
            records = readExcel(file, sheetName);
        } else if ("sql".equals(ext)) {
            records = readSqlFile(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }

        // 限制返回行数
        if (limit > 0 && records.size() > limit) {
            records = records.subList(0, limit);
        }

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
     * 读取 SQL 文件
     */
    private List<Map<String, Object>> readSqlFile(File file) {
        List<Map<String, Object>> records = new ArrayList<>();
        // SQL 文件读取逻辑（简化版 - 解析 INSERT 语句）
        try {
            String content = cn.hutool.core.io.FileUtil.readString(file, "UTF-8");
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

    /**
     * 创建 Workbook
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
    private String getCellValueAsString(Cell cell) {
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
    private Object getCellValue(Cell cell) {
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
}
