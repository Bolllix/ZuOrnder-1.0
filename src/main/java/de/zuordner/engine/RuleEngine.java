package de.zuordner.engine;

import de.zuordner.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================================
 * Dynamic Rule Engine (Regel-Auswertungs-Service)
 * ============================================================================
 * Diese Klasse ist das Herzstück des flexiblen Regelsystems. Sie bewertet für
 * jede beliebige Kombination aus (Person, Bett, Raum) alle aktiven Regeln,
 * ohne dass Java-Code verändert werden muss.
 * 
 * Funktionsweise:
 * 1. Harte Regeln (HARD / FORBID): Wenn eine harte Regel verletzt wird (z.B. Senior
 *    über 65 Jahre in oberem Hochbett), wird die Kombination als unzulässig markiert
 *    und der Score auf FORBIDDEN_SCORE (-999999) gesetzt.
 * 2. Weiche Regeln (SOFT): Fügt dem Score einer Zuordnung Belohnungspunkte hinzu
 *    (+50 für Paare im selben Zimmer, +20 für Gruppen) oder zieht Punkte ab.
 * 3. Transparente Begründungen: Für jede angewendete Regel wird ein ScoreExplanation-
 *    Objekt erzeugt, sodass der Benutzer in der GUI genau nachvollziehen kann, warum
 *    eine Person einem bestimmten Bett zugeordnet wurde.
 */
@Service
public class RuleEngine {

    /** Strafwert für unzulässige / verbotene Zuordnungen (Harte Regelverletzung) */
    public static final int FORBIDDEN_SCORE = -999999;

    /**
     * Ergebnis-Container einer Einzelbewertung für ein (Person, Bett, Raum)-Tupel.
     */
    public static class EvaluationResult {
        private int score;
        private boolean forbidden;
        private List<ScoreExplanation> explanations;

        public EvaluationResult() {
            this.score = 0;
            this.forbidden = false;
            this.explanations = new ArrayList<>();
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void addScore(int points) {
            this.score += points;
        }

        public boolean isForbidden() {
            return forbidden;
        }

        public void setForbidden(boolean forbidden) {
            this.forbidden = forbidden;
        }

        public List<ScoreExplanation> getExplanations() {
            return explanations;
        }

        public void addExplanation(ScoreExplanation explanation) {
            this.explanations.add(explanation);
        }
    }

    /**
     * Bewertet eine einzelne (Person, Bett, Raum) Zuordnung gegen alle aktiven Regeln im Scope BED_PERSON und ROOM_PERSON.
     *
     * @param person  Die zu bewertende Person
     * @param bed     Das Zielbett
     * @param room    Der zugehörige Raum
     * @param rules   Liste aller im Projekt definierten Regeln
     * @return EvaluationResult mit Akkumuliertem Score und Erklärungsliste
     */
    public EvaluationResult evaluatePersonBedPair(Person person, Bed bed, Room room, List<DynamicRule> rules) {
        EvaluationResult result = new EvaluationResult();
        if (person == null || bed == null) {
            return result;
        }

        for (DynamicRule rule : rules) {
            if (!rule.isActive()) continue;

            TargetScope scope = rule.getTargetScope();
            // Relationale Scopes (Paare, Gruppen) werden in der Matrix-Orchestrierung ausgewertet
            if (scope != TargetScope.BED_PERSON && scope != TargetScope.ROOM_PERSON) {
                continue;
            }

            boolean matches = evaluateConditions(rule.getConditions(), person, bed, room);
            if (matches) {
                applyRuleAction(rule, result, person, bed, room);
            }
        }

        return result;
    }

