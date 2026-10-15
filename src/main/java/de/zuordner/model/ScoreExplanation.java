package de.zuordner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Eine Begründung im Ergebnis, z.B. "Paare im selben Zimmer: +50 Punkte,
 * weil Partnerin Erika im selben Zimmer ist". Wird im Dashboard angezeigt,
 * wenn man auf ein belegtes Bett klickt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreExplanation {
    private String ruleId;
    private String ruleName;
    private int points;
    private RuleType ruleType;
    private String reason;

    public ScoreExplanation() {
    }

    public ScoreExplanation(String ruleId, String ruleName, int points, RuleType ruleType, String reason) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.points = points;
        this.ruleType = ruleType;
        this.reason = reason;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
