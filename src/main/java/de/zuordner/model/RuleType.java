package de.zuordner.model;

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
