package de.zuordner.engine;

import de.zuordner.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ============================================================================
 * AssignmentService – berechnet die komplette Zimmer- und Bettenbelegung
 * ============================================================================
 * Ablauf von {@link #calculateAssignment} in 6 Schritten:
 *
 *  1. Alle Betten und Räume aus allen Gebäuden einsammeln.
 *  2. Tabelle "Person x Bett": Wie gut passt jede Person in jedes Bett?
 *     (RuleEngine prüft z.B. "Senioren nicht ins obere Hochbett")
 *  3. Tabelle "Person x Person": Was passiert, wenn zwei Personen im selben
 *     Zimmer schlafen? (Paare/Gruppen zusammen = Pluspunkte,
 *     Mann + Frau bei Geschlechtertrennung = Verbot)
 *  4. Startverteilung mit dem Ungarischen Algorithmus (nutzt nur Tabelle 2).
 *  5. Verbesserung mit dem {@link LocalSearchOptimizer} (nutzt Tabelle 2 UND 3).
 *  6. Ergebnis zusammenbauen: Punkte, Begründungen, Personen ohne Bett (mit Grund).
 *
 * Rangfolge der Ziele (dafür sorgen die Strafwerte unten):
 *   1. Keine harte Regel verletzen          (Strafe pro Verstoß: 1.000.000)
 *   2. Möglichst jede Person unterbringen   (Strafe pro Person ohne Bett: 100.000)
 *   3. Möglichst viele Wunsch-Punkte sammeln (typisch 10 bis 50 Punkte)
 * Lieber bleibt also eine Person ohne Bett (und erscheint im Dashboard mit
 * Begründung), als dass ein Verbot gebrochen wird.
 */
@Service
public class AssignmentService {

    /** Strafpunkte für einen Verstoß gegen eine harte Regel */
    static final long HARD_PENALTY = 1_000_000L;
    /** Strafpunkte für jede Person, die kein Bett bekommt */
    static final long UNASSIGNED_PENALTY = 100_000L;
    /** Fester Startwert für den Zufallsgenerator -> gleiche Eingabe, gleiches Ergebnis */
    private static final long RANDOM_SEED = 20261014L;

    private final RuleEngine ruleEngine;

    /** Spring übergibt hier automatisch die RuleEngine ("Dependency Injection"). */
    public AssignmentService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * Berechnet die beste Verteilung der Personen auf die Betten.
     *
     * @param persons   alle Teilnehmer
     * @param buildings alle Gebäude (mit Räumen und Betten)
     * @param rules     alle Regeln (ausgeschaltete werden ignoriert)
     * @return das fertige Ergebnis für Dashboard und Excel-Export
     */
    public AssignmentResult calculateAssignment(List<Person> persons, List<Building> buildings, List<DynamicRule> rules) {
        long startTime = System.currentTimeMillis();

        // Leere Listen statt null, damit wir unten nicht ständig prüfen müssen
        persons = persons == null ? new ArrayList<>() : withoutNulls(persons);
        buildings = buildings == null ? new ArrayList<>() : withoutNulls(buildings);
        rules = rules == null ? new ArrayList<>() : withoutNulls(rules);

        // ---------------------------------------------------------------------
        // Schritt 1: Betten und Räume einsammeln
        // ---------------------------------------------------------------------
        List<Bed> allBeds = new ArrayList<>();
        List<Room> allRooms = new ArrayList<>();
        List<Integer> bedRoomList = new ArrayList<>(); // für jedes Bett: Nummer seines Raums

        for (Building building : buildings) {
            if (building.getRooms() == null) continue;
            for (Room room : building.getRooms()) {
                if (room == null || room.getBeds() == null) continue;
                int roomIndex = allRooms.size();
                allRooms.add(room);
                for (Bed bed : room.getBeds()) {
                    if (bed == null) continue;
                    allBeds.add(bed);
                    bedRoomList.add(roomIndex);
                }
            }
        }

        int numPeople = persons.size();
        int numBeds = allBeds.size();
        int[] bedRoom = bedRoomList.stream().mapToInt(Integer::intValue).toArray();

        // Ohne Personen oder ohne Betten gibt es nichts zu verteilen
        if (numPeople == 0 || numBeds == 0) {
            AssignmentResult emptyResult = new AssignmentResult();
            for (Person p : persons) {
                emptyResult.getUnassignedPersonIds().add(p.getId());
                emptyResult.getUnassignedReasons().put(p.getId(), "Es sind noch keine Betten angelegt.");
            }
            for (Bed b : allBeds) emptyResult.getUnassignedBedIds().add(b.getId());
            emptyResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return emptyResult;
        }

        // ---------------------------------------------------------------------
        // Schritt 2: Tabelle "Person x Bett"
        // ---------------------------------------------------------------------
        RuleEngine.EvaluationResult[][] evalMatrix = new RuleEngine.EvaluationResult[numPeople][numBeds];
        int[][] scoreMatrix = new int[numPeople][numBeds];   // für den Ungarischen Algorithmus
        long[][] unary = new long[numPeople][numBeds];       // für den LocalSearchOptimizer

        for (int i = 0; i < numPeople; i++) {
            Person person = persons.get(i);
            for (int j = 0; j < numBeds; j++) {
                Bed bed = allBeds.get(j);
                Room room = allRooms.get(bedRoom[j]);

                RuleEngine.EvaluationResult eval = ruleEngine.evaluatePersonBedPair(person, bed, room, rules);
                evalMatrix[i][j] = eval;
                scoreMatrix[i][j] = eval.getScore();
                unary[i][j] = eval.isForbidden() ? -HARD_PENALTY : eval.getScore();
            }
        }

        // ---------------------------------------------------------------------
        // Schritt 3: Tabelle "Person x Person" (Zimmernachbarn)
        // ---------------------------------------------------------------------
        long[][] pair = new long[numPeople][numPeople];
        if (hasActiveRoommateRules(rules)) {
            for (int p = 0; p < numPeople; p++) {
                for (int q = p + 1; q < numPeople; q++) {
                    // Punkte für p (wegen q) + Punkte für q (wegen p)
                    long value = roommateValue(persons.get(p), persons.get(q), rules)
                            + roommateValue(persons.get(q), persons.get(p), rules);
                    pair[p][q] = value;
                    pair[q][p] = value;
                }
            }
        }

        // ---------------------------------------------------------------------
        // Schritt 4: Startverteilung mit dem Ungarischen Algorithmus
        // ---------------------------------------------------------------------
        int[] initialAssignment = HungarianSolver.solveMaxWeightMatching(scoreMatrix);

        // ---------------------------------------------------------------------
        // Schritt 5: Verbesserung (berücksichtigt jetzt auch die Zimmernachbarn)
        // ---------------------------------------------------------------------
        LocalSearchOptimizer optimizer = new LocalSearchOptimizer(
                unary, pair, bedRoom, allRooms.size(), initialAssignment, UNASSIGNED_PENALTY);
        int[] bedOf = optimizer.optimize(RANDOM_SEED, startTemperature(unary, pair));

        // Sicherheitsnetz: Niemand darf in einem für ihn verbotenen Bett liegen
        for (int i = 0; i < numPeople; i++) {
            if (bedOf[i] >= 0 && evalMatrix[i][bedOf[i]].isForbidden()) {
                bedOf[i] = -1;
            }
        }

        // ---------------------------------------------------------------------
        // Schritt 6: Ergebnis zusammenbauen
        // ---------------------------------------------------------------------
        return buildResult(persons, allBeds, allRooms, bedRoom, rules, evalMatrix, bedOf, startTime);
    }

    /**
     * Baut aus der fertigen Verteilung das Ergebnis-Objekt: für jede Person die
     * Punkte und Begründungen, für Personen ohne Bett einen verständlichen Grund.
     */
    private AssignmentResult buildResult(List<Person> persons, List<Bed> allBeds, List<Room> allRooms, int[] bedRoom,
                                         List<DynamicRule> rules, RuleEngine.EvaluationResult[][] evalMatrix,
                                         int[] bedOf, long startTime) {
        int numPeople = persons.size();
        int numBeds = allBeds.size();

        // Wer liegt in welchem Raum? (Raum-Nummer -> Liste von Personen-Nummern)
        List<List<Integer>> roomOccupants = new ArrayList<>();
        for (int r = 0; r < allRooms.size(); r++) roomOccupants.add(new ArrayList<>());
        boolean[] bedTaken = new boolean[numBeds];
        for (int i = 0; i < numPeople; i++) {
            if (bedOf[i] >= 0) {
                roomOccupants.get(bedRoom[bedOf[i]]).add(i);
                bedTaken[bedOf[i]] = true;
            }
        }

        AssignmentResult result = new AssignmentResult();
        int totalScore = 0;
        int hardViolations = 0;

        for (int i = 0; i < numPeople; i++) {
            Person person = persons.get(i);
            int bedIndex = bedOf[i];

            if (bedIndex < 0) {
                result.getUnassignedPersonIds().add(person.getId());
                result.getUnassignedReasons().put(person.getId(),
                        explainUnassigned(i, persons, allRooms, bedRoom, rules, evalMatrix, bedTaken, roomOccupants));
                continue;
            }

            Bed bed = allBeds.get(bedIndex);
            Room room = allRooms.get(bedRoom[bedIndex]);
            RuleEngine.EvaluationResult eval = evalMatrix[i][bedIndex];

            // Punkte und Begründungen aus Schritt 2 (Person + Bett) ...
            int score = eval.getScore();
            List<ScoreExplanation> explanations = new ArrayList<>(eval.getExplanations());

            // ... plus alles, was durch die Zimmernachbarn dazukommt
            for (int k : roomOccupants.get(bedRoom[bedIndex])) {
                if (k == i) continue;
                for (RuleEngine.RoommateEffect effect : ruleEngine.evaluateRoommates(person, persons.get(k), rules)) {
                    score += effect.getPoints();
                    explanations.add(effect.toExplanation());
                    // Jeden Verstoß nur einmal zählen (nicht bei beiden Personen)
                    if (effect.isHardViolation() && i < k) {
                        hardViolations++;
                    }
                }
            }

            totalScore += score;
            result.getAssignments().add(new AssignmentPair(
                    person.getId(),
                    bed.getId(),
                    person.getFullName(),
                    bed.getName(),
                    room.getName(),
                    room.getBuildingName() != null ? room.getBuildingName() : bed.getBuildingName(),
                    score,
                    explanations
            ));
        }

        // Freie Betten auflisten
        for (int b = 0; b < numBeds; b++) {
            if (!bedTaken[b]) {
                result.getUnassignedBedIds().add(allBeds.get(b).getId());
            }
        }

        result.setTotalScore(totalScore);
        result.setHardRuleViolationsCount(hardViolations);
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Findet einen verständlichen Grund, warum Person i kein Bett bekommen hat.
     * Dazu wird jedes freie Bett geprüft: Ist es für die Person verboten oder
     * würde ein Zimmernachbar eine harte Regel verletzen (z.B. Geschlechtertrennung)?
     */
    private String explainUnassigned(int i, List<Person> persons, List<Room> allRooms, int[] bedRoom,
                                     List<DynamicRule> rules, RuleEngine.EvaluationResult[][] evalMatrix,
                                     boolean[] bedTaken, List<List<Integer>> roomOccupants) {
        int freeBeds = 0;
        for (boolean taken : bedTaken) {
            if (!taken) freeBeds++;
        }
        if (freeBeds == 0) {
            return "Alle Betten sind belegt (" + persons.size() + " Personen, " + bedTaken.length + " Betten).";
        }

        // Welche harten Regeln blockieren die freien Betten?
        Set<String> blockingRules = new LinkedHashSet<>();
        boolean someFreeBedAllowed = false;
        for (int b = 0; b < bedTaken.length; b++) {
            if (bedTaken[b]) continue;
            boolean blocked = false;

            for (ScoreExplanation e : evalMatrix[i][b].getExplanations()) {
                if (e.getRuleType() == RuleType.HARD) {
                    blockingRules.add(e.getRuleName());
                    blocked = true;
                }
            }
            for (int k : roomOccupants.get(bedRoom[b])) {
                for (RuleEngine.RoommateEffect effect : ruleEngine.evaluateRoommates(persons.get(i), persons.get(k), rules)) {
                    if (effect.isHardViolation()) {
                        blockingRules.add(effect.getRuleName());
                        blocked = true;
                    }
                }
            }
            if (!blocked) someFreeBedAllowed = true;
        }

        if (!someFreeBedAllowed) {
            return "Kein passendes freies Bett: Alle " + freeBeds + " freien Betten verstoßen gegen harte Regeln ("
                    + String.join(", ", blockingRules) + ").";
        }
        return "Keine passende Zuordnung gefunden – bitte Regeln und Bettenanzahl prüfen.";
    }

    /**
     * Gesamtwert für Person {@code self}, wenn {@code other} im selben Zimmer ist.
     * Harte Verstöße werden auf die Strafe {@link #HARD_PENALTY} gesetzt.
     */
    private long roommateValue(Person self, Person other, List<DynamicRule> rules) {
        long value = 0;
        for (RuleEngine.RoommateEffect effect : ruleEngine.evaluateRoommates(self, other, rules)) {
            value += effect.isHardViolation() ? -HARD_PENALTY : effect.getPoints();
        }
        return value;
    }

    /** Gibt es mindestens eine eingeschaltete Regel, die Zimmernachbarn betrifft? */
    private boolean hasActiveRoommateRules(List<DynamicRule> rules) {
        for (DynamicRule rule : rules) {
            if (rule.isActive() && rule.getTargetScope() != null && rule.getTargetScope().isRoommateScope()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Anfangstemperatur fürs Simulated Annealing: so groß wie der größte
     * "normale" Punktwert (Strafen für harte Regeln zählen nicht mit),
     * aber mindestens 10 und höchstens 1000.
     */
    private double startTemperature(long[][] unary, long[][] pair) {
        long maxSoft = 0;
        for (long[] row : unary) {
            for (long v : row) {
                if (Math.abs(v) < HARD_PENALTY / 2) maxSoft = Math.max(maxSoft, Math.abs(v));
            }
        }
        for (long[] row : pair) {
            for (long v : row) {
                if (Math.abs(v) < HARD_PENALTY / 2) maxSoft = Math.max(maxSoft, Math.abs(v));
            }
        }
        return Math.min(1000, Math.max(10, maxSoft));
    }

    /** Kopie einer Liste ohne null-Einträge (schützt vor kaputten Projektdateien). */
    private static <T> List<T> withoutNulls(List<T> list) {
        List<T> copy = new ArrayList<>(list.size());
        for (T item : list) {
            if (item != null) copy.add(item);
        }
        return copy;
    }
}
