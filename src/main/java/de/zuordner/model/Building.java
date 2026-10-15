package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * Building – ein Gebäude (z.B. "Haupthaus") mit seinen Räumen
 * ============================================================================
 * Aufbau der Unterkunft:  Gebäude -> Räume -> Betten
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Building {
    private String id;
    private String name;
    private List<Room> rooms;

    public Building() {
        this.id = UUID.randomUUID().toString();
        this.rooms = new ArrayList<>();
    }

    public Building(String name) {
        this();
        this.name = name;
    }

    public Building(String name, List<Room> rooms) {
        this(name);
        this.rooms = rooms;
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

    /**
     * Ändert den Gebäudenamen. Räume und Betten merken sich den Gebäudenamen
     * ebenfalls (für Anzeige und Excel-Export), darum wird er dort mitgeändert.
     */
    public void setName(String name) {
        this.name = name;
        if (rooms == null) return;
        for (Room room : rooms) {
            room.setBuildingName(name);
            for (Bed bed : room.getBeds()) {
                bed.setBuildingName(name);
            }
        }
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    /**
     * Alle Betten aus allen Räumen dieses Gebäudes.
     * {@code @JsonIgnore}: wird nicht gespeichert (die Betten stehen schon in den Räumen).
     */
    @JsonIgnore
    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Room room : rooms) {
            allBeds.addAll(room.getBeds());
        }
        return allBeds;
    }
}
