package de.zuordner.model;

/**
 * ============================================================================
 * StandardRule – die 4 "Praxis-Standard"-Regeln mit eigenem An/Aus-Schalter
 * ============================================================================
 * Im Regel-Editor gibt es vier große Schalter (Karten). Jede Karte gehört zu
 * genau EINER Regel im Projekt. Damit das Programm diese Regel sicher
 * wiederfindet, bekommt sie einen festen Schlüssel ("standardKey"), z.B.
 * COUPLES_TOGETHER.
 *
 * Warum ist das wichtig? (Das war der Fehler in Version 1.x)
 * Früher wurden die Standard-Regeln über ihren NAMEN gesucht, z.B. "enthält
 * das Wort 'paar'". Die Geschlechter-Regel hieß aber "Harte Geschlechtertrennung
 * (mit Paar-Ausnahme)" – sie enthält also auch "Paar"! Dadurch hat der Schalter
 * "Paare im selben Zimmer" in Wahrheit die Geschlechter-Regel umgeschaltet.
 * Zusätzlich bekamen neue Regeln alle die leere ID "" und ließen sich danach
 * nicht mehr umschalten. Mit festen Schlüsseln + echten IDs kann beides nicht
 * mehr passieren.
 *
 * Jeder Eintrag weiß außerdem, wie "seine" Regel standardmäßig aussieht
 * ({@link #createDefaultRule()}). So steht die Definition nur an EINER Stelle.
 */
public enum StandardRule {

    /** Männer und Frauen schlafen in getrennten Zimmern. */
    GENDER_SEPARATION,

    /** Paare (gleiche Paar-ID) möglichst ins selbe Zimmer. */
    COUPLES_TOGETHER,

    /** Personen derselben Gruppe/Klasse möglichst ins selbe Zimmer. */
    GROUPS_TOGETHER,

    /** Personen über 65 Jahre niemals ins obere Hochbett. */
    SENIORS_NO_TOP_BUNK;

    /**
     * Baut eine neue Regel mit den Standard-Einstellungen für diesen Schlüssel.
     * Die Regel ist aktiv und hat eine frisch erzeugte, eindeutige ID.
     *
     * @return eine neue {@link DynamicRule}, die noch keinem Projekt gehört
     */
    public DynamicRule createDefaultRule() {
        DynamicRule rule;
        switch (this) {
            case GENDER_SEPARATION:
                rule = new DynamicRule(
                        "Geschlechtertrennung im Zimmer",
                        "Männer und Frauen schlafen nicht im selben Zimmer. Als Mädchen- bzw. Jungenzimmer markierte "
                                + "Räume sind dann nur für Mädchen bzw. Jungen. Ist zusätzlich \"Paare im selben Zimmer\" "
                                + "aktiv, dürfen angemeldete Paare (gleiche Paar-ID) trotzdem zusammen schlafen.",
                        RuleType.HARD,
                        TargetScope.GENDER_SEPARATION,
                        RuleAction.FORBID,
                        -999999
                );
                break;
            case COUPLES_TOGETHER:
                rule = new DynamicRule(
                        "Paare im selben Zimmer",
                        "Personen mit der gleichen Paar-ID werden bevorzugt im selben Zimmer untergebracht "
                                + "(+50 Punkte pro Person), wenn möglich im Doppelbett (+10). "
                                + "Funktioniert mit und ohne Geschlechtertrennung.",
                        RuleType.SOFT,
                        TargetScope.PAIR_CO_LOCATION,
                        RuleAction.ADD_POINTS,
                        50
                );
                break;
            case GROUPS_TOGETHER:
                rule = new DynamicRule(
                        "Gruppenzusammenhalt im Zimmer",
                        "Personen der gleichen Gruppe/Klasse werden bevorzugt im selben Zimmer untergebracht "
                                + "(+20 Punkte pro Gruppenmitglied im Zimmer).",
                        RuleType.SOFT,
                        TargetScope.GROUP_CO_LOCATION,
                        RuleAction.ADD_POINTS,
                        20
                );
                break;
            case SENIORS_NO_TOP_BUNK:
            default:
                rule = new DynamicRule(
                        "Senioren nicht im oberen Hochbett",
                        "Personen über 65 Jahren dürfen aus Sicherheitsgründen nicht im oberen Hochbett schlafen.",
                        RuleType.HARD,
                        TargetScope.BED_PERSON,
                        RuleAction.FORBID,
                        -999999
                );
                // WENN Alter > 65 UND Bett = oberes Hochbett -> verboten
                rule.addCondition(new Condition("person.age", Operator.GREATER_THAN, 65));
                rule.addCondition(new Condition("bed.isTopBunk", Operator.EQUALS, true));
                break;
        }
        rule.setStandardKey(this);
        return rule;
    }
}
