package de.zuordner.importengine;

import de.zuordner.model.Person;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================================
 * TableMappingService – macht aus Tabellenzeilen Teilnehmer (Personen)
 * ============================================================================
 * Im Import-Assistenten ordnet man jeder Spalte der Excel-/CSV-Datei eine
 * Bedeutung zu, z.B. "Spalte 'Vorname' -> firstname". Diese Zuordnung
 * (columnMapping) wendet diese Klasse auf jede Zeile an und erzeugt daraus
 * Person-Objekte. Probleme (z.B. ungültiges Alter) werden als Warnung gesammelt,
 * statt den ganzen Import abzubrechen.
 */
@Service
public class TableMappingService {

    /**
     * Wandelt alle Tabellenzeilen anhand der Spaltenzuordnung in Personen um.
     *
     * @param tableData     die eingelesene Tabelle (Überschriften + Zeilen)
     * @param columnMapping Spaltenname -> Ziel-Eigenschaft (z.B. "Alter" -> "age",
     *                      "IGNORE" = Spalte überspringen)
     * @return gefundene Personen plus Warnungen/Fehler
     */
    public ImportValidationResult mapTableToPersons(TableData tableData, Map<String, String> columnMapping) {
        ImportValidationResult result = new ImportValidationResult();
        if (tableData == null || tableData.getRows() == null) {
            result.getErrors().add("Keine Tabellendaten zum Importieren gefunden.");
            return result;
        }
        if (columnMapping == null) {
            columnMapping = new HashMap<>();
        }

        List<String> headers = tableData.getHeaders();
        List<List<String>> rows = tableData.getRows();
        result.setTotalRowsProcessed(rows.size());

        // Für jede Spalten-NUMMER merken, in welche Eigenschaft sie gehört.
        // Die Zuordnung kann über den Spaltennamen ("Vorname"), "col_0" oder
        // "Spalte 1" angegeben sein.
        Map<Integer, String> colIndexToAttributeMap = new HashMap<>();
        for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
            String colHeader = headers.get(colIdx);
            String targetAttr = columnMapping.get(colHeader);
            if (targetAttr == null) {
                targetAttr = columnMapping.get("col_" + colIdx);
            }
            if (targetAttr == null) {
                targetAttr = columnMapping.get("Spalte " + (colIdx + 1));
            }
            if (targetAttr != null && !targetAttr.equalsIgnoreCase("IGNORE")) {
                colIndexToAttributeMap.put(colIdx, targetAttr.toLowerCase().trim());
            }
        }

        // rowIndex = Zeilennummer wie in Excel (Zeile 1 = Überschrift), für verständliche Warnungen
        int rowIndex = 1;
        for (List<String> row : rows) {
            rowIndex++;
            if (row == null || row.isEmpty()) continue;

            Person person = new Person();
            boolean hasData = false;

            for (Map.Entry<Integer, String> entry : colIndexToAttributeMap.entrySet()) {
                int colIdx = entry.getKey();
                String attribute = entry.getValue();

                if (colIdx < row.size()) {
                    String rawValue = row.get(colIdx);
                    if (rawValue != null && !rawValue.trim().isEmpty()) {
                        hasData = true;
                        populatePersonAttribute(person, attribute, rawValue.trim(), rowIndex, result);
                    }
                }
            }

            if (hasData) {
                // Ohne Namen bekommt die Person einen Platzhalternamen
                if (person.getFirstname() == null && person.getLastname() == null) {
                    person.setLastname("Teilnehmer " + (result.getValidPersons().size() + 1));
                    result.getWarnings().add("Zeile " + rowIndex + ": Kein Name angegeben - Standardname zugewiesen.");
                }
                result.getValidPersons().add(person);
            }
        }

        return result;
    }

    /**
     * Schreibt einen Zellenwert in die passende Eigenschaft der Person.
     * Mehrere Schreibweisen sind erlaubt (z.B. "age" oder "alter").
     * Unbekannte Ziel-Eigenschaften landen als Zusatz-Attribut in customAttributes
     * und können dann in eigenen Regeln verwendet werden ("person.custom.<name>").
     */
    private void populatePersonAttribute(Person person, String attribute, String rawValue, int rowIndex, ImportValidationResult result) {
        switch (attribute) {
            case "name":
            case "fullname":
                // "Max Mustermann" -> Vorname "Max", Nachname "Mustermann"
                String[] parts = rawValue.split("\\s+", 2);
                if (parts.length > 1) {
                    person.setFirstname(parts[0]);
                    person.setLastname(parts[1]);
                } else {
                    person.setLastname(rawValue);
                }
                break;
            case "firstname":
            case "vorname":
                person.setFirstname(rawValue);
                break;
            case "lastname":
            case "nachname":
                person.setLastname(rawValue);
                break;
            case "gender":
            case "geschlecht":
                person.setGender(normalizeGender(rawValue));
                break;
            case "age":
            case "alter":
                try {
                    // Math.round, damit auch "12,0" bzw. "12.0" aus Excel funktionieren
                    int age = (int) Math.round(Double.parseDouble(rawValue.replace(',', '.')));
                    person.setAge(age);
                } catch (Exception e) {
                    result.getWarnings().add("Zeile " + rowIndex + ": Ungültiges Alter '" + rawValue + "' -> Standard 20 gesetzt.");
                    person.setAge(20);
                }
                break;
            case "partnerid":
            case "paar_id":
            case "partner_id":
            case "paar":
                person.setPartnerId(rawValue);
                break;
            case "groupid":
            case "gruppe":
            case "gruppen_id":
            case "group":
                person.setGroupId(rawValue);
                break;
            case "desiredfloor":
            case "etage":
            case "wunsch_etage":
                try {
                    person.setDesiredFloor((int) Math.round(Double.parseDouble(rawValue.replace(',', '.'))));
                } catch (Exception e) {
                    result.getWarnings().add("Zeile " + rowIndex + ": Ungültige Wunsch-Etage '" + rawValue + "'.");
                }
                break;
            case "desiredroom":
            case "zimmer":
            case "wunsch_zimmer":
                person.setDesiredRoom(rawValue);
                break;
            case "specialneeds":
            case "anforderungen":
            case "besonderheiten":
                person.setSpecialNeeds(rawValue);
                break;
            default:
                person.getCustomAttributes().put(attribute, rawValue);
                break;
        }
    }

    /**
     * Vereinheitlicht Geschlechtsangaben: "Weiblich", "w", "Mädchen", "F" -> "weiblich",
     * "Männlich", "m", "Junge", "male" -> "maennlich", "d"/"divers" -> "divers".
     * Alles andere bleibt unverändert stehen.
     *
     * Reihenfolge ist wichtig: "Mädchen" beginnt mit "m" und "female" enthält
     * "male" – darum wird WEIBLICH zuerst geprüft. (In Version 1.x wurde
     * "Mädchen" fälschlich als männlich erkannt.)
     */
    static String normalizeGender(String input) {
        if (input == null) return "unbekannt";
        String lower = input.trim().toLowerCase(Locale.GERMAN);
        if (lower.startsWith("mäd") || lower.startsWith("maed") || lower.startsWith("w") || lower.startsWith("f")
                || lower.contains("weiblich") || lower.contains("female")) {
            return "weiblich";
        }
        if (lower.startsWith("m") || lower.startsWith("j") || lower.contains("männlich") || lower.contains("maennlich")) {
            return "maennlich";
        }
        if (lower.startsWith("d") || lower.contains("divers")) {
            return "divers";
        }
        return input;
    }
}
