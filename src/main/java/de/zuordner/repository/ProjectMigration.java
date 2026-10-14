package de.zuordner.repository;

import de.zuordner.model.*;

import java.util.*;

/**
 * ============================================================================
 * ProjectMigration – bringt alte Projektdateien (Version 1.x) auf den Stand 2.0
 * ============================================================================
 * Wird beim Programmstart für jedes geladene Projekt aufgerufen. Gibt es etwas
 * zu reparieren, legt das {@link ProjectRepository} vorher eine Sicherheitskopie
 * der alten Datei an ("<id>.json.v1-backup") und speichert danach die neue Fassung.
 *
 * Was wird repariert?
 *  1. Regeln ohne ID (leere ID "") oder mit doppelter ID bekommen eine neue,
 *     eindeutige ID. Exakte Kopien, die durch den alten ID-Fehler entstanden
 *     sind, werden entfernt.
 *  2. Die 4 Standard-Regeln werden erkannt und bekommen ihren festen Schlüssel
 *     ({@link StandardRule}). Die alte Geschlechtertrennung (Raum-Regel ohne
 *     Bedingungen) wird in den neuen Regel-Typ GENDER_SEPARATION umgewandelt.
 *  3. Alte "Zusammen ins selbe Zimmer"-Regeln mit WENN-Bedingung (z.B. "Alter > 65")
 *     waren fälschlich als Paar-Regel gespeichert. Sie werden zu
 *     CONDITION_CO_LOCATION – das entspricht dem, was im Regel-Editor angezeigt wurde.
 *  4. Gab es nur die alten Mädchenzimmer-/Jungenzimmer-Regeln, wird zusätzlich
 *     die neue Standard-Geschlechtertrennung angelegt (gleicher An/Aus-Zustand).
 */
public final class ProjectMigration {

    private ProjectMigration() {
        // Nur statische Hilfsmethoden – keine Objekte nötig
    }