    /**
     * Bewertet relationale Regeln (Paare im selben Zimmer, Gruppen im selben Zimmer).
     *
     * @param person1 Erste Person
     * @param room1   Raum der ersten Person
     * @param person2 Zweite Person
     * @param room2   Raum der zweiten Person
     * @param rule    Die auszuwertende relationale Regel
     * @return Punktebonus (z.B. +50), wenn die relationale Regel erfüllt ist, sonst 0.
     */
    public int evaluateCoLocationBonus(Person person1, Room room1, Person person2, Room room2, DynamicRule rule) {
        if (!rule.isActive() || person1 == null || person2 == null || room1 == null || room2 == null) {
            return 0;
        }

        TargetScope scope = rule.getTargetScope();
        if (scope == TargetScope.PAIR_CO_LOCATION) {
            String p1Partner = person1.getPartnerId();
            String p2Partner = person2.getPartnerId();

            // Prüfen, ob beide Personen dieselbe Partner-ID besitzen
            if (p1Partner != null && !p1Partner.isEmpty() && p1Partner.equalsIgnoreCase(p2Partner)) {
                if (room1.getId().equals(room2.getId())) {
                    return rule.getWeight(); // Belohnungs-Score für gemeinsame Zimmerunterbringung
                }
            }
        } else if (scope == TargetScope.GROUP_CO_LOCATION) {
            String g1 = person1.getGroupId();
            String g2 = person2.getGroupId();

            // Prüfen, ob beide Personen zur selben Gruppe / Klasse gehören
            if (g1 != null && !g1.isEmpty() && g1.equalsIgnoreCase(g2)) {
                if (room1.getId().equals(room2.getId())) {
                    return rule.getWeight(); // Belohnung für Gruppenzusammenhalt im Zimmer
                }
            }
        }

        return 0;
    }

    /**
     * Hilfsmethode: Prüft, ob alle WENN-Bedingungen einer Regel erfüllt sind (UND-Verknüpfung).
     */
    private boolean evaluateConditions(List<Condition> conditions, Person person, Bed bed, Room room) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // Keine Bedingungen angegeben -> Regel trifft universell zu
        }

