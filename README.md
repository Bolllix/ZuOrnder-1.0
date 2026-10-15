# ZuORDNER – Webanwendung zur automatisierten Zimmer- & Bettenzuordnung

**Version 2.1.0** · Was ist neu? → [CHANGELOG.md](CHANGELOG.md)

Eine datengetriebene Anwendung zur optimalen Verteilung von Teilnehmern auf verfügbare Räume und Betten in Jugendherbergen und Unterkünften. Läuft als Webanwendung oder als Windows-Desktop-Programm (Ordner `desktop-app/`).

---

## 🚀 Highlights & Kernfunktionen

1. **Visuelle Gebäude- & Einzelbettenverwaltung**
   * Gebäude, Etagen, Räume (optional als Mädchen- oder Jungenzimmer markiert) und einzelne Betten (Einzelbett, Hochbett Oben/Unten, Doppelbett, Schlafsofa).
   * Gebäude als Vorlage speichern und in anderen Projekten wiederverwenden.
   * Mitgelieferte Vorlagen: **Seminarhaus Neumühle** (11 Zimmer, 45 Betten) und die zwei Beispielgebäude – im Reiter "Gebäude & Betten" unter "Aus Vorlage / anderem Projekt".

2. **Regelsystem (ohne Codeänderungen)**
   * **4 Praxis-Standards mit eigenem Schalter**, die sich gegenseitig nicht beeinflussen:
     Geschlechtertrennung · Paare im selben Zimmer · Gruppenzusammenhalt · Senioren nicht ins obere Hochbett.
     Sind Geschlechtertrennung und Paar-Regel beide aktiv, dürfen angemeldete Paare trotzdem zusammen schlafen.
   * **Harte Regeln (Verbote)** werden nie gebrochen – lieber bleibt eine Person ohne Bett (mit Begründung).
   * **Weiche Regeln (Wünsche)** geben Plus- oder Minuspunkte.
   * **Eigene Regeln** im Baukasten "WER → WO → BEWERTUNG" (z. B. "Personen über 65 → Erdgeschoss bevorzugen").

3. **Excel- & CSV-Import-Assistent**
   * `.xlsx`, `.xls`, `.csv` (Trennzeichen wird automatisch erkannt), Spalten frei zuordnen, Prüfung mit Warnungen vor dem Import.

4. **Optimierung in zwei Stufen**
   * **Ungarischer Algorithmus** (Kuhn-Munkres, $O(N^3)$) liefert eine Startverteilung.
   * **Lokale Suche** (Simulated Annealing + Hill Climbing) verbessert sie und berücksichtigt dabei auch, *wer mit wem* im Zimmer liegt (Paare, Gruppen, Geschlechtertrennung).
   * Transparente Begründung für jede Zuordnung, Liste aller Personen ohne Bett mit Grund, Excel-Export.

---

## 🛠️ Technologie-Stack

* **Backend**: Java 17 oder neuer, Spring Boot 3.2.4, Apache POI 5.2.5, OpenCSV 5.9, Jackson JSON, Maven
* **Frontend**: React 19, TypeScript, Vite, TailwindCSS, Lucide React Icons
* **Speicherung**: JSON-Dateien (`data/projects/` bzw. unter Windows `%APPDATA%\ZuORDNER\data\`)

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
Automatisierte Unit- & Szenario-Tests (39 Tests) ausführen:

```powershell
.\tools\apache-maven-3.9.6\bin\mvn.cmd test
```

### 4. Windows-Desktop-Paket bauen
```powershell
.\desktop-app\build_windows_app.bat
```
> Baut die Oberfläche, die JAR-Datei `target/zuordner-app.jar` und aktualisiert `desktop-app/ZuORDNER-Desktop` samt ZIP.

### Neue Version veröffentlichen
1. Versionsnummer in `pom.xml` (`<version>`) und `desktop-app/version.json` erhöhen.
2. Desktop-Paket bauen (siehe oben) und alles nach `main` übernehmen – die installierten Desktop-Programme bieten das Update dann automatisch an.

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
| `POST` | `/api/projects/{id}/persons` | Teilnehmer hinzufügen |
| `PUT` | `/api/projects/{id}/persons/{pId}` | Teilnehmer ändern |
| `POST` | `/api/projects/{id}/rules` | Eigene Regel anlegen (ID vergibt der Server) |
| `PUT` | `/api/projects/{id}/rules/{ruleId}` | Regel ändern / an- und ausschalten |
| `PUT` | `/api/projects/{id}/rules/standard/{key}` | Standard-Regel schalten, Body `{"active": true}` (Keys: `GENDER_SEPARATION`, `COUPLES_TOGETHER`, `GROUPS_TOGETHER`, `SENIORS_NO_TOP_BUNK`) |
| `POST` | `/api/projects/{id}/import/preview` | Tabelle parsen für den Import-Assistenten |
| `POST` | `/api/projects/{id}/import/map` | Spaltenzuordnung anwenden & prüfen |
| `POST` | `/api/projects/{id}/import/save` | Geprüfte Teilnehmer übernehmen |
| `POST` | `/api/projects/{id}/assignment/calculate` | **Optimierung berechnen & Zuordnungsergebnis abrufen** |
| `GET` | `/api/projects/{id}/assignment/export/excel` | Belegungsplan als Excel-Datei |
| `GET` | `/api/system/version` | Programmversion & Update-Prüfung |

---

## 📄 Ausführliche Dokumentation

* `DOCUMENTATION.html` bzw. `ZuORDNER_Technische_Dokumentation.pdf` – Architektur, Algorithmen und wichtigste Klassen.
* Der Quellcode ist ausführlich auf Deutsch kommentiert – ein guter Einstieg sind
  `src/main/java/de/zuordner/engine/AssignmentService.java` (Ablauf der Berechnung) und
  `frontend/src/App.tsx` (Aufbau der Oberfläche).
