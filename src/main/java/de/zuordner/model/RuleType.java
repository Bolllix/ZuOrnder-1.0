package de.zuordner.model;

/**
 * HARD = strikte Regel: darf nie verletzt werden (lieber bleibt eine Person ohne Bett).
 * SOFT = Wunsch: bringt Plus- oder Minuspunkte, darf aber verletzt werden,
 *        wenn es nicht anders geht.
 */
public enum RuleType {
    HARD("Harte Regel (Verbot)"),
    SOFT("Weiche Regel (Wunsch/Gewichtung)");

    private final String description;

    RuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
