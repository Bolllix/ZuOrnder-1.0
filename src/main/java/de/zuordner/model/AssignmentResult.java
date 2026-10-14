package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * AssignmentResult – das Ergebnis einer Belegungs-Berechnung
 * ============================================================================
 * Enthält, wer in welchem Bett schläft ({@link #assignments}), wer kein Bett
 * bekommen hat (und warum), welche Betten frei bleiben und ein paar Kennzahlen.
 * Wird im Projekt gespeichert und im Dashboard angezeigt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentResult {
    /** Alle Zuordnungen "Person -> Bett" inklusive Punkte und Begründungen */
    private List<AssignmentPair> assignments;
    /** IDs der Personen, die kein Bett bekommen haben */
    private List<String> unassignedPersonIds;
    /** Grund pro Person ohne Bett (Person-ID -> Text), z.B. "Alle Betten sind belegt" */
    private Map<String, String> unassignedReasons;
    /** IDs der Betten, die frei bleiben */
    private List<String> unassignedBedIds;
    /** Summe aller Punkte der Zuordnungen (je höher, desto mehr Wünsche erfüllt) */
    private int totalScore;
    /** Anzahl verletzter harter Regeln in der fertigen Belegung (Ziel: 0) */
    private int hardRuleViolationsCount;
    /** Wie lange die Berechnung gedauert hat (Millisekunden) */
    private long executionTimeMs;

    public AssignmentResult() {
        this.assignments = new ArrayList<>();
        this.unassignedPersonIds = new ArrayList<>();
        this.unassignedReasons = new LinkedHashMap<>();
        this.unassignedBedIds = new ArrayList<>();
    }

    public List<AssignmentPair> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentPair> assignments) {
        this.assignments = assignments;
    }

    public List<String> getUnassignedPersonIds() {
        return unassignedPersonIds;
    }

    public void setUnassignedPersonIds(List<String> unassignedPersonIds) {
        this.unassignedPersonIds = unassignedPersonIds;
    }

    public Map<String, String> getUnassignedReasons() {
        return unassignedReasons;
    }

    public void setUnassignedReasons(Map<String, String> unassignedReasons) {
        this.unassignedReasons = unassignedReasons != null ? unassignedReasons : new LinkedHashMap<>();
    }

    public List<String> getUnassignedBedIds() {
        return unassignedBedIds;
    }

    public void setUnassignedBedIds(List<String> unassignedBedIds) {
        this.unassignedBedIds = unassignedBedIds;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getHardRuleViolationsCount() {
        return hardRuleViolationsCount;
    }

    public void setHardRuleViolationsCount(int hardRuleViolationsCount) {
        this.hardRuleViolationsCount = hardRuleViolationsCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
