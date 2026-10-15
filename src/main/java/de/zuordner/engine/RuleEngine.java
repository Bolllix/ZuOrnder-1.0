package de.zuordner.engine;

import de.zuordner.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================================
 * RuleEngine – prüft, welche Regeln für eine Zuordnung gelten
 * ============================================================================
 * Diese Klasse ist das Herzstück des Regelsystems. Sie beantwortet zwei Fragen:
 *
 * 1) "Wie gut passt Person X in Bett Y?"
 *    -> {@link #evaluatePersonBedPair}: prüft alle Regeln für EINE Person und EIN Bett
 *       (z.B. "Senioren nicht ins obere Hochbett", "Mädchen bevorzugt ins Erdgeschoss").
 *
 * 2) "Was passiert, wenn Person A und Person B im selben Zimmer schlafen?"
 *    -> {@link #evaluateRoommates}: prüft alle Regeln für ZIMMERNACHBARN
 *       (Geschlechtertrennung, Paare zusammen, Gruppen zusammen, ...).
 *
 * Ergebnis ist immer ein Punktwert plus eine Liste von Begründungen
 * ({@link ScoreExplanation}), damit man in der Oberfläche nachlesen kann,
 * WARUM eine Person in einem bestimmten Bett gelandet ist.
 *
 * Harte Regeln (Verbote) bekommen den riesigen Minuswert {@link #FORBIDDEN_SCORE}.
 * So wird der Optimierer sie immer vermeiden, solange es irgendwie möglich ist.
 *
 * WICHTIG (Änderung in Version 2.0): Regeln werden nur noch über ihren Typ
 * ({@link TargetScope}) erkannt – NICHT mehr über Wörter im Namen. Früher hat
 * z.B. jede Regel mit "Paar" im Namen als Paar-Regel gegolten. Dadurch haben
 * sich die Paar-Regel und die Geschlechtertrennung gegenseitig beeinflusst.
 *
 * {@code @Service} sagt Spring Boot: "Erzeuge von dieser Klasse automatisch
 * ein Objekt und gib es jedem, der es braucht" (z.B. dem AssignmentService).
 */
@Service
public class RuleEngine {

    /** Strafwert für unzulässige / verbotene Zuordnungen (Harte Regelverletzung) */
    public static final int FORBIDDEN_SCORE = -999999;

    /**
     * Kleiner Zusatzbonus der Paar-Regel: Personen mit Paar-ID bekommen im
     * Doppelbett diese Punkte extra. So landen Paare bevorzugt in Doppelbetten
     * statt z.B. in zwei Einzelbetten (das Zusammensein im Zimmer zählt mit
     * +50 pro Person aber deutlich mehr).
     */
    public static final int COUPLE_DOUBLE_BED_BONUS = 10;

    /**
     * Vereinfachtes Geschlecht für die Geschlechtertrennung.
     * OTHER = divers, unbekannt oder leer – solche Personen werden von der
     * automatischen Geschlechtertrennung NICHT eingeschränkt (das kann die
     * Leitung bei Bedarf mit eigenen Regeln oder von Hand festlegen).
     */
    public enum Gender { MALE, FEMALE, OTHER }

    // =========================================================================
    // Ergebnis-Klassen
    // =========================================================================

    /**
     * Ergebnis der Prüfung "Person X in Bett Y": Punktzahl, ob es verboten ist,
     * und die Begründungen.
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
     * Eine einzelne Auswirkung, die ein Zimmernachbar auf eine Person hat,
     * z.B. "+50 Punkte, weil der Partner im selben Zimmer ist" oder
     * "Verbot, weil ein Mann mit einer Frau im Zimmer wäre".
     * Die Punkte gelten für die betrachtete Person (der Nachbar bekommt bei
     * seiner eigenen Prüfung ebenfalls Punkte).
     */
    public static class RoommateEffect {
        private final String ruleId;
        private final String ruleName;
        private final int points;
        private final boolean hardViolation;
        private final String reason;

        public RoommateEffect(String ruleId, String ruleName, int points, boolean hardViolation, String reason) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.points = points;
            this.hardViolation = hardViolation;
            this.reason = reason;
        }

        public String getRuleId() {
            return ruleId;
        }

        public String getRuleName() {
            return ruleName;
        }

        /** Punkte für die betrachtete Person ({@link #FORBIDDEN_SCORE} bei einem Verbot) */
        public int getPoints() {
            return points;
        }

        /** true = eine harte Regel wäre verletzt (darf nicht passieren) */
        public boolean isHardViolation() {
            return hardViolation;
        }

        public String getReason() {
            return reason;
        }

        /** Wandelt die Auswirkung in eine Begründung für die Oberfläche um. */
        public ScoreExplanation toExplanation() {
            return new ScoreExplanation(ruleId, ruleName, points, hardViolation ? RuleType.HARD : RuleType.SOFT, reason);
        }
    }

    // =========================================================================
    // 1) Regeln für EINE Person in EINEM Bett
    // =========================================================================

    /**
     * Bewertet, wie gut eine Person in ein bestimmtes Bett passt.
     * Geprüft werden alle aktiven Regeln vom Typ BED_PERSON und ROOM_PERSON,
     * die Mädchen-/Jungenzimmer-Markierung (gehört zur Geschlechtertrennung)
     * und der Doppelbett-Bonus der Paar-Regel.
     *
     * @param person  Die zu bewertende Person
     * @param bed     Das Zielbett
     * @param room    Der Raum, in dem das Bett steht
     * @param rules   Alle Regeln des Projekts (ausgeschaltete werden übersprungen)
     * @return Punkte, Verbots-Markierung und Begründungen
     */
    public EvaluationResult evaluatePersonBedPair(Person person, Bed bed, Room room, List<DynamicRule> rules) {
        EvaluationResult result = new EvaluationResult();
        if (person == null || bed == null || rules == null) {
            return result;
        }

        for (DynamicRule rule : rules) {
            if (rule == null || !rule.isActive() || rule.getTargetScope() == null) continue;

            TargetScope scope = rule.getTargetScope();

            // Die Geschlechtertrennung wirkt hier nur auf markierte Räume
            // (Mädchenzimmer / Jungenzimmer). Gemischte Zimmer werden bei den
            // Zimmernachbarn geprüft (siehe evaluateRoommates).
            if (scope == TargetScope.GENDER_SEPARATION) {
                checkMarkedRoomGender(rule, result, person, room);
                continue;
            }

            // Paar-Regel: kleiner Bonus fürs Doppelbett (das Zusammensein selbst
            // wird bei den Zimmernachbarn bewertet, siehe evaluateRoommates)
            if (scope == TargetScope.PAIR_CO_LOCATION) {
                checkCoupleDoubleBed(rule, result, person, bed);
                continue;
            }

            // Andere Regeln für Zimmernachbarn (Gruppen, ...) gehören nicht hierher
            if (scope.isRoommateScope()) {
                continue;
            }

            // Sicherheitsnetz für sehr alte Projektdateien: Eine Raum-Regel OHNE
            // Bedingungen würde sonst auf JEDES Bett zutreffen.
            if (scope == TargetScope.ROOM_PERSON && (rule.getConditions() == null || rule.getConditions().isEmpty())) {
                continue;
            }

            // Treffen ALLE WENN-Bedingungen zu? Dann Wirkung anwenden.
            if (evaluateConditions(rule.getConditions(), person, bed, room)) {
                applyRuleAction(rule, result);
            }
        }

        return result;
    }

    /**
     * Teil der Geschlechtertrennung: Jungen nicht ins Mädchenzimmer und
     * Mädchen nicht ins Jungenzimmer (nur für Räume, die so markiert sind).
     */
    private void checkMarkedRoomGender(DynamicRule rule, EvaluationResult result, Person person, Room room) {
        if (room == null) return;

        Gender gender = genderOf(person);
        String reason = null;
        if (room.isGirlsRoom() && gender == Gender.MALE) {
            reason = "Männliche Teilnehmer dürfen nicht im Mädchenzimmer '" + room.getName() + "' schlafen.";
        } else if (room.isBoysRoom() && gender == Gender.FEMALE) {
            reason = "Weibliche Teilnehmer dürfen nicht im Jungenzimmer '" + room.getName() + "' schlafen.";
        }

        if (reason != null) {
            result.setForbidden(true);
            result.addScore(FORBIDDEN_SCORE);
            result.addExplanation(new ScoreExplanation(
                    rule.getId(), rule.getName(), FORBIDDEN_SCORE, RuleType.HARD, "Harte Regel verletzt: " + reason));
        }
    }

    /**
     * Teil der Paar-Regel: Personen mit Paar-ID bekommen im Doppelbett einen
     * kleinen Bonus ({@link #COUPLE_DOUBLE_BED_BONUS}) – nur wenn die Paar-Regel
     * Paare zusammen haben MÖCHTE (also kein Verbot und positive Punkte).
     */
    private void checkCoupleDoubleBed(DynamicRule rule, EvaluationResult result, Person person, Bed bed) {
        boolean hasPartner = person.getPartnerId() != null && !person.getPartnerId().isBlank();
        if (!hasPartner || bed.getBedType() != BedType.DOUBLE) return;
        if (isForbidRule(rule) || signedPoints(rule) <= 0) return;
        if (!evaluateConditions(rule.getConditions(), person, null, null)) return;

        result.addScore(COUPLE_DOUBLE_BED_BONUS);
        result.addExplanation(new ScoreExplanation(rule.getId(), rule.getName(), COUPLE_DOUBLE_BED_BONUS, RuleType.SOFT,
                "Paar im Doppelbett (" + formatPoints(COUPLE_DOUBLE_BED_BONUS) + " Pkt)"));
    }

    /**
     * Wendet die Wirkung einer zutreffenden Regel an:
     * Verbot setzen ODER Punkte hinzufügen/abziehen.
     */
    private void applyRuleAction(DynamicRule rule, EvaluationResult result) {
        if (isForbidRule(rule)) {
            result.setForbidden(true);
            result.addScore(FORBIDDEN_SCORE);
            String text = rule.getDescription() != null && !rule.getDescription().isBlank()
                    ? rule.getDescription()
                    : rule.getName();
            result.addExplanation(new ScoreExplanation(
                    rule.getId(), rule.getName(), FORBIDDEN_SCORE, RuleType.HARD, "Harte Regel verletzt: " + text));
            return;
        }

        int points = signedPoints(rule);
        if (points == 0) return;

        result.addScore(points);
        String reason = points > 0
                ? "Wunsch erfüllt: " + rule.getName() + " (" + formatPoints(points) + " Pkt)"
                : "Unerwünscht: " + rule.getName() + " (" + formatPoints(points) + " Pkt)";
        result.addExplanation(new ScoreExplanation(rule.getId(), rule.getName(), points, RuleType.SOFT, reason));
    }

    // =========================================================================
    // 2) Regeln für ZIMMERNACHBARN (zwei Personen im selben Zimmer)
    // =========================================================================

    /**
     * Prüft alle Regeln, die davon abhängen, WER zusammen in einem Zimmer schläft.
     * Die Methode beschreibt die Sicht von {@code self}: "Was bedeutet es für
     * mich, dass {@code other} in meinem Zimmer ist?"
     *
     * Regeln, die hier geprüft werden:
     * - Geschlechtertrennung (GENDER_SEPARATION): Mann + Frau im selben Zimmer = Verbot.
     *   AUSNAHME: Die beiden sind ein Paar UND eine aktive Paar-Regel möchte sie zusammen.
     * - Paare / Gruppen / gleiche Eigenschaft zusammen (PAIR_, GROUP_, CONDITION_CO_LOCATION):
     *   Pluspunkte (oder Minuspunkte bzw. Verbot, falls die Regel so eingestellt ist).
     *
     * Die Geschlechtertrennung und die Paar-Regel sind dabei UNABHÄNGIG schaltbar:
     * - nur Geschlechtertrennung an  -> auch Paare werden nach Geschlecht getrennt
     * - nur Paar-Regel an            -> Paare bekommen Bonuspunkte, gemischte Zimmer erlaubt
     * - beide an                     -> getrennte Zimmer, aber Paare dürfen zusammen
     *
     * @return Liste aller Auswirkungen (leer, wenn keine Regel betroffen ist)
     */
    public List<RoommateEffect> evaluateRoommates(Person self, Person other, List<DynamicRule> rules) {
        List<RoommateEffect> effects = new ArrayList<>();
        if (self == null || other == null || rules == null) {
            return effects;
        }
        String otherName = other.getFullName();

        // --- Regel A: Geschlechtertrennung ---------------------------------
        DynamicRule genderRule = findActiveRule(rules, TargetScope.GENDER_SEPARATION);
        if (genderRule != null && isMaleFemaleMix(self, other)) {
            if (coupleRuleWantsThemTogether(rules, self, other)) {
                // Erlaubt: angemeldetes Paar und die Paar-Regel ist eingeschaltet
                effects.add(new RoommateEffect(genderRule.getId(), genderRule.getName(), 0, false,
                        "Paar-Ausnahme: darf trotz Geschlechtertrennung mit Partner/in " + otherName + " im Zimmer sein"));
            } else {
                effects.add(new RoommateEffect(genderRule.getId(), genderRule.getName(), FORBIDDEN_SCORE, true,
                        "Harte Regel verletzt: Männer und Frauen im selben Zimmer (" + otherName + ")"));
            }
        }

        // --- Regel B: "Zusammen ins selbe Zimmer"-Regeln --------------------
        for (DynamicRule rule : rules) {
            if (rule == null || !rule.isActive() || rule.getTargetScope() == null) continue;
            TargetScope scope = rule.getTargetScope();
            if (scope == TargetScope.GENDER_SEPARATION || !scope.isRoommateScope()) continue;

            // Gehören die beiden laut dieser Regel zusammen? (gleiches Paar / gleiche Gruppe / ...)
            if (!belongTogether(rule, self, other)) continue;

            if (isForbidRule(rule)) {
                // Eine harte "Zusammen"-Regel bedeutet: Diese Personen dürfen NICHT zusammen sein.
                effects.add(new RoommateEffect(rule.getId(), rule.getName(), FORBIDDEN_SCORE, true,
                        "Harte Regel verletzt: darf nicht mit " + otherName + " im selben Zimmer sein"));
                continue;
            }

            int points = signedPoints(rule);
            if (points == 0) continue;
            String reason = points > 0
                    ? "Zusammen mit " + otherName + " im Zimmer (" + formatPoints(points) + " Pkt)"
                    : "Unerwünscht: mit " + otherName + " im selben Zimmer (" + formatPoints(points) + " Pkt)";
            effects.add(new RoommateEffect(rule.getId(), rule.getName(), points, false, reason));
        }

        return effects;
    }

    /**
     * Gehören zwei Personen laut einer "Zusammen"-Regel zusammen?
     * - Paar-Regel:       gleiche Paar-ID
     * - Gruppen-Regel:    gleiche Gruppen-ID
     * - Eigenschafts-Regel: beide erfüllen die WENN-Bedingungen (z.B. beide älter als 65)
     * Zusätzliche WENN-Bedingungen (z.B. "nur Gruppe Klasse 8a") müssen für BEIDE gelten.
     */
    private boolean belongTogether(DynamicRule rule, Person a, Person b) {
        switch (rule.getTargetScope()) {
            case PAIR_CO_LOCATION:
                if (!isCouple(a, b)) return false;
                break;
            case GROUP_CO_LOCATION:
                if (!isSameGroup(a, b)) return false;
                break;
            case CONDITION_CO_LOCATION:
                break;
            default:
                return false;
        }
        // Bett/Raum sind hier unbekannt (null) – es zählen nur Personen-Eigenschaften
        return evaluateConditions(rule.getConditions(), a, null, null)
                && evaluateConditions(rule.getConditions(), b, null, null);
    }

    /**
     * Gibt es eine eingeschaltete Paar-Regel, die genau dieses Paar zusammen
     * haben möchte? Nur dann gilt die Paar-Ausnahme bei der Geschlechtertrennung.
     */
    private boolean coupleRuleWantsThemTogether(List<DynamicRule> rules, Person a, Person b) {
        for (DynamicRule rule : rules) {
            if (rule == null || !rule.isActive()) continue;
            if (rule.getTargetScope() != TargetScope.PAIR_CO_LOCATION) continue;
            if (isForbidRule(rule) || signedPoints(rule) <= 0) continue;
            if (belongTogether(rule, a, b)) return true;
        }
        return false;
    }

    /** Sucht die erste eingeschaltete Regel mit dem angegebenen Typ (oder null). */
    private DynamicRule findActiveRule(List<DynamicRule> rules, TargetScope scope) {
        for (DynamicRule rule : rules) {
            if (rule != null && rule.isActive() && rule.getTargetScope() == scope) {
                return rule;
            }
        }
        return null;
    }

    // =========================================================================
    // Kleine Hilfsmethoden rund um Personen und Regeln
    // =========================================================================

    /**
     * Übersetzt die Geschlechtsangabe einer Person in MALE / FEMALE / OTHER.
     * Erkannt werden z.B. "maennlich", "männlich", "m", "Junge", "male" sowie
     * "weiblich", "w", "f", "Frau", "Mädchen", "female".
     */
    public static Gender genderOf(Person person) {
        if (person == null || person.getGender() == null) return Gender.OTHER;
        String g = person.getGender().trim().toLowerCase(Locale.GERMAN);
        if (g.isEmpty()) return Gender.OTHER;

        // Weiblich zuerst prüfen: "mädchen" beginnt sonst mit "m" wie "männlich"
        // und "female" enthält "male"!
        if (g.startsWith("mäd") || g.startsWith("maed") || g.startsWith("w") || g.startsWith("f")
                || g.contains("weiblich") || g.contains("female")) {
            return Gender.FEMALE;
        }
        if (g.startsWith("m") || g.startsWith("j") || g.contains("männlich") || g.contains("maennlich")) {
            return Gender.MALE;
        }
        return Gender.OTHER;
    }

    /** true, wenn genau einer männlich und der andere weiblich ist. */
    public static boolean isMaleFemaleMix(Person a, Person b) {
        Gender ga = genderOf(a);
        Gender gb = genderOf(b);
        return (ga == Gender.MALE && gb == Gender.FEMALE) || (ga == Gender.FEMALE && gb == Gender.MALE);
    }

    /** true, wenn beide dieselbe (nicht leere) Paar-ID haben. Groß-/Kleinschreibung egal. */
    public static boolean isCouple(Person a, Person b) {
        return sameNonBlankText(a.getPartnerId(), b.getPartnerId());
    }

    /** true, wenn beide dieselbe (nicht leere) Gruppen-ID haben. Groß-/Kleinschreibung egal. */
    public static boolean isSameGroup(Person a, Person b) {
        return sameNonBlankText(a.getGroupId(), b.getGroupId());
    }

    private static boolean sameNonBlankText(String x, String y) {
        if (x == null || y == null) return false;
        String tx = x.trim();
        return !tx.isEmpty() && tx.equalsIgnoreCase(y.trim());
    }

    /**
     * Ist die Regel ein striktes Verbot?
     * Das ist der Fall bei einer HARTEN Regel mit Aktion "Verbieten" (oder negativem Gewicht).
     */
    public static boolean isForbidRule(DynamicRule rule) {
        return rule.getRuleType() == RuleType.HARD
                && (rule.getAction() == RuleAction.FORBID || rule.getWeight() < 0);
    }

    /**
     * Punkte einer Wunsch-Regel mit Vorzeichen:
     * "Punkte hinzufügen" -> +Gewicht, "Punkte abziehen" -> -Gewicht.
     * (Ein negatives Gewicht bei "hinzufügen" ergibt ebenfalls einen Abzug.)
     */
    public static int signedPoints(DynamicRule rule) {
        if (rule.getAction() == RuleAction.ADD_POINTS) return rule.getWeight();
        if (rule.getAction() == RuleAction.SUBTRACT_POINTS) return -rule.getWeight();
        return 0;
    }

    /** Formatiert Punkte mit Vorzeichen: 50 -> "+50", -25 -> "-25". */
    public static String formatPoints(int points) {
        return points > 0 ? "+" + points : String.valueOf(points);
    }

    // =========================================================================
    // WENN-Bedingungen auswerten
    // =========================================================================

    /**
     * Prüft, ob ALLE WENN-Bedingungen einer Regel erfüllt sind (UND-Verknüpfung).
     * Keine Bedingungen = Regel gilt für alle.
     */
    private boolean evaluateConditions(List<Condition> conditions, Person person, Bed bed, Room room) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }

        for (Condition condition : conditions) {
            if (condition == null) continue;
            if (!evaluateCondition(condition, person, bed, room)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prüft EINE Bedingung, z.B. "person.age GREATER_THAN 65":
     * 1. Den echten Wert holen (Alter der Person, z.B. 70)
     * 2. Mit dem Wert aus der Regel vergleichen (65) -> 70 > 65 -> erfüllt
     */
    private boolean evaluateCondition(Condition condition, Person person, Bed bed, Room room) {
        String field = condition.getField();
        Operator op = condition.getOperator();
        Object targetVal = condition.getValue();
        if (op == null) return false;

        Object actualVal = extractFieldValue(field, person, bed, room);

        // Sonderfall: Vergleich mit einem ANDEREN Feld (z.B. Wunsch-Etage == Raum-Etage)
        if (op == Operator.MATCH_ROOM_PROP) {
            return Objects.equals(actualVal, extractFieldValue(String.valueOf(targetVal), person, bed, room));
        }

        return compareValues(actualVal, op, targetVal);
    }

    /**
     * Holt den Wert eines Feldes anhand seines Namens, z.B. "person.age" -> 70,
     * "bed.isTopBunk" -> true. Eigene Zusatzspalten aus dem Import erreicht man
     * über "person.custom.<spaltenname>".
     * Groß-/Kleinschreibung des Feldnamens ist egal.
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
            case "person.specialneeds":
            case "specialneeds":
                return person != null ? person.getSpecialNeeds() : null;
            case "bed.bedtype":
            case "bedtype":
                return bed != null && bed.getBedType() != null ? bed.getBedType().name() : null;
            case "bed.istopbunk":
            case "topbunk":
                return bed != null ? bed.isTopBunk() : null;
            case "bed.isbottombunk":
            case "bottombunk":
                return bed != null ? bed.isBottomBunk() : null;
            case "room.floor":
            case "floor":
                return room != null ? room.getFloor() : (bed != null ? bed.getFloor() : null);
            case "room.girlsroom":
            case "girlsroom":
                return room != null ? room.isGirlsRoom() : null;
            case "room.boysroom":
            case "boysroom":
                return room != null ? room.isBoysRoom() : null;
            case "room.name":
            case "roomname":
                return room != null ? room.getName() : (bed != null ? bed.getRoomName() : null);
            default:
                // Eigene Zusatz-Attribute aus den "customAttributes"-Listen
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
     * Vergleicht den echten Wert mit dem Regel-Wert (=, !=, >, >=, <, <=, enthält, in Liste).
     * Fehlt der echte Wert (null), ist nur "!=" erfüllt.
     */
    private boolean compareValues(Object actual, Operator op, Object target) {
        if (actual == null) {
            return op == Operator.NOT_EQUALS && target != null;
        }

        switch (op) {
            case EQUALS:
                return compareEquals(actual, target);
            case NOT_EQUALS:
                return !compareEquals(actual, target);
            case GREATER_THAN:
            case GREATER_EQUAL:
            case LESS_THAN:
            case LESS_EQUAL: {
                Double a = toNumber(actual);
                Double b = toNumber(target);
                // Kein Zahlenvergleich möglich (z.B. Text) -> Bedingung nicht erfüllt
                if (a == null || b == null) return false;
                int cmp = Double.compare(a, b);
                if (op == Operator.GREATER_THAN) return cmp > 0;
                if (op == Operator.GREATER_EQUAL) return cmp >= 0;
                if (op == Operator.LESS_THAN) return cmp < 0;
                return cmp <= 0;
            }
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

    /** Gleichheit für Zahlen (65 == 65.0), Wahrheitswerte und Texte (ohne Groß-/Kleinschreibung). */
    private boolean compareEquals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;

        if (a instanceof Boolean || b instanceof Boolean) {
            return Boolean.valueOf(a.toString()).equals(Boolean.valueOf(b.toString()));
        }

        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }

        return a.toString().trim().equalsIgnoreCase(b.toString().trim());
    }

    /** Versucht, einen Wert in eine Zahl umzuwandeln ("65" -> 65.0). Gibt null zurück, wenn das nicht geht. */
    private Double toNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) return null;
        try {
            return Double.parseDouble(String.valueOf(value).trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
