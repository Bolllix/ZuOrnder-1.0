Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

Dim currentDir, jarPath, tempPath, jarUrl, psCmd, ret, ans, ansStart
currentDir = fso.GetParentFolderName(WScript.ScriptFullName)
jarPath = currentDir & "\programm\zuordner-app.jar"
tempPath = currentDir & "\programm\zuordner-app.jar.download"
jarUrl = "https://raw.githubusercontent.com/Bolllix/ZuOrnder-1.0/main/desktop-app/ZuORDNER-Desktop/programm/zuordner-app.jar"

ans = MsgBox("Möchten Sie ZuORDNER jetzt auf die neueste Version aktualisieren?" & vbCrLf & vbCrLf & "Ihre gespeicherten Gebäude, Räume und Teilnehmer bleiben dabei zu 100% erhalten.", vbQuestion + vbYesNo, "ZuORDNER Update")

If ans = vbYes Then
    ' PowerShell Befehl zum sauberen Herunterladen der neuen JAR
    psCmd = "powershell -WindowStyle Hidden -Command ""$ProgressPreference = 'SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('" & jarUrl & "', '" & tempPath & "'); if ((Test-Path '" & tempPath & "') -and ((Get-Item '" & tempPath & "').Length -gt 10000000)) { Move-Item -Force '" & tempPath & "' '" & jarPath & "'; exit 0 } else { Remove-Item -Force '" & tempPath & "' -ErrorAction SilentlyContinue; exit 1 }"""
    
    ret = WshShell.Run(psCmd, 0, True)
    
    If ret = 0 Then
        ansStart = MsgBox("ZuORDNER wurde erfolgreich auf den neuesten Stand gebracht!" & vbCrLf & vbCrLf & "Möchten Sie das Programm jetzt direkt starten?", vbInformation + vbYesNo, "Update erfolgreich")
        If ansStart = vbYes Then
            WshShell.Run "wscript.exe """ & currentDir & "\ZuORDNER starten.vbs"""
        End If
    Else
        MsgBox "Das Update konnte nicht heruntergeladen werden." & vbCrLf & "Bitte stellen Sie sicher, dass eine aktive Internetverbindung besteht.", vbCritical, "Update fehlgeschlagen"
    End If
End If
