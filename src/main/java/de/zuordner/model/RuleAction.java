package de.zuordner.model;

public enum RuleAction {
    FORBID("Verbieten (-999999 Pkt)"),
    ADD_POINTS("Punkte hinzufügen (+Punkte)"),
    SUBTRACT_POINTS("Punkte abziehen (-Punkte)");

    private final String description;

    RuleAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
