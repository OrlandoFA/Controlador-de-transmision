#Requires AutoHotkey v2.0

; Next Wave Script
; Triggers the next wave of zombies

; Activate game window
if !WinExist("Plants vs. Zombies") {
    ExitApp 1
}

WinActivate "Plants vs. Zombies"
Sleep 100

; Send next wave key (customize based on your trainer/mod)
Send "{F10}"

ExitApp 0