# Skyblock Extras

Fabric 26.2 client-side SkyBlock utility mod.

## Current project structure

- Farming RNG tracker
  - Harvest Feast crop drops are data-driven and individually toggleable.
  - Epic Slug
  - Legendary Slug
  - Farming dyes
  - Separate last-drop timestamps
  - Chat-only notifications (no RNG HUD)
- Pet overlay framework
  - Intended layout follows the supplied reference image.
  - Individual fields can be enabled/disabled.
  - Position and scale are stored in config.

## Important

The exact 27 Harvest Feast crop-drop names from the supplied screenshot have intentionally NOT been guessed here. Put the exact names into `config/skyblockextras.json` under `harvestFeastDrops`, with `true` or `false` values.

The same applies to the farming dye whitelist.

## Build

Requires JDK 25 for Minecraft 26.2.

Windows:
```powershell
.\gradlew.bat build
```

Linux/macOS:
```bash
./gradlew build
```

The finished JAR is placed in `build/libs`.

Fabric's current 26.2 documentation confirms JDK 25 and the `build` Gradle task workflow.
