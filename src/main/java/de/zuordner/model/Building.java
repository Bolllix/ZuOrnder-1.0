package de.zuordner.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public void setName(String name) {
        this.name = name;
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

    public List<Bed> getAllBeds() {
        List<Bed> allBeds = new ArrayList<>();
        for (Room room : rooms) {
            allBeds.addAll(room.getBeds());
        }
        return allBeds;
    }
}
