package de.zuordner.model;

public class Condition {
    private String field; // e.g. "person.age", "person.gender", "bed.bedType", "room.floor", "person.partnerId"
    private Operator operator;
    private Object value; // e.g. 65, "TOP_BUNK", "weiblich", 1

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
