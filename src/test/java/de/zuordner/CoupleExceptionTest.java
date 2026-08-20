package de.zuordner;

import de.zuordner.engine.AssignmentService;
import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CoupleExceptionTest {

    @Test
    public void testCoupleAllowedInSameRoomWithHardGenderRule() {
        RuleEngine ruleEngine = new RuleEngine();
        AssignmentService assignmentService = new AssignmentService(ruleEngine);

        // 1. Create a Couple (Max & Erika, partnerId = "PAAR1")
        List<Person> persons = new ArrayList<>();
        Person p1 = new Person("1", "Mustermann", "Max", "maennlich", 30, "PAAR1", null);
        Person p2 = new Person("2", "Musterfrau", "Erika", "weiblich", 28, "PAAR1", null);
        persons.add(p1);
        persons.add(p2);

        // 2. Create 1 Room with 2 Beds
        Building building = new Building("Haupthaus");
        Room room = new Room("Zimmer 101", 0, false, building.getName());
        room.addSingleBed("Bett 1");
        room.addSingleBed("Bett 2");
        building.getRooms().add(room);

        // 3. Define Hard Gender Segregation Rule + Soft Couple Rule
        List<DynamicRule> rules = new ArrayList<>();
        DynamicRule hardGenderRule = new DynamicRule(
                "Harte Geschlechtertrennung (mit Paar-Ausnahme)",
                "Geschlechtertrennung",
                RuleType.HARD,
                TargetScope.ROOM_PERSON,
                RuleAction.FORBID,
                -999999
        );
        rules.add(hardGenderRule);

        DynamicRule coupleRule = new DynamicRule(
                "Paare im selben Zimmer",
                "Paare bevorzugt zusammen",
                RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION,
                RuleAction.ADD_POINTS,
                50
        );
        rules.add(coupleRule);

        // 4. Calculate Assignment
        AssignmentResult result = assignmentService.calculateAssignment(persons, List.of(building), rules);

        assertNotNull(result);
        assertEquals(2, result.getAssignments().size());
        assertEquals(0, result.getHardRuleViolationsCount(), "Couples should NOT trigger hard gender violations!");
        assertEquals(0, result.getUnassignedPersonIds().size());

        // Verify both couple members are assigned to Room 101
        assertEquals("Zimmer 101", result.getAssignments().get(0).getRoomName());
        assertEquals("Zimmer 101", result.getAssignments().get(1).getRoomName());
    }
}
