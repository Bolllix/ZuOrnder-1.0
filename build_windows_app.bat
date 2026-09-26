@echo off
chcp 65001 >nul
echo =======================================================
echo Erstelle ZuORDNER Windows-Desktop Paket...
echo =======================================================

echo.
echo 1. Bauen des React Frontends...
cd frontend
call npm run build
cd ..

echo.
echo 2. Kopieren der Frontend-Dateien in den Backend-Ressourcen-Ordner...
if not exist "src\main\resources\static" mkdir "src\main\resources\static"
xcopy /E /Y /I "frontend\dist\*" "src\main\resources\static\"

echo.
echo 3. Paketieren des Spring Boot Backends (JAR Executable)...
call .\tools\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests

echo.
echo 4. Erstelle fertigen Desktop-Paket-Ordner...
if not exist "ZuORDNER-Desktop-Paket" mkdir "ZuORDNER-Desktop-Paket"
copy /Y "target\zuordner-backend-1.0.0-SNAPSHOT.jar" "ZuORDNER-Desktop-Paket\"
copy /Y "Start_ZuORDNER.vbs" "ZuORDNER-Desktop-Paket\"
copy /Y "Desktop_Shortcut_Erstellen.bat" "ZuORDNER-Desktop-Paket\"

echo.
echo =======================================================
echo FERTIG! Der Ordner 'ZuORDNER-Desktop-Paket' wurde erfolgreich erstellt.
echo Du kannst diesen Ordner einfach deiner Mutter schicken!
echo =======================================================
echo.
pause
