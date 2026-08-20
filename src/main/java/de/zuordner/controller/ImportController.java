package de.zuordner.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zuordner.importengine.*;
import de.zuordner.model.Person;
import de.zuordner.model.Project;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ImportController {

    private final ExcelCsvImporter importer;
    private final TableMappingService mappingService;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public ImportController(ExcelCsvImporter importer, TableMappingService mappingService, ProjectRepository projectRepository) {
        this.importer = importer;
        this.mappingService = mappingService;
        this.projectRepository = projectRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping({"/api/import/sheets", "/api/projects/{projectId}/import/sheets"})
    public ResponseEntity<List<String>> getSheetNames(
            @PathVariable(required = false) String projectId,
            @RequestParam("file") MultipartFile file) {
        try {
            List<String> sheets = importer.getSheetNames(file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(sheets);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping({"/api/import/preview", "/api/projects/{projectId}/import/preview"})
    public ResponseEntity<TableData> getPreview(
            @PathVariable(required = false) String projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "headerRowIndex", defaultValue = "0") int headerRowIndex,
            @RequestParam(value = "maxRows", defaultValue = "10") int maxRows) {
        try {
            TableData data = importer.readTablePreview(file.getInputStream(), file.getOriginalFilename(), sheetName, headerRowIndex, maxRows);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping({"/api/import/map", "/api/projects/{projectId}/import/map"})
    public ResponseEntity<ImportValidationResult> mapAndValidate(
            @PathVariable(required = false) String projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "headerRowIndex", defaultValue = "0") int headerRowIndex,
            @RequestParam("columnMapping") String columnMappingJson) {
        try {
            // maxRows = 0 parses ALL rows in the table
            TableData fullData = importer.readTablePreview(file.getInputStream(), file.getOriginalFilename(), sheetName, headerRowIndex, 0);
            Map<String, String> mapping = objectMapper.readValue(columnMappingJson, new TypeReference<Map<String, String>>() {});
            ImportValidationResult result = mappingService.mapTableToPersons(fullData, mapping);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping({"/api/import/process", "/api/projects/{projectId}/import/process"})
    public ResponseEntity<ImportValidationResult> processImport(
            @PathVariable(required = false) String projectId,
            @RequestBody TableMappingRequest request) {
        try {
            TableData tableData = new TableData("Sheet1", request.getHeaders(), request.getRows());
            ImportValidationResult result = mappingService.mapTableToPersons(tableData, request.getColumnMapping());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping({"/api/import/project/{projectId}", "/api/projects/{projectId}/import/save", "/api/projects/{projectId}/persons/batch"})
    public ResponseEntity<Project> importToProject(
            @PathVariable String projectId,
            @RequestBody List<Person> personsToImport) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getPersons().addAll(personsToImport);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public static class TableMappingRequest {
        private List<String> headers;
        private List<List<String>> rows;
        private Map<String, String> columnMapping;

        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }

        public List<List<String>> getRows() { return rows; }
        public void setRows(List<List<String>> rows) { this.rows = rows; }

        public Map<String, String> getColumnMapping() { return columnMapping; }
        public void setColumnMapping(Map<String, String> columnMapping) { this.columnMapping = columnMapping; }
    }
}
