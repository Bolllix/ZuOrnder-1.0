package de.zuordner.importengine;

import de.zuordner.model.Person;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TableMappingService {

    /**
     * Maps table data rows into Person objects according to user specified column mapping.
     */
    public ImportValidationResult mapTableToPersons(TableData tableData, Map<String, String> columnMapping) {
        ImportValidationResult result = new ImportValidationResult();
        if (tableData == null || tableData.getRows() == null) {
            result.getErrors().add("Keine Tabellendaten zum Importieren gefunden.");
            return result;
        }

        List<String> headers = tableData.getHeaders();
        List<List<String>> rows = tableData.getRows();
        result.setTotalRowsProcessed(rows.size());

        // Map column name or index string to mapped attribute name
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
                // Ensure default name if missing
                if (person.getFirstname() == null && person.getLastname() == null) {
                    person.setLastname("Teilnehmer " + (result.getValidPersons().size() + 1));
                    result.getWarnings().add("Zeile " + rowIndex + ": Kein Name angegeben - Standardname zugewiesen.");
                }
                result.getValidPersons().add(person);
            }
        }

        return result;
    }

    private void populatePersonAttribute(Person person, String attribute, String rawValue, int rowIndex, ImportValidationResult result) {
        switch (attribute) {
            case "name":
            case "fullname":
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
                    int age = (int) Math.round(Double.parseDouble(rawValue));
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
                    person.setDesiredFloor((int) Math.round(Double.parseDouble(rawValue)));
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

    private String normalizeGender(String input) {
        if (input == null) return "unbekannt";
        String lower = input.trim().toLowerCase();
        if (lower.startsWith("m") || lower.startsWith("j") || lower.contains("männlich") || lower.contains("male")) {
            return "maennlich";
        }
        if (lower.startsWith("w") || lower.startsWith("f") || lower.contains("weiblich") || lower.contains("female")) {
            return "weiblich";
        }
        if (lower.startsWith("d") || lower.contains("divers")) {
            return "divers";
        }
        return input;
    }
}
