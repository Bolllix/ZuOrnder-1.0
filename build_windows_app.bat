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
echo 4. Aktualisiere fertigen Desktop-Paket-Ordner...
if not exist "ZuORDNER-Desktop\programm" mkdir "ZuORDNER-Desktop\programm"
copy /Y "target\zuordner-backend-1.0.0-SNAPSHOT.jar" "ZuORDNER-Desktop\programm\zuordner-app.jar"

echo.
echo 5. Erstelle ZIP-Archiv fuer einfachen Download/Versand...
powershell -Command "Compress-Archive -Path 'ZuORDNER-Desktop/*' -DestinationPath 'ZuORDNER-Desktop.zip' -Force"

echo.
echo =======================================================
echo FERTIG! 
echo Der Ordner 'ZuORDNER-Desktop' und 'ZuORDNER-Desktop.zip' wurden erstellt.
echo Du kannst deiner Mutter einfach die ZIP-Datei oder den Ordner schicken!
echo =======================================================
echo.
pause
