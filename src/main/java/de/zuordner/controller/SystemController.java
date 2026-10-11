package de.zuordner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.zuordner.ZuordnerApplication;
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

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    public static final String CURRENT_VERSION = "1.1.0";
    private static final String REMOTE_VERSION_URL = "https://raw.githubusercontent.com/Bolllix/ZuOrnder-1.0/main/desktop-app/version.json";
    private static final String DEFAULT_JAR_URL = "https://raw.githubusercontent.com/Bolllix/ZuOrnder-1.0/main/desktop-app/ZuORDNER-Desktop/programm/zuordner-app.jar";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicInteger updateProgress = new AtomicInteger(0);
    private volatile String updateStep = "IDLE"; // IDLE, DOWNLOADING, RESTARTING, ERROR, COMPLETE
    private volatile String updateErrorMessage = null;

    private boolean isDesktopMode() {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        boolean isRender = System.getenv("RENDER") != null;
        return isWindows && !isRender;
    }

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

    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getVersionInfo() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("currentVersion", CURRENT_VERSION);
        boolean desktop = isDesktopMode();
        resp.put("isDesktop", desktop);

        File runningJar = getRunningJarFile();
        resp.put("isJarRunning", runningJar != null);

        // Fetch remote version from GitHub
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
                String remoteVersion = root.path("version").asText(CURRENT_VERSION);
                String notes = root.path("notes").asText("");
                String releaseDate = root.path("buildDate").asText("");
                String jarUrl = root.path("jarUrl").asText(DEFAULT_JAR_URL);

                resp.put("latestVersion", remoteVersion);
                resp.put("releaseNotes", notes);
                resp.put("releaseDate", releaseDate);
                resp.put("downloadUrl", jarUrl);
                resp.put("updateAvailable", isNewerVersion(remoteVersion, CURRENT_VERSION));
            } else {
                resp.put("latestVersion", CURRENT_VERSION);
                resp.put("updateAvailable", false);
            }
        } catch (Exception e) {
            resp.put("latestVersion", CURRENT_VERSION);
            resp.put("updateAvailable", false);
            resp.put("offline", true);
        }

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/update-status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("inProgress", updateInProgress.get());
        resp.put("progressPercent", updateProgress.get());
        resp.put("step", updateStep);
        resp.put("errorMessage", updateErrorMessage);
        return ResponseEntity.ok(resp);
    }

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

        long contentLength = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);

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

        // Validate downloaded file size (> 10 MB)
        if (tempJar.length() < 10_000_000) {
            throw new IOException("Die heruntergeladene Datei ist unvollständig (" + tempJar.length() + " Bytes).");
        }

        updateProgress.set(100);
        updateStep = "RESTARTING";

        // Create updater batch script in temp dir
        File updaterBat = File.createTempFile("zuordner_apply_update_", ".bat");
        File appDir = runningJar.getParentFile() != null ? runningJar.getParentFile().getParentFile() : null;

        String appDirPath = (appDir != null) ? appDir.getAbsolutePath() : "";

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

        // Spawn detached process
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "/b", updaterBat.getAbsolutePath());
        pb.start();

        // Brief delay before exit to allow response to be returned
        Thread.sleep(800);
        System.exit(0);
    }

    private boolean isNewerVersion(String remote, String current) {
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

    private int parseVersionNumber(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
