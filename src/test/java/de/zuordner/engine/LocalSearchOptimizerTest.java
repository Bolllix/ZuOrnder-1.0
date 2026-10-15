package de.zuordner.engine;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft die Rechen-Abkürzung im {@link LocalSearchOptimizer}:
 * delta() berechnet die Punkte-Änderung eines Umzugs, ohne alles neu zu
 * berechnen. Hier wird für tausende zufällige Umzüge kontrolliert, dass das
 * Ergebnis exakt mit einer kompletten Neuberechnung übereinstimmt.
 *
 * (Dieser Test liegt im selben Paket "de.zuordner.engine" wie der Optimierer,
 * damit er die nicht-öffentlichen Methoden delta() und apply() benutzen darf.)
 */
class LocalSearchOptimizerTest {

    @Test
    void deltaStimmtImmerMitDerNeuberechnungUeberein() {
        Random random = new Random(7);
        int persons = 12;
        int beds = 10;          // weniger Betten als Personen -> es gibt auch "kein Bett"
        int rooms = 4;

        // Zufällige Punkte-Tabellen
        long[][] unary = new long[persons][beds];
        for (int i = 0; i < persons; i++) {
            for (int b = 0; b < beds; b++) {
                unary[i][b] = random.nextInt(5) == 0 ? -1_000_000 : random.nextInt(101) - 50;
            }
        }
        long[][] pair = new long[persons][persons];
        for (int p = 0; p < persons; p++) {
            for (int q = p + 1; q < persons; q++) {
                long value = random.nextInt(6) == 0 ? -1_000_000 : random.nextInt(201) - 100;
                pair[p][q] = value;
                pair[q][p] = value;
            }
        }
        int[] bedRoom = new int[beds];
        for (int b = 0; b < beds; b++) bedRoom[b] = b % rooms;

        int[] start = new int[persons];
        for (int i = 0; i < persons; i++) start[i] = i < beds ? i : -1;

        LocalSearchOptimizer optimizer = new LocalSearchOptimizer(unary, pair, bedRoom, rooms, start, 100_000);

        for (int step = 0; step < 5_000; step++) {
            int i = random.nextInt(persons);
            int target = random.nextInt(beds + 1) - 1; // -1 = Person ohne Bett
            long before = optimizer.totalScore();
            long delta = optimizer.delta(i, target);
            optimizer.apply(i, target);
            long after = optimizer.totalScore();
            assertEquals(before + delta, after, "delta() hat sich bei Schritt " + step + " verrechnet");
        }
    }

    @Test
    void optimierungFindetDieOffensichtlichBesteLoesung() {
        // 2 Räume mit je 2 Betten. Person 0+1 und 2+3 wollen jeweils zusammen sein.
        long[][] unary = new long[4][4];
        long[][] pair = new long[4][4];
        pair[0][1] = pair[1][0] = 100;
        pair[2][3] = pair[3][2] = 100;
        int[] bedRoom = {0, 0, 1, 1};

        // Schlechter Start: 0+2 und 1+3 liegen zusammen
        int[] start = {0, 2, 1, 3};
        LocalSearchOptimizer optimizer = new LocalSearchOptimizer(unary, pair, bedRoom, 2, start, 100_000);
        int[] result = optimizer.optimize(1, 50);

        assertEquals(bedRoom[result[0]], bedRoom[result[1]]);
        assertEquals(bedRoom[result[2]], bedRoom[result[3]]);
        assertEquals(200, optimizer.totalScore());
    }
}
