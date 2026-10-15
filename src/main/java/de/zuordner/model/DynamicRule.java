package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================================
 * DynamicRule – eine Regel für die Zimmerverteilung
 * ============================================================================
 * Eine Regel besteht aus:
 *   - WENN-Bedingungen ({@link #conditions}), z.B. "Alter > 65" UND "oberes Hochbett"
 *   - einem Bezug ({@link #targetScope}): eine Person+Bett, oder zwei Zimmernachbarn
 *   - einer Wirkung ({@link #ruleType} + {@link #action} + {@link #weight}):
 *       HARD + FORBID      -> strikt verboten (darf niemals passieren)
 *       SOFT + ADD_POINTS  -> Wunsch: Pluspunkte (oder Minuspunkte bei negativem Gewicht)
 *
 * Der Optimierer sucht am Ende die Verteilung mit den meisten Punkten, bei der
 * keine harte Regel verletzt wird.
 *
 * Die vielen get/set-Methoden ("Getter" und "Setter") braucht die Bibliothek
 * Jackson, um Regeln in JSON (Textformat für Browser und Speicherdateien) und
 * zurück zu verwandeln.
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)}: Unbekannte Felder in einer
 * JSON-Datei werden ignoriert statt einen Fehler auszulösen.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicRule {
    /** Eindeutige Kennung (UUID), z.B. "3f92...". Wird zum Ändern/Löschen gebraucht. */
    private String id;
    /** Anzeigename, z.B. "Paare im selben Zimmer" */
    private String name;
    /** Erklärung in normaler Sprache */
    private String description;
    /** Ist die Regel eingeschaltet? Ausgeschaltete Regeln werden komplett ignoriert. */
    private boolean active;
    /** HARD = Verbot, SOFT = Wunsch mit Punkten */
    private RuleType ruleType;
    /** Worauf bezieht sich die Regel? (Bett, Raum, Paare, Gruppen, ...) */
    private TargetScope targetScope;
    /** Alle WENN-Bedingungen – sie müssen ALLE erfüllt sein (UND-Verknüpfung) */
    private List<Condition> conditions;
    /** Was passiert, wenn die Bedingungen zutreffen? */
    private RuleAction action;
    /** Punktwert (positiv = Belohnung, negativ = Abzug) */
    private int weight;
    /**
     * Nur bei den 4 Standard-Regeln gesetzt (z.B. COUPLES_TOGETHER), sonst null.
     * Damit findet der Regel-Editor "seine" Regel eindeutig wieder.
     */
    private StandardRule standardKey;

    /** Leere Regel mit Standardwerten (wird z.B. von Jackson beim Einlesen benutzt). */
    public DynamicRule() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
        this.ruleType = RuleType.SOFT;
        this.targetScope = TargetScope.BED_PERSON;
        this.conditions = new ArrayList<>();
        this.action = RuleAction.ADD_POINTS;
        this.weight = 10;
    }

    /** Bequemer Konstruktor für Regeln, die im Code erzeugt werden (Bedingungen danach mit addCondition). */
    public DynamicRule(String name, String description, RuleType ruleType, TargetScope targetScope, RuleAction action, int weight) {
        this();
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.targetScope = targetScope;
        this.action = action;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public TargetScope getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(TargetScope targetScope) {
        this.targetScope = targetScope;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public StandardRule getStandardKey() {
        return standardKey;
    }

    public void setStandardKey(StandardRule standardKey) {
        this.standardKey = standardKey;
    }

    /** Fügt eine weitere WENN-Bedingung hinzu. */
    public void addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
    }
}
