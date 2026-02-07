#Requires AutoHotkey v2.0

; Resume Game Script

; Activate game window
if !WinExist("Plants vs. Zombies") {
    ExitApp 1
}

WinActivate "Plants vs. Zombies"
Sleep 100

; Send resume key (Space toggles pause in PvZ)
Send "{Space}"

ExitApp 0