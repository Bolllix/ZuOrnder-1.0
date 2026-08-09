package de.zuordner.controller;

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
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {

    private final ExcelCsvImporter importer;
    private final TableMappingService mappingService;
    private final ProjectRepository projectRepository;

    public ImportController(ExcelCsvImporter importer, TableMappingService mappingService, ProjectRepository projectRepository) {
        this.importer = importer;
        this.mappingService = mappingService;
        this.projectRepository = projectRepository;
    }

    @PostMapping("/sheets")
    public ResponseEntity<List<String>> getSheetNames(@RequestParam("file") MultipartFile file) {
        try {
            List<String> sheets = importer.getSheetNames(file.getInputStream(), file.getOriginalFilename());
            return ResponseEntity.ok(sheets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<TableData> getPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "headerRowIndex", defaultValue = "0") int headerRowIndex,
            @RequestParam(value = "maxRows", defaultValue = "10") int maxRows) {
        try {
            TableData data = importer.readTablePreview(file.getInputStream(), file.getOriginalFilename(), sheetName, headerRowIndex, maxRows);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/map")
    public ResponseEntity<ImportValidationResult> mapAndValidate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "headerRowIndex", defaultValue = "0") int headerRowIndex,
            @RequestBody Map<String, String> columnMapping) {
        try {
            TableData fullData = importer.readTablePreview(file.getInputStream(), file.getOriginalFilename(), sheetName, headerRowIndex, 0);
            ImportValidationResult result = mappingService.mapTableToPersons(fullData, columnMapping);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/project/{projectId}")
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
}
