package de.zuordner.model;

public enum TargetScope {
    BED_PERSON("Person an Bett (z.B. Alter vs. Bettart)"),
    ROOM_PERSON("Person an Raum (z.B. Geschlecht vs. Raumtyp/Etage)"),
    PAIR_CO_LOCATION("Paare (z.B. Personen mit gleicher PartnerID ins selbe Zimmer)"),
    GROUP_CO_LOCATION("Gruppen (z.B. Gruppe möglichst im selben Zimmer/Gebäude)");

    private final String description;

    TargetScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
