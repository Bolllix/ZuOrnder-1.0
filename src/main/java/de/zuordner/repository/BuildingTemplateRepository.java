package de.zuordner.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.zuordner.model.Bed;
import de.zuordner.model.Building;
import de.zuordner.model.Project;
import de.zuordner.model.Room;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BuildingTemplateRepository {

    private final Map<String, Building> templates = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path storageDir;
    private final ProjectRepository projectRepository;

    public BuildingTemplateRepository(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.trim().isEmpty()) {
            this.storageDir = Paths.get(appData, "ZuORDNER", "data", "building-templates");
        } else {
            this.storageDir = Paths.get("data", "building-templates");
        }

        initStorage();
        loadTemplatesFromDisk();
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

    private void loadTemplatesFromDisk() {
        try {
            File dir = storageDir.toFile();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            Building template = objectMapper.readValue(f, Building.class);
                            if (template != null && template.getId() != null) {
                                templates.put(template.getId(), template);
                            }
                        } catch (Exception e) {
                            System.err.println("Fehler beim Laden der Gebäude-Vorlage " + f.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Building> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }

    /**
     * Sammelt alle verfügbaren Gebäude: gespeicherte Vorlagen PLUS alle Gebäude aus allen bisherigen Projekten!
     */
    public List<Map<String, Object>> getAllAvailableBuildingsWithSource() {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        // 1. Zuerst explizit gespeicherte Vorlagen
        for (Building t : templates.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("name", t.getName());
            item.put("source", "Bibliothek / Vorlage");
            item.put("roomCount", t.getRooms().size());
            item.put("bedCount", t.getAllBeds().size());
            item.put("building", t);
            result.add(item);
            seenNames.add(t.getName().toLowerCase());
        }

        // 2. Gebäude aus allen existierenden Projekten
        for (Project p : projectRepository.getAllProjects()) {
            if (p.getBuildings() != null) {
                for (Building b : p.getBuildings()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", b.getId());
                    item.put("name", b.getName());
                    item.put("source", "Projekt: " + p.getName());
                    item.put("roomCount", b.getRooms().size());
                    item.put("bedCount", b.getAllBeds().size());
                    item.put("building", b);
                    result.add(item);
                }
            }
        }

        return result;
    }

    public Building saveTemplate(Building building) {
        if (building.getId() == null || building.getId().trim().isEmpty()) {
            building.setId(UUID.randomUUID().toString());
        }
        templates.put(building.getId(), building);
        saveTemplateToDisk(building);
        return building;
    }

    public boolean deleteTemplate(String id) {
        Building removed = templates.remove(id);
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

    private void saveTemplateToDisk(Building building) {
        try {
            Path filePath = storageDir.resolve(building.getId() + ".json");
            objectMapper.writeValue(filePath.toFile(), building);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Erstellt eine saubere Tiefenkopie eines Gebäudes mit neuen UUIDs für Betten und Räume.
     */
    public Building cloneBuilding(Building source) {
        Building copy = new Building(source.getName());
        if (source.getRooms() != null) {
            for (Room r : source.getRooms()) {
                Room rCopy = new Room(r.getName(), r.getFloor(), r.isGirlsRoom(), copy.getName());
                rCopy.setBoysRoom(r.isBoysRoom());
                if (r.getBeds() != null) {
                    for (Bed b : r.getBeds()) {
                        Bed bCopy = new Bed(b.getName(), b.getBedType(), rCopy.getName(), rCopy.getFloor(), copy.getName());
                        rCopy.getBeds().add(bCopy);
                    }
                }
                copy.getRooms().add(rCopy);
            }
        }
        return copy;
    }
}
