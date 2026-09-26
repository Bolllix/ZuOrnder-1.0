Set WshShell = CreateObject("WScript.Shell")
Dim fso
Set fso = CreateObject("Scripting.FileSystemObject")

Dim currentDir
currentDir = fso.GetParentFolderName(WScript.ScriptFullName)

' Check if Java is available
Dim javaCmd
javaCmd = "java -jar """ & currentDir & "\zuordner-backend-1.0.0-SNAPSHOT.jar"""

' Start Spring Boot Backend silently in background (0 = hidden window)
WshShell.Run "cmd /c start /b " & javaCmd, 0, False

' Wait 3 seconds for server startup
WScript.Sleep 3000

' Open application in default web browser
WshShell.Run "http://localhost:8080"
