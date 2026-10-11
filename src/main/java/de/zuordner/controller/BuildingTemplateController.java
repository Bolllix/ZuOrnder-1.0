package de.zuordner.controller;

import de.zuordner.model.Building;
import de.zuordner.model.Project;
import de.zuordner.repository.BuildingTemplateRepository;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class BuildingTemplateController {

    private final BuildingTemplateRepository buildingTemplateRepository;
    private final ProjectRepository projectRepository;

    public BuildingTemplateController(BuildingTemplateRepository buildingTemplateRepository, ProjectRepository projectRepository) {
        this.buildingTemplateRepository = buildingTemplateRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Liefert alle verfügbaren Gebäude-Vorlagen sowie Gebäude aus anderen Projekten.
     */
    @GetMapping("/api/building-templates")
    public ResponseEntity<List<Map<String, Object>>> getAvailableBuildings() {
        return ResponseEntity.ok(buildingTemplateRepository.getAllAvailableBuildingsWithSource());
    }

    /**
     * Speichert ein Gebäude als dauerhafte Vorlage in die Bibliothek.
     */
    @PostMapping("/api/building-templates")
    public ResponseEntity<Building> saveBuildingTemplate(@RequestBody Building building) {
        Building saved = buildingTemplateRepository.saveTemplate(building);
        return ResponseEntity.ok(saved);
    }

    /**
     * Löscht eine Gebäude-Vorlage aus der Bibliothek.
     */
    @DeleteMapping("/api/building-templates/{templateId}")
    public ResponseEntity<Void> deleteBuildingTemplate(@PathVariable String templateId) {
        boolean deleted = buildingTemplateRepository.deleteTemplate(templateId);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Importiert ein Gebäude (aus Vorlage oder aus einem anderen Projekt) in ein Zielprojekt.
     */
    @PostMapping("/api/projects/{projectId}/buildings/import-template")
    public ResponseEntity<Project> importBuildingToProject(
            @PathVariable String projectId,
            @RequestBody Building sourceBuilding) {
        return projectRepository.getProjectById(projectId)
                .map(project -> {
                    // Erstelle eine saubere Kopie mit frischen IDs
                    Building cloned = buildingTemplateRepository.cloneBuilding(sourceBuilding);
                    project.getBuildings().add(cloned);
                    Project saved = projectRepository.saveProject(project);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
