package de.zuordner.model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentResult {
    private List<AssignmentPair> assignments;
    private List<String> unassignedPersonIds;
    private List<String> unassignedBedIds;
    private int totalScore;
    private int hardRuleViolationsCount;
    private long executionTimeMs;

    public AssignmentResult() {
        this.assignments = new ArrayList<>();
        this.unassignedPersonIds = new ArrayList<>();
        this.unassignedBedIds = new ArrayList<>();
    }

    public List<AssignmentPair> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<AssignmentPair> assignments) {
        this.assignments = assignments;
    }

    public List<String> getUnassignedPersonIds() {
        return unassignedPersonIds;
    }

    public void setUnassignedPersonIds(List<String> unassignedPersonIds) {
        this.unassignedPersonIds = unassignedPersonIds;
    }

    public List<String> getUnassignedBedIds() {
        return unassignedBedIds;
    }

    public void setUnassignedBedIds(List<String> unassignedBedIds) {
        this.unassignedBedIds = unassignedBedIds;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getHardRuleViolationsCount() {
        return hardRuleViolationsCount;
    }

    public void setHardRuleViolationsCount(int hardRuleViolationsCount) {
        this.hardRuleViolationsCount = hardRuleViolationsCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
