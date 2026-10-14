# Änderungen (Changelog)

## Version 2.0.0 – 14.10.2026

Überarbeitung der Version 1.1.0. Schwerpunkt: Die Regeln funktionieren unabhängig voneinander und wirken sich wirklich auf die Verteilung aus.

### Behobene Fehler

* **Paar-Regel und Geschlechtertrennung beeinflussten sich gegenseitig.**
  Die Schalter im Regel-Editor haben "ihre" Regel über Wörter im Namen gesucht. Die Geschlechter-Regel hieß z. B. "Harte Geschlechtertrennung (mit Paar-Ausnahme)", deshalb hat der Paar-Schalter in Wahrheit die Geschlechter-Regel umgeschaltet. Umgekehrt enthielt die Beschreibung der Paar-Regel das Wort "Geschlechtertrennung". Jetzt hat jede Standard-Regel einen festen Schlüssel (`standardKey`), und jeder Schalter ändert genau eine Regel.
  Auch im Server hing die Paar-Ausnahme an diesem Wortvergleich und war deshalb immer an, sobald die Geschlechtertrennung aktiv war.
* **Neue Regeln ließen sich nicht mehr umschalten.** Alle neuen Regeln bekamen die leere ID `""`, Änderungen daran lehnte der Server ab. Jetzt vergibt der Server eindeutige IDs.
* **Paare, Gruppen und Geschlechtertrennung hatten keinen Einfluss auf die Verteilung.** Diese Regeln wurden erst *nach* der Berechnung ausgewertet. Im Beispielprojekt gab es dadurch 34 Regelverstöße (gemischte Zimmer). Jetzt verbessert eine zusätzliche lokale Suche die Verteilung unter Berücksichtigung dieser Regeln: Das Beispielprojekt hat 0 Verstöße, alle Paare und Gruppen liegen zusammen.
* "Zusammen ins selbe Zimmer" im Regel-Baukasten erzeugte immer eine Paar-Regel, z. B. bei "Personen über 65". Jetzt entsteht die passende Gruppen- bzw. Eigenschafts-Regel.
* Bettart "Schlafsofa" konnte nicht gespeichert werden (`SOFA_BED` statt `SOFA`).
* Import: "Mädchen" und "female" wurden als männlich erkannt.
* `POST /api/projects/{id}/persons/batch` war doppelt vergeben und lieferte immer Fehler 500.
* Desktop-Version: Das Programm hat die eigene JAR-Datei nie erkannt (Spring Boot 3.2), deshalb fehlte der Knopf "Jetzt automatisch aktualisieren".
* Projektdateien enthielten alle Betten dreifach (berechnete Listen `allBeds` wurden mitgespeichert).
* Excel-Export und Dashboard zeigten negative Punkte als "+-25".

### Neu

* Geschlechtertrennung beachtet als Mädchen-/Jungenzimmer markierte Räume.
* Paar-Regel bevorzugt zusätzlich Doppelbetten (+10 Punkte).
* Hinweis im Regel-Editor, wie Geschlechtertrennung und Paar-Regel gerade zusammenspielen.
* Dashboard und Excel-Export zeigen für jede Person ohne Bett den Grund (z. B. "Alle Betten sind belegt" oder "Alle freien Betten verstoßen gegen harte Regeln (Geschlechtertrennung im Zimmer)").
* Neuer Endpunkt `PUT /api/projects/{id}/rules/standard/{key}` zum Schalten genau einer Standard-Regel.
* Neues, durchgängig lösbares Beispielprojekt.
* Die Versionsnummer steht nur noch in `pom.xml`; die JAR heißt immer `target/zuordner-app.jar`.
* Ausführliche deutsche Kommentare im gesamten Code, 27 neue automatische Tests (insgesamt 34).

### Geändertes Verhalten

* **Alte Projekte werden beim ersten Start automatisch umgewandelt** (Regel-IDs, Standard-Regeln). Vorher wird je Projekt eine Sicherheitskopie `<projekt>.json.v1-backup` im selben Ordner angelegt.
* Harte Regeln werden nie gebrochen. Passt eine Person nirgends hin, bleibt sie ohne Bett und erscheint mit Grund in der Liste.
* Die automatische Geschlechtertrennung trennt nur männlich und weiblich. Personen mit "divers" oder ohne Angabe schränkt sie nicht ein; das regelt die Leitung bei Bedarf selbst.
* Das Beispielprojekt wird nur noch beim allerersten Start angelegt und kommt nach dem Löschen nicht ungefragt zurück.
* Die Berechnung dauert etwas länger als vorher (ca. 0,4 s für 100 und 0,8 s für 200 Personen), findet dafür aber deutlich bessere Verteilungen.

### Hinweis zum Testen von Version 2.0

Solange Version 2.0 nur auf dem eigenen Branch liegt, holen das Update-Skript "ZuORDNER aktualisieren.vbs" und der Update-Knopf die Version aus dem `main`-Branch. Das Skript würde also wieder 1.1.0 installieren. Der Knopf in der App bietet keine ältere Version an.
