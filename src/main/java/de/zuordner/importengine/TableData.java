package de.zuordner.importengine;

import java.util.ArrayList;
import java.util.List;

public class TableData {
    private String sheetName;
    private List<String> headers;
    private List<List<String>> rows;
    private int totalRows;

    public TableData() {
        this.headers = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    public TableData(String sheetName, List<String> headers, List<List<String>> rows) {
        this.sheetName = sheetName;
        this.headers = headers != null ? headers : new ArrayList<>();
        this.rows = rows != null ? rows : new ArrayList<>();
        this.totalRows = this.rows.size();
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public void setRows(List<List<String>> rows) {
        this.rows = rows;
        this.totalRows = rows != null ? rows.size() : 0;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }
}
