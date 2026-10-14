package de.zuordner;

import de.zuordner.controller.RuleController;
import de.zuordner.model.*;
import de.zuordner.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft den gemeldeten Fehler aus Version 1.x:
 * "Schalte ich die Paar-Regel um, wird die Geschlechtertrennung mit umgeschaltet."
 *
 * Jeder Test spielt nach, was der Regel-Editor macht, und kontrolliert danach,
 * dass sich NUR die gewünschte Regel geändert hat.
 *
 * {@code @TempDir}: JUnit legt einen leeren Ordner an, der nach dem Test wieder
 * gelöscht wird. So schreiben die Tests nie in die echten Projektdaten.
 */
class StandardRuleSwitchTest {

    @TempDir
    Path tempDir;

    private ProjectRepository repository;
    private RuleController controller;
    private String projectId;

    @BeforeEach
    void setUp() {
        repository = new ProjectRepository(tempDir);
        controller = new RuleController(repository);

        // Ein frisches Projekt OHNE Regeln (wie nach "Neues Projekt anlegen")
        Project project = new Project("Testfreizeit", "");
        projectId = repository.saveProject(project).getId();
    }

    /** Hilfsmethode: Standard-Regel über den neuen Endpunkt schalten (wie der Regel-Editor). */
    private void switchRule(StandardRule key, boolean active) {
        controller.setStandardRuleActive(projectId, key, Map.of("active", active));
    }

    /** Hilfsmethode: Regel mit diesem Schlüssel im gespeicherten Projekt suchen. */
    private DynamicRule rule(StandardRule key) {
        return repository.getProjectById(projectId).orElseThrow().getRules().stream()
                .filter(r -> r.getStandardKey() == key)
                .findFirst()
                .orElse(null);
    }

    @Test
    void paarSchalterAendertNurDiePaarRegel() {
        switchRule(StandardRule.GENDER_SEPARATION, true);
        switchRule(StandardRule.COUPLES_TOGETHER, true);

        // Paar-Regel AUS -> Geschlechtertrennung muss AN bleiben
        switchRule(StandardRule.COUPLES_TOGETHER, false);
        assertFalse(rule(StandardRule.COUPLES_TOGETHER).isActive());
        assertTrue(rule(StandardRule.GENDER_SEPARATION).isActive(), "Geschlechtertrennung darf sich nicht mitändern");

        // Geschlechtertrennung AUS -> Paar-Regel bleibt AUS, nichts anderes passiert
        switchRule(StandardRule.GENDER_SEPARATION, false);
        assertFalse(rule(StandardRule.GENDER_SEPARATION).isActive());
        assertFalse(rule(StandardRule.COUPLES_TOGETHER).isActive());

        // Paar-Regel wieder AN -> Geschlechtertrennung bleibt AUS
        switchRule(StandardRule.COUPLES_TOGETHER, true);
        assertTrue(rule(StandardRule.COUPLES_TOGETHER).isActive());
        assertFalse(rule(StandardRule.GENDER_SEPARATION).isActive(), "Geschlechtertrennung darf sich nicht mitändern");
    }

    @Test
    void jedeStandardRegelGibtEsNurEinmal() {
        // Mehrfaches Umschalten darf keine Kopien erzeugen
        for (int i = 0; i < 3; i++) {
            switchRule(StandardRule.COUPLES_TOGETHER, true);
            switchRule(StandardRule.COUPLES_TOGETHER, false);
        }
        long count = repository.getProjectById(projectId).orElseThrow().getRules().stream()
                .filter(r -> r.getStandardKey() == StandardRule.COUPLES_TOGETHER)
                .count();
        assertEquals(1, count);
    }

    @Test
    void neueRegelnBekommenEindeutigeIds() {
        // Genau das hat das alte Frontend geschickt: neue Regeln mit leerer ID ""
        DynamicRule first = new DynamicRule("Regel A", "", RuleType.SOFT, TargetScope.BED_PERSON, RuleAction.ADD_POINTS, 10);
        first.setId("");
        DynamicRule second = new DynamicRule("Regel B", "", RuleType.SOFT, TargetScope.BED_PERSON, RuleAction.ADD_POINTS, 20);
        second.setId("");

        controller.addRule(projectId, first);
        controller.addRule(projectId, second);

        Project project = repository.getProjectById(projectId).orElseThrow();
        String idA = project.getRules().get(0).getId();
        String idB = project.getRules().get(1).getId();
        assertFalse(idA.isBlank());
        assertFalse(idB.isBlank());
        assertNotEquals(idA, idB, "Zwei Regeln dürfen nie dieselbe ID haben");

        // Regel B ausschalten darf Regel A nicht verändern
        DynamicRule updateB = new DynamicRule("Regel B", "", RuleType.SOFT, TargetScope.BED_PERSON, RuleAction.ADD_POINTS, 20);
        updateB.setActive(false);
        controller.updateRule(projectId, idB, updateB);

        project = repository.getProjectById(projectId).orElseThrow();
        assertEquals("Regel A", project.getRules().get(0).getName());
        assertTrue(project.getRules().get(0).isActive());
        assertFalse(project.getRules().get(1).isActive());
    }

    @Test
    void aendernBehaeltDenStandardSchluessel() {
        switchRule(StandardRule.GROUPS_TOGETHER, true);
        DynamicRule groups = rule(StandardRule.GROUPS_TOGETHER);

        // Ein normales "Regel ändern" ohne standardKey darf die Standard-Regel nicht "vergessen"
        DynamicRule changed = new DynamicRule("Gruppen", "", RuleType.SOFT, TargetScope.GROUP_CO_LOCATION, RuleAction.ADD_POINTS, 30);
        controller.updateRule(projectId, groups.getId(), changed);

        DynamicRule after = rule(StandardRule.GROUPS_TOGETHER);
        assertNotNull(after);
        assertEquals(30, after.getWeight());
    }
}
