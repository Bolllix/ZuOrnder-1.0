@echo off
chcp 65001 >nul
echo Erstelle Desktop-Verknüpfung für ZuORDNER...

wscript.exe "%~dp0Desktop_Shortcut_Erstellen.vbs"

if %ERRORLEVEL% EQU 0 (
    echo =======================================================
    echo Die Verknüpfung 'ZuORDNER' wurde erfolgreich auf dem Desktop erstellt!
    echo =======================================================
) else (
    echo Es gab ein Problem beim Erstellen der Verknüpfung.
)

echo.
pause
