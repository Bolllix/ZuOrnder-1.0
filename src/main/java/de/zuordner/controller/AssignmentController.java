package de.zuordner.controller;

import de.zuordner.engine.AssignmentService;
import de.zuordner.model.*;
import de.zuordner.repository.ProjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * ============================================================================
 * AssignmentController – Belegung berechnen und als Excel exportieren
 * ============================================================================
 * Endpunkte (alle unter /api/projects/{projectId}/assignment):
 *   POST /calculate     -> Optimierung starten, Ergebnis im Projekt speichern
 *   POST /override      -> Zuordnungen von Hand setzen (aktuell nicht im Frontend benutzt)
 *   GET  /export/excel  -> Belegungsplan als .xlsx-Datei herunterladen
 *
 * {@code @CrossOrigin(origins = "*")}: Erlaubt Aufrufe auch von einer anderen
 * Adresse (z.B. vom Vite-Entwicklungsserver auf Port 5173).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/assignment")
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final ProjectRepository projectRepository;

    public AssignmentController(AssignmentService assignmentService, ProjectRepository projectRepository) {
        this.assignmentService = assignmentService;
        this.projectRepository = projectRepository;
    }

    /**
     * Berechnet die Belegung für das Projekt (siehe AssignmentService) und speichert
     * das Ergebnis im Projekt, damit es nach einem Neustart noch da ist.
     */
    @PostMapping("/calculate")
    public ResponseEntity<AssignmentResult> calculateAssignment(@PathVariable String projectId) {
        return projectRepository.getProjectById(projectId)
                .map(project -> {
                    AssignmentResult result = assignmentService.calculateAssignment(
                            project.getPersons(),
                            project.getBuildings(),
                            project.getRules()
                    );
                    project.setAssignmentResult(result);
                    projectRepository.saveProject(project);
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Setzt Zuordnungen von Hand: Map aus Personen-ID -> Bett-ID
     * (leere Bett-ID = Person aus ihrem Bett entfernen).
     */
    @PostMapping("/override")
    public ResponseEntity<Project> manualOverride(
            @PathVariable String projectId,
            @RequestBody Map<String, String> personToBedMap) { // personId -> bedId
        return projectRepository.getProjectById(projectId)
                .map(project -> {
                    AssignmentResult targetResult = project.getAssignmentResult();
                    if (targetResult == null) {
                        targetResult = new AssignmentResult();
                        project.setAssignmentResult(targetResult);
                    }
                    final AssignmentResult finalResult = targetResult;

                    // Perform manual override updates
                    personToBedMap.forEach((personId, bedId) -> {
                        finalResult.getAssignments().removeIf(p -> p.getPersonId().equals(personId) || p.getBedId().equals(bedId));
                        if (bedId != null && !bedId.isEmpty()) {
                            AssignmentPair pair = new AssignmentPair();
                            pair.setPersonId(personId);
                            pair.setBedId(bedId);
                            pair.setManualOverride(true);
                            pair.setScore(0);
                            finalResult.getAssignments().add(pair);
                        }
                    });

                    Project saved = projectRepository.saveProject(project);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Erstellt eine Excel-Datei mit zwei Tabellenblättern:
     *   1. "Zimmerbelegung": jedes Bett mit seiner Belegung (oder "FREIES BETT")
     *   2. "Teilnehmerliste": jede Person mit Zimmer und Bett (oder Grund, warum nicht zugeordnet)
     * Gibt es noch kein Ergebnis, wird die Belegung vorher berechnet.
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String projectId) {
        Optional<Project> optProject = projectRepository.getProjectById(projectId);
        if (optProject.isEmpty()) return ResponseEntity.notFound().build();

        Project project = optProject.get();
        AssignmentResult result = project.getAssignmentResult();
        if (result == null) {
            result = assignmentService.calculateAssignment(project.getPersons(), project.getBuildings(), project.getRules());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Tabellenblatt 1: Zimmerbelegung
            Sheet sheet1 = workbook.createSheet("Zimmerbelegung");

            // Aussehen der Überschriftenzeile: fett, weiße Schrift auf dunkelblauem Grund
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow1 = sheet1.createRow(0);
            String[] headers1 = {"Gebäude", "Zimmer", "Etage", "Bett", "Teilnehmer Name", "Geschlecht", "Alter", "Paar-ID", "Gruppe", "Score"};
            for (int i = 0; i < headers1.length; i++) {
                Cell cell = headerRow1.createCell(i);
                cell.setCellValue(headers1[i]);
                cell.setCellStyle(headerStyle);
            }

            // Nachschlage-Tabellen ("Maps"), damit wir schnell finden, wer in welchem Bett liegt
            Map<String, AssignmentPair> bedAssignmentMap = new HashMap<>();
            for (AssignmentPair pair : result.getAssignments()) {
                bedAssignmentMap.put(pair.getBedId(), pair);
            }

            Map<String, Person> personMap = new HashMap<>();
            for (Person p : project.getPersons()) {
                personMap.put(p.getId(), p);
            }

            int rowIdx1 = 1;
            for (Building b : project.getBuildings()) {
                for (Room r : b.getRooms()) {
                    for (Bed bed : r.getBeds()) {
                        Row row = sheet1.createRow(rowIdx1++);
                        row.createCell(0).setCellValue(b.getName());
                        row.createCell(1).setCellValue(r.getName());
                        row.createCell(2).setCellValue(r.getFloor());
                        row.createCell(3).setCellValue(bed.getName());

                        AssignmentPair pair = bedAssignmentMap.get(bed.getId());
                        if (pair != null) {
                            Person p = personMap.get(pair.getPersonId());
                            row.createCell(4).setCellValue(pair.getPersonName());
                            row.createCell(5).setCellValue(p != null && p.getGender() != null ? p.getGender() : "");
                            row.createCell(6).setCellValue(p != null ? p.getAge() : 0);
                            row.createCell(7).setCellValue(p != null && p.getPartnerId() != null ? p.getPartnerId() : "");
                            row.createCell(8).setCellValue(p != null && p.getGroupId() != null ? p.getGroupId() : "");
                            row.createCell(9).setCellValue(formatScore(pair.getScore()));
                        } else {
                            row.createCell(4).setCellValue("-- FREIES BETT --");
                        }
                    }
                }
            }

            for (int i = 0; i < headers1.length; i++) {
                sheet1.autoSizeColumn(i);
            }

            // Tabellenblatt 2: Teilnehmerliste
            Sheet sheet2 = workbook.createSheet("Teilnehmerliste");
            Row headerRow2 = sheet2.createRow(0);
            String[] headers2 = {"Vorname", "Nachname", "Geschlecht", "Alter", "Paar-ID", "Gruppe", "Zugewiesener Raum", "Zugewiesenes Bett", "Hinweis"};
            for (int i = 0; i < headers2.length; i++) {
                Cell cell = headerRow2.createCell(i);
                cell.setCellValue(headers2[i]);
                cell.setCellStyle(headerStyle);
            }

            Map<String, AssignmentPair> personAssignmentMap = new HashMap<>();
            for (AssignmentPair pair : result.getAssignments()) {
                personAssignmentMap.put(pair.getPersonId(), pair);
            }

            int rowIdx2 = 1;
            for (Person p : project.getPersons()) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(p.getFirstname() != null ? p.getFirstname() : "");
                row.createCell(1).setCellValue(p.getLastname() != null ? p.getLastname() : "");
                row.createCell(2).setCellValue(p.getGender() != null ? p.getGender() : "");
                row.createCell(3).setCellValue(p.getAge());
                row.createCell(4).setCellValue(p.getPartnerId() != null ? p.getPartnerId() : "");
                row.createCell(5).setCellValue(p.getGroupId() != null ? p.getGroupId() : "");

                AssignmentPair pair = personAssignmentMap.get(p.getId());
                if (pair != null) {
                    row.createCell(6).setCellValue(pair.getBuildingName() + " - " + pair.getRoomName());
                    row.createCell(7).setCellValue(pair.getBedName());
                } else {
                    row.createCell(6).setCellValue("NICHT ZUGEORDNET");
                    row.createCell(7).setCellValue("-");
                    // Grund aus der Berechnung (z.B. "Alle Betten sind belegt")
                    String reason = result.getUnassignedReasons() != null ? result.getUnassignedReasons().get(p.getId()) : null;
                    row.createCell(8).setCellValue(reason != null ? reason : "");
                }
            }

            for (int i = 0; i < headers2.length; i++) {
                sheet2.autoSizeColumn(i);
            }

            // Arbeitsmappe in den Speicher schreiben und als Download zurückgeben
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            // Sonderzeichen im Dateinamen durch "_" ersetzen (z.B. Leerzeichen, Umlaute)
            String fileName = "Belegungsplan_" + project.getName().replaceAll("[^a-zA-Z0-9_-]", "_") + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Punkte mit Vorzeichen für die Excel-Datei: 50 -> "+50 Pkt", -25 -> "-25 Pkt". */
    private static String formatScore(int score) {
        return (score > 0 ? "+" : "") + score + " Pkt";
    }
}
