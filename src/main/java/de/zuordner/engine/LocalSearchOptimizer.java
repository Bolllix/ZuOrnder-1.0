package de.zuordner.engine;

import java.util.Arrays;
import java.util.Random;

/**
 * ============================================================================
 * LocalSearchOptimizer – verbessert eine Zimmerverteilung Schritt für Schritt
 * ============================================================================
 * Warum gibt es diese Klasse? (neu in Version 2.0)
 *
 * Der Ungarische Algorithmus ({@link HungarianSolver}) ist sehr gut darin,
 * jeder Person das für SIE beste Bett zu geben. Er kann aber nicht erkennen,
 * WER mit WEM im Zimmer landet. Regeln wie "Paare zusammen", "Gruppen
 * zusammen" oder "Jungen und Mädchen getrennt" hängen aber genau davon ab.
 * In Version 1.x wurden diese Regeln deshalb erst NACH der Verteilung
 * angeschaut – sie hatten also gar keinen Einfluss auf das Ergebnis.
 *
 * Diese Klasse nimmt die Verteilung des Ungarischen Algorithmus als Startpunkt
 * und probiert danach sehr viele kleine Änderungen aus:
 *   - Person in ein freies Bett umziehen lassen
 *   - zwei Personen die Betten tauschen lassen
 *   - eine Person (vorerst) ohne Bett lassen
 * Jede Änderung wird mit einer Punktzahl bewertet (siehe "Gesamtpunktzahl" unten).
 *
 * Verfahren 1 – "Simulated Annealing" (simuliertes Abkühlen):
 *   Verbesserungen werden immer angenommen. Am Anfang werden auch kleine
 *   Verschlechterungen manchmal angenommen (so wie heißes Metall noch formbar
 *   ist). Dadurch bleibt die Suche nicht in einer "Sackgasse" stecken. Mit der
 *   Zeit "kühlt" das Verfahren ab und nimmt nur noch Verbesserungen an.
 *
 * Verfahren 2 – "Hill Climbing" (Bergsteigen) am Ende:
 *   Systematisch ALLE möglichen Umzüge/Tausche durchprobieren und jede
 *   Verbesserung sofort übernehmen – so lange, bis es nicht mehr besser geht.
 *
 * Gesamtpunktzahl einer Verteilung =
 *     Summe der Punkte "Person i in Bett b"            (Tabelle unary[i][b])
 *   + Summe der Punkte "Person p und q im selben Zimmer" (Tabelle pair[p][q])
 *   - Strafe für jede Person ohne Bett                  (unassignedPenalty)
 *
 * Der Zufallsgenerator startet immer mit demselben Startwert ("Seed"). Darum
 * kommt bei gleichen Eingaben auch immer dieselbe Verteilung heraus.
 */
public final class LocalSearchOptimizer {

    /** Mindestanzahl an Versuchen beim Simulated Annealing */
    private static final long MIN_ITERATIONS = 50_000L;
    /** Versuche pro Person (mehr Personen = mehr Versuche) */
    private static final long ITERATIONS_PER_PERSON = 2_000L;
    /** Höchstanzahl an Versuchen (damit es auch bei sehr großen Projekten schnell bleibt) */
    private static final long MAX_ITERATIONS = 3_000_000L;
    /** Höchstanzahl kompletter Durchläufe beim Hill Climbing */
    private static final int MAX_HILL_CLIMB_PASSES = 50;
    /** Endtemperatur: so "kühl", dass praktisch nur noch Verbesserungen angenommen werden */
    private static final double END_TEMPERATURE = 0.5;

    // --- Eingaben (werden nicht verändert) ----------------------------------
    private final long[][] unary;          // unary[i][b] = Punkte für Person i in Bett b
    private final long[][] pair;           // pair[p][q]  = Punkte, wenn p und q im selben Zimmer sind
    private final int[] bedRoom;           // bedRoom[b]  = Nummer des Raums, in dem Bett b steht
    private final int[][] roomBeds;        // roomBeds[r] = alle Betten im Raum r
    private final long unassignedPenalty;  // Strafe für eine Person ohne Bett
    private final int numPersons;
    private final int numBeds;

    // --- Aktueller Zustand ----------------------------------------------------
    private final int[] bedOf;             // bedOf[i] = Bett von Person i (-1 = kein Bett)
    private final int[] occupant;          // occupant[b] = Person in Bett b (-1 = frei)

