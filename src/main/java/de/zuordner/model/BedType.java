package de.zuordner.model;

public enum BedType {
    SINGLE("Einzelbett"),
    DOUBLE("Doppelbett"),
    TOP_BUNK("Hochbett Oben"),
    BOTTOM_BUNK("Hochbett Unten"),
    SOFA("Schlafsofa"),
    OTHER("Sonstige Bettart");

    private final String displayName;

    BedType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTopBunk() {
        return this == TOP_BUNK;
    }

    public boolean isBottomBunk() {
        return this == BOTTOM_BUNK;
    }
}
