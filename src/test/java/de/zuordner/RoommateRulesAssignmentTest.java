package de.zuordner;

import de.zuordner.engine.AssignmentService;
import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import de.zuordner.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass die Optimierung die "Zimmernachbar"-Regeln wirklich beachtet:
 * Geschlechtertrennung, Paare zusammen, Gruppen zusammen – jeweils einzeln und
 * zusammen geschaltet. (In Version 1.x hatten diese Regeln keinen Einfluss auf
 * die Verteilung, sie wurden erst hinterher ausgewertet.)
 */
class RoommateRulesAssignmentTest {

    private final AssignmentService service = new AssignmentService(new RuleEngine());

    // ------------------------------------------------------------------------
    // Hilfsmethoden
    // ------------------------------------------------------------------------

    /** Ordnet jeder Person-ID den Raumnamen aus dem Ergebnis zu. */
    private static Map<String, String> roomOfPerson(AssignmentResult result) {
        Map<String, String> map = new HashMap<>();
        for (AssignmentPair pair : result.getAssignments()) {
            map.put(pair.getPersonId(), pair.getRoomName());
        }
        return map;
    }

    /** Gibt es ein Zimmer, in dem ein Mann und eine Frau liegen, die KEIN Paar sind? */
    private static boolean hasMixedNonCoupleRoom(List<Person> persons, AssignmentResult result, boolean couplesAllowed) {
        Map<String, String> rooms = roomOfPerson(result);
        for (Person a : persons) {
            for (Person b : persons) {
                if (a == b || !rooms.containsKey(a.getId()) || !rooms.containsKey(b.getId())) continue;
                if (!rooms.get(a.getId()).equals(rooms.get(b.getId()))) continue;
                boolean mixed = RuleEngine.isMaleFemaleMix(a, b);
                boolean couple = RuleEngine.isCouple(a, b);
                if (mixed && !(couplesAllowed && couple)) return true;
            }
        }
        return false;
    }

    private static Project sampleWithOnly(StandardRule... activeKeys) {
        Project project = ProjectRepository.createSampleProject();
        Set<StandardRule> active = EnumSet.noneOf(StandardRule.class);
        active.addAll(Arrays.asList(activeKeys));
        for (DynamicRule rule : project.getRules()) {
            rule.setActive(active.contains(rule.getStandardKey()));
        }
        return project;
    }

    private static Person personNamed(Project project, String firstname) {
        return project.getPersons().stream().filter(p -> p.getFirstname().equals(firstname)).findFirst().orElseThrow();
    }

