package de.zuordner;

import de.zuordner.engine.AssignmentService;
import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentServiceScenarioTest {

    @Test
    void testRealisticTwentyPersonsScenario() {
        RuleEngine ruleEngine = new RuleEngine();
        AssignmentService service = new AssignmentService(ruleEngine);

        // 1. Create 20 Participants
        List<Person> persons = new ArrayList<>();

        // Couple 1
        persons.add(new Person("1", "Meier", "Ernst", "maennlich", 72, "PAAR1", "Senioren"));
        persons.add(new Person("2", "Meier", "Erna", "weiblich", 70, "PAAR1", "Senioren"));

        // Couple 2
        persons.add(new Person("3", "Kovacs", "Janos", "maennlich", 35, "PAAR2", "Familie Kovacs"));
        persons.add(new Person("4", "Kovacs", "Elena", "weiblich", 33, "PAAR2", "Familie Kovacs"));

        // Seniors
        persons.add(new Person("5", "Bauer", "Kurt", "maennlich", 81, null, "Senioren"));
        persons.add(new Person("6", "Schulz", "Gerti", "weiblich", 79, null, "Senioren"));

        // Youths / Young adults (14 persons)
        for (int i = 7; i <= 13; i++) {
            persons.add(new Person(String.valueOf(i), "Junge" + i, "Max" + i, "maennlich", 18 + i, null, "Jugendgruppe Jungs"));
        }
        for (int i = 14; i <= 20; i++) {
            persons.add(new Person(String.valueOf(i), "Mädchen" + i, "Lisa" + i, "weiblich", 16 + i, null, "Jugendgruppe Mädels"));
        }

        assertEquals(20, persons.size());

        // 2. Create 3 Rooms with total 20 Beds
        Building b = new Building("Jugendherberge Hauptgebäude");

        // Zimmer 1: Seniorenzimmer (4 Einzelbetten)
        Room r1 = new Room("Seniorenzimmer 101", 0, false, b.getName());
        r1.addSingleBed("Bett 101-1");
        r1.addSingleBed("Bett 101-2");
        r1.addSingleBed("Bett 101-3");
        r1.addSingleBed("Bett 101-4");

        // Zimmer 2: Jungenzimmer (4 Etagenbetten = 8 Betten)
        Room r2 = new Room("Jungenzimmer 201", 1, false, b.getName());
        r2.setBoysRoom(true);
        r2.addBunkBed("EB 1");
        r2.addBunkBed("EB 2");
        r2.addBunkBed("EB 3");
        r2.addBunkBed("EB 4");

        // Zimmer 3: Mädchenzimmer (4 Etagenbetten = 8 Betten)
        Room r3 = new Room("Mädchenzimmer 202", 1, true, b.getName());
        r3.addBunkBed("EB 5");
        r3.addBunkBed("EB 6");
        r3.addBunkBed("EB 7");
        r3.addBunkBed("EB 8");

        b.getRooms().add(r1);
        b.getRooms().add(r2);
        b.getRooms().add(r3);

        assertEquals(20, b.getAllBeds().size());

        // 3. Define Rules
        List<DynamicRule> rules = new ArrayList<>();

        // Harte Regel: Senioren (>68 Jahre) nicht im oberen Hochbett
        DynamicRule seniorRule = new DynamicRule(
                "Senioren unten",
                "Personen über 68 nicht ins obere Hochbett",
                RuleType.HARD,
                TargetScope.BED_PERSON,
                RuleAction.FORBID,
                -999999
        );
        seniorRule.addCondition(new Condition("person.age", Operator.GREATER_THAN, 68));
        seniorRule.addCondition(new Condition("bed.isTopBunk", Operator.EQUALS, true));
        rules.add(seniorRule);

        // Weiche Regel: Paare im selben Zimmer
        DynamicRule coupleRule = new DynamicRule(
                "Paare zusammen",
                "Paare bevorzugt im selben Zimmer",
                RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION,
                RuleAction.ADD_POINTS,
                50
        );
        rules.add(coupleRule);

        // Weiche Regel: Mädchen im Mädchenzimmer
        DynamicRule girlsRule = new DynamicRule(
                "Mädchenzimmer bevorzugen",
                "Weibliche Personen bevorzugt im Mädchenzimmer",
                RuleType.SOFT,
                TargetScope.ROOM_PERSON,
                RuleAction.ADD_POINTS,
                30
        );
        girlsRule.addCondition(new Condition("person.gender", Operator.EQUALS, "weiblich"));
        girlsRule.addCondition(new Condition("room.girlsRoom", Operator.EQUALS, true));
        rules.add(girlsRule);

        // 4. Calculate Assignment
        AssignmentResult result = service.calculateAssignment(persons, List.of(b), rules);

        assertNotNull(result);
        assertEquals(20, result.getAssignments().size());
        assertEquals(0, result.getUnassignedPersonIds().size());
        assertEquals(0, result.getUnassignedBedIds().size());
        assertEquals(0, result.getHardRuleViolationsCount());

        // Verify Seniors Ernst Meier (72), Erna Meier (70), Kurt Bauer (81), Gerti Schulz (79) are NOT on top bunks
        for (AssignmentPair pair : result.getAssignments()) {
            Person p = persons.stream().filter(pers -> pers.getId().equals(pair.getPersonId())).findFirst().orElse(null);
            Bed bed = b.getAllBeds().stream().filter(bd -> bd.getId().equals(pair.getBedId())).findFirst().orElse(null);

            assertNotNull(p);
            assertNotNull(bed);

            if (p.getAge() > 68) {
                assertFalse(bed.isTopBunk(), p.getFullName() + " (Alter " + p.getAge() + ") darf nicht im oberen Hochbett schlafen!");
            }
        }
    }
}