    /**
     * Repariert das Projekt (falls nötig) direkt im übergebenen Objekt.
     *
     * @return true, wenn etwas verändert wurde (dann sollte gespeichert werden)
     */
    public static boolean migrate(Project project) {
        boolean changed = false;

        if (project.getRules() == null) {
            project.setRules(new ArrayList<>());
            changed = true;
        }
        if (project.getPersons() == null) {
            project.setPersons(new ArrayList<>());
            changed = true;
        }
        if (project.getBuildings() == null) {
            project.setBuildings(new ArrayList<>());
            changed = true;
        }

        // --- 1. IDs reparieren und Fehler-Duplikate entfernen ------------------
        Set<String> seenIds = new HashSet<>();
        List<DynamicRule> cleaned = new ArrayList<>();
        for (DynamicRule rule : project.getRules()) {
            if (rule == null) {
                changed = true;
                continue;
            }
            if (rule.getConditions() == null) {
                rule.setConditions(new ArrayList<>());
                changed = true;
            }

            boolean hadBlankId = rule.getId() == null || rule.getId().isBlank();
            if (hadBlankId && containsSameContent(cleaned, rule)) {
                // Exakte Kopie, entstanden durch den alten Fehler mit der leeren ID
                changed = true;
                continue;
            }
            if (hadBlankId || seenIds.contains(rule.getId())) {
                rule.setId(UUID.randomUUID().toString());
                changed = true;
            }
            seenIds.add(rule.getId());
            cleaned.add(rule);
        }
        project.setRules(cleaned);

        // --- 2./3. Standard-Regeln erkennen, alte Regel-Typen umwandeln --------
        Set<StandardRule> present = EnumSet.noneOf(StandardRule.class);
        for (DynamicRule rule : cleaned) {
            if (rule.getStandardKey() != null) present.add(rule.getStandardKey());
        }

        for (DynamicRule rule : cleaned) {
            if (rule.getStandardKey() != null || rule.getTargetScope() == null) continue;

            StandardRule detected = detectLegacyStandardRule(rule);
            if (detected != null && !present.contains(detected)) {
                rule.setStandardKey(detected);
                present.add(detected);
                if (detected == StandardRule.GENDER_SEPARATION) {
                    rule.setTargetScope(TargetScope.GENDER_SEPARATION);
                }
                changed = true;
            } else if (rule.getTargetScope() == TargetScope.PAIR_CO_LOCATION && hasConditions(rule)) {
                rule.setTargetScope(TargetScope.CONDITION_CO_LOCATION);
                changed = true;
            }
        }

        // --- 4. Nur alte Mädchen-/Jungenzimmer-Regeln vorhanden? ---------------
        if (!present.contains(StandardRule.GENDER_SEPARATION)) {
            boolean legacyExists = false;
            boolean legacyActive = false;
            for (DynamicRule rule : cleaned) {
                if (rule.getRuleType() == RuleType.HARD && mentionsGenderSeparation(rule)) {
                    legacyExists = true;
                    legacyActive |= rule.isActive();
                }
            }
            if (legacyExists) {
                DynamicRule genderRule = StandardRule.GENDER_SEPARATION.createDefaultRule();
                genderRule.setActive(legacyActive);
                cleaned.add(0, genderRule);
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Erkennt anhand von Typ und Aufbau einer alten Regel, welche Standard-Regel
     * sie ist (oder null, wenn es eine eigene Regel ist).
     */
    static StandardRule detectLegacyStandardRule(DynamicRule rule) {
        TargetScope scope = rule.getTargetScope();
        boolean noConditions = !hasConditions(rule);

        if (scope == TargetScope.GENDER_SEPARATION) {
            return StandardRule.GENDER_SEPARATION;
        }
        // In Version 1.x war die Geschlechtertrennung eine harte Raum-Regel OHNE Bedingungen
        if (scope == TargetScope.ROOM_PERSON && rule.getRuleType() == RuleType.HARD && noConditions) {
            return StandardRule.GENDER_SEPARATION;
        }
        if (scope == TargetScope.PAIR_CO_LOCATION && noConditions) {
            return StandardRule.COUPLES_TOGETHER;
        }
        if (scope == TargetScope.GROUP_CO_LOCATION && noConditions) {
            return StandardRule.GROUPS_TOGETHER;
        }
        if (scope == TargetScope.BED_PERSON && rule.getRuleType() == RuleType.HARD
                && rule.getName() != null && rule.getName().toLowerCase().contains("senior")) {
            return StandardRule.SENIORS_NO_TOP_BUNK;
        }
        return null;
    }

    private static boolean hasConditions(DynamicRule rule) {
        return rule.getConditions() != null && !rule.getConditions().isEmpty();
    }

    private static boolean mentionsGenderSeparation(DynamicRule rule) {
        String text = ((rule.getName() == null ? "" : rule.getName()) + " "
                + (rule.getDescription() == null ? "" : rule.getDescription())).toLowerCase();
        return text.contains("geschlechtertrennung");
    }

    /** Gibt es in der Liste schon eine Regel mit exakt gleichem Inhalt (ID und An/Aus ausgenommen)? */
    private static boolean containsSameContent(List<DynamicRule> rules, DynamicRule candidate) {
        for (DynamicRule r : rules) {
            if (Objects.equals(r.getName(), candidate.getName())
                    && Objects.equals(r.getDescription(), candidate.getDescription())
                    && r.getRuleType() == candidate.getRuleType()
                    && r.getTargetScope() == candidate.getTargetScope()
                    && r.getAction() == candidate.getAction()
                    && r.getWeight() == candidate.getWeight()
                    && sameConditions(r.getConditions(), candidate.getConditions())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameConditions(List<Condition> a, List<Condition> b) {
        List<Condition> x = a == null ? List.of() : a;
        List<Condition> y = b == null ? List.of() : b;
        if (x.size() != y.size()) return false;
        for (int i = 0; i < x.size(); i++) {
            Condition c1 = x.get(i);
            Condition c2 = y.get(i);
            if (c1 == null || c2 == null) {
                if (c1 != c2) return false;
                continue;
            }
            if (!Objects.equals(c1.getField(), c2.getField())
                    || c1.getOperator() != c2.getOperator()
                    || !Objects.equals(String.valueOf(c1.getValue()), String.valueOf(c2.getValue()))) {
                return false;
            }
        }
        return true;
    }
}
