package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * ============================================================================
 * Condition – eine WENN-Bedingung einer Regel
 * ============================================================================
 * Beispiel: field = "person.age", operator = GREATER_THAN, value = 65
 *           bedeutet: "WENN das Alter der Person größer als 65 ist"
 * Welche Feldnamen es gibt, steht in RuleEngine.extractFieldValue().
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Condition {
    /** Welches Feld wird geprüft? z.B. "person.age", "person.gender", "bed.bedType", "room.floor" */
    private String field;
    /** Wie wird verglichen? (=, !=, >, <, enthält, ...) */
    private Operator operator;
    /** Womit wird verglichen? z.B. 65, "TOP_BUNK", "weiblich", true */
    private Object value;

    public Condition() {
    }

    public Condition(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
