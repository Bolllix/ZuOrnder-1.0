package de.zuordner.controller;

import de.zuordner.model.Person;
import de.zuordner.model.Project;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/persons")
@CrossOrigin(origins = "*")
public class PersonController {

    private final ProjectRepository projectRepository;

    public PersonController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<Person>> getPersons(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> ResponseEntity.ok(p.getPersons()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> addPerson(@PathVariable String projectId, @RequestBody Person person) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getPersons().add(person);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{personId}")
    public ResponseEntity<Project> updatePerson(@PathVariable String projectId, @PathVariable String personId, @RequestBody Person updated) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (int i = 0; i < p.getPersons().size(); i++) {
                        if (p.getPersons().get(i).getId().equals(personId)) {
                            updated.setId(personId);
                            p.getPersons().set(i, updated);
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{personId}")
    public ResponseEntity<Project> deletePerson(@PathVariable String projectId, @PathVariable String personId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getPersons().removeIf(per -> per.getId().equals(personId));
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
