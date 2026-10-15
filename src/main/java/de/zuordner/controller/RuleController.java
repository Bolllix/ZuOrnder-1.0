package de.zuordner.controller;

import de.zuordner.model.DynamicRule;
import de.zuordner.model.Project;
import de.zuordner.model.StandardRule;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * RuleController – Regeln eines Projekts anlegen, ändern, löschen, schalten
 * ============================================================================
 * Endpunkte (alle unter /api/projects/{projectId}/rules):
 *   GET    /                 -> alle Regeln
 *   POST   /                 -> neue (eigene) Regel anlegen
 *   PUT    /{ruleId}         -> Regel ändern (z.B. an-/ausschalten)
 *   DELETE /{ruleId}         -> Regel löschen
 *   PUT    /standard/{key}   -> eine der 4 Standard-Regeln an-/ausschalten
 *                               (wird bei Bedarf automatisch angelegt)
 *
 * Fehlerbehebung in Version 2.0:
 * Das Frontend hat neue Regeln früher mit der leeren ID "" geschickt. Dadurch
 * hatten mehrere Regeln dieselbe (leere) ID, und eine solche Regel ließ sich
 * danach nicht mehr ändern oder an-/ausschalten (die Adresse .../rules/ mit
 * leerer ID beantwortet der Server mit einem Fehler). Jetzt vergibt der Server
 * selbst eine eindeutige ID, wenn keine (oder eine schon benutzte) mitgeschickt wird.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/rules")
@CrossOrigin(origins = "*")
public class RuleController {

    private final ProjectRepository projectRepository;

    public RuleController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /** Liefert alle Regeln eines Projekts. */
    @GetMapping
    public ResponseEntity<List<DynamicRule>> getRules(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> ResponseEntity.ok(p.getRules()))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Legt eine neue Regel an und gibt das komplette, gespeicherte Projekt zurück. */
    @PostMapping
    public ResponseEntity<Project> addRule(@PathVariable String projectId, @RequestBody DynamicRule rule) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    // Eindeutige ID sicherstellen (leer oder schon vergeben -> neue UUID)
                    if (rule.getId() == null || rule.getId().isBlank() || findRule(p, rule.getId()) != null) {
                        rule.setId(UUID.randomUUID().toString());
                    }
                    // Jede Standard-Regel darf es pro Projekt nur EINMAL geben
                    if (rule.getStandardKey() != null && findStandardRule(p, rule.getStandardKey()) != null) {
                        rule.setStandardKey(null);
                    }
                    p.getRules().add(rule);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Ersetzt eine bestehende Regel durch die mitgeschickte Version. */
    @PutMapping("/{ruleId}")
    public ResponseEntity<Project> updateRule(@PathVariable String projectId, @PathVariable String ruleId, @RequestBody DynamicRule updatedRule) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (int i = 0; i < p.getRules().size(); i++) {
                        DynamicRule existing = p.getRules().get(i);
                        if (existing.getId().equals(ruleId)) {
                            updatedRule.setId(ruleId);
                            // Ob eine Regel eine Standard-Regel ist, entscheidet nur der Server
                            updatedRule.setStandardKey(existing.getStandardKey());
                            p.getRules().set(i, updatedRule);
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Löscht eine Regel. */
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

    /**
     * Schaltet eine der 4 Standard-Regeln an oder aus.
     * Beispiel: PUT /api/projects/abc/rules/standard/COUPLES_TOGETHER  mit  {"active": true}
     *
     * Gibt es die Regel im Projekt noch nicht, wird sie mit den Standardwerten
     * angelegt. Es wird IMMER nur genau diese eine Regel verändert – die anderen
     * Standard-Regeln bleiben unberührt.
     */
    @PutMapping("/standard/{key}")
    public ResponseEntity<Project> setStandardRuleActive(@PathVariable String projectId,
                                                         @PathVariable StandardRule key,
                                                         @RequestBody Map<String, Boolean> body) {
        boolean active = body != null && Boolean.TRUE.equals(body.get("active"));
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    DynamicRule rule = findStandardRule(p, key);
                    if (rule == null) {
                        rule = key.createDefaultRule();
                        p.getRules().add(rule);
                    }
                    rule.setActive(active);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Sucht eine Regel anhand ihrer ID (oder null). */
    private DynamicRule findRule(Project project, String ruleId) {
        for (DynamicRule r : project.getRules()) {
            if (ruleId.equals(r.getId())) return r;
        }
        return null;
    }

    /** Sucht die Standard-Regel mit dem angegebenen Schlüssel (oder null). */
    private DynamicRule findStandardRule(Project project, StandardRule key) {
        for (DynamicRule r : project.getRules()) {
            if (r.getStandardKey() == key) return r;
        }
        return null;
    }
}
