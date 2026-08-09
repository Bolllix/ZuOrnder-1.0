package de.zuordner.controller;

import de.zuordner.model.DynamicRule;
import de.zuordner.model.Project;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/rules")
@CrossOrigin(origins = "*")
public class RuleController {

    private final ProjectRepository projectRepository;

    public RuleController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<DynamicRule>> getRules(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> ResponseEntity.ok(p.getRules()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> addRule(@PathVariable String projectId, @RequestBody DynamicRule rule) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getRules().add(rule);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<Project> updateRule(@PathVariable String projectId, @PathVariable String ruleId, @RequestBody DynamicRule updatedRule) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (int i = 0; i < p.getRules().size(); i++) {
                        if (p.getRules().get(i).getId().equals(ruleId)) {
                            updatedRule.setId(ruleId);
                            p.getRules().set(i, updatedRule);
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Project> deleteRule(@PathVariable String projectId, @PathVariable String ruleId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getRules().removeIf(r -> r.getId().equals(ruleId));
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
