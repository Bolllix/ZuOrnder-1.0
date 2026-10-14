# ZuORDNER – Oberfläche (Frontend)

Die Oberfläche ist mit **React** und **TypeScript** gebaut. **Vite** ist das Werkzeug, das sie während der Entwicklung startet und für die fertige App zusammenpackt.

## Starten (Entwicklung)

```powershell
npm install    # nur beim ersten Mal: Bibliotheken herunterladen
npm run dev    # startet die Oberfläche auf http://localhost:5173
```

Das Java-Backend muss dabei parallel laufen (Port 8080). Alle Aufrufe an `/api/...` leitet Vite automatisch dorthin weiter (siehe `vite.config.ts`).

## Weitere Befehle

| Befehl | Was passiert |
| :--- | :--- |
| `npm run build` | Typen prüfen und fertige Dateien nach `dist/` bauen |
| `npm run lint` | Code mit oxlint auf typische Fehler prüfen |

Für die Desktop-App kopiert `desktop-app/build_windows_app.bat` den Inhalt von `dist/` nach `src/main/resources/static/`. Dort liefert der Java-Server die Seite aus.

## Wo steht was?

| Datei / Ordner | Inhalt |
| :--- | :--- |
| `src/main.tsx` | Einstiegspunkt: hängt die App in die Seite ein |
| `src/App.tsx` | Hauptkomponente: gewähltes Projekt, aktiver Reiter |
| `src/types/index.ts` | Datentypen (gleich aufgebaut wie die Java-Klassen im Ordner `model`) |
| `src/services/api.ts` | Alle Aufrufe an den Server an einer Stelle |
| `src/components/BuildingManager.tsx` | Reiter "Gebäude & Betten" |
| `src/components/ParticipantManager.tsx` | Reiter "Teilnehmer" |
| `src/components/ImportWizard.tsx` | Reiter "Excel/CSV Import" |
| `src/components/RuleEditor.tsx` | Reiter "Regel-Editor" (Standard-Schalter + eigene Regeln) |
| `src/components/AssignmentDashboard.tsx` | Reiter "Belegungs-Optimierung" (Ergebnis + Begründungen) |
| `src/components/Navbar.tsx` | Obere Leiste |
| `src/components/ProjectSelectorModal.tsx` | Fenster zum Wechseln/Anlegen von Projekten |
| `src/components/UpdateModal.tsx` | Fenster "Updates & Version" |

## So fließen die Daten

1. Eine Komponente ruft eine Funktion aus `api.ts` auf, z. B. `api.setStandardRuleActive(...)`.
2. Der Java-Server ändert das Projekt, speichert es und schickt das **komplette** Projekt zurück.
3. Die Komponente gibt es mit `onProjectUpdate(...)` an `App.tsx` weiter. React zeichnet dann alle Reiter mit dem neuen Stand neu.
