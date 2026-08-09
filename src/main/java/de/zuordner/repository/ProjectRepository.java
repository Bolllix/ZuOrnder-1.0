package de.zuordner.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.zuordner.model.*;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProjectRepository {

    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path storageDir;

    public ProjectRepository() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.storageDir = Paths.get("data", "projects");
        initStorage();
        loadProjectsFromDisk();

        // Always ensure sample project exists
        if (projects.isEmpty() || !projects.containsKey("sample-project-01")) {
            Project sample = createSampleProject();
            saveProject(sample);
        }
    }

    private void initStorage() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProjectsFromDisk() {
        try {
            File dir = storageDir.toFile();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            Project project = objectMapper.readValue(f, Project.class);
                            if (project != null && project.getId() != null) {
                                projects.put(project.getId(), project);
                            }
                        } catch (Exception e) {
                            System.err.println("Fehler beim Laden von Projekt " + f.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Project> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    public Optional<Project> getProjectById(String id) {
        return Optional.ofNullable(projects.get(id));
    }

    public Project saveProject(Project project) {
        if (project.getId() == null || project.getId().trim().isEmpty()) {
            project.setId(UUID.randomUUID().toString());
        }
        projects.put(project.getId(), project);
        saveProjectToDisk(project);
        return project;
    }

    public boolean deleteProject(String id) {
        Project removed = projects.remove(id);
        if (removed != null) {
            try {
                Path filePath = storageDir.resolve(id + ".json");
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void saveProjectToDisk(Project project) {
        try {
            Path filePath = storageDir.resolve(project.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), project);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Project createSampleProject() {
        Project p = new Project("Muster-Jugendherberge (Vollständig)", "Test-Projekt mit 22 Teilnehmern (Max Mustermann & Co.), 3 Gebäuden und allen Harte/Weiche Regeln.");
        p.setId("sample-project-01");

        // 22 Participants with clean Mustermann names testing ALL rules
        List<Person> persons = new ArrayList<>();

        // Couple 1 (Seniors)
        persons.add(new Person("Mustermann", "Max", "maennlich", 72, "PAAR_MUSTERMANN"));
        persons.add(new Person("Musterfrau", "Erika", "weiblich", 70, "PAAR_MUSTERMANN"));

        // Couple 2 (Young Adults)
        persons.add(new Person("Mustermann", "Felix", "maennlich", 28, "PAAR_FELIX_LISA"));
        persons.add(new Person("Musterfrau", "Lisa", "weiblich", 26, "PAAR_FELIX_LISA"));

        // Single Seniors (>65) -> Tests Senior Bottom Bunk Rule
        persons.add(new Person("Mustermann", "Otto", "maennlich", 78, null, "Senioren-Club"));
        persons.add(new Person("Musterfrau", "Gerti", "weiblich", 75, null, "Senioren-Club"));

        // Boys Group (8 members) -> Tests Boys Room & Group Rules
        for (int i = 1; i <= 8; i++) {
            persons.add(new Person("Mustermann", "Junge " + i, "maennlich", 16 + i, null, "Jugendgruppe-Jungs"));
        }

        // Girls Group (8 members) -> Tests Girls Room & Group Rules
        for (int i = 1; i <= 8; i++) {
            persons.add(new Person("Musterfrau", "Mädchen " + i, "weiblich", 15 + i, null, "Jugendgruppe-Mädels"));
        }

        p.setPersons(persons);

        // Buildings & Rooms setup (Total 22 beds)
        Building b1 = new Building("Gebäude A (Senioren & Paare)");
        Room r1 = new Room("Raum EG-01 (Senioren)", 0, false, b1.getName());
        r1.addSingleBed("Einzelbett EG-1");
        r1.addSingleBed("Einzelbett EG-2");
        r1.addSingleBed("Einzelbett EG-3");
        r1.addSingleBed("Einzelbett EG-4");

        Room r2 = new Room("Raum 101 (Paarzimmer)", 1, false, b1.getName());
        r2.addDoubleBed("Doppelbett 1");
        r2.addDoubleBed("Doppelbett 2");

        b1.getRooms().add(r1);
        b1.getRooms().add(r2);

        Building b2 = new Building("Gebäude B (Jugendtrakt)");
        Room r3 = new Room("Jungenzimmer 201", 1, false, b2.getName());
        r3.setBoysRoom(true);
        r3.addBunkBed("Jungs Hochbett 1");
        r3.addBunkBed("Jungs Hochbett 2");
        r3.addBunkBed("Jungs Hochbett 3");
        r3.addBunkBed("Jungs Hochbett 4");

        Room r4 = new Room("Mädchenzimmer 202", 1, true, b2.getName());
        r4.addBunkBed("Mädels Hochbett 1");
        r4.addBunkBed("Mädels Hochbett 2");
        r4.addBunkBed("Mädels Hochbett 3");
        r4.addBunkBed("Mädels Hochbett 4");

        b2.getRooms().add(r3);
        b2.getRooms().add(r4);

        p.getBuildings().add(b1);
        p.getBuildings().add(b2);

        // Default Rules Configuration
        List<DynamicRule> rules = new ArrayList<>();

        // Standard-Regel 1: HARTE GESCHLECHTERTRENNUNG (Männer im Mädchenzimmer verboten)
        DynamicRule hardGirlsRoom = new DynamicRule(
                "Harte Geschlechtertrennung (Mädchenzimmer)",
                "Männliche Teilnehmer dürfen unter keinen Umständen im Mädchenzimmer untergebracht werden.",
                RuleType.HARD,
                TargetScope.ROOM_PERSON,
                RuleAction.FORBID,
                -999999
        );
        hardGirlsRoom.addCondition(new Condition("person.gender", Operator.EQUALS, "maennlich"));
        hardGirlsRoom.addCondition(new Condition("room.girlsRoom", Operator.EQUALS, true));
        rules.add(hardGirlsRoom);

        // Standard-Regel 2: HARTE GESCHLECHTERTRENNUNG (Frauen im Jungenzimmer verboten)
        DynamicRule hardBoysRoom = new DynamicRule(
                "Harte Geschlechtertrennung (Jungenzimmer)",
                "Weibliche Teilnehmer dürfen unter keinen Umständen im Jungenzimmer untergebracht werden.",
                RuleType.HARD,
                TargetScope.ROOM_PERSON,
                RuleAction.FORBID,
                -999999
        );
        hardBoysRoom.addCondition(new Condition("person.gender", Operator.EQUALS, "weiblich"));
        hardBoysRoom.addCondition(new Condition("room.boysRoom", Operator.EQUALS, true));
        rules.add(hardBoysRoom);

        // Standard-Regel 3: HARTE REGEL: Senioren (>65 Jahre) nicht im oberen Hochbett
        DynamicRule seniorTopBunkRule = new DynamicRule(
                "Senioren nicht im oberen Hochbett",
                "Personen ab 65 Jahren dürfen aus Sicherheitsgründen nicht im oberen Hochbett schlafen.",
                RuleType.HARD,
                TargetScope.BED_PERSON,
                RuleAction.FORBID,
                -999999
        );
        seniorTopBunkRule.addCondition(new Condition("person.age", Operator.GREATER_THAN, 65));
        seniorTopBunkRule.addCondition(new Condition("bed.isTopBunk", Operator.EQUALS, true));
        rules.add(seniorTopBunkRule);

        // Standard-Regel 4: WEICHE REGEL: Paare im selben Zimmer
        DynamicRule coupleRule = new DynamicRule(
                "Paare im selben Zimmer",
                "Personen mit der gleichen Paar-ID sollen bevorzugt im selben Zimmer untergebracht werden (+50 Punkte).",
                RuleType.SOFT,
                TargetScope.PAIR_CO_LOCATION,
                RuleAction.ADD_POINTS,
                50
        );
        rules.add(coupleRule);

        // Standard-Regel 5: WEICHE REGEL: Gruppen im selben Zimmer
        DynamicRule groupRule = new DynamicRule(
                "Gruppenzusammenhalt im Zimmer",
                "Personen der gleichen Gruppe sollen bevorzugt im selben Zimmer untergebracht werden (+20 Punkte).",
                RuleType.SOFT,
                TargetScope.GROUP_CO_LOCATION,
                RuleAction.ADD_POINTS,
                20
        );
        rules.add(groupRule);

        p.setRules(rules);
        return p;
    }
}
