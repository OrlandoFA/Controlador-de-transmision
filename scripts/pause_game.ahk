#Requires AutoHotkey v2.0

; Pause Game Script

; Activate game window
if !WinExist("Plants vs. Zombies") {
    ExitApp 1
}

WinActivate "Plants vs. Zombies"
Sleep 100

; Send pause key (Space is default in PvZ)
Send "{Space}"

ExitApp 0