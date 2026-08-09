package de.zuordner;

import de.zuordner.engine.HungarianSolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HungarianSolverTest {

    @Test
    void testSimpleMaxMatching() {
        int[][] scores = {
                { 10,  2 },
                {  1, 20 }
        };

        int[] assignment = HungarianSolver.solveMaxWeightMatching(scores);

        assertEquals(0, assignment[0]); // Person 0 -> Bed 0 (score 10)
        assertEquals(1, assignment[1]); // Person 1 -> Bed 1 (score 20)
    }

    @Test
    void testNonSquareMatrixMatching() {
        int[][] scores = {
                { 5, 10, 15 },
                { 20, 2,  1 }
        }; // 2 people, 3 beds

        int[] assignment = HungarianSolver.solveMaxWeightMatching(scores);

        assertEquals(2, assignment[0]); // Person 0 -> Bed 2 (score 15)
        assertEquals(0, assignment[1]); // Person 1 -> Bed 0 (score 20)
    }
}
