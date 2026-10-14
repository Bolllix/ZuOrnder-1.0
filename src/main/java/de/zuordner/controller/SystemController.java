package de.zuordner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zuordner.ZuordnerApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================================
 * SystemController – Versionsanzeige & automatisches Update (Desktop-Version)
 * ============================================================================
 * Ein "Controller" ist in Spring Boot die Klasse, die Anfragen aus dem Browser
 * entgegennimmt. Jede Methode mit @GetMapping/@PostMapping ist eine Adresse
 * (ein "Endpunkt"), die das Frontend aufrufen kann – hier alle unter /api/system.
 *
 * So funktioniert das Update-System:
 * 1. GET  /api/system/version       -> Liest die eigene Version und lädt
 *                                      desktop-app/version.json von GitHub (main).
 *                                      Ist die Version dort höher, gibt es ein Update.
 * 2. POST /api/system/update        -> Startet im Hintergrund den Download der neuen JAR.
 * 3. GET  /api/system/update-status -> Das Frontend fragt regelmäßig den Fortschritt ab.
 * 4. Nach dem Download schreibt das Programm ein kleines Windows-Skript (.bat),
 *    beendet sich selbst, das Skript ersetzt die alte JAR durch die neue und
 *    startet ZuORDNER neu. (Eine laufende JAR kann sich nicht selbst überschreiben,
 *    deshalb der Umweg über das Skript.)
 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    /** Hier liegt die "offizielle" neueste Versionsnummer (Release-Kanal = main-Branch). */
    private static final String REMOTE_VERSION_URL = "https://raw.githubusercontent.com/Bolllix/ZuOrnder-1.0/main/desktop-app/version.json";
    /** Hier liegt die neueste fertige Programmdatei. */
    private static final String DEFAULT_JAR_URL = "https://raw.githubusercontent.com/Bolllix/ZuOrnder-1.0/main/desktop-app/ZuORDNER-Desktop/programm/zuordner-app.jar";

    /**
     * Die Version dieses Programms. Sie kommt aus application.properties
     * (zuordner.version), wo Maven beim Bauen die <version> aus der pom.xml einsetzt.
     */
    private final String currentVersion;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    // Zustand des laufenden Updates. "Atomic"/"volatile" sorgen dafür, dass der
    // Hintergrund-Download und die Status-Abfragen sich nicht in die Quere kommen.
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicInteger updateProgress = new AtomicInteger(0);
    private volatile String updateStep = "IDLE"; // IDLE, DOWNLOADING, RESTARTING, ERROR, COMPLETE
    private volatile String updateErrorMessage = null;

    /**
     * Spring ruft diesen Konstruktor automatisch auf und setzt über @Value die
     * Versionsnummer ein. Steht dort noch der unersetzte Platzhalter (z.B. wenn
     * man das Programm direkt aus einer Entwicklungsumgebung startet), wird
     * "0.0.0-dev" verwendet.
     */
    public SystemController(@Value("${zuordner.version:0.0.0-dev}") String configuredVersion) {
        boolean placeholderNotReplaced = configuredVersion == null || configuredVersion.contains("@");
        this.currentVersion = placeholderNotReplaced ? "0.0.0-dev" : configuredVersion.trim();
    }

    /** Liefert die aktuelle Programmversion (z.B. "2.0.0"). */
    public String getCurrentVersion() {
        return currentVersion;
    }

    /**
     * Desktop-Modus = Programm läuft lokal unter Windows (nicht auf dem Render.com-Server).
     * Nur dort ergibt ein automatisches Update Sinn.
     */
    private boolean isDesktopMode() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        boolean isRender = System.getenv("RENDER") != null;
        return isWindows && !isRender;
    }

    /**
     * Findet heraus, aus welcher JAR-Datei das Programm gerade läuft.
     * Gibt null zurück, wenn es keine JAR ist (z.B. Start aus der Entwicklungsumgebung).
     */
    private File getRunningJarFile() {
        try {
            URI uri = ZuordnerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File file = new File(uri);
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                return file;
            }
        } catch (Exception e) {
            System.err.println("Konnte laufende JAR-Datei nicht ermitteln: " + e.getMessage());
        }
        return null;
    }

    /**
     * GET /api/system/version
     * Vergleicht die eigene Version mit der Version auf GitHub.
     * Ohne Internet wird einfach "offline" gemeldet – das Programm funktioniert trotzdem.
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersionInfo() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("currentVersion", currentVersion);
        boolean desktop = isDesktopMode();
        resp.put("isDesktop", desktop);

        File runningJar = getRunningJarFile();
        resp.put("isJarRunning", runningJar != null);

        // version.json von GitHub laden. Der Zusatz "?t=<Zeit>" verhindert,
        // dass eine alte, zwischengespeicherte (gecachte) Datei geliefert wird.
        try {
            String urlWithCacheBuster = REMOTE_VERSION_URL + "?t=" + System.currentTimeMillis();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(urlWithCacheBuster))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> httpResp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(httpResp.body());
                String remoteVersion = root.path("version").asText(currentVersion);
                String notes = root.path("notes").asText("");
                String releaseDate = root.path("buildDate").asText("");
                String jarUrl = root.path("jarUrl").asText(DEFAULT_JAR_URL);

                resp.put("latestVersion", remoteVersion);
                resp.put("releaseNotes", notes);
                resp.put("releaseDate", releaseDate);
                resp.put("downloadUrl", jarUrl);
                resp.put("updateAvailable", isNewerVersion(remoteVersion, currentVersion));
            } else {
                resp.put("latestVersion", currentVersion);
                resp.put("updateAvailable", false);
            }
        } catch (Exception e) {
            // Kein Internet oder GitHub nicht erreichbar -> kein Update anbieten
            resp.put("latestVersion", currentVersion);
            resp.put("updateAvailable", false);
            resp.put("offline", true);
        }

        return ResponseEntity.ok(resp);
    }

    /** GET /api/system/update-status – Fortschritt des laufenden Updates (0–100 %). */
    @GetMapping("/update-status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("inProgress", updateInProgress.get());
        resp.put("progressPercent", updateProgress.get());
        resp.put("step", updateStep);
        resp.put("errorMessage", updateErrorMessage);
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /api/system/update – startet das Update.
     * Der eigentliche Download läuft im Hintergrund (CompletableFuture.runAsync),
     * damit der Browser sofort eine Antwort bekommt und den Fortschritt anzeigen kann.
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> triggerUpdate() {
        if (!isDesktopMode()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Updates sind nur in der lokalen Windows-Desktop-Version verfügbar."
            ));
        }

        File runningJar = getRunningJarFile();
        if (runningJar == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Anwendung läuft nicht als eigenständige JAR-Datei (Entwicklungsmodus)."
            ));
        }

        if (updateInProgress.get()) {
            return ResponseEntity.ok(Map.of(
                    "status", "already_running",
                    "step", updateStep,
                    "message", "Ein Update wird bereits ausgeführt."
            ));
        }

        updateInProgress.set(true);
        updateProgress.set(0);
        updateStep = "DOWNLOADING";
        updateErrorMessage = null;

        CompletableFuture.runAsync(() -> {
            try {
                executeDownloadAndRestart(runningJar);
            } catch (Exception e) {
                e.printStackTrace();
                updateStep = "ERROR";
                updateErrorMessage = "Update fehlgeschlagen: " + e.getMessage();
                updateInProgress.set(false);
            }
        });

        return ResponseEntity.ok(Map.of(
                "status", "started",
                "message", "Update-Download wurde erfolgreich gestartet."
        ));
    }

    /**
     * Lädt die neue JAR in eine temporäre Datei, prüft sie grob, schreibt ein
     * Austausch-Skript und beendet danach das laufende Programm.
     */
    private void executeDownloadAndRestart(File runningJar) throws Exception {
        File tempJar = File.createTempFile("zuordner_update_", ".jar");
        tempJar.deleteOnExit();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(DEFAULT_JAR_URL))
                .timeout(Duration.ofMinutes(3))
                .GET()
                .build();

        HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("Download fehlgeschlagen mit HTTP-Status " + resp.statusCode());
        }

        // Gesamtgröße (falls der Server sie mitteilt) – für die Prozentanzeige
        long contentLength = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);

        // Datei in 64-KB-Häppchen herunterladen und dabei den Fortschritt mitzählen
        try (InputStream in = resp.body();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(tempJar))) {
            byte[] buffer = new byte[65536];
            long totalRead = 0;
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                totalRead += n;
                if (contentLength > 0) {
                    int pct = (int) Math.min(99, (totalRead * 100) / contentLength);
                    updateProgress.set(pct);
                }
            }
        }

        // Plausibilitätsprüfung: Eine echte ZuORDNER-JAR ist deutlich größer als 10 MB.
        // Ist sie kleiner, wurde vermutlich eine Fehlerseite statt der Datei geladen.
        if (tempJar.length() < 10_000_000) {
            throw new IOException("Die heruntergeladene Datei ist unvollständig (" + tempJar.length() + " Bytes).");
        }

        updateProgress.set(100);
        updateStep = "RESTARTING";

        // Austausch-Skript im Temp-Ordner anlegen
        File updaterBat = File.createTempFile("zuordner_apply_update_", ".bat");
        // Programmordner = Ordner über "programm\" (dort liegt "ZuORDNER starten.vbs")
        File appDir = runningJar.getParentFile() != null ? runningJar.getParentFile().getParentFile() : null;

        String appDirPath = (appDir != null) ? appDir.getAbsolutePath() : "";

        // Inhalt des Skripts: 3 Sekunden warten (bis dieses Programm beendet ist),
        // dann die JAR kopieren (bis zu 8 Wiederholungen, falls Windows die Datei
        // noch sperrt), temporäre Datei löschen, ZuORDNER neu starten, sich selbst löschen.
        StringBuilder bat = new StringBuilder();
        bat.append("@echo off\r\n");
        bat.append("chcp 65001 >nul\r\n");
        bat.append("set TARGET_JAR=").append(runningJar.getAbsolutePath()).append("\r\n");
        bat.append("set TEMP_JAR=").append(tempJar.getAbsolutePath()).append("\r\n");
        bat.append("set APP_DIR=").append(appDirPath).append("\r\n");
        bat.append("timeout /t 3 /nobreak >nul\r\n");
        bat.append("set RETRY=0\r\n");
        bat.append(":COPY_LOOP\r\n");
        bat.append("copy /Y \"%TEMP_JAR%\" \"%TARGET_JAR%\" >nul 2>&1\r\n");
        bat.append("if errorlevel 1 (\r\n");
        bat.append("    set /a RETRY+=1\r\n");
        bat.append("    if %RETRY% leq 8 (\r\n");
        bat.append("        timeout /t 1 /nobreak >nul\r\n");
        bat.append("        goto COPY_LOOP\r\n");
        bat.append("    )\r\n");
        bat.append(")\r\n");
        bat.append("del /Q \"%TEMP_JAR%\" >nul 2>&1\r\n");
        bat.append("if exist \"%APP_DIR%\\ZuORDNER starten.vbs\" (\r\n");
        bat.append("    cd /d \"%APP_DIR%\"\r\n");
        bat.append("    start \"\" wscript.exe \"%APP_DIR%\\ZuORDNER starten.vbs\"\r\n");
        bat.append(") else (\r\n");
        bat.append("    start \"\" /b java -jar \"%TARGET_JAR%\"\r\n");
        bat.append(")\r\n");
        bat.append("del /Q \"%~f0\" >nul 2>&1\r\n");
        bat.append("exit\r\n");

        Files.writeString(updaterBat.toPath(), bat.toString(), StandardCharsets.UTF_8);

        // Skript als eigenständigen Prozess starten (läuft weiter, wenn wir uns beenden)
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "/b", updaterBat.getAbsolutePath());
        pb.start();

        // Kurz warten, damit der Browser die letzte Statusmeldung noch abholen kann
        Thread.sleep(800);
        System.exit(0);
    }

    /**
     * Vergleicht zwei Versionsnummern Stück für Stück, z.B. "2.0.0" mit "1.1.0":
     * 2 > 1 -> neuer. Fehlende Stellen zählen als 0 ("2.1" == "2.1.0").
     *
     * @return true, wenn {@code remote} neuer ist als {@code current}
     */
    static boolean isNewerVersion(String remote, String current) {
        if (remote == null || remote.trim().isEmpty() || remote.equals(current)) {
            return false;
        }
        String[] rParts = remote.split("\\.");
        String[] cParts = current.split("\\.");
        int len = Math.max(rParts.length, cParts.length);
        for (int i = 0; i < len; i++) {
            int r = (i < rParts.length) ? parseVersionNumber(rParts[i]) : 0;
            int c = (i < cParts.length) ? parseVersionNumber(cParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false;
    }

    /** Macht aus einem Versions-Stück wie "0-dev" die Zahl 0 (alles außer Ziffern wird entfernt). */
    private static int parseVersionNumber(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
