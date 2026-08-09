package de.zuordner.importengine;

import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ExcelCsvImporter {

    public List<String> getSheetNames(InputStream inputStream, String filename) throws Exception {
        if (filename.toLowerCase().endsWith(".csv")) {
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
        if (filename.toLowerCase().endsWith(".csv")) {
            return readCsvTable(inputStream, headerRowIndex, maxPreviewRows);
        } else {
            return readExcelTable(inputStream, sheetName, headerRowIndex, maxPreviewRows);
        }
    }

    private TableData readCsvTable(InputStream inputStream, int headerRowIndex, int maxPreviewRows) throws Exception {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> allLines = reader.readAll();
            if (allLines.isEmpty()) {
                return new TableData("CSV Data", headers, rows);
            }

            int lineCount = 0;
            for (int i = 0; i < allLines.size(); i++) {
                String[] line = allLines.get(i);
                if (i == headerRowIndex) {
                    for (int c = 0; c < line.length; c++) {
                        String colName = line[c] != null && !line[c].trim().isEmpty() ? line[c].trim() : "Spalte " + (c + 1);
                        headers.add(colName);
                    }
                } else if (i > headerRowIndex) {
                    if (maxPreviewRows > 0 && lineCount >= maxPreviewRows) {
                        break;
                    }
                    rows.add(Arrays.asList(line));
                    lineCount++;
                }
            }
        }

        return new TableData("CSV Data", headers, rows);
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
