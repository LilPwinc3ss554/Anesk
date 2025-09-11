# Anesk (Arcade Pulse — Snake)

**Version:** 1.5.7  
A modernized take on the classic Snake arcade game, built in Java.  
Includes multiple skins, labyrinth maps, power-ups, and a full sound set.  
Designed as part of the **Web Weavers World** demo deck.

---

## ✨ Features

- Classic Snake gameplay with modern twists
- Multiple skin packs (`classic`, `pride`, `extra`)
- Labyrinth maps (`lab-01`, `lab-02`, `lab-03`) for added challenge
- Score multipliers and bonus items
- Full SFX set (`pickup`, `turn`, `pause`, `resume`, `levelup`, `gameover`, `start`)
- Configurable keybinds (`Controls.java`, `ControlsController.java`)
- Loading screen + skin previews
- Cross-platform (runs on any JVM)

---

## 📂 Project Structure

```shell
Anesk-structured-v1.5.7/
  └─ Anesk-structured-v1.5.2.0.2/
     ├─ src/Anesk/       # Main Java source files
     │   ├─ Anesk.java          # Main game panel
     │   ├─ Main.java           # Launcher
     │   ├─ LoadingScreen.java  # Boot/loading UI
     │   ├─ Labyrinth.java      # Map logic
     │   ├─ SkinUtil.java       # Skin/theme selector
     │   └─ ... (other game logic)
     ├─ assets/
     │   ├─ icons/        # Game icons & favicons
     │   ├─ labs/         # Text-based labyrinth levels
     │   ├─ misc/         # Extra metadata & docs
     │   ├─ skins/        # Skin pack directories
     │   └─ sounds/       # .wav sound effects
     ├─ build/            # Scripts for compiling & running
     │   ├─ compile.ps1
     │   ├─ run.ps1
     │   └─ jar.ps1
     └─ .vscode/          # Workspace & dev configs
```

---

## 🚀 Running the Game

### Requirements

- Java 17+ (recommended)
- Windows, Linux, or macOS

### Compile & Run

From inside the `build/` folder:

```powershell
# Compile sources
./compile.ps1

# Run directly
./run.ps1

# Or package into JAR
./jar.ps1
```

You can also run manually:

```bash
javac -d bin src/Anesk/*.java
java -cp bin Anesk.Main
```

---

## 🎮 Controls

- **Arrow Keys** — Move snake
- **Space/Enter** — Start / Pause / Resume
- **Additional binds** handled via `Controls.java`

---

## 🛠 Development Notes

- Labs (`.txt`) are ASCII-based level layouts
- Skins (`SkinUtil.java`, `/assets/skins`) define color palettes & sprite sets
- Sounds (`.wav`) preload on boot

---

## 📜 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
