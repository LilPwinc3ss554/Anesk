# Anesk v1.5.1

**Anesk** is a Java-based Snake variant developed as part of the Web Weavers World project.  
This build (v1.5.1) includes multiple game modes, skin system, audio effects, and favicon integration.

---

## Project Structure

```
Anesk/
  Anesk/
    Anesk.java        # Main game panel, handles rendering & state
    Config.java       # Configuration and constants
    Controls.java     # Key bindings and input mapping
    Labyrinth.java    # Labyrinth/maze game mode
    Main.java         # Entry point (launches JFrame)
    Sfx.java          # Sound effect manager
    Skins.java        # Skin system (25 skins total, incl. pride & base colors)
    Toast.java        # Toast/notification system
  aneskfavi/
    android-chrome-192x192.png
    android-chrome-512x512.png
    anesk.ico
    apple-touch-icon.png
    favicon-16x16.png
    favicon-32x32.png
    favicon.ico
    site.webmanifest
  Favicon/
    favicon-16x16.png
    favicon-32x32.png
  sounds/
    bonus.wav
    gameover.wav
    levelup.wav
    pause.wav
    pickup.wav
    resume.wav
    speed.wav
    start.wav
    turn.wav
```

---

## Features

- **Core Gameplay**
  - Classic Snake mechanics with wrap-around walls
  - Labyrinth mode with obstacles
  - Score tracking & level progression

- **Skins**
  - 25 skins total:
    - **Base Colors**: Neon (main), Crimson, Orange, Lime, Emerald, Teal, Cyan, Cobalt, Violet, Pink, Midnight
    - **Special FX**: Rainbow, Solar, Silver, Gold
    - **Pride Flags**: Trans, Lesbian, Bi, Pan, Nonbinary, Asexual, Aromantic, Genderfluid, Intersex, Pride
  - Base skins always unlocked
  - Special/Pride skins unlockable via progress or dev tools

- **Audio**
  - 9 sound effects (bonus, gameover, levelup, pause, pickup, resume, speed, start, turn)
  - Managed through `Sfx.java`

- **UI**
  - Toast notifications for events
  - Favicon integration for builds

---

## How to Run

1. Compile the project:
   ```bash
   javac Anesk/*.java
   ```

2. Run the main class:
   ```bash
   java Anesk.Main
   ```

3. Controls:
   - Arrow keys / WASD: Move
   - Space / Enter: Start game
   - P: Pause
   - R: Restart
   - M: Mute
   - [ / ]: Speed control
   - Tab: Toggle mode
   - K: Cycle skin
   - Ctrl+Shift+R: Reset progress

---

## Version

- **v1.5.1**
  - Added 10 new base color skins (total 25 skins now)
  - Updated skin cycling logic
  - Cleaned persistence (base skins no longer use prefs)
  - Polished audio + favicon integration
