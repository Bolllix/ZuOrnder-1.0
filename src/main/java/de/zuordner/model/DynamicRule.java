package de.zuordner.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DynamicRule {
    private String id;
    private String name;
    private String description;
    private boolean active;
    private RuleType ruleType;
    private TargetScope targetScope;
    private List<Condition> conditions;
    private RuleAction action;
    private int weight; // Score adjustment value (positive for reward, negative for penalty)

    public DynamicRule() {
        this.id = UUID.randomUUID().toString();
        this.active = true;
        this.ruleType = RuleType.SOFT;
        this.targetScope = TargetScope.BED_PERSON;
        this.conditions = new ArrayList<>();
        this.action = RuleAction.ADD_POINTS;
        this.weight = 10;
    }

    public DynamicRule(String name, String description, RuleType ruleType, TargetScope targetScope, RuleAction action, int weight) {
        this();
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.targetScope = targetScope;
        this.action = action;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public TargetScope getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(TargetScope targetScope) {
        this.targetScope = targetScope;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public RuleAction getAction() {
        return action;
    }

    public void setAction(RuleAction action) {
        this.action = action;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void addCondition(Condition condition) {
        this.conditions.add(condition);
    }
}
