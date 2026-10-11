Set WshShell = CreateObject("WScript.Shell")
Dim fso
Set fso = CreateObject("Scripting.FileSystemObject")

Dim currentDir, jarPath
currentDir = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = currentDir & "\programm\zuordner-app.jar"

' Start Spring Boot Backend im Hintergrund (0 = unsichtbares Fenster)
WshShell.Run "cmd /c start /b java -jar """ & jarPath & """", 0, False

' 3 Sekunden warten bis Server hochgefahren ist
WScript.Sleep 3000

' Anwendung im Standardbrowser öffnen
WshShell.Run "http://localhost:8080"
