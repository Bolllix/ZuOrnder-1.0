package de.zuordner.engine;

import de.zuordner.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssignmentService {

    private final RuleEngine ruleEngine;

    public AssignmentService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public AssignmentResult calculateAssignment(List<Person> persons, List<Building> buildings, List<DynamicRule> rules) {
        long startTime = System.currentTimeMillis();

        if (persons == null) persons = new ArrayList<>();
        if (buildings == null) buildings = new ArrayList<>();
        if (rules == null) rules = new ArrayList<>();

        List<Bed> allBeds = new ArrayList<>();
        Map<String, Room> bedToRoomMap = new HashMap<>();

        for (Building building : buildings) {
            for (Room room : building.getRooms()) {
                for (Bed bed : room.getBeds()) {
                    allBeds.add(bed);
                    bedToRoomMap.put(bed.getId(), room);
                }
            }
        }

        int numPeople = persons.size();
        int numBeds = allBeds.size();

        if (numPeople == 0 || numBeds == 0) {
            AssignmentResult emptyResult = new AssignmentResult();
            for (Person p : persons) emptyResult.getUnassignedPersonIds().add(p.getId());
            for (Bed b : allBeds) emptyResult.getUnassignedBedIds().add(b.getId());
            emptyResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return emptyResult;
        }

        // Check if an active Hard Gender Segregation Rule exists
        boolean hasHardGenderRule = rules.stream().anyMatch(r ->
                r.isActive() && r.getRuleType() == RuleType.HARD &&
                        (r.getName().toLowerCase().contains("geschlecht") ||
                                r.getDescription().toLowerCase().contains("geschlecht") ||
                                (r.getTargetScope() == TargetScope.ROOM_PERSON && r.getAction() == RuleAction.FORBID))
        );

        // Build scoring and explanation matrices
        int[][] scoreMatrix = new int[numPeople][numBeds];
        RuleEngine.EvaluationResult[][] evalMatrix = new RuleEngine.EvaluationResult[numPeople][numBeds];

        for (int i = 0; i < numPeople; i++) {
            Person person = persons.get(i);
            for (int j = 0; j < numBeds; j++) {
                Bed bed = allBeds.get(j);
                Room room = bedToRoomMap.get(bed.getId());

                RuleEngine.EvaluationResult eval = ruleEngine.evaluatePersonBedPair(person, bed, room, rules);
                scoreMatrix[i][j] = eval.getScore();
                evalMatrix[i][j] = eval;
            }
        }

        // Solve via Hungarian Algorithm
        int[] assignments = HungarianSolver.solveMaxWeightMatching(scoreMatrix);

        // Assemble Result
        AssignmentResult result = new AssignmentResult();
        Set<String> assignedBedIds = new HashSet<>();
        Set<String> assignedPersonIds = new HashSet<>();
        int totalScore = 0;
        int hardViolations = 0;

        for (int i = 0; i < numPeople; i++) {
            Person person = persons.get(i);
            int bedIndex = assignments[i];

            if (bedIndex >= 0 && bedIndex < numBeds) {
                Bed bed = allBeds.get(bedIndex);
                Room room = bedToRoomMap.get(bed.getId());
                RuleEngine.EvaluationResult eval = evalMatrix[i][bedIndex];

                if (eval.isForbidden()) {
                    hardViolations++;
                    result.getUnassignedPersonIds().add(person.getId());
                    continue;
                }

                int score = eval.getScore();
                totalScore += score;

                // Evaluate co-location bonuses for relational rules (couples, groups) & gender segregation
                List<ScoreExplanation> explanations = new ArrayList<>(eval.getExplanations());
                for (int k = 0; k < numPeople; k++) {
                    if (k == i) continue;
                    int kBedIndex = assignments[k];
                    if (kBedIndex >= 0 && kBedIndex < numBeds) {
                        Person otherPerson = persons.get(k);
                        Bed otherBed = allBeds.get(kBedIndex);
                        Room otherRoom = bedToRoomMap.get(otherBed.getId());

                        // Check if in same room
                        if (room != null && otherRoom != null && room.getId().equals(otherRoom.getId())) {
                            boolean differentGender = !person.getGender().equalsIgnoreCase(otherPerson.getGender());
                            boolean isCouple = (person.getPartnerId() != null && !person.getPartnerId().trim().isEmpty() &&
                                    person.getPartnerId().equalsIgnoreCase(otherPerson.getPartnerId()));

                            // REQUIREMENT 3: Hard Gender Segregation with Couple Exception
                            if (hasHardGenderRule && differentGender && !isCouple) {
                                hardViolations++;
                                score += RuleEngine.FORBIDDEN_SCORE;
                                totalScore += RuleEngine.FORBIDDEN_SCORE;
                                explanations.add(new ScoreExplanation(
                                        "hard-gender-segregation",
                                        "Harte Geschlechtertrennung verletzt",
                                        RuleEngine.FORBIDDEN_SCORE,
                                        RuleType.HARD,
                                        "Gemischtgeschlechtliche Unterbringung im selben Zimmer ohne Paar-ID verboten (" + otherPerson.getFullName() + ")"
                                ));
                            }
                        }

                        for (DynamicRule rule : rules) {
                            if (!rule.isActive()) continue;
                            int bonus = ruleEngine.evaluateCoLocationBonus(person, room, otherPerson, otherRoom, rule);
                            if (bonus > 0) {
                                score += bonus;
                                totalScore += bonus;
                                explanations.add(new ScoreExplanation(
                                        rule.getId(),
                                        rule.getName(),
                                        bonus,
                                        RuleType.SOFT,
                                        "Zusammenunterbringung erfüllt mit " + otherPerson.getFullName() + " (+ " + bonus + " Pkt)"
                                ));
                            }
                        }
                    }
                }

                AssignmentPair pair = new AssignmentPair(
                        person.getId(),
                        bed.getId(),
                        person.getFullName(),
                        bed.getName(),
                        room != null ? room.getName() : bed.getRoomName(),
                        room != null ? room.getBuildingName() : bed.getBuildingName(),
                        score,
                        explanations
                );

                result.getAssignments().add(pair);
                assignedPersonIds.add(person.getId());
                assignedBedIds.add(bed.getId());
            } else {
                result.getUnassignedPersonIds().add(person.getId());
            }
        }

        // Unassigned beds
        for (Bed bed : allBeds) {
            if (!assignedBedIds.contains(bed.getId())) {
                result.getUnassignedBedIds().add(bed.getId());
            }
        }

        result.setTotalScore(totalScore);
        result.setHardRuleViolationsCount(hardViolations);
        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);

        return result;
    }
}
