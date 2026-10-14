package de.zuordner.controller;

import de.zuordner.model.Bed;
import de.zuordner.model.Building;
import de.zuordner.model.Project;
import de.zuordner.model.Room;
import de.zuordner.repository.ProjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================================
 * BuildingController – Gebäude, Räume und Betten eines Projekts verwalten
 * ============================================================================
 * Endpunkte (alle unter /api/projects/{projectId}/buildings):
 *   GET    /                                   -> alle Gebäude
 *   POST   /                                   -> Gebäude anlegen
 *   DELETE /{buildingId}                       -> Gebäude löschen
 *   POST   /{buildingId}/rooms                 -> Raum (mit Betten) anlegen
 *   DELETE /{buildingId}/rooms/{roomId}        -> Raum löschen
 *   POST   /{buildingId}/rooms/{roomId}/beds   -> einzelnes Bett hinzufügen
 *   DELETE /{buildingId}/rooms/{roomId}/beds/{bedId} -> Bett löschen
 * Alle Änderungen geben das komplette, gespeicherte Projekt zurück.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/buildings")
@CrossOrigin(origins = "*")
public class BuildingController {

    private final ProjectRepository projectRepository;

    public BuildingController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<Building>> getBuildings(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> ResponseEntity.ok(p.getBuildings()))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Neues Gebäude anlegen. */
    @PostMapping
    public ResponseEntity<Project> addBuilding(@PathVariable String projectId, @RequestBody Building building) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getBuildings().add(building);
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Gebäude samt Räumen und Betten löschen. */
    @DeleteMapping("/{buildingId}")
    public ResponseEntity<Project> deleteBuilding(@PathVariable String projectId, @PathVariable String buildingId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    p.getBuildings().removeIf(b -> b.getId().equals(buildingId));
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Neuen Raum anlegen. Gebäudename, Raumname und Etage werden auf alle
     * mitgeschickten Betten übertragen.
     */
    @PostMapping("/{buildingId}/rooms")
    public ResponseEntity<Project> addRoom(@PathVariable String projectId, @PathVariable String buildingId, @RequestBody Room room) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (Building b : p.getBuildings()) {
                        if (b.getId().equals(buildingId)) {
                            room.setBuildingName(b.getName());
                            for (Bed bed : room.getBeds()) {
                                bed.setBuildingName(b.getName());
                                bed.setRoomName(room.getName());
                                bed.setFloor(room.getFloor());
                            }
                            b.getRooms().add(room);
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Raum samt Betten löschen. */
    @DeleteMapping("/{buildingId}/rooms/{roomId}")
    public ResponseEntity<Project> deleteRoom(@PathVariable String projectId, @PathVariable String buildingId, @PathVariable String roomId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (Building b : p.getBuildings()) {
                        if (b.getId().equals(buildingId)) {
                            b.getRooms().removeIf(r -> r.getId().equals(roomId));
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Ein einzelnes Bett zu einem Raum hinzufügen. */
    @PostMapping("/{buildingId}/rooms/{roomId}/beds")
    public ResponseEntity<Project> addBed(@PathVariable String projectId, @PathVariable String buildingId, @PathVariable String roomId, @RequestBody Bed bed) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (Building b : p.getBuildings()) {
                        if (b.getId().equals(buildingId)) {
                            for (Room r : b.getRooms()) {
                                if (r.getId().equals(roomId)) {
                                    bed.setBuildingName(b.getName());
                                    bed.setRoomName(r.getName());
                                    bed.setFloor(r.getFloor());
                                    r.getBeds().add(bed);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Ein einzelnes Bett löschen. */
    @DeleteMapping("/{buildingId}/rooms/{roomId}/beds/{bedId}")
    public ResponseEntity<Project> deleteBed(@PathVariable String projectId, @PathVariable String buildingId, @PathVariable String roomId, @PathVariable String bedId) {
        return projectRepository.getProjectById(projectId)
                .map(p -> {
                    for (Building b : p.getBuildings()) {
                        if (b.getId().equals(buildingId)) {
                            for (Room r : b.getRooms()) {
                                if (r.getId().equals(roomId)) {
                                    r.getBeds().removeIf(bd -> bd.getId().equals(bedId));
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    Project saved = projectRepository.saveProject(p);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
