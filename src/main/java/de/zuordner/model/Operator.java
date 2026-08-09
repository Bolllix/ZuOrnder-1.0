package de.zuordner.model;

public enum Operator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    LESS_EQUAL("<="),
    IN("enthält in Liste"),
    CONTAINS("enthält Text"),
    MATCH_ROOM_PROP("stimmt mit Raumeigenschaft überein");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
