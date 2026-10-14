package de.zuordner.model;

/**
 * Vergleichsarten für WENN-Bedingungen.
 * Ein "enum" ist eine feste Liste erlaubter Werte – hier z.B. EQUALS ("=")
 * oder GREATER_THAN (">"). Der Text in Klammern ist das Anzeigesymbol.
 */
public enum Operator {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_EQUAL(">="),
    LESS_THAN("<"),
    LESS_EQUAL("<="),
    IN("enthält in Liste"),
    CONTAINS("enthält Text"),
    /** Vergleicht zwei Felder miteinander, z.B. Wunsch-Etage == Raum-Etage */
    MATCH_ROOM_PROP("stimmt mit Raumeigenschaft überein");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
