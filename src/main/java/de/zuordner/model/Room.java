package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {
    private String id;
    private String name;
    private int floor;
    private String buildingName;
    private boolean girlsRoom;
    private boolean boysRoom;
    private List<Bed> beds;
    private Map<String, Object> customAttributes;

    public Room() {
        this.id = UUID.randomUUID().toString();
        this.beds = new ArrayList<>();
        this.customAttributes = new HashMap<>();
    }

    public Room(String name, int floor, boolean girlsRoom, String buildingName) {
        this();
        this.name = name;
        this.floor = floor;
        this.girlsRoom = girlsRoom;
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

    public boolean isGirlsRoom() {
        return girlsRoom;
    }

    public void setGirlsRoom(boolean girlsRoom) {
        this.girlsRoom = girlsRoom;
    }

    public boolean isBoysRoom() {
        return boysRoom;
    }

    public void setBoysRoom(boolean boysRoom) {
        this.boysRoom = boysRoom;
    }

    public List<Bed> getBeds() {
        return beds;
    }

    public void setBeds(List<Bed> beds) {
        this.beds = beds;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public int getCapacity() {
        return beds.size();
    }

    public void addBed(Bed bed) {
        bed.setRoomName(this.name);
        bed.setFloor(this.floor);
        bed.setBuildingName(this.buildingName);
        this.beds.add(bed);
    }

    public void addSingleBed(String bedName) {
        addBed(new Bed(bedName, BedType.SINGLE, this.name, this.floor, this.buildingName));
    }

    public void addDoubleBed(String bedNamePrefix) {
        addBed(new Bed(bedNamePrefix + " (A)", BedType.DOUBLE, this.name, this.floor, this.buildingName));
        addBed(new Bed(bedNamePrefix + " (B)", BedType.DOUBLE, this.name, this.floor, this.buildingName));
    }

    public void addBunkBed(String bunkNamePrefix) {
        addBed(new Bed(bunkNamePrefix + " (Unten)", BedType.BOTTOM_BUNK, this.name, this.floor, this.buildingName));
        addBed(new Bed(bunkNamePrefix + " (Oben)", BedType.TOP_BUNK, this.name, this.floor, this.buildingName));
    }
}
