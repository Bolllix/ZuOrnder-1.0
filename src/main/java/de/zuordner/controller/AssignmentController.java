package de.zuordner.controller;

import de.zuordner.engine.AssignmentService;
import de.zuordner.model.AssignmentPair;
import de.zuordner.model.AssignmentResult;
import de.zuordner.model.Project;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/assignment")
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ProjectRepository projectRepository;

    public AssignmentController(AssignmentService assignmentService, ProjectRepository projectRepository) {
        this.assignmentService = assignmentService;
        this.projectRepository = projectRepository;
    }

    @PostMapping("/calculate")
    public ResponseEntity<AssignmentResult> calculateAssignment(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(project -> {
                    AssignmentResult result = assignmentService.calculateAssignment(
                            project.getPersons(),
                            project.getBuildings(),
                            project.getRules()
                    );
                    project.setAssignmentResult(result);
                    projectRepository.saveProject(project);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/override")
    public ResponseEntity<Project> manualOverride(
            @PathVariable String projectId,
            @RequestBody Map<String, String> personToBedMap) { // personId -> bedId
        return projectRepository.getProjectById(projectId)
                .map(project -> {
                    AssignmentResult targetResult = project.getAssignmentResult();
                    if (targetResult == null) {
                        targetResult = new AssignmentResult();
                        project.setAssignmentResult(targetResult);
                    }
                    final AssignmentResult finalResult = targetResult;

                    // Perform manual override updates
                    personToBedMap.forEach((personId, bedId) -> {
                        finalResult.getAssignments().removeIf(p -> p.getPersonId().equals(personId) || p.getBedId().equals(bedId));
                        if (bedId != null && !bedId.isEmpty()) {
                            AssignmentPair pair = new AssignmentPair();
                            pair.setPersonId(personId);
                            pair.setBedId(bedId);
                            pair.setManualOverride(true);
                            pair.setScore(0);
                            finalResult.getAssignments().add(pair);
                        }
                    });

                    Project saved = projectRepository.saveProject(project);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
