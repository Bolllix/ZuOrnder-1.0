@echo off
REM ==========================================================================
REM  build_windows_app.bat - baut das fertige Windows-Desktop-Paket
REM ==========================================================================
REM  Ablauf in 5 Schritten:
REM   1. Die React-Oberflaeche (Ordner "frontend") wird zu fertigen
REM      HTML/JS/CSS-Dateien uebersetzt (Ergebnis: frontend\dist).
REM   2. Diese Dateien werden ins Backend kopiert (src\main\resources\static),
REM      damit der Java-Server die Webseite selbst ausliefern kann.
REM   3. Maven baut daraus EINE startbare Datei: target\zuordner-app.jar
REM   4. Die JAR wird in den Desktop-Ordner kopiert.
REM   5. Der Desktop-Ordner wird zusaetzlich als ZIP verpackt.
REM
REM  Hinweis: Die Versionsnummer wird in pom.xml (und desktop-app\version.json)
REM  gepflegt - dieses Skript muss bei einer neuen Version NICHT geaendert werden.
REM ==========================================================================
chcp 65001 >nul
echo =======================================================
echo Erstelle ZuORDNER Windows-Desktop Paket...
echo =======================================================

REM %~dp0 = Ordner, in dem dieses Skript liegt (desktop-app\)
set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."

echo.
echo 1. Bauen des React Frontends...
cd /d "%ROOT_DIR%\frontend"
call npm run build

echo.
echo 2. Kopieren der Frontend-Dateien in den Backend-Ressourcen-Ordner...
REM Alte Build-Dateien zuerst loeschen, damit keine veralteten JS/CSS-Dateien liegen bleiben
if exist "%ROOT_DIR%\src\main\resources\static\assets" rmdir /S /Q "%ROOT_DIR%\src\main\resources\static\assets"
if not exist "%ROOT_DIR%\src\main\resources\static" mkdir "%ROOT_DIR%\src\main\resources\static"
xcopy /E /Y /I "%ROOT_DIR%\frontend\dist\*" "%ROOT_DIR%\src\main\resources\static\"

echo.
echo 3. Paketieren des Spring Boot Backends (JAR Executable)...
cd /d "%ROOT_DIR%"
call .\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

echo.
echo 4. Aktualisiere fertigen Desktop-Paket-Ordner...
if not exist "%SCRIPT_DIR%ZuORDNER-Desktop\programm" mkdir "%SCRIPT_DIR%ZuORDNER-Desktop\programm"
REM Der Dateiname "zuordner-app.jar" ist in pom.xml (<finalName>) festgelegt
copy /Y "%ROOT_DIR%\target\zuordner-app.jar" "%SCRIPT_DIR%ZuORDNER-Desktop\programm\zuordner-app.jar"

echo.
echo 5. Erstelle ZIP-Archiv fuer einfachen Download/Versand...
powershell -Command "Compress-Archive -Path '%SCRIPT_DIR%ZuORDNER-Desktop/*' -DestinationPath '%SCRIPT_DIR%ZuORDNER-Desktop.zip' -Force"

echo.
echo =======================================================
echo FERTIG!
echo Der Ordner 'desktop-app\ZuORDNER-Desktop' und die ZIP-Datei wurden erfolgreich aktualisiert.
echo =======================================================
echo.
pause
