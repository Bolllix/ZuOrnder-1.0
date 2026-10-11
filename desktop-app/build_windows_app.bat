@echo off
chcp 65001 >nul
echo =======================================================
echo Erstelle ZuORDNER Windows-Desktop Paket...
echo =======================================================

set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."

echo.
echo 1. Bauen des React Frontends...
cd /d "%ROOT_DIR%\frontend"
call npm run build

echo.
echo 2. Kopieren der Frontend-Dateien in den Backend-Ressourcen-Ordner...
if not exist "%ROOT_DIR%\src\main\resources\static" mkdir "%ROOT_DIR%\src\main\resources\static"
xcopy /E /Y /I "%ROOT_DIR%\frontend\dist\*" "%ROOT_DIR%\src\main\resources\static\"

echo.
echo 3. Paketieren des Spring Boot Backends (JAR Executable)...
cd /d "%ROOT_DIR%"
call .\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

echo.
echo 4. Aktualisiere fertigen Desktop-Paket-Ordner...
if not exist "%SCRIPT_DIR%ZuORDNER-Desktop\programm" mkdir "%SCRIPT_DIR%ZuORDNER-Desktop\programm"
copy /Y "%ROOT_DIR%\target\zuordner-backend-1.0.0-SNAPSHOT.jar" "%SCRIPT_DIR%ZuORDNER-Desktop\programm\zuordner-app.jar"

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
