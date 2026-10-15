package de.zuordner;

import de.zuordner.engine.AssignmentService;
import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die "Paar-Ausnahme" bei der Geschlechtertrennung:
 * - Geschlechtertrennung + Paar-Regel aktiv -> Paar darf zusammen schlafen
 * - nur Geschlechtertrennung aktiv          -> auch das Paar wird getrennt
 */
public class CoupleExceptionTest {

    private final AssignmentService assignmentService = new AssignmentService(new RuleEngine());

    /** Ein Paar (Max & Erika, gleiche Paar-ID) */
    private List<Person> couple() {
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("1", "Mustermann", "Max", "maennlich", 30, "PAAR1", null));
        persons.add(new Person("2", "Musterfrau", "Erika", "weiblich", 28, "PAAR1", null));
        return persons;
    }

    /** Ein Gebäude mit zwei Zweibettzimmern */
    private Building twoDoubleRooms() {
        Building building = new Building("Haupthaus");
        for (String name : List.of("Zimmer 101", "Zimmer 102")) {
            Room room = new Room(name, 0, false, building.getName());
            room.addSingleBed(name + " Bett 1");
            room.addSingleBed(name + " Bett 2");
            building.getRooms().add(room);
        }
        return building;
    }

    @Test
    public void testCoupleAllowedInSameRoomWithHardGenderRule() {
        List<DynamicRule> rules = List.of(
                StandardRule.GENDER_SEPARATION.createDefaultRule(),
                StandardRule.COUPLES_TOGETHER.createDefaultRule());

        AssignmentResult result = assignmentService.calculateAssignment(couple(), List.of(twoDoubleRooms()), rules);

        assertEquals(2, result.getAssignments().size());
        assertEquals(0, result.getHardRuleViolationsCount(), "Paare dürfen keine Verstöße auslösen!");
        assertEquals(0, result.getUnassignedPersonIds().size());
        // Beide im selben Zimmer
        assertEquals(result.getAssignments().get(0).getRoomName(), result.getAssignments().get(1).getRoomName());
        // Die Begründung nennt die Paar-Ausnahme
        assertTrue(result.getAssignments().get(0).getExplanations().stream()
                .anyMatch(e -> e.getReason().contains("Paar-Ausnahme")));
    }

    @Test
    public void testCoupleSeparatedWhenOnlyGenderRuleIsActive() {
        DynamicRule coupleRule = StandardRule.COUPLES_TOGETHER.createDefaultRule();
        coupleRule.setActive(false); // Paar-Regel ausgeschaltet
        List<DynamicRule> rules = List.of(StandardRule.GENDER_SEPARATION.createDefaultRule(), coupleRule);

        AssignmentResult result = assignmentService.calculateAssignment(couple(), List.of(twoDoubleRooms()), rules);

        assertEquals(2, result.getAssignments().size());
        assertEquals(0, result.getHardRuleViolationsCount());
        assertNotEquals(result.getAssignments().get(0).getRoomName(), result.getAssignments().get(1).getRoomName(),
                "Ohne Paar-Regel gilt die Geschlechtertrennung auch für Paare");
    }
}
