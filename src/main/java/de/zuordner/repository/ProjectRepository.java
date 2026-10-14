package de.zuordner.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.zuordner.model.*;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * ProjectRepository – lädt und speichert alle Projekte
 * ============================================================================
 * Jedes Projekt wird als eigene JSON-Datei gespeichert (JSON = Textformat,
 * das man auch mit einem Editor öffnen kann). Speicherort:
 *   - Windows-Desktop-Version: %APPDATA%\ZuORDNER\data\projects\  (bleibt bei Updates erhalten)
 *   - sonst (z.B. Entwicklung):  data/projects/ im Programmordner
 *
 * Beim Start werden alle Dateien eingelesen und im Arbeitsspeicher gehalten
 * (in der Map {@link #projects}). Jede Änderung wird sofort wieder auf die
 * Festplatte geschrieben.
 *
 * {@code @Repository} ist für Spring Boot ein Hinweis: "Diese Klasse kümmert
 * sich um das Speichern von Daten" – Spring erzeugt automatisch ein Objekt davon.
 */
@Repository
public class ProjectRepository {

    /** ID des mitgelieferten Beispielprojekts */
    public static final String SAMPLE_PROJECT_ID = "sample-project-01";

    /** Alle Projekte im Arbeitsspeicher: Projekt-ID -> Projekt */
    private final Map<String, Project> projects = new ConcurrentHashMap<>();
    /** Wandelt Java-Objekte in JSON-Text um und zurück (INDENT_OUTPUT = schön eingerückt) */
    private final ObjectMapper objectMapper;
    /** Ordner, in dem die Projektdateien liegen */
    private final Path storageDir;

    public ProjectRepository() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        // Unter Windows gibt es die Umgebungsvariable APPDATA (z.B. C:\Users\Max\AppData\Roaming)
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.trim().isEmpty()) {
            this.storageDir = Paths.get(appData, "ZuORDNER", "data", "projects");
        } else {
            this.storageDir = Paths.get("data", "projects");
        }

        initStorage();
        loadProjectsFromDisk();

        // Beim allerersten Start (noch gar keine Projekte) das Beispielprojekt anlegen.
        // Löscht man das Beispielprojekt später, kommt es nicht ungefragt zurück.
        if (projects.isEmpty()) {
            Project sample = createSampleProject();
            saveProject(sample);
        }
    }

    /** Legt den Speicherordner an, falls er noch nicht existiert. */
    private void initStorage() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Liest alle *.json-Dateien aus dem Speicherordner ein. Alte Dateien aus
     * Version 1.x werden dabei automatisch repariert ({@link ProjectMigration});
     * vorher wird eine Sicherheitskopie "*.json.v1-backup" angelegt.
     */
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
                                if (ProjectMigration.migrate(project)) {
                                    backupBeforeMigration(f);
                                    saveProjectToDisk(project);
                                }
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

    /** Kopiert eine Projektdatei nach "<name>.v1-backup" (nur einmal, eine vorhandene Kopie bleibt). */
    private void backupBeforeMigration(File original) {
        try {
            Path backup = original.toPath().resolveSibling(original.getName() + ".v1-backup");
            if (!Files.exists(backup)) {
                Files.copy(original.toPath(), backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception e) {
            System.err.println("Sicherheitskopie von " + original.getName() + " fehlgeschlagen: " + e.getMessage());
        }
    }

    /** Liefert alle Projekte als Liste. */
    public List<Project> getAllProjects() {
        return new ArrayList<>(projects.values());
    }

    /**
     * Sucht ein Projekt anhand seiner ID. "Optional" ist eine Hülle, die entweder
     * ein Projekt enthält oder leer ist (statt null) – so vergisst man die
     * "nicht gefunden"-Prüfung nicht so leicht.
     */
    public Optional<Project> getProjectById(String id) {
        return Optional.ofNullable(projects.get(id));
    }

    /** Speichert ein Projekt (neu oder geändert) im Arbeitsspeicher UND auf der Festplatte. */
    public Project saveProject(Project project) {
        if (project.getId() == null || project.getId().trim().isEmpty()) {
            project.setId(UUID.randomUUID().toString());
        }
        projects.put(project.getId(), project);
        saveProjectToDisk(project);
        return project;
    }

    /** Löscht ein Projekt samt Datei. Gibt false zurück, wenn es das Projekt nicht gab. */
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

    /** Schreibt ein Projekt als "<id>.json" in den Speicherordner. */
    private void saveProjectToDisk(Project project) {
        try {
            Path filePath = storageDir.resolve(project.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), project);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Baut das Beispielprojekt "Muster-Jugendherberge", mit dem man alle
     * Standard-Regeln ausprobieren kann:
     *  - 2 Paare (eins davon Senioren) -> Paar-Regel + Paar-Ausnahme bei Geschlechtertrennung
     *  - 2 einzelne Senioren          -> Senioren-Regel (nicht ins obere Hochbett)
     *  - 8 Jungen, 8 Mädchen in Gruppen -> Gruppen-Regel + Geschlechtertrennung
     * Es gibt genug passende Betten, sodass ALLE Regeln gleichzeitig erfüllt werden können.
     * ("static" = man braucht dafür kein ProjectRepository-Objekt, praktisch für Tests.)
     */
    public static Project createSampleProject() {
        Project p = new Project("Muster-Jugendherberge (Vollständig)",
                "Beispielprojekt mit 22 Teilnehmern (2 Paare, Senioren, Jungen- und Mädchengruppe), "
                        + "2 Gebäuden und allen Standard-Regeln.");
        p.setId(SAMPLE_PROJECT_ID);

        // --- Teilnehmer ---------------------------------------------------------
        List<Person> persons = new ArrayList<>();

        // Paar 1 (Senioren)
        persons.add(new Person("Mustermann", "Max", "maennlich", 72, "PAAR_MUSTERMANN"));
        persons.add(new Person("Musterfrau", "Erika", "weiblich", 70, "PAAR_MUSTERMANN"));

        // Paar 2 (junge Erwachsene)
        persons.add(new Person("Mustermann", "Felix", "maennlich", 28, "PAAR_FELIX_LISA"));
        persons.add(new Person("Musterfrau", "Lisa", "weiblich", 26, "PAAR_FELIX_LISA"));

        // Einzelne Senioren (> 65) -> testet "Senioren nicht ins obere Hochbett"
        persons.add(new Person("Mustermann", "Otto", "maennlich", 78, null, "Senioren-Club"));
        persons.add(new Person("Musterfrau", "Gerti", "weiblich", 75, null, "Senioren-Club"));

        // Jungengruppe (8 Personen) -> testet Gruppen-Regel und Geschlechtertrennung
        for (int i = 1; i <= 8; i++) {
            persons.add(new Person("Mustermann", "Junge " + i, "maennlich", 12 + i, null, "Jugendgruppe-Jungs"));
        }

        // Mädchengruppe (8 Personen)
        for (int i = 1; i <= 8; i++) {
            persons.add(new Person("Musterfrau", "Mädchen " + i, "weiblich", 12 + i, null, "Jugendgruppe-Mädels"));
        }

        p.setPersons(persons);

        // --- Gebäude, Räume, Betten (24 Betten, alle Räume neutral) -------------
        // Neutral = nicht als Mädchen-/Jungenzimmer markiert. Wer wohin kommt,
        // entscheidet die Optimierung anhand der Regeln.
        Building b1 = new Building("Gebäude A (Paare & Senioren)");

        Room r1 = new Room("Doppelzimmer 101", 1, false, b1.getName());
        r1.addDoubleBed("Doppelbett 101");

        Room r2 = new Room("Doppelzimmer 102", 1, false, b1.getName());
        r2.addDoubleBed("Doppelbett 102");

        Room r3 = new Room("Zweibettzimmer EG-01", 0, false, b1.getName());
        r3.addSingleBed("Einzelbett EG-01 A");
        r3.addSingleBed("Einzelbett EG-01 B");

        Room r4 = new Room("Zweibettzimmer EG-02", 0, false, b1.getName());
        r4.addSingleBed("Einzelbett EG-02 A");
        r4.addSingleBed("Einzelbett EG-02 B");

        b1.getRooms().add(r1);
        b1.getRooms().add(r2);
        b1.getRooms().add(r3);
        b1.getRooms().add(r4);

        Building b2 = new Building("Gebäude B (Jugendtrakt)");
        Room r5 = new Room("Zimmer 201", 1, false, b2.getName());
        r5.addBunkBed("Hochbett 1");
        r5.addBunkBed("Hochbett 2");
        r5.addBunkBed("Hochbett 3");
        r5.addBunkBed("Hochbett 4");

        Room r6 = new Room("Zimmer 202", 1, false, b2.getName());
        r6.addBunkBed("Hochbett 5");
        r6.addBunkBed("Hochbett 6");
        r6.addBunkBed("Hochbett 7");
        r6.addBunkBed("Hochbett 8");

        b2.getRooms().add(r5);
        b2.getRooms().add(r6);

        p.getBuildings().add(b1);
        p.getBuildings().add(b2);

        // --- Regeln: alle 4 Standard-Regeln, eingeschaltet ----------------------
        List<DynamicRule> rules = new ArrayList<>();
        for (StandardRule standard : StandardRule.values()) {
            rules.add(standard.createDefaultRule());
        }
        p.setRules(rules);
        return p;
    }
}
