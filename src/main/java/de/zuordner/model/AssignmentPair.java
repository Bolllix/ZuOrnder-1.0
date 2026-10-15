package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine einzelne Zuordnung im Ergebnis: Person -> Bett, mit Punkten und
 * Begründungen. Namen von Person, Bett, Raum und Gebäude sind zusätzlich
 * gespeichert, damit das Dashboard sie direkt anzeigen kann.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentPair {
    private String personId;
    private String bedId;
    private String personName;
    private String bedName;
    private String roomName;
    private String buildingName;
    private int score;
    private List<ScoreExplanation> explanations;
    /** true = von Hand gesetzt (nicht vom Optimierer) */
    private boolean manualOverride;

    public AssignmentPair() {
        this.explanations = new ArrayList<>();
    }

    public AssignmentPair(String personId, String bedId, String personName, String bedName, String roomName, String buildingName, int score, List<ScoreExplanation> explanations) {
        this.personId = personId;
        this.bedId = bedId;
        this.personName = personName;
        this.bedName = bedName;
        this.roomName = roomName;
        this.buildingName = buildingName;
        this.score = score;
        this.explanations = explanations != null ? explanations : new ArrayList<>();
        this.manualOverride = false;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getBedId() {
        return bedId;
    }

    public void setBedId(String bedId) {
        this.bedId = bedId;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getBedName() {
        return bedName;
    }

    public void setBedName(String bedName) {
        this.bedName = bedName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<ScoreExplanation> getExplanations() {
        return explanations;
    }

    public void setExplanations(List<ScoreExplanation> explanations) {
        this.explanations = explanations;
    }

    public boolean isManualOverride() {
        return manualOverride;
    }

    public void setManualOverride(boolean manualOverride) {
        this.manualOverride = manualOverride;
    }
}
