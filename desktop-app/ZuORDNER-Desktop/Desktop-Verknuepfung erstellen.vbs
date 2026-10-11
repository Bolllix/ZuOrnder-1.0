Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strDesktop = WshShell.SpecialFolders("Desktop")
strCurrentDir = fso.GetParentFolderName(WScript.ScriptFullName)
strStarterPath = strCurrentDir & "\ZuORDNER starten.vbs"

Set objShortcut = WshShell.CreateShortcut(strDesktop & "\ZuORDNER.lnk")
objShortcut.TargetPath = "wscript.exe"
objShortcut.Arguments = """" & strStarterPath & """"
objShortcut.WorkingDirectory = strCurrentDir
objShortcut.IconLocation = "shell32.dll,275"
objShortcut.Save

MsgBox "Die Verknüpfung 'ZuORDNER' wurde erfolgreich auf Ihrem Desktop erstellt!", vbInformation, "ZuORDNER Setup"
