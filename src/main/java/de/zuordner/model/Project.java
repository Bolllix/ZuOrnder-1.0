package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * Project – ein komplettes Projekt (z.B. "Sommerfreizeit 2026")
 * ============================================================================
 * Ein Projekt bündelt alles, was für eine Zimmerverteilung nötig ist:
 *   - Gebäude (mit Räumen und Betten)
 *   - Teilnehmer (Personen)
 *   - Regeln
 *   - das zuletzt berechnete Ergebnis
 * Jedes Projekt wird als eigene JSON-Datei gespeichert (siehe ProjectRepository).
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}: Unbekannte Felder in alten
 * oder neueren Projektdateien werden beim Einlesen einfach übersprungen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    private String id;
    private String name;
    private String description;
    private List<Building> buildings;
    private List<Person> persons;
    private List<DynamicRule> rules;
    /** Letztes Berechnungsergebnis (null, solange noch nichts berechnet wurde) */
    private AssignmentResult assignmentResult;
    private String createdAt;
    private String updatedAt;

    public Project() {
        this.id = UUID.randomUUID().toString();
        this.buildings = new ArrayList<>();
        this.persons = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.createdAt = LocalDateTime.now().toString();
        this.updatedAt = LocalDateTime.now().toString();
    }

    public Project(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public void setBuildings(List<Building> buildings) {
        this.buildings = buildings;
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    public List<DynamicRule> getRules() {
        return rules;
    }

    public void setRules(List<DynamicRule> rules) {
        this.rules = rules;
    }

    public AssignmentResult getAssignmentResult() {
        return assignmentResult;
    }

    public void setAssignmentResult(AssignmentResult assignmentResult) {
        this.assignmentResult = assignmentResult;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Alle Betten aller Gebäude in einer Liste.
     * {@code @JsonIgnore}: wird NICHT mitgespeichert – die Betten stehen ja schon
     * in den Gebäuden. (Vor Version 2.0 standen sie dadurch doppelt in jeder Datei.)
     */
    @JsonIgnore
    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Building building : buildings) {
            allBeds.addAll(building.getAllBeds());
        }
        return allBeds;
    }
}
