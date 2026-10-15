package de.zuordner;

import de.zuordner.engine.AssignmentService;
import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import de.zuordner.repository.BuildingTemplateRepository;
import de.zuordner.repository.BuiltInBuildings;
import de.zuordner.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die mitgelieferten Gebäude-Vorlagen, vor allem das nachgebaute
 * Seminarhaus Neumühle (5x 2-Bett, 1x 3-Bett, 2x 5-Bett, 1x 6-Bett, 2x 8-Bett).
 */
class BuiltInBuildingsTest {

    @TempDir
    Path tempDir;

    @Test
    void neumuehleHatDieRichtigenZimmer() {
        Building neumuehle = BuiltInBuildings.seminarhausNeumuehle();

        assertEquals(11, neumuehle.getRooms().size());
        assertEquals(45, neumuehle.getAllBeds().size());

        List<Integer> sizes = new ArrayList<>();
        for (Room room : neumuehle.getRooms()) sizes.add(room.getBeds().size());
        Collections.sort(sizes);
        assertEquals(List.of(2, 2, 2, 2, 2, 3, 5, 5, 6, 8, 8), sizes);

        // "Die meisten Zimmer haben Hochbetten"
        long roomsWithBunks = neumuehle.getRooms().stream()
                .filter(r -> r.getBeds().stream().anyMatch(Bed::isTopBunk))
                .count();
        assertTrue(roomsWithBunks > neumuehle.getRooms().size() / 2);
    }

    @Test
    void vorlagenListeEnthaeltDieMitgeliefertenGebaeude() {
        ProjectRepository projects = new ProjectRepository(tempDir.resolve("projects"));
        BuildingTemplateRepository templates = new BuildingTemplateRepository(projects, tempDir.resolve("templates"));

        List<Map<String, Object>> available = templates.getAllAvailableBuildingsWithSource();
        List<Object> builtInNames = available.stream()
                .filter(item -> "Mitgelieferte Vorlage".equals(item.get("source")))
                .map(item -> item.get("name"))
                .toList();

        assertEquals(List.of("Seminarhaus Neumühle", "Gebäude A (Paare & Senioren)", "Gebäude B (Jugendtrakt)"), builtInNames);
        // Mitgelieferte Vorlagen lassen sich nicht löschen
        assertFalse(templates.deleteTemplate(BuiltInBuildings.NEUMUEHLE_ID));
    }

    @Test
    void uebernehmenErzeugtEineKopieMitNeuenIds() {
        ProjectRepository projects = new ProjectRepository(tempDir.resolve("projects"));
        BuildingTemplateRepository templates = new BuildingTemplateRepository(projects, tempDir.resolve("templates"));

        Building original = BuiltInBuildings.seminarhausNeumuehle();
        Building copy = templates.cloneBuilding(original);

        assertNotEquals(original.getId(), copy.getId());
        assertEquals(45, copy.getAllBeds().size());
        assertEquals("Seminarhaus Neumühle", copy.getRooms().get(0).getBeds().get(0).getBuildingName());
    }

    @Test
    void beispielprojektBehaeltSeineZweiGebaeude() {
        Project sample = ProjectRepository.createSampleProject();
        assertEquals(2, sample.getBuildings().size());
        assertEquals("Gebäude A (Paare & Senioren)", sample.getBuildings().get(0).getName());
        assertEquals("Gebäude B (Jugendtrakt)", sample.getBuildings().get(1).getName());
        // eigene IDs, nicht die der Vorlagen
        assertNotEquals(BuiltInBuildings.EXAMPLE_A_ID, sample.getBuildings().get(0).getId());
    }

    @Test
    void freizeitInDerNeumuehleWirdRegelkonformVerteilt() {
        // 2 Paare, 16 Jungen (2 Gruppen) und 16 Mädchen (2 Gruppen) = 36 Personen für 45 Betten
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("p1", "Paar", "Max", "maennlich", 35, "P1", null));
        persons.add(new Person("p2", "Paar", "Mia", "weiblich", 34, "P1", null));
        persons.add(new Person("p3", "Paar", "Tom", "maennlich", 40, "P2", null));
        persons.add(new Person("p4", "Paar", "Tina", "weiblich", 41, "P2", null));
        for (int i = 0; i < 16; i++) {
            persons.add(new Person("j" + i, "Junge", "J" + i, "maennlich", 13, null, "Jungs-" + (i % 2)));
            persons.add(new Person("m" + i, "Mädchen", "M" + i, "weiblich", 13, null, "Mädels-" + (i % 2)));
        }

        List<DynamicRule> rules = new ArrayList<>();
        for (StandardRule key : StandardRule.values()) rules.add(key.createDefaultRule());

        AssignmentResult result = new AssignmentService(new RuleEngine())
                .calculateAssignment(persons, List.of(BuiltInBuildings.seminarhausNeumuehle()), rules);

        assertEquals(36, result.getAssignments().size(), "Alle 36 Personen müssen ein Bett bekommen");
        assertEquals(0, result.getHardRuleViolationsCount());

        Map<String, String> room = new HashMap<>();
        for (AssignmentPair pair : result.getAssignments()) room.put(pair.getPersonId(), pair.getRoomName());
        assertEquals(room.get("p1"), room.get("p2"), "Paar 1 soll zusammen sein");
        assertEquals(room.get("p3"), room.get("p4"), "Paar 2 soll zusammen sein");
    }
}