    private AssignmentResult calculate(Project project) {
        return service.calculateAssignment(project.getPersons(), project.getBuildings(), project.getRules());
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    @Test
    void beispielprojektMitAllenRegelnErfuelltAlles() {
        Project project = ProjectRepository.createSampleProject();
        AssignmentResult result = calculate(project);
        Map<String, String> rooms = roomOfPerson(result);

        assertEquals(22, result.getAssignments().size(), "Alle 22 Personen müssen ein Bett bekommen");
        assertEquals(0, result.getHardRuleViolationsCount());
        assertFalse(hasMixedNonCoupleRoom(project.getPersons(), result, true), "Keine gemischten Zimmer (außer Paare)");

        // Beide Paare liegen jeweils zusammen
        assertEquals(rooms.get(personNamed(project, "Max").getId()), rooms.get(personNamed(project, "Erika").getId()));
        assertEquals(rooms.get(personNamed(project, "Felix").getId()), rooms.get(personNamed(project, "Lisa").getId()));

        // Jungen- und Mädchengruppe jeweils komplett in einem Zimmer
        Set<String> boysRooms = new HashSet<>();
        Set<String> girlsRooms = new HashSet<>();
        for (Person p : project.getPersons()) {
            if ("Jugendgruppe-Jungs".equals(p.getGroupId())) boysRooms.add(rooms.get(p.getId()));
            if ("Jugendgruppe-Mädels".equals(p.getGroupId())) girlsRooms.add(rooms.get(p.getId()));
        }
        assertEquals(1, boysRooms.size(), "Jungengruppe soll zusammen sein");
        assertEquals(1, girlsRooms.size(), "Mädchengruppe soll zusammen sein");

        // Senioren nie im oberen Hochbett
        Map<String, Bed> beds = new HashMap<>();
        for (Bed bed : project.getAllBeds()) beds.put(bed.getId(), bed);
        for (AssignmentPair pair : result.getAssignments()) {
            Person p = project.getPersons().stream().filter(x -> x.getId().equals(pair.getPersonId())).findFirst().orElseThrow();
            if (p.getAge() > 65) {
                assertFalse(beds.get(pair.getBedId()).isTopBunk(), p.getFullName() + " liegt oben im Hochbett");
            }
        }
    }

    @Test
    void nurGeschlechtertrennungTrenntAuchPaare() {
        Project project = sampleWithOnly(StandardRule.GENDER_SEPARATION);
        AssignmentResult result = calculate(project);

        assertEquals(22, result.getAssignments().size());
        assertEquals(0, result.getHardRuleViolationsCount());
        // Ohne Paar-Regel gibt es KEINE Ausnahme: überhaupt keine gemischten Zimmer
        assertFalse(hasMixedNonCoupleRoom(project.getPersons(), result, false));
    }

    @Test
    void nurPaarRegelBringtPaareZusammen() {
        Project project = sampleWithOnly(StandardRule.COUPLES_TOGETHER);
        AssignmentResult result = calculate(project);
        Map<String, String> rooms = roomOfPerson(result);

        assertEquals(22, result.getAssignments().size());
        assertEquals(rooms.get(personNamed(project, "Max").getId()), rooms.get(personNamed(project, "Erika").getId()));
        assertEquals(rooms.get(personNamed(project, "Felix").getId()), rooms.get(personNamed(project, "Lisa").getId()));
    }

    @Test
    void markiertesMaedchenzimmerBleibtFuerJungenTabu() {
        Building building = new Building("Haus");
        Room girls = new Room("Mädchenzimmer", 0, true, building.getName());
        girls.addSingleBed("M1");
        girls.addSingleBed("M2");
        Room neutral = new Room("Neutral", 0, false, building.getName());
        neutral.addSingleBed("N1");
        neutral.addSingleBed("N2");
        building.getRooms().add(girls);
        building.getRooms().add(neutral);

        List<Person> persons = List.of(
                new Person("1", "A", "Anna", "weiblich", 14, null, null),
                new Person("2", "B", "Ben", "maennlich", 14, null, null),
                new Person("3", "C", "Carl", "maennlich", 14, null, null));

        List<DynamicRule> rules = List.of(StandardRule.GENDER_SEPARATION.createDefaultRule());
        AssignmentResult result = service.calculateAssignment(persons, List.of(building), rules);
        Map<String, String> rooms = roomOfPerson(result);

        assertEquals(3, result.getAssignments().size());
        assertEquals("Mädchenzimmer", rooms.get("1"));
        assertEquals("Neutral", rooms.get("2"));
        assertEquals("Neutral", rooms.get("3"));
    }

    @Test
    void zuWenigeBettenWerdenMitGrundGemeldet() {
        Building building = new Building("Haus");
        Room room = new Room("Zimmer", 0, false, building.getName());
        room.addSingleBed("Bett 1");
        building.getRooms().add(room);

        List<Person> persons = List.of(
                new Person("1", "A", "Anna", "weiblich", 30, null, null),
                new Person("2", "B", "Berta", "weiblich", 30, null, null));

        AssignmentResult result = service.calculateAssignment(persons, List.of(building), List.of());

        assertEquals(1, result.getAssignments().size());
        assertEquals(1, result.getUnassignedPersonIds().size());
        String unassignedId = result.getUnassignedPersonIds().get(0);
        assertTrue(result.getUnassignedReasons().get(unassignedId).contains("Alle Betten sind belegt"));
    }

    @Test
    void geschlechtertrennungWirdNieGebrochenLieberOhneBett() {
        // Ein einziges 4-Bett-Zimmer, 3 Männer und 1 Frau: Mit Geschlechtertrennung
        // passt die Frau nicht dazu -> sie bekommt kein Bett (statt eines Regelverstoßes)
        Building building = new Building("Haus");
        Room room = new Room("Vierer", 0, false, building.getName());
        for (int i = 1; i <= 4; i++) room.addSingleBed("Bett " + i);
        building.getRooms().add(room);

        List<Person> persons = List.of(
                new Person("1", "A", "Anton", "maennlich", 20, null, null),
                new Person("2", "B", "Bernd", "maennlich", 20, null, null),
                new Person("3", "C", "Clara", "weiblich", 20, null, null),
                new Person("4", "D", "Dirk", "maennlich", 20, null, null));

        List<DynamicRule> rules = List.of(StandardRule.GENDER_SEPARATION.createDefaultRule());
        AssignmentResult result = service.calculateAssignment(persons, List.of(building), rules);

        assertEquals(0, result.getHardRuleViolationsCount());
        assertEquals(List.of("3"), result.getUnassignedPersonIds(), "Clara bekommt kein Bett");
        assertTrue(result.getUnassignedReasons().get("3").contains("Geschlechtertrennung"));
    }

    @Test
    void eigeneRegelSeniorenZusammenWirkt() {
        // "Alle Personen über 65 zusammen ins selbe Zimmer" (CONDITION_CO_LOCATION)
        Building building = new Building("Haus");
        for (String name : List.of("Zimmer A", "Zimmer B")) {
            Room room = new Room(name, 0, false, building.getName());
            room.addSingleBed(name + " 1");
            room.addSingleBed(name + " 2");
            building.getRooms().add(room);
        }

        // Reihenfolge so gewählt, dass der Ungarische Algorithmus allein die Senioren trennen würde
        List<Person> persons = List.of(
                new Person("1", "A", "Oma", "weiblich", 80, null, null),
                new Person("2", "B", "Kind1", "weiblich", 10, null, null),
                new Person("3", "C", "Opa", "maennlich", 82, null, null),
                new Person("4", "D", "Kind2", "maennlich", 10, null, null));

        DynamicRule seniorsTogether = new DynamicRule("Senioren zusammen", "", RuleType.SOFT,
                TargetScope.CONDITION_CO_LOCATION, RuleAction.ADD_POINTS, 50);
        seniorsTogether.addCondition(new Condition("person.age", Operator.GREATER_THAN, 65));

        AssignmentResult result = service.calculateAssignment(persons, List.of(building), List.of(seniorsTogether));
        Map<String, String> rooms = roomOfPerson(result);
        assertEquals(rooms.get("1"), rooms.get("3"), "Oma und Opa sollen zusammen sein");
    }

    @Test
    void gleicheEingabeErgibtGleichesErgebnis() {
        AssignmentResult first = calculate(ProjectRepository.createSampleProject());
        AssignmentResult second = calculate(ProjectRepository.createSampleProject());

        // IDs sind bei jedem Aufbau neu – darum über Personen- und Bettnamen vergleichen
        List<String> a = first.getAssignments().stream().map(p -> p.getPersonName() + "->" + p.getBedName()).sorted().toList();
        List<String> b = second.getAssignments().stream().map(p -> p.getPersonName() + "->" + p.getBedName()).sorted().toList();
        assertEquals(a, b);
        assertEquals(first.getTotalScore(), second.getTotalScore());
    }

    @Test
    void auchGrosseProjekteSindSchnellUndRegelkonform() {
        // 200 Personen (100 m / 100 w, 20 Paare, 20 Gruppen), 44 Zimmer mit 4–6 Betten
        List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            String gender = i % 2 == 0 ? "maennlich" : "weiblich";
            String partner = i < 40 ? "P" + (i / 2) : null;     // Personen 0+1, 2+3, ... sind Paare
            String group = i >= 40 ? "G" + (i % 20) : null;
            persons.add(new Person(String.valueOf(i), "Nachname" + i, "Vorname" + i, gender, 12 + (i % 60), partner, group));
        }
        Building building = new Building("Großes Haus");
        for (int r = 0; r < 44; r++) {
            Room room = new Room("Zimmer " + r, r % 3, false, building.getName());
            int beds = 4 + (r % 3);
            for (int b = 0; b < beds; b++) room.addSingleBed("Bett " + r + "-" + b);
            building.getRooms().add(room);
        }

        List<DynamicRule> rules = new ArrayList<>();
        for (StandardRule key : StandardRule.values()) rules.add(key.createDefaultRule());

        long start = System.currentTimeMillis();
        AssignmentResult result = service.calculateAssignment(persons, List.of(building), rules);
        long duration = System.currentTimeMillis() - start;

        assertEquals(0, result.getHardRuleViolationsCount());
        assertFalse(hasMixedNonCoupleRoom(persons, result, true));
        assertTrue(duration < 20_000, "Berechnung dauerte zu lange: " + duration + " ms");
        assertTrue(result.getAssignments().size() >= 190,
                "Fast alle sollten ein Bett bekommen, es waren aber nur " + result.getAssignments().size());
    }
}
