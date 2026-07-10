# Mod Info Overlay

A small, client-only [Fabric](https://fabricmc.net/) mod for Minecraft Java Edition. It displays:

- Block coordinates (`XYZ`)
- The current biome
- The current world day
- Optional eight-direction heading and Minecraft clock/weather
- On-demand light, chunk, and single-player slime-chunk details

The overlay is visible by default. Its toggle shortcut is intentionally unbound; assign one on the mod's Appearance settings page if desired. Pressing Minecraft's normal **F1** “Hide GUI” key also hides the overlay.

## Settings

Open **Options → Mod Info Settings…**. The button appears in the upper-left of Minecraft's normal Options screen. Settings are saved when you click **Done** and persist separately for each launcher instance in `config/mod-info.json`.

The screen is divided into **Info**, **Appearance**, **Announce**, and **Technical** pages. It provides:

- Independent on/off controls for coordinates, biome, and days played
- Field labels on/off for a compact values-only display
- A separate technical-label toggle for light, chunk, and slime rows
- Eight-direction facing (`North`, `Northeast`, etc.), either beside coordinates or on its own row, with a `Facing`/`Heading` label choice
- A Minecraft clock with independently optional day/night and weather status
- Background on/off and opacity from 0% (fully transparent) to 100% (opaque)
- Box spacing/size from 50% to 200%
- Font size from 50% to 200%
- Nine screen anchors: each corner, each edge center, and screen center
- Per-row text alignment within the box: Left (default), Center, or Right
- Modifier-aware shortcut capture; click a shortcut button, then press Ctrl/Shift/Alt plus the desired key
- New-day announcements (enabled by default)
- New-biome announcements (disabled by default)
- Optional dimension prefix and optional `Entering` wording for biome announcements
- Manual day/biome announcements for recording sessions
- On-demand block/sky light, chunk/local coordinates, and slime-chunk status
- Independently toggled frame-rate row
- **Reset**, **Cancel**, and **Done** actions

Shortcuts are intentionally managed on this screen rather than added to Minecraft's global Key Binds list. Defaults are **Unbound** for the regular overlay, **Ctrl+I** for technical information, **Ctrl+N** for a manual day-plus-biome announcement, and **Unbound** for frame rate. While capturing a shortcut, press **Backspace** or **Delete** to unbind it, or **Escape** to cancel.

Minecraft 26.2 assigns **O** to its Friends List. Version 1.6.2 migrates the mod's former default O binding to Unbound; customized bindings remain configurable on the Appearance page.

Facing and the clock are disabled by default to preserve the original compact layout. Technical light and chunk-position fields are selected by default but remain hidden until their shortcut is pressed. Slime-chunk status is disabled by default because accurate results are only available when the world seed is known, normally in single-player.

## Technical information

**Ctrl+I** toggles the selected technical rows together without opening Settings:

- Block and sky light at the player position
- Chunk X/Z plus local X/Z within that chunk
- Slime-chunk status in single-player

Slime-chunk calculation requires the world seed. The integrated single-player server exposes it, but a normal multiplayer server does not; multiplayer therefore displays `Unavailable (server seed unknown)` rather than guessing.

Technical labels are independent of the general information labels. With technical labels enabled, slime status is `Slime chunk: Yes/No`; with labels disabled it becomes the self-explanatory `Slimes/No Slimes`.

### Multiplayer seed overrides

While connected to a multiplayer server, open **Technical → Set current server seed…** to provide a signed 64-bit numeric world seed. Overrides are stored under a normalized `host:port` key and support multiple named profiles for servers that reset or rotate worlds. Click **Seed profile** to cycle the active profile, then click the main settings screen's **Done** button to save.

Example configuration:

```json
"serverSeedOverrides": {
  "play.example.com:25565": {
    "activeProfile": "season-4",
    "profiles": {
      "season-3": 123456789,
      "season-4": -987654321
    }
  }
}
```

Limitations and safety notes:

- Vanilla clients are not normally told a multiplayer world's seed; the override must be exact.
- A wrong seed produces plausible-looking but incorrect slime-chunk results.
- Server-list names and resolved IPs are not used as identity; the entered host/IP and effective port are normalized instead.
- Proxy networks, rotating worlds, and multiworld servers may require separate named profiles or manual profile changes.
- Server mods can alter slime spawning or the slime-chunk algorithm.
- Some public servers prohibit seed-based tools. Follow the server's rules even when the seed is known.
- Overrides are stored as plain text in `config/mod-info.json`, not encrypted secret storage.

Frame rate has its own shortcut and visibility state; it is not affected by the `Ctrl+I` technical group. It records individual frame times in a fixed-size ten-second rolling window and refreshes the average FPS and 99th-percentile frame-time result once per second. Average FPS is calculated as sampled frames divided by total sampled frame time, rather than averaging individual FPS readings.

The row initially shows `Sampling…` while at least two seconds of data is collected. Sampling resets after pauses, focus loss, hidden HUD, world changes, loading gaps, or toggling the row. With technical labels enabled it displays `FPS: 120 • Avg: 114 • 1% Low: 92`; without them it displays `120 FPS • 114 Avg • 92 Low`.

## Day and biome announcements

When enabled, a transition displays a large centered message over a dark contrasting backdrop. The message fades in for 350 milliseconds, remains readable, then fades out over 650 milliseconds; total display time is three seconds.

- A new day displays a gold `Day N` message.
- A new biome displays a cyan `Entering Biome Name` message.
- Biome messages can include the dimension (`Overworld: Forest`) and can omit `Entering`.
- Joining a world establishes the current day and biome silently, so login does not create a misleading transition announcement. Traveling to another dimension can announce its destination biome.
- If a day and biome change together, the messages are queued and shown in order instead of one replacing the other.
- Announcements are independent of the regular overlay toggle, but **F1** hides both as part of Minecraft's HUD.
- The configured manual shortcut queues Day, Biome, or Day + Biome regardless of whether automatic transition announcements are enabled.

## Compatibility

| Component | Version |
| --- | --- |
| Minecraft Java Edition | 26.2 |
| Java/JDK used to build | 25 |
| Fabric Loader | 0.19.3 or newer compatible release |
| Fabric API | 0.154.2+26.2 |

Minecraft mods are version-specific. This JAR will not load on Minecraft 1.21.x, 26.1.x, or a later feature release without porting and rebuilding it. It is client-only: servers do not need the mod, and the mod does not modify saved worlds.

## Build from source

Install a 64-bit JDK 25 and verify it is active:

```text
java -version
```

The output must report Java 25. From this project directory, run:

```bash
# Linux/macOS
./gradlew build
```

```powershell
# Windows PowerShell
.\gradlew.bat build
```

The first build downloads Gradle, Minecraft development files, Fabric Loader, and Fabric API, so it requires an internet connection. The distributable file is:

```text
build/libs/mod-info-1.10.0.jar
```

Do not install the `-sources.jar`; that archive is for IDE source browsing.

## Fast development test

Run Fabric's isolated development client:

```bash
# Linux/macOS
./gradlew runClient
```

```powershell
# Windows PowerShell
.\gradlew.bat runClient
```

This uses the project-local `run/` game directory, not your normal Minecraft saves. Create a temporary Creative world and verify:

1. The overlay appears at the upper-left after entering the world.
2. XYZ changes when the player moves.
3. Biome changes after crossing a biome boundary (or use `/locate biome minecraft:desert` and teleport there).
4. Day advances after `/time add 24000`.
5. The overlay toggle starts Unbound; binding it on the Appearance page hides and restores the overlay without colliding with Friends List.
6. **Options → Mod Info Settings…** opens the settings screen.
7. Each information toggle works, including all three being disabled.
8. Disabling **Field labels** removes `XYZ:`, `Biome:`, and `Days played:` while retaining context such as `42 Days`.
9. Background Off and 0% opacity both remove the rectangle while leaving text visible.
10. Box size, font size, all nine positions, and key capture work.
11. Left, Center, and Right alignment position every row correctly within the widest-row box width.
12. Facing reports all eight headings and can share the coordinate row or use a separate row.
13. Clock, day/night, and weather controls compose one optional time row.
14. **Ctrl+I** toggles only the selected technical fields; chunk/local coordinates update correctly across a chunk boundary.
15. General and technical label toggles operate independently.
16. Single-player slime status is `Slime chunk: Yes/No` with technical labels and `Slimes/No Slimes` without them; multiplayer reports that the seed is unavailable.
17. On multiplayer, set a signed numeric seed under Technical, enable slime chunks, and verify the selected server profile produces expected results.
18. Add a second seed profile, cycle the active profile, save, and confirm it persists after restarting.
19. Binding **Frame-rate toggle** shows and hides only the FPS row; it defaults to Unbound, begins in `Sampling…`, and produces a ten-second average plus 1% low after sufficient data.
20. With day announcements enabled, `/time add 24000` shows a centered `Day N` message that fades in and out.
21. With biome announcements enabled, crossing a biome boundary shows the configured dimension/wording format once.
22. **Ctrl+N** queues the configured manual announcement content.
23. Turning either automatic announcement setting off suppresses that transition type.
24. **Cancel** discards edits; **Done** persists them after restarting Minecraft.
25. **F1** hides the overlay and announcements with the rest of the HUD.

“Days played” is Minecraft's current Overworld day count: `floor(overworldClockTime / 24000) + 1`. The first in-game day is therefore displayed as 1. Because it follows world time, commands that set the time can change the number; it is not the player's real-world session duration.

## Install with Prism Launcher

1. Build the JAR as described above.
2. In Prism, click **Add Instance**.
3. Select Minecraft **26.2**, choose **Fabric** in the mod-loader list, select the starred/latest stable compatible loader, and create the instance.
4. Select the instance, click **Edit → Mods**.
5. Click **Add File** and select `build/libs/mod-info-1.10.0.jar`.
6. Add the matching **Fabric API** JAR as well. Prism's **Download Mods** button can find it; filter for Minecraft 26.2 and Fabric.
7. In **Edit → Settings → Java**, use a Java 25 runtime if Prism did not select one automatically.
8. Launch the instance and run the test checklist above.

Keeping this in a separate Prism instance is the safest way to prevent version or mod conflicts with other profiles.

## Install with the official Minecraft Launcher

1. Build `build/libs/mod-info-1.10.0.jar`.
2. Download the Fabric Installer from Fabric's official [Minecraft Launcher installer page](https://fabricmc.net/use/installer/).
3. Close Minecraft and the launcher, run the installer, choose **Minecraft 26.2**, keep the latest compatible loader selected, ensure **Create Profile** is checked, and install the client profile.
4. Download **Fabric API 0.154.2+26.2** (or a newer compatible 26.2 build) from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
5. Put both `mod-info-1.10.0.jar` and the Fabric API JAR in the game directory's `mods` folder. Remove any older `mod-info` JAR first, then create the game directory's `mods` folder if it does not exist:

   - Windows: `%APPDATA%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`

6. Start the launcher, choose the Fabric 26.2 installation, and click **Play**.

For isolation similar to Prism, edit the Fabric installation in the official launcher and set **Game Directory** to a new empty folder before the first launch. Then place the two JARs in that folder's `mods` directory. This avoids mixing 26.2 mods with a different Minecraft version.

## Troubleshooting

- **“Unsupported class file” or Java error while building:** `JAVA_HOME` or `java` points to something older than JDK 25.
- **Fabric says a dependency is missing:** install Fabric API for Minecraft 26.2 in the same `mods` folder.
- **The mod is not listed:** confirm the launcher profile is Fabric 26.2 and that you installed `mod-info-1.10.0.jar`, not the sources JAR.
- **Minecraft reports an incompatible mod set:** remove mods built for other Minecraft versions; a separate launcher instance/game directory is easiest.
- **The overlay is absent:** enter a world, press **F1** once, then check the information toggles and toggle key under **Options → Mod Info Settings…**.
- **Settings need to be completely cleared:** close Minecraft and delete `config/mod-info.json`; defaults are recreated on the next launch.
- **Need logs:** inspect `logs/latest.log` inside the selected game/instance directory. Prism also shows the log in its console window.

## How it works

Fabric Loader reads `fabric.mod.json` and invokes `ModInfoClient` through the client entrypoint. The client loads `config/mod-info.json`, polls configured modifier-aware shortcuts only while normal gameplay has focus, and adds its native paged settings screen to Minecraft's Options screen through Fabric Screen API. A HUD element attached before the vanilla chat layer reads the local player position, heading, biome registry key, lighting, weather, and synchronized world time, then draws the selected lines with the configured scale, background, and screen anchor. The same client tick observes day and biome transitions and feeds a timed announcement queue rendered in that HUD layer. Single-player slime checks use the integrated server's world seed; no server packets, mixins, Cloth Config, or Mod Menu dependency is required.

Java sources use the domain-owned base package `media.jlt.minecraft.mods.info`, and the Gradle Maven group matches it. The Fabric mod ID and resource namespace remain `mod-info`.

Useful upstream documentation:

- [Fabric for Minecraft 26.2](https://fabricmc.net/2026/06/15/262.html)
- [Fabric development environment (JDK 25)](https://docs.fabricmc.net/develop/getting-started/setting-up)
- [Custom screens](https://docs.fabricmc.net/develop/rendering/gui/custom-screens)
- [HUD rendering](https://docs.fabricmc.net/develop/rendering/hud)
- [Building a mod](https://docs.fabricmc.net/develop/getting-started/building-a-mod)
- [Prism Launcher Fabric installation](https://wiki.fabricmc.net/player:tutorials:third-party:prism)
- [Installing Fabric mods](https://wiki.fabricmc.net/player:tutorials:adding_mods)

## Updating to another Minecraft version

Change the pinned versions in `gradle.properties`, update the Minecraft/Java constraints in `src/main/resources/fabric.mod.json`, then run:

```bash
./gradlew build --refresh-dependencies
```

Minecraft and Fabric APIs can change between releases, so a successful compile and the in-game checklist are both required before distributing the new JAR. Use Fabric's [Develop page](https://fabricmc.net/develop/) for compatible version values and its porting guides when crossing a major toolchain change.
