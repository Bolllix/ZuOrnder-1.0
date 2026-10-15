package de.zuordner.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft den Versionsvergleich, auf dem das automatische Update beruht.
 */
class SystemControllerTest {

    @Test
    void neuereVersionWirdErkannt() {
        assertTrue(SystemController.isNewerVersion("2.0.0", "1.1.0"));
        assertTrue(SystemController.isNewerVersion("2.0.1", "2.0.0"));
        assertTrue(SystemController.isNewerVersion("2.1", "2.0.9"));
    }

    @Test
    void gleicheOderAeltereVersionIstKeinUpdate() {
        assertFalse(SystemController.isNewerVersion("2.0.0", "2.0.0"));
        assertFalse(SystemController.isNewerVersion("1.1.0", "2.0.0"), "Version 2.0 darf nicht auf 1.1 'aktualisiert' werden");
        assertFalse(SystemController.isNewerVersion("2.0", "2.0.0"));
        assertFalse(SystemController.isNewerVersion("", "2.0.0"));
    }

    @Test
    void versionKommtAusDerKonfiguration() {
        assertEquals("2.0.0", new SystemController("2.0.0").getCurrentVersion());
        // Platzhalter nicht ersetzt (Start ohne Maven) -> Entwicklungsversion
        assertEquals("0.0.0-dev", new SystemController("@project.version@").getCurrentVersion());
    }
}
