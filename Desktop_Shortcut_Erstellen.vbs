Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

strDesktop = WshShell.SpecialFolders("Desktop")
strCurrentDir = fso.GetParentFolderName(WScript.ScriptFullName)
strVbsPath = strCurrentDir & "\Start_ZuORDNER.vbs"

Set objShortcut = WshShell.CreateShortcut(strDesktop & "\ZuORDNER.lnk")
objShortcut.TargetPath = "wscript.exe"
objShortcut.Arguments = """" & strVbsPath & """"
objShortcut.WorkingDirectory = strCurrentDir
objShortcut.IconLocation = "shell32.dll,275"
objShortcut.Save

MsgBox "Die Verknüpfung 'ZuORDNER' wurde erfolgreich auf Ihrem Desktop erstellt!", vbInformation, "ZuORDNER Desktop Setup"
