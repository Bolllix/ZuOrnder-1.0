# ZuORDNER – Webanwendung zur automatisierten Zimmer- & Bettenzuordnung

Eine moderne, datengetriebene Webanwendung zur optimalen Verteilung von Teilnehmern auf verfügbare Räume und Betten in Jugendherbergen und Unterkünften.

---

## 🚀 Highlights & Kernfunktionen

1. **Visuelle Gebäude- & Einzelbettenverwaltung**
   * Verwalten von Gebäuden, Etagen, Räumen (z.B. Mädchenzimmer, Jungenzimmer) und einzelnen Betten (Einzelbett, Hochbett Oben/Unten, Doppelbett, Schlafsofa).
   * Flexibles Hinzufügen oder Löschen einzelner Betten, Räume und kompletter Gebäude.

2. **Dynamische Rule Engine (Regel-Auswertung ohne Codeänderungen)**
   * **Harte Regeln (HARD / FORBID)**: Absolute Verbote (z.B. *Senioren > 65 nicht ins obere Hochbett*, *Harte Geschlechtertrennung in Mädchen-/Jungenzimmern*).
   * **Weiche Regeln (SOFT / PREFERENCES)**: Punkteboni/Abzüge (z.B. *Paare ins selbe Zimmer (+50 Pkt)*, *Gruppen zusammenhalten (+20 Pkt)*).
   * **"Mehr Infos"-Button**: Klare Aufschlüsselung der Funktionsweise und WENN-Bedingungen jeder Regel in der Oberfläche.

3. **6-Schritt Excel & CSV Import-Wizard**
   * Schritt 1: Datei-Upload (`.xlsx`, `.xls`, `.csv`).
   * Schritt 2: Auswahl von Tabellenblatt & Headerzeile.
   * Schritt 3: Tabellen-Vorschau.
   * Schritt 4: **Manuelles Spalten-Mapping** (Zuordnung beliebiger Spalten zu Name, Alter, Geschlecht, Paar-ID, Gruppe oder benutzerdefinierten Attributen).
   * Schritt 5: Datensatz-Validierung & Warnungsvorschau.
   * Schritt 6: Import in die Teilnehmerliste.
   * **Pluggable Architecture**: Sauber entkoppeltes `TableMappingService`-Interface für zukünftige KI-Spaltenerkennungen.

4. **Kuhn-Munkres (Hungarian Algorithm) Optimierung**
   * Polynomielle $O(N^3)$ Optimierung zur Maximierung der globalen Punkte-Gesamtsumme.
   * Automatisches Padding für asymmetrische Matrizen (Personen $\neq$ Betten).
   * Transparente Erklärungs-Engine: Zeigt für jede Zuordnung exakt an, warum sie gewählt wurde und welche Regeln erfüllt wurden.

---

## 🛠️ Technologie-Stack

* **Backend**: Java 25, Spring Boot 3.2.4, Apache POI 5.2.5, OpenCSV 5.9, Jackson JSON, Maven
* **Frontend**: React 18, TypeScript, Vite 8, TailwindCSS, Lucide React Icons
* **Architektur**: REST API, JSON-Dateispeicher (`data/projects/`), Pluggable Services

---

## 📦 Schnellstart-Anleitung

### 1. Backend starten (Java Spring Boot)
Im Hauptverzeichnis des Projekts ausführen:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```
> Das Backend läuft anschließend auf `http://localhost:8080`.

### 2. Frontend starten (React + Vite)
In einem zweiten Terminal im Ordner `frontend` ausführen:

```powershell
cd frontend
npm install
npm run dev
```
> Öffnen Sie im Browser: `http://localhost:5173`.

### 3. Tests ausführen
Automatisierte Unit- & Szenario-Tests ausführen:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd test
```

---

## 📡 REST API Übersicht

| Methode | Endpunkt | Beschreibung |
| :--- | :--- | :--- |
| `GET` | `/api/projects` | Liste aller Projekte abrufen |
| `POST` | `/api/projects` | Neues Projekt anlegen |
| `DELETE` | `/api/projects/{id}` | Projekt löschen |
| `POST` | `/api/projects/{id}/buildings` | Gebäude hinzufügen |
| `DELETE` | `/api/projects/{id}/buildings/{bId}` | Gebäude löschen |
| `POST` | `/api/projects/{id}/buildings/{bId}/rooms` | Raum hinzufügen |
| `DELETE` | `/api/projects/{id}/buildings/{bId}/rooms/{rId}` | Raum löschen |
| `POST` | `/api/projects/{id}/buildings/{bId}/rooms/{rId}/beds` | Einzelnes Bett hinzufügen |
| `DELETE` | `/api/projects/{id}/buildings/{bId}/rooms/{rId}/beds/{bedId}` | Einzelnes Bett löschen |
| `POST` | `/api/projects/{id}/persons` | Teilnehmer hinzufügen/aktualisieren |
| `POST` | `/api/projects/{id}/import/preview` | Tabelle parsen für Import Wizard |
| `POST` | `/api/projects/{id}/import/process` | Spaltenzuordnung verarbeiten & importieren |
| `POST` | `/api/projects/{id}/assignment/calculate` | **Optimierung berechnen & Zuordnungsergebnis abrufen** |

---

## 📄 Ausführliche Handbuch-Dokumentation & PDF

Eine umfassende technische Handbuch-Dokumentation mit detaillierten Erklärungen aller Klassen, Algorithmen und mathematischen Formeln wurde generiert und liegt als **PDF** im Projektstammverzeichnis bereit:

* `ZuORDNER_Technische_Dokumentation.pdf`
