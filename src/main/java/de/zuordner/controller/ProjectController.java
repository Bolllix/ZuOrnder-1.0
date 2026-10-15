package de.zuordner.controller;

import de.zuordner.model.Project;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * ProjectController – Projekte auflisten, anlegen, ändern, löschen
 * ============================================================================
 * So kommt eine Anfrage aus dem Browser hier an (Beispiel):
 *   Browser:  GET http://localhost:8080/api/projects
 *   Spring:   ruft getAllProjects() auf
 *   Antwort:  die Projektliste als JSON-Text
 *
 * Die Annotationen bedeuten:
 *   {@code @RestController}  = diese Klasse beantwortet Web-Anfragen mit Daten (JSON)
 *   {@code @RequestMapping}  = gemeinsamer Anfang aller Adressen dieser Klasse
 *   {@code @GetMapping} usw. = Adresse + HTTP-Methode (GET = lesen, POST = anlegen,
 *                              PUT = ändern, DELETE = löschen)
 *   {@code @PathVariable}    = Wert aus der Adresse, z.B. {id}
 *   {@code @RequestBody}     = Daten, die der Browser mitschickt (JSON -> Java-Objekt)
 */
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /** Alle Projekte */
    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.getAllProjects();
    }

    /** Ein Projekt anhand seiner ID (404 "Not Found", wenn es nicht existiert) */
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable String id) {
        return projectRepository.getProjectById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Neues Projekt anlegen */
    @PostMapping
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        Project saved = projectRepository.saveProject(project);
        return ResponseEntity.ok(saved);
    }

    /** Projekt komplett ersetzen (die ID bleibt gleich) */
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable String id, @RequestBody Project updated) {
        return projectRepository.getProjectById(id)
                .map(existing -> {
                    updated.setId(id);
                    Project saved = projectRepository.saveProject(updated);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Projekt löschen (204 "No Content" bei Erfolg) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        if (projectRepository.deleteProject(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
