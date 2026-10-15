package de.zuordner.repository;

import de.zuordner.model.Building;
import de.zuordner.model.Room;

import java.util.List;

/**
 * ============================================================================
 * BuiltInBuildings – mitgelieferte Beispiel-Gebäude
 * ============================================================================
 * Diese Gebäude sind fest im Programm eingebaut. Sie erscheinen im Fenster
 * "Gebäude aus Vorlage / anderem Projekt" als "Mitgelieferte Vorlage" und können
 * in jedes Projekt übernommen werden (dabei entsteht eine Kopie mit neuen IDs).
 * Weil sie im Programm selbst stecken, kommen neue Vorlagen automatisch mit
 * jedem Update – auch bei Installationen, die schon Projekte haben.
 *
 * Gebäude A und B werden außerdem im Beispielprojekt verwendet
 * (siehe ProjectRepository.createSampleProject).
 *
 * Bausteine (siehe Room.java):
 *   addSingleBed  -> 1 Schlafplatz (Einzelbett)
 *   addDoubleBed  -> 2 Schlafplätze (Doppelbett, Seite A und B)
 *   addBunkBed    -> 2 Schlafplätze (Hochbett unten + oben)
 */
public final class BuiltInBuildings {

    /** Feste IDs, damit die Vorlagen bei jedem Start gleich heißen */
    public static final String NEUMUEHLE_ID = "builtin-seminarhaus-neumuehle";
    public static final String EXAMPLE_A_ID = "builtin-beispiel-gebaeude-a";
    public static final String EXAMPLE_B_ID = "builtin-beispiel-gebaeude-b";

    private BuiltInBuildings() {
        // Nur statische Methoden – keine Objekte nötig
    }

    /** Alle mitgelieferten Vorlagen (jedes Mal frisch erzeugt). */
    public static List<Building> all() {
        return List.of(seminarhausNeumuehle(), exampleBuildingA(), exampleBuildingB());
    }

    /**
     * Seminarhaus Neumühle (Hünstetten-Beuerbach) – nachgebaut nach den Angaben
     * 5x 2-Bett, 1x 3-Bett, 2x 5-Bett, 1x 6-Bett, 2x 8-Bett = 11 Zimmer, 45 Betten.
     * Die meisten Zimmer haben Hochbetten.
     *
     * ANNAHMEN (nicht von der echten Webseite): Verteilung auf Erdgeschoss,
     * 1. Obergeschoss und Dachgeschoss sowie Einzel- vs. Hochbetten. Die
     * 2-Bett-Zimmer haben Einzelbetten (gut für Senioren), die großen Zimmer Hochbetten.
     * Nach dem Übernehmen ins Projekt kann man alles im Reiter "Gebäude & Betten" anpassen.
     */
    public static Building seminarhausNeumuehle() {
        Building b = new Building("Seminarhaus Neumühle");
        b.setId(NEUMUEHLE_ID);

        // --- Erdgeschoss (Etage 0): 2x 2-Bett, 1x 3-Bett -----------------------
        b.getRooms().add(twoBedRoom("Zimmer 1 (2-Bett)", 0, b));
        b.getRooms().add(twoBedRoom("Zimmer 2 (2-Bett)", 0, b));

        Room z3 = new Room("Zimmer 3 (3-Bett)", 0, false, b.getName());
        z3.addBunkBed("Hochbett 1");
        z3.addSingleBed("Einzelbett 1");
        b.getRooms().add(z3);

        // --- 1. Obergeschoss (Etage 1): 3x 2-Bett, 1x 6-Bett --------------------
        b.getRooms().add(twoBedRoom("Zimmer 4 (2-Bett)", 1, b));
        b.getRooms().add(twoBedRoom("Zimmer 5 (2-Bett)", 1, b));
        b.getRooms().add(twoBedRoom("Zimmer 6 (2-Bett)", 1, b));
        b.getRooms().add(bunkRoom("Zimmer 7 (6-Bett)", 1, 3, b));

        // --- Dachgeschoss (Etage 2): 2x 5-Bett, 2x 8-Bett -----------------------
        for (String name : List.of("Zimmer 8 (5-Bett)", "Zimmer 9 (5-Bett)")) {
            Room room = bunkRoom(name, 2, 2, b);   // 2 Hochbetten = 4 Plätze
            room.addSingleBed("Einzelbett 1");      // + 1 Einzelbett = 5 Plätze
            b.getRooms().add(room);
        }
        b.getRooms().add(bunkRoom("Zimmer 10 (8-Bett)", 2, 4, b));
        b.getRooms().add(bunkRoom("Zimmer 11 (8-Bett)", 2, 4, b));

        return b;
    }

    /**
     * Beispiel-Gebäude A: zwei Doppelzimmer (für Paare) und zwei Zweibettzimmer
     * im Erdgeschoss (für Senioren) – 8 Betten.
     */
    public static Building exampleBuildingA() {
        Building b = new Building("Gebäude A (Paare & Senioren)");
        b.setId(EXAMPLE_A_ID);

        Room r1 = new Room("Doppelzimmer 101", 1, false, b.getName());
        r1.addDoubleBed("Doppelbett 101");

        Room r2 = new Room("Doppelzimmer 102", 1, false, b.getName());
        r2.addDoubleBed("Doppelbett 102");

        Room r3 = new Room("Zweibettzimmer EG-01", 0, false, b.getName());
        r3.addSingleBed("Einzelbett EG-01 A");
        r3.addSingleBed("Einzelbett EG-01 B");

        Room r4 = new Room("Zweibettzimmer EG-02", 0, false, b.getName());
        r4.addSingleBed("Einzelbett EG-02 A");
        r4.addSingleBed("Einzelbett EG-02 B");

        b.getRooms().add(r1);
        b.getRooms().add(r2);
        b.getRooms().add(r3);
        b.getRooms().add(r4);
        return b;
    }

    /** Beispiel-Gebäude B: zwei Jugendzimmer mit je 4 Hochbetten – 16 Betten. */
    public static Building exampleBuildingB() {
        Building b = new Building("Gebäude B (Jugendtrakt)");
        b.setId(EXAMPLE_B_ID);

        Room r5 = new Room("Zimmer 201", 1, false, b.getName());
        r5.addBunkBed("Hochbett 1");
        r5.addBunkBed("Hochbett 2");
        r5.addBunkBed("Hochbett 3");
        r5.addBunkBed("Hochbett 4");

        Room r6 = new Room("Zimmer 202", 1, false, b.getName());
        r6.addBunkBed("Hochbett 5");
        r6.addBunkBed("Hochbett 6");
        r6.addBunkBed("Hochbett 7");
        r6.addBunkBed("Hochbett 8");

        b.getRooms().add(r5);
        b.getRooms().add(r6);
        return b;
    }

    // ------------------------------------------------------------------------
    // Kleine Bauhelfer
    // ------------------------------------------------------------------------

    /** Zimmer mit zwei Einzelbetten. */
    private static Room twoBedRoom(String name, int floor, Building building) {
        Room room = new Room(name, floor, false, building.getName());
        room.addSingleBed("Einzelbett 1");
        room.addSingleBed("Einzelbett 2");
        return room;
    }

    /** Zimmer mit {@code bunks} Hochbetten (= 2 x bunks Schlafplätze). */
    private static Room bunkRoom(String name, int floor, int bunks, Building building) {
        Room room = new Room(name, floor, false, building.getName());
        for (int i = 1; i <= bunks; i++) {
            room.addBunkBed("Hochbett " + i);
        }
        return room;
    }
}