    /**
     * @param unary             Punkte-Tabelle Person x Bett
     * @param pair              Punkte-Tabelle Person x Person (für dasselbe Zimmer), symmetrisch
     * @param bedRoom           Raum-Nummer jedes Bettes
     * @param numRooms          Anzahl der Räume
     * @param initialBedOf      Startverteilung (z.B. vom Ungarischen Algorithmus), -1 = kein Bett
     * @param unassignedPenalty Strafpunkte für eine Person ohne Bett (positiver Wert)
     */
    public LocalSearchOptimizer(long[][] unary, long[][] pair, int[] bedRoom, int numRooms,
                                int[] initialBedOf, long unassignedPenalty) {
        this.unary = unary;
        this.pair = pair;
        this.bedRoom = bedRoom;
        this.unassignedPenalty = unassignedPenalty;
        this.numPersons = unary.length;
        this.numBeds = bedRoom.length;

        // Für jeden Raum die Liste seiner Betten aufbauen (zwei Durchläufe:
        // erst zählen, dann einsortieren)
        int[] bedsPerRoom = new int[numRooms];
        for (int b = 0; b < numBeds; b++) {
            bedsPerRoom[bedRoom[b]]++;
        }
        this.roomBeds = new int[numRooms][];
        for (int r = 0; r < numRooms; r++) {
            roomBeds[r] = new int[bedsPerRoom[r]];
        }
        int[] fill = new int[numRooms];
        for (int b = 0; b < numBeds; b++) {
            int r = bedRoom[b];
            roomBeds[r][fill[r]++] = b;
        }

        // Startverteilung übernehmen (doppelt belegte Betten werden ignoriert)
        this.bedOf = new int[numPersons];
        this.occupant = new int[numBeds];
        Arrays.fill(bedOf, -1);
        Arrays.fill(occupant, -1);
        for (int i = 0; i < numPersons; i++) {
            int b = (initialBedOf != null && i < initialBedOf.length) ? initialBedOf[i] : -1;
            if (b >= 0 && b < numBeds && occupant[b] < 0) {
                bedOf[i] = b;
                occupant[b] = i;
            }
        }
    }

    /**
     * Führt die Optimierung aus.
     *
     * @param seed               Startwert für den Zufallsgenerator (gleicher Seed = gleiches Ergebnis)
     * @param startTemperature   Anfangstemperatur fürs Simulated Annealing (etwa so groß wie
     *                           typische Wunsch-Punkte, z.B. 100)
     * @return bedOf: für jede Person der Index ihres Bettes (-1 = kein Bett)
     */
    public int[] optimize(long seed, double startTemperature) {
        if (numPersons == 0 || numBeds == 0) {
            return bedOf.clone();
        }

        simulatedAnnealing(new Random(seed), Math.max(startTemperature, 1.0));
        hillClimb();
        return bedOf.clone();
    }

    /** Berechnet die Gesamtpunktzahl der aktuellen Verteilung komplett neu. */
    public long totalScore() {
        long total = 0;
        for (int i = 0; i < numPersons; i++) {
            total += unaryOf(i, bedOf[i]);
        }
        for (int[] beds : roomBeds) {
            // jedes Paar (p, q) im Raum genau einmal zählen
            for (int x = 0; x < beds.length; x++) {
                int p = occupant[beds[x]];
                if (p < 0) continue;
                for (int y = x + 1; y < beds.length; y++) {
                    int q = occupant[beds[y]];
                    if (q >= 0) total += pair[p][q];
                }
            }
        }
        return total;
    }

    // =========================================================================
    // Verfahren 1: Simulated Annealing
    // =========================================================================

    private void simulatedAnnealing(Random random, double startTemperature) {
        long iterations = Math.min(MAX_ITERATIONS, Math.max(MIN_ITERATIONS, ITERATIONS_PER_PERSON * numPersons));

        // Die Temperatur sinkt in jedem Schritt um denselben Faktor (von Start- bis Endtemperatur)
        double temperature = startTemperature;
        double coolingFactor = Math.pow(END_TEMPERATURE / startTemperature, 1.0 / iterations);

        long current = totalScore();
        long best = current;
        int[] bestBedOf = bedOf.clone();

        for (long it = 0; it < iterations; it++, temperature *= coolingFactor) {
            // Zufälligen Versuch auswählen: Person i soll in Bett "target"
            // (target = -1 bedeutet: Person bekommt vorerst kein Bett)
            int i = random.nextInt(numPersons);
            int target = random.nextInt(numBeds + 1) - 1;
            if (target == bedOf[i]) continue;

            long delta = delta(i, target);

            // Verbesserung (oder gleich gut) -> immer annehmen.
            // Verschlechterung -> nur mit einer Wahrscheinlichkeit, die mit sinkender
            // Temperatur kleiner wird. Verstöße gegen harte Regeln (-1.000.000 Punkte)
            // werden so praktisch nie angenommen.
            if (delta >= 0 || random.nextDouble() < Math.exp(delta / temperature)) {
                apply(i, target);
                current += delta;
                if (current > best) {
                    best = current;
                    bestBedOf = bedOf.clone();
                }
            }
        }

        // Die beste jemals gefundene Verteilung wiederherstellen
        restore(bestBedOf);
    }