        for (Condition condition : conditions) {
            if (!evaluateCondition(condition, person, bed, room)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Einzelauswertung einer WENN-Bedingung über Feldwert-Extraktion und Operator-Vergleich.
     */
    private boolean evaluateCondition(Condition condition, Person person, Bed bed, Room room) {
        String field = condition.getField();
        Operator op = condition.getOperator();
        Object targetVal = condition.getValue();

        Object actualVal = extractFieldValue(field, person, bed, room);

        if (op == Operator.MATCH_ROOM_PROP) {
            return Objects.equals(actualVal, extractFieldValue(String.valueOf(targetVal), person, bed, room));
        }

        return compareValues(actualVal, op, targetVal);
    }

    /**
     * Extrahiert dynamisch Feldwerte von Personen, Betten und Räumen (einschließlich benutzerdefinierter Attribute).
     */
    private Object extractFieldValue(String field, Person person, Bed bed, Room room) {
        if (field == null) return null;
        field = field.toLowerCase().trim();

        switch (field) {
            case "person.age":
            case "age":
                return person != null ? person.getAge() : null;
            case "person.gender":
            case "gender":
                return person != null ? person.getGender() : null;
            case "person.partnerid":
            case "partnerid":
                return person != null ? person.getPartnerId() : null;
            case "person.groupid":
            case "groupid":
                return person != null ? person.getGroupId() : null;
            case "person.desiredfloor":
            case "desiredfloor":
                return person != null ? person.getDesiredFloor() : null;
            case "person.desiredroom":
            case "desiredroom":
                return person != null ? person.getDesiredRoom() : null;
            case "bed.bedtype":
            case "bedtype":
                return bed != null ? bed.getBedType().name() : null;
            case "bed.istopbunk":
            case "topbunk":
                return bed != null ? bed.isTopBunk() : false;
            case "bed.isbottombunk":
            case "bottombunk":
                return bed != null ? bed.isBottomBunk() : false;
            case "room.floor":
            case "floor":
                return room != null ? room.getFloor() : (bed != null ? bed.getFloor() : null);
            case "room.girlsroom":
            case "girlsroom":
                return room != null ? room.isGirlsRoom() : false;
            case "room.boysroom":
            case "boysroom":
                return room != null ? room.isBoysRoom() : false;
            case "room.name":
            case "roomname":
                return room != null ? room.getName() : (bed != null ? bed.getRoomName() : null);
            default:
                // Dynamische Attributabfrage aus Custom-Attribute-Maps
                if (field.startsWith("person.custom.") && person != null && person.getCustomAttributes() != null) {
                    return person.getCustomAttributes().get(field.substring("person.custom.".length()));
                }
                if (field.startsWith("room.custom.") && room != null && room.getCustomAttributes() != null) {
                    return room.getCustomAttributes().get(field.substring("room.custom.".length()));
                }
                if (field.startsWith("bed.custom.") && bed != null && bed.getCustomAttributes() != null) {
                    return bed.getCustomAttributes().get(field.substring("bed.custom.".length()));
                }
                return null;
        }
    }

    /**
     * Vergleicht Werte basierend auf dem Operator (=, !=, >, >=, <, <=, CONTAINS, IN).
     */
    private boolean compareValues(Object actual, Operator op, Object target) {
        if (actual == null && op != Operator.NOT_EQUALS) {
            return false;
        }

        switch (op) {
            case EQUALS:
                return compareEquals(actual, target);
            case NOT_EQUALS:
                return !compareEquals(actual, target);
            case GREATER_THAN:
                return compareNumbers(actual, target) > 0;
            case GREATER_EQUAL:
                return compareNumbers(actual, target) >= 0;
            case LESS_THAN:
                return compareNumbers(actual, target) < 0;
            case LESS_EQUAL:
                return compareNumbers(actual, target) <= 0;
            case CONTAINS:
                return String.valueOf(actual).toLowerCase().contains(String.valueOf(target).toLowerCase());
            case IN:
                if (target instanceof Collection) {
                    return ((Collection<?>) target).contains(actual);
                }
                return String.valueOf(target).contains(String.valueOf(actual));
            default:
                return false;
        }
    }

    private boolean compareEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (a instanceof Boolean || b instanceof Boolean) {
            return Boolean.valueOf(a.toString()).equals(Boolean.valueOf(b.toString()));
        }

        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }

        return a.toString().equalsIgnoreCase(b.toString());
    }

    private int compareNumbers(Object a, Object b) {
        try {
            double numA = Double.parseDouble(String.valueOf(a));
            double numB = Double.parseDouble(String.valueOf(b));
            return Double.compare(numA, numB);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Wendet die Aktion einer erfüllten Regel an (Verbot setzen oder Punkte hinzufügen/abziehen).
     */
    private void applyRuleAction(DynamicRule rule, EvaluationResult result, Person person, Bed bed, Room room) {
        RuleAction action = rule.getAction();
        RuleType type = rule.getRuleType();
        int weight = rule.getWeight();

        if (type == RuleType.HARD && (action == RuleAction.FORBID || weight < 0)) {
            result.setForbidden(true);
            result.addScore(FORBIDDEN_SCORE);
            result.addExplanation(new ScoreExplanation(
                    rule.getId(),
                    rule.getName(),
                    FORBIDDEN_SCORE,
                    RuleType.HARD,
                    "Harte Regel verletzt: " + rule.getDescription()
            ));
        } else if (action == RuleAction.ADD_POINTS) {
            result.addScore(weight);
            result.addExplanation(new ScoreExplanation(
                    rule.getId(),
                    rule.getName(),
                    weight,
                    RuleType.SOFT,
                    "Punkte erhalten (+ " + weight + "): " + rule.getName()
            ));
        } else if (action == RuleAction.SUBTRACT_POINTS) {
            result.addScore(-weight);
            result.addExplanation(new ScoreExplanation(
                    rule.getId(),
                    rule.getName(),
                    -weight,
                    RuleType.SOFT,
                    "Punkteabzug (- " + weight + "): " + rule.getName()
            ));
        }
    }
}
