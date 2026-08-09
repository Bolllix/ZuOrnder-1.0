package de.zuordner.model;

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
