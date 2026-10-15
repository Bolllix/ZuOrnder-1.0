package de.zuordner.importengine;

import java.util.HashMap;
import java.util.Map;

/**
 * Beschreibung einer Spaltenzuordnung (welche Spalte -> welche Eigenschaft).
 * Hinweis: Wird aktuell vom Frontend nicht benutzt – der ImportController
 * nimmt die Zuordnung direkt als JSON-Text bzw. als TableMappingRequest entgegen.
 */
public class ColumnMappingRequest {
    private String sheetName;
    private int headerRowIndex = 0; // 0-based
    private Map<String, String> columnToAttributeMap; // e.g. "Spalte A" / "Column 0" -> "lastname"

    public ColumnMappingRequest() {
        this.columnToAttributeMap = new HashMap<>();
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    public Map<String, String> getColumnToAttributeMap() {
        return columnToAttributeMap;
    }

    public void setColumnToAttributeMap(Map<String, String> columnToAttributeMap) {
        this.columnToAttributeMap = columnToAttributeMap;
    }
}
