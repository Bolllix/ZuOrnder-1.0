package de.zuordner.importengine;

import de.zuordner.engine.RuleEngine;
import de.zuordner.model.Person;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass Geschlechtsangaben aus Excel-Listen richtig erkannt werden –
 * im Import (TableMappingService) und in der Regel-Auswertung (RuleEngine).
 * In Version 1.x wurde "Mädchen" (beginnt mit "M") als männlich erkannt.
 */
class GenderNormalizationTest {

    @Test
    void importErkenntAlleSchreibweisen() {
        assertEquals("weiblich", TableMappingService.normalizeGender("Mädchen"));
        assertEquals("weiblich", TableMappingService.normalizeGender("Maedchen"));
        assertEquals("weiblich", TableMappingService.normalizeGender("Weiblich"));
        assertEquals("weiblich", TableMappingService.normalizeGender("w"));
        assertEquals("weiblich", TableMappingService.normalizeGender("female"));
        assertEquals("weiblich", TableMappingService.normalizeGender("Frau"));

        assertEquals("maennlich", TableMappingService.normalizeGender("Männlich"));
        assertEquals("maennlich", TableMappingService.normalizeGender("m"));
        assertEquals("maennlich", TableMappingService.normalizeGender("Junge"));
        assertEquals("maennlich", TableMappingService.normalizeGender("male"));

        assertEquals("divers", TableMappingService.normalizeGender("divers"));
        assertEquals("divers", TableMappingService.normalizeGender("d"));
    }

    @Test
    void regelAuswertungErkenntAlleSchreibweisen() {
        assertEquals(RuleEngine.Gender.FEMALE, RuleEngine.genderOf(personWithGender("Mädchen")));
        assertEquals(RuleEngine.Gender.FEMALE, RuleEngine.genderOf(personWithGender("weiblich")));
        assertEquals(RuleEngine.Gender.FEMALE, RuleEngine.genderOf(personWithGender("female")));
        assertEquals(RuleEngine.Gender.MALE, RuleEngine.genderOf(personWithGender("maennlich")));
        assertEquals(RuleEngine.Gender.MALE, RuleEngine.genderOf(personWithGender("Junge")));
        assertEquals(RuleEngine.Gender.OTHER, RuleEngine.genderOf(personWithGender("divers")));
        assertEquals(RuleEngine.Gender.OTHER, RuleEngine.genderOf(personWithGender(null)));
    }

    private static Person personWithGender(String gender) {
        Person person = new Person();
        person.setGender(gender);
        return person;
    }
}
