package de.zuordner;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zuordner.model.*;
import de.zuordner.repository.ProjectMigration;
import de.zuordner.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass alte Projektdateien aus Version 1.x beim Laden richtig repariert werden.
 */
class ProjectMigrationTest {

    @TempDir
    Path tempDir;

    /** Baut eine Regel wie das alte Frontend: mit leerer ID "". */
    private static DynamicRule legacyRule(String name, String description, RuleType type, TargetScope scope,
                                          RuleAction action, int weight, boolean active) {
        DynamicRule rule = new DynamicRule(name, description, type, scope, action, weight);
        rule.setId("");
        rule.setActive(active);
        return rule;
    }

    private static DynamicRule findByKey(Project project, StandardRule key) {
        return project.getRules().stream().filter(r -> r.getStandardKey() == key).findFirst().orElse(null);
    }

    @Test
    void doppelteRegelnAusDemAltenIdFehlerWerdenBereinigt() {
        // So sah ein Projekt nach dem alten Fehler aus: Die Geschlechter-Regel wurde
        // von der Paar-Regel überschrieben, und es gibt zwei Kopien der Paar-Regel.
        // Die ERSTE Kopie ist die zuletzt geänderte (Updates trafen immer die erste "").
        String desc = "Angemeldete Paare mit gleicher Paar-ID werden bevorzugt im selben Zimmer / Doppelbett "
                + "untergebracht (funktioniert mit und ohne Geschlechtertrennung).";
        Project project = new Project("Alt", "");
        project.getRules().add(legacyRule("Paare im selben Zimmer", desc, RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION, RuleAction.ADD_POINTS, 50, false));
        project.getRules().add(legacyRule("Paare im selben Zimmer", desc, RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION, RuleAction.ADD_POINTS, 50, true));

        assertTrue(ProjectMigration.migrate(project));

        assertEquals(1, project.getRules().size(), "Die Kopie muss entfernt werden");
        DynamicRule couple = project.getRules().get(0);
        assertEquals(StandardRule.COUPLES_TOGETHER, couple.getStandardKey());
        assertFalse(couple.getId().isBlank());
        assertFalse(couple.isActive(), "Der zuletzt gespeicherte Zustand (aus) bleibt erhalten");
        // Die Beschreibung enthält zwar "Geschlechtertrennung", es ist aber KEINE harte Regel
        assertNull(findByKey(project, StandardRule.GENDER_SEPARATION));
    }

    @Test
    void alteGeschlechterRegelWirdZurNeuenStandardRegel() {
        // Die alte Muster-Regel hieß "...(mit Paar-Ausnahme)" – sie enthält "Paar",
        // darf aber trotzdem NICHT als Paar-Regel erkannt werden.
        Project project = new Project("Alt", "");
        DynamicRule gender = new DynamicRule("Harte Geschlechtertrennung (mit Paar-Ausnahme)",
                "Unangemeldete Männer und Frauen dürfen nicht im selben Zimmer untergebracht werden.",
                RuleType.HARD, TargetScope.ROOM_PERSON, RuleAction.FORBID, -999999);
        DynamicRule couple = new DynamicRule("Paare im selben Zimmer", "", RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION, RuleAction.ADD_POINTS, 50);
        project.getRules().add(gender);
        project.getRules().add(couple);

        assertTrue(ProjectMigration.migrate(project));

        assertEquals(StandardRule.GENDER_SEPARATION, gender.getStandardKey());
        assertEquals(TargetScope.GENDER_SEPARATION, gender.getTargetScope());
        assertEquals(StandardRule.COUPLES_TOGETHER, couple.getStandardKey());
        assertNotSame(findByKey(project, StandardRule.GENDER_SEPARATION), findByKey(project, StandardRule.COUPLES_TOGETHER));
    }

    @Test
    void alteMaedchenzimmerRegelnBekommenDieStandardGeschlechtertrennung() {
        // Das alte Beispielprojekt hatte nur Regeln mit Bedingungen (Mädchenzimmer / Jungenzimmer)
        Project project = new Project("Alt", "");
        DynamicRule girls = new DynamicRule("Harte Geschlechtertrennung (Mädchenzimmer)",
                "Männliche Teilnehmer dürfen nicht im Mädchenzimmer untergebracht werden.",
                RuleType.HARD, TargetScope.ROOM_PERSON, RuleAction.FORBID, -999999);
        girls.addCondition(new Condition("person.gender", Operator.EQUALS, "maennlich"));
        girls.addCondition(new Condition("room.girlsRoom", Operator.EQUALS, true));
        project.getRules().add(girls);

        assertTrue(ProjectMigration.migrate(project));

        DynamicRule standard = findByKey(project, StandardRule.GENDER_SEPARATION);
        assertNotNull(standard, "Die neue Standard-Geschlechtertrennung muss angelegt werden");
        assertTrue(standard.isActive());
        assertNull(girls.getStandardKey(), "Die alte Regel bleibt als eigene Regel bestehen");
    }

    @Test
    void zusammenRegelMitBedingungWirdZurEigenschaftsRegel() {
        // Altes Frontend: "Personen über 65 -> zusammen ins selbe Zimmer" wurde als Paar-Regel gespeichert
        Project project = new Project("Alt", "");
        DynamicRule seniorsTogether = new DynamicRule("Senioren zusammen", "", RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION, RuleAction.ADD_POINTS, 25);
        seniorsTogether.addCondition(new Condition("person.age", Operator.GREATER_THAN, 65));
        project.getRules().add(seniorsTogether);

        assertTrue(ProjectMigration.migrate(project));
        assertEquals(TargetScope.CONDITION_CO_LOCATION, seniorsTogether.getTargetScope());
        assertNull(seniorsTogether.getStandardKey());
    }

    @Test
    void neuesProjektBrauchtKeineReparatur() {
        Project project = ProjectRepository.createSampleProject();
        assertFalse(ProjectMigration.migrate(project));
    }

    @Test
    void beimLadenWirdEineSicherheitskopieAngelegt() throws Exception {
        // Alte Projektdatei mit leerer Regel-ID in den (temporären) Speicherordner legen
        Project old = new Project("Alt", "");
        old.setId("altes-projekt");
        old.setRules(new ArrayList<>(List.of(legacyRule("Paare im selben Zimmer", "", RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION, RuleAction.ADD_POINTS, 50, true))));
        new ObjectMapper().writeValue(tempDir.resolve("altes-projekt.json").toFile(), old);

        ProjectRepository repository = new ProjectRepository(tempDir);

        assertTrue(Files.exists(tempDir.resolve("altes-projekt.json.v1-backup")), "Sicherheitskopie fehlt");
        Project loaded = repository.getProjectById("altes-projekt").orElseThrow();
        assertFalse(loaded.getRules().get(0).getId().isBlank());
        assertEquals(StandardRule.COUPLES_TOGETHER, loaded.getRules().get(0).getStandardKey());
        // Es gibt schon ein Projekt -> kein Beispielprojekt anlegen
        assertTrue(repository.getProjectById(ProjectRepository.SAMPLE_PROJECT_ID).isEmpty());
    }
}
