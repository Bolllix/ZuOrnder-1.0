package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * Bed – ein einzelner Schlafplatz
 * ============================================================================
 * Achtung: Ein Doppelbett oder Hochbett besteht aus ZWEI Bed-Einträgen
 * (zwei Schlafplätze). Jeder Eintrag kann genau eine Person aufnehmen.
 *
 * Raumname, Etage und Gebäudename werden zusätzlich im Bett gespeichert,
 * damit man sie bei der Anzeige direkt zur Hand hat.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bed {
    private String id;
    private String name;
    private BedType bedType;
    private String roomName;
    private int floor;
    private String buildingName;
    /** Wird aktuell nicht verwendet (die Belegung steht im AssignmentResult) */
    private String occupantId;
    /** Freie Zusatzangaben für eigene Regeln */
    private Map<String, Object> customAttributes;

    public Bed() {
        this.id = UUID.randomUUID().toString();
        this.bedType = BedType.SINGLE;
        this.customAttributes = new HashMap<>();
    }

    public Bed(String name, BedType bedType, String roomName, int floor, String buildingName) {
        this();
        this.name = name;
        this.bedType = bedType;
        this.roomName = roomName;
        this.floor = floor;
        this.buildingName = buildingName;
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

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getOccupantId() {
        return occupantId;
    }

    public void setOccupantId(String occupantId) {
        this.occupantId = occupantId;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    /** true, wenn dies der obere Platz eines Hochbetts ist */
    public boolean isTopBunk() {
        return bedType == BedType.TOP_BUNK;
    }

    /** true, wenn dies der untere Platz eines Hochbetts ist */
    public boolean isBottomBunk() {
        return bedType == BedType.BOTTOM_BUNK;
    }
}