    // =========================================================================
    // Verfahren 2: Hill Climbing
    // =========================================================================

    private void hillClimb() {
        boolean improved = true;
        int passes = 0;
        while (improved && passes < MAX_HILL_CLIMB_PASSES) {
            improved = false;
            passes++;
            for (int i = 0; i < numPersons; i++) {
                // -1 (kein Bett) und jedes Bett ausprobieren
                for (int target = -1; target < numBeds; target++) {
                    if (target == bedOf[i]) continue;
                    if (delta(i, target) > 0) {
                        apply(i, target);
                        improved = true;
                    }
                }
            }
        }
    }

    // =========================================================================
    // Bausteine: Punkte-Änderung berechnen und Änderung ausführen
    // =========================================================================

    /** Punkte für Person i in Bett b (b = -1: Strafe für "kein Bett"). */
    private long unaryOf(int i, int b) {
        return b < 0 ? -unassignedPenalty : unary[i][b];
    }

    /**
     * Summe der Zimmernachbar-Punkte von Person p mit allen Personen im Raum,
     * außer p selbst und den (bis zu zwei) ausgeschlossenen Personen.
     */
    private long roomPairSum(int p, int room, int exclude1, int exclude2) {
        if (room < 0) return 0;
        long sum = 0;
        for (int b : roomBeds[room]) {
            int q = occupant[b];
            if (q >= 0 && q != p && q != exclude1 && q != exclude2) {
                sum += pair[p][q];
            }
        }
        return sum;
    }

    /**
     * Um wie viele Punkte ändert sich die Gesamtpunktzahl, wenn Person i in
     * Bett "target" wechselt? (Positiv = besser.) Die Änderung wird NICHT ausgeführt.
     *
     * Statt alles neu zu berechnen, schauen wir nur auf die betroffenen Räume –
     * das ist viel schneller.
     */
    long delta(int i, int target) {
        int a = bedOf[i];
        if (target == a) return 0;
        int roomA = a < 0 ? -1 : bedRoom[a];

        if (target >= 0 && occupant[target] >= 0) {
            // Fall 1: Das Zielbett ist belegt -> Tausch mit Person k
            int k = occupant[target];
            int roomC = bedRoom[target];
            long d = unaryOf(i, target) + unaryOf(k, a) - unaryOf(i, a) - unaryOf(k, target);
            if (roomA != roomC) {
                // i verlässt Raum A und kommt in Raum C (dort ist k dann nicht mehr)
                d += roomPairSum(i, roomC, k, -1) - roomPairSum(i, roomA, -1, -1);
                // k verlässt Raum C und kommt in Raum A (dort ist i dann nicht mehr)
                d += roomPairSum(k, roomA, i, -1) - roomPairSum(k, roomC, -1, -1);
            }
            return d;
        }

        // Fall 2: Umzug in ein freies Bett (oder target = -1: Person ohne Bett)
        int roomT = target < 0 ? -1 : bedRoom[target];
        long d = unaryOf(i, target) - unaryOf(i, a);
        if (roomA != roomT) {
            d += roomPairSum(i, roomT, -1, -1) - roomPairSum(i, roomA, -1, -1);
        }
        return d;
    }

    /** Führt den Umzug/Tausch von Person i in Bett "target" wirklich aus. */
    private void apply(int i, int target) {
        int a = bedOf[i];
        if (target >= 0 && occupant[target] >= 0) {
            // Tausch: k bekommt das alte Bett von i (oder kein Bett, falls i keins hatte)
            int k = occupant[target];
            bedOf[k] = a;
            if (a >= 0) occupant[a] = k;
            bedOf[i] = target;
            occupant[target] = i;
        } else {
            // Umzug in freies Bett bzw. Bett abgeben
            if (a >= 0) occupant[a] = -1;
            bedOf[i] = target;
            if (target >= 0) occupant[target] = i;
        }
    }

    /** Setzt den Zustand auf eine gespeicherte Verteilung zurück. */
    private void restore(int[] savedBedOf) {
        Arrays.fill(occupant, -1);
        for (int i = 0; i < numPersons; i++) {
            bedOf[i] = savedBedOf[i];
            if (bedOf[i] >= 0) occupant[bedOf[i]] = i;
        }
    }
}
