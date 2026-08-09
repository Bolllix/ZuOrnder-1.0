package de.zuordner;

import de.zuordner.engine.RuleEngine;
import de.zuordner.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    @Test
    void testHardRuleForbidSeniorsOnTopBunk() {
        Person senior = new Person("Müller", "Hans", "maennlich", 70, null);
        Bed topBunk = new Bed("Bett 1 (Oben)", BedType.TOP_BUNK, "Zimmer 1", 0, "Gebäude A");
        Room room = new Room("Zimmer 1", 0, false, "Gebäude A");

        DynamicRule rule = new DynamicRule(
                "Senioren nicht nach oben",
                "Verbot für Alter > 65 auf oberen Hochbetten",
                RuleType.HARD,
                TargetScope.BED_PERSON,
                RuleAction.FORBID,
                -999999
        );
        rule.addCondition(new Condition("person.age", Operator.GREATER_THAN, 65));
        rule.addCondition(new Condition("bed.isTopBunk", Operator.EQUALS, true));

        RuleEngine.EvaluationResult result = ruleEngine.evaluatePersonBedPair(senior, topBunk, room, List.of(rule));

        assertTrue(result.isForbidden());
        assertEquals(RuleEngine.FORBIDDEN_SCORE, result.getScore());
        assertEquals(1, result.getExplanations().size());
    }

    @Test
    void testSoftRuleGirlsRoomBonus() {
        Person girl = new Person("Schmidt", "Anna", "weiblich", 22, null);
        Bed bed = new Bed("Bett 1", BedType.SINGLE, "Mädchenzimmer", 0, "Gebäude A");
        Room room = new Room("Mädchenzimmer", 0, true, "Gebäude A");

        DynamicRule rule = new DynamicRule(
                "Mädchenzimmer bevorzugen",
                "Bonus für weibliche Personen",
                RuleType.SOFT,
                TargetScope.ROOM_PERSON,
                RuleAction.ADD_POINTS,
                25
        );
        rule.addCondition(new Condition("person.gender", Operator.EQUALS, "weiblich"));
        rule.addCondition(new Condition("room.girlsRoom", Operator.EQUALS, true));

        RuleEngine.EvaluationResult result = ruleEngine.evaluatePersonBedPair(girl, bed, room, List.of(rule));

        assertFalse(result.isForbidden());
        assertEquals(25, result.getScore());
        assertEquals(1, result.getExplanations().size());
    }
}
