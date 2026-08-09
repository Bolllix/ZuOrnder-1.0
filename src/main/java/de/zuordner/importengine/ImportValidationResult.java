package de.zuordner.importengine;

import de.zuordner.model.Person;

import java.util.ArrayList;
import java.util.List;

public class ImportValidationResult {
    private List<Person> validPersons;
    private List<String> warnings;
    private List<String> errors;
    private int totalRowsProcessed;

    public ImportValidationResult() {
        this.validPersons = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public List<Person> getValidPersons() {
        return validPersons;
    }

    public void setValidPersons(List<Person> validPersons) {
        this.validPersons = validPersons;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }
}
