package de.zuordner.importengine;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ExcelCsvImporter {

    public List<String> getSheetNames(InputStream inputStream, String filename) throws Exception {
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return List.of("CSV Data");
        }

        List<String> sheets = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetName(i));
            }
        }
        return sheets;
    }

    public TableData readTablePreview(InputStream inputStream, String filename, String sheetName, int headerRowIndex, int maxPreviewRows) throws Exception {
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            return readCsvTable(inputStream, headerRowIndex, maxPreviewRows);
        } else {
            return readExcelTable(inputStream, sheetName, headerRowIndex, maxPreviewRows);
        }
    }

    private TableData readCsvTable(InputStream inputStream, int headerRowIndex, int maxPreviewRows) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        if (bytes.length == 0) {
            return new TableData("CSV Data", new ArrayList<>(), new ArrayList<>());
        }

        // Try UTF-8 first, fallback to ISO-8859-1 only if replacement character \uFFFD is present
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.contains("\uFFFD")) {
            content = new String(bytes, StandardCharsets.ISO_8859_1);
        }

        // Strip UTF-8 Byte Order Mark (BOM) if present (in UTF-8 or Windows-1252/ISO-8859-1 format)
        content = content.replace("\uFEFF", "").replace("\u00EF\u00BB\u00BF", "").replace("ï»¿", "");

        // Detect CSV Delimiter (; vs , vs \t vs |)
        char delimiter = detectCsvDelimiter(content);

        CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).build();
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new StringReader(content)).withCSVParser(parser).build()) {
            List<String[]> allLines = reader.readAll();
            if (allLines.isEmpty()) {
                return new TableData("CSV Data", headers, rows);
            }

            int lineCount = 0;
            for (int i = 0; i < allLines.size(); i++) {
                String[] line = allLines.get(i);
                if (i == headerRowIndex) {
                    for (int c = 0; c < line.length; c++) {
                        String rawVal = line[c] != null ? line[c].replace("\uFEFF", "").replace("\u00EF\u00BB\u00BF", "").replace("ï»¿", "").trim() : "";
                        String colName = !rawVal.isEmpty() ? rawVal : "Spalte " + (c + 1);
                        headers.add(colName);
                    }
                } else if (i > headerRowIndex) {
                    if (maxPreviewRows > 0 && lineCount >= maxPreviewRows) {
                        break;
                    }
                    // Filter empty lines
                    boolean hasContent = false;
                    List<String> rowVals = new ArrayList<>();
                    for (String val : line) {
                        if (val != null && !val.trim().isEmpty()) {
                            hasContent = true;
                        }
                        rowVals.add(val != null ? val.trim() : "");
                    }
                    if (hasContent) {
                        rows.add(rowVals);
                        lineCount++;
                    }
                }
            }
        }

        return new TableData("CSV Data", headers, rows);
    }

    private char detectCsvDelimiter(String content) {
        if (content == null || content.isEmpty()) return ',';

        String[] lines = content.split("\r?\n");
        if (lines.length == 0) return ',';

        String firstLine = lines[0];
        long semicolons = firstLine.chars().filter(ch -> ch == ';').count();
        long commas = firstLine.chars().filter(ch -> ch == ',').count();
        long tabs = firstLine.chars().filter(ch -> ch == '\t').count();
        long pipes = firstLine.chars().filter(ch -> ch == '|').count();

        if (semicolons >= commas && semicolons >= tabs && semicolons >= pipes && semicolons > 0) {
            return ';';
        }
        if (tabs >= commas && tabs >= pipes && tabs > 0) {
            return '\t';
        }
        if (pipes >= commas && pipes > 0) {
            return '|';
        }
        return ',';
    }

    private TableData readExcelTable(InputStream inputStream, String sheetName, int headerRowIndex, int maxPreviewRows) throws Exception {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = (sheetName != null && !sheetName.isEmpty()) ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow != null) {
                for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                    Cell cell = headerRow.getCell(c);
                    String val = getCellValueAsString(cell);
                    String colName = (val != null && !val.trim().isEmpty()) ? val.trim() : "Spalte " + (c + 1);
                    headers.add(colName);
                }
            }

            int rowCounter = 0;
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                if (maxPreviewRows > 0 && rowCounter >= maxPreviewRows) {
                    break;
                }

                List<String> rowVals = new ArrayList<>();
                boolean hasContent = false;

                int colCount = headers.size() > 0 ? headers.size() : row.getLastCellNum();
                for (int c = 0; c < colCount; c++) {
                    Cell cell = row.getCell(c);
                    String val = getCellValueAsString(cell);
                    if (val != null && !val.trim().isEmpty()) {
                        hasContent = true;
                    }
                    rowVals.add(val != null ? val.trim() : "");
                }

                if (hasContent) {
                    rows.add(rowVals);
                    rowCounter++;
                }
            }
        }

        return new TableData(sheetName != null ? sheetName : "Sheet1", headers, rows);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }
}
