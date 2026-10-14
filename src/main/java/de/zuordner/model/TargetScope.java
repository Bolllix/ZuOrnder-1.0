package de.zuordner.model;

/**
 * ============================================================================
 * TargetScope – WORAUF bezieht sich eine Regel?
 * ============================================================================
 * Es gibt zwei Arten von Regeln:
 *
 * 1) Regeln für EINE Person und EIN Bett (werden für jede Kombination einzeln geprüft):
 *    - BED_PERSON  : z.B. "Alter > 65 UND oberes Hochbett -> verboten"
 *    - ROOM_PERSON : z.B. "Geschlecht weiblich UND Etage 0 -> +25 Punkte"
 *
 * 2) Regeln für ZWEI Personen im selben Zimmer (Zimmernachbarn):
 *    - GENDER_SEPARATION     : Männer und Frauen nicht im selben Zimmer
 *    - PAIR_CO_LOCATION      : Paare (gleiche Paar-ID) zusammen
 *    - GROUP_CO_LOCATION     : Gruppen (gleiche Gruppen-ID) zusammen
 *    - CONDITION_CO_LOCATION : alle Personen, auf die die WENN-Bedingungen
 *                              zutreffen (z.B. "Alter > 65"), zusammen
 *
 * Die Beschreibungstexte werden u.a. in der Oberfläche angezeigt.
 */
public enum TargetScope {
    BED_PERSON("Person an Bett (z.B. Alter vs. Bettart)"),
    ROOM_PERSON("Person an Raum (z.B. Geschlecht vs. Raumtyp/Etage)"),
    GENDER_SEPARATION("Geschlechtertrennung (Männer und Frauen in getrennten Zimmern)"),
    PAIR_CO_LOCATION("Paare (z.B. Personen mit gleicher PartnerID ins selbe Zimmer)"),
    GROUP_CO_LOCATION("Gruppen (z.B. Gruppe möglichst im selben Zimmer/Gebäude)"),
    CONDITION_CO_LOCATION("Personen mit gleicher Eigenschaft (WENN-Bedingungen) ins selbe Zimmer");

    private final String description;

    TargetScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return true, wenn diese Regel zwei Personen im selben Zimmer betrifft
     *         (und nicht nur eine Person und ihr Bett)
     */
    public boolean isRoommateScope() {
        return this == GENDER_SEPARATION
                || this == PAIR_CO_LOCATION
                || this == GROUP_CO_LOCATION
                || this == CONDITION_CO_LOCATION;
    }
}
