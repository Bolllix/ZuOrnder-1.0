@echo off
chcp 65001 >nul
echo Erstelle Desktop-Verknüpfung für ZuORDNER...

set "TARGET_VBS=%~dp0Start_ZuORDNER.vbs"
set "DESKTOP_PATH=%USERPROFILE%\Desktop"
set "SHORTCUT_PATH=%DESKTOP_PATH%\ZuORDNER.lnk"

powershell -Command "$s = (New-Object -COM WScript.Shell).CreateShortcut('%SHORTCUT_PATH%'); $s.TargetPath = 'wscript.exe'; $s.Arguments = '\"%TARGET_VBS%\"'; $s.WorkingDirectory = '%~dp0'; $s.IconLocation = 'shell32.dll,275'; $s.Save()"

echo.
echo =======================================================
echo Die Verknüpfung 'ZuORDNER' wurde erfolgreich auf dem Desktop erstellt!
echo =======================================================
echo.
pause
