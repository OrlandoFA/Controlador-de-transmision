#Requires AutoHotkey v2.0

; Spawn Zombie Script
; Arguments: zombie_type, count

zombieType := A_Args.Has(1) ? A_Args[1] : "normal"
count := A_Args.Has(2) ? Integer(A_Args[2]) : 1

; Map zombie types to F-keys (customize based on your trainer/mod)
zombieKeys := Map(
    "normal", "F1",
    "conehead", "F2",
    "buckethead", "F3",
    "flag", "F4",
    "pole", "F5",
    "newspaper", "F6",
    "football", "F7",
    "dancer", "F8",
    "gargantuar", "F9"
)

if !zombieKeys.Has(zombieType) {
    ExitApp 1
}

key := zombieKeys[zombieType]

; Activate game window
if !WinExist("Plants vs. Zombies") {
    ExitApp 1
}

WinActivate "Plants vs. Zombies"
Sleep 100

; Send key presses
Loop count {
    Send "{" key "}"
    Sleep 200
}

ExitApp 0