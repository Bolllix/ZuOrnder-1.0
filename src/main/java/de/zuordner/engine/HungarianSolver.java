package de.zuordner.engine;

import java.util.Arrays;

/**
 * ============================================================================
 * Hungarian Solver (Ungarischer Algorithmus / Kuhn-Munkres Algorithmus)
 * ============================================================================
 * Der Ungarische Algorithmus löst das klassische Zuordnungsproblem (Assignment Problem)
 * für gewichtete bipartite Graphen in polynomieller Laufzeit O(N^3).
 * 
 * Mathematischer Ablauf:
 * 1. Eingabe: Eine 2D Matrix S[N][M], in der S[i][j] den Eignungs-Score von Person i
 *    für Bett j angibt.
 * 2. Quadratisierung: Wenn Personenanzahl != Bettenanzahl, wird die Matrix mit Dummyelementen
 *    auf K x K aufgestockt (K = max(N, M)).
 * 3. Maximierung zu Minimierung: Da der Standard-Hungarian ein Min-Cost-Problem löst,
 *    konvertieren wir die Scores: Cost[i][j] = MaxScore - Score[i][j].
 *    (Für unzulässige/verbotene Kombinationen setzen wir Cost[i][j] = FORBIDDEN_COST).
 * 4. Zeilen- & Spalten-Reduktion & Augmentierende Pfade: Findet in O(K^3) Zeit das globale
 *    Optimum, welches die Gesamtsumme aller Punktwerte maximiert.
 */
public class HungarianSolver {

    /** Sehr hohe Kosten für verbotene/unzulässige Zuordnungen im Min-Cost-Problem */
    private static final int FORBIDDEN_COST = 1_000_000;

    /**
     * Ermittelt die optimale 1-zu-1 Zuordnung zur Maximierung des Gesamtscores.
     * 
     * @param scoreMatrix Matrix der Größe [Personen][Betten] mit Punktwerten
     * @return Array der Größe [Personen], wobei result[i] den Index des zugeordneten Betts angibt (-1 wenn unzugeordnet)
     */
    public static int[] solveMaxWeightMatching(int[][] scoreMatrix) {
        if (scoreMatrix == null || scoreMatrix.length == 0 || scoreMatrix[0].length == 0) {
            return new int[0];
        }

        int people = scoreMatrix.length;
        int beds = scoreMatrix[0].length;
        int n = Math.max(people, beds); // Quadratische Dimension K = max(N, M)

        // Maximalen Score finden für die Min-Cost Transformation
        int maxScore = 0;
        for (int i = 0; i < people; i++) {
            for (int j = 0; j < beds; j++) {
                if (scoreMatrix[i][j] > RuleEngine.FORBIDDEN_SCORE / 2) {
                    maxScore = Math.max(maxScore, scoreMatrix[i][j]);
                }
            }
        }

        // Matrix umwandeln: Max Weight -> Min Cost (K x K)
        int[][] costMatrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i < people && j < beds) {
                    int val = scoreMatrix[i][j];
                    if (val <= RuleEngine.FORBIDDEN_SCORE / 2) {
                        costMatrix[i][j] = FORBIDDEN_COST; // Verbotene Kombination
                    } else {
                        costMatrix[i][j] = maxScore - val; // Invertieren für Minimierung
                    }
                } else {
                    costMatrix[i][j] = 0; // Dummy Zeile/Spalte (Kosten 0)
                }
            }
        }

        // Ungarischen Min-Cost-Algorithmus ausführen
        int[] minCostAssignment = solveMinCostMatching(costMatrix, n);
        int[] result = new int[people];
        Arrays.fill(result, -1);

        // Ergebnis auswerten & Dummies filtern
        for (int i = 0; i < people; i++) {
            int bedIndex = minCostAssignment[i];
            if (bedIndex >= 0 && bedIndex < beds) {
                // Prüfen, ob es sich um ein gültiges (nicht verbotenes) Bett handelt
                if (scoreMatrix[i][bedIndex] > RuleEngine.FORBIDDEN_SCORE / 2) {
                    result[i] = bedIndex;
                }
            }
        }

        return result;
    }

    /**
     * Standard-Kuhn-Munkres Implementierung für Min-Cost Bipartite Matching in O(N^3).
     */
    private static int[] solveMinCostMatching(int[][] cost, int n) {
        int[] u = new int[n + 1];
        int[] v = new int[n + 1];
        int[] p = new int[n + 1];
        int[] way = new int[n + 1];

        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            int[] minv = new int[n + 1];
            boolean[] used = new boolean[n + 1];
            Arrays.fill(minv, Integer.MAX_VALUE);

            do {
                used[j0] = true;
                int i0 = p[j0];
                int delta = Integer.MAX_VALUE;
                int j1 = 0;

                for (int j = 1; j <= n; j++) {
                    if (!used[j]) {
                        int cur = cost[i0 - 1][j - 1] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }

                for (int j = 0; j <= n; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;

            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] result = new int[n];
        Arrays.fill(result, -1);
        for (int j = 1; j <= n; j++) {
            if (p[j] > 0) {
                result[p[j] - 1] = j - 1;
            }
        }
        return result;
    }
}
