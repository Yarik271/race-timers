# Race Timers (Fabric, 1.21-1.21.11)

Client-side race timer mod for Minecraft. Works for elytra, boats, horses, and other route-based runs.

## Features

- Create track zones from two points.
- Select the active track used as the lap trigger.
- HUD with `total`, `best`, and `current` lap times.
- Shared HUD settings across all worlds.
- Per-world track and stats storage.
- Mod Menu config screen powered by AlinLib.

## Commands

- `/lap help`
- `/lap <x1> <y1> <z1> <x2> <y2> <z2>`
- `/lap pos1`
- `/lap pos2`
- `/lap create`
- `/lap list`
- `/lap set <id>`
- `/lap target <count>`
- `/lap remove <id>`
- `/lap run reset`
- `/lap run stop`
- `/lap run status`

Legacy alias:

- `/circle ...`

## Build

```powershell
.\gradlew.bat build
```

## Test Client

```powershell
.\gradlew.bat runClient
```

## Data File

`config/circletimer/circle_timer_data.json`

## Requirements

- Minecraft `1.21` - `1.21.11`
- Fabric Loader `0.18.2+`
- Fabric API `0.133.4+1.21.8`
- AlinLib `2.1.0-rc.5`

## License

CC0-1.0
