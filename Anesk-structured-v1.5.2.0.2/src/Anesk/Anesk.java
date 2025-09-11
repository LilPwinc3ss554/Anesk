package Anesk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;
import java.util.prefs.Preferences;
import static Anesk.Controls.KeyAction.*;

/**
 * Web Weavers • Snake (Arcade Pulse Edition)
 * Start screen, SFX, wrap walls, Labyrinth mode, bonus shard.
 */
@SuppressWarnings("unused")
public class Anesk extends JPanel implements ActionListener {

  /* ------------------------ Launcher ------------------------ */
  // Anesk.java (inside class Anesk)
  public static void launchWithLoader() {
    LoadingScreen.run(
        "Arcade Pulse — Snake",
        920, 640,
        LoadingScreen.defaultSkinsWWW(),
        sink -> {
          // === real loading steps ===
          sink.accept(5);
          try {
            Sfx.preload();
          } catch (Exception ignored) {
          }

          sink.accept(25);
          try {
            SpriteBank.loadAll();
          } catch (Exception ignored) {
          }

          sink.accept(45);
          try {
            Skins.loadAll();
          } catch (Exception ignored) {
          }

          sink.accept(65);
          try {
            Prefs.init();
          } catch (Exception ignored) {
          }

          sink.accept(85);
          try {
            Maps.loadAll();
          } catch (Exception ignored) {
          }

          sink.accept(100);
        },
        Anesk::new // <- creates your game panel when done
    );
  }

  /* -------------------- Enums & constants ------------------- */
  // Progress prefs
  private static final String PREF_LEVEL = "anesksnake.level";
  private static final String PREF_XP = "anesksnake.xp";

  // Directions / state / mode
  private enum Dir {
    LEFT, RIGHT, UP, DOWN
  }

  private enum State {
    START, PLAYING, PAUSED, OVER
  }

  private enum Mode {
    CLASSIC, LABYRINTH
  }

  // Timer speed control
  private static final int TICK_START = 110;
  private static final int TICK_FLOOR = 55;
  private static final int TICK_STEP = 4;

  // XP per pickup
  private static final int XP_APPLE = 10;
  private static final int XP_RARE = 30;

  // --- Score Multiplier (streak) ---
  private static final int MULT_MAX_TIER = 5; // x1..x5
  private static final int MULT_METER_FULL_MS = 6000; // time to gain/lose one tier
  private static final int MULT_GAIN_ON_APPLE_MS = 35000; // meter added per apple

  private void normalizeProgress() {
    // If saved XP overshoots current requirement, carry it forward
    int guard = 0;
    while (xp >= currentXpNeeded() && guard++ < 10000) {
      xp -= currentXpNeeded();
      level++;
    }
  }

  /* --------------------------- Fields ----------------------- */
  // Core
  private final Preferences prefs = Preferences.userNodeForPackage(Anesk.class);
  private final Skins.Manager skins = new Skins.Manager(prefs);

  // Board / snake buffers
  private final int maxLen = Config.COLS * Config.ROWS;
  private final int[] sx = new int[maxLen];
  private final int[] sy = new int[maxLen];
  private boolean[][] wall = new boolean[Config.COLS][Config.ROWS];

  // Loop & speed
  private javax.swing.Timer timer;
  private int currentTickDelay = TICK_START;
  private int speedIndex = Config.START_SPEED_INDEX;

  // Snake & apple
  private int length;
  private int appleX, appleY;

  // Score multiplier state
  private int multTier = 1; // 1..MULT_MAX_TIER
  private int multMeterMs = 0; // 0..MULT_METER_FULL_MS-1 (progress within current tier)
  private long multLastMs = System.currentTimeMillis();

  // Power-ups
  private final PowerUps.Inventory inventory = new PowerUps.Inventory();
  private final PowerUps.Effects effects = new PowerUps.Effects();

  private int score;
  private int highScore = 0;

  // Run state
  private boolean running;
  private boolean paused;
  private Dir dir = Dir.RIGHT;
  private State state = State.START;
  private Mode mode = Mode.CLASSIC;

  // RNG & visuals
  private final Random rng = new Random();
  private long headFlashUntil = 0L;

  // Bonus shard
  private boolean bonusActive = false;
  private int bonusX, bonusY, bonusTicks = 0;

  // Toasts
  private final java.util.ArrayDeque<Toast> toastQ = new java.util.ArrayDeque<>();
  private Toast activeToast;

  // Level / XP
  private int level = 1;
  private int xp = 0;
  // private int xpNeeded; // recomputed each level
  private int flashTicks = 0;

  // --- XP requirement formula (tweak as you like)
  private int xpForLevel(int lvl) {
    // Linear growth: L1=100, L2=120, L3=140, ...
    return 100 + Math.max(0, (lvl - 1)) * 20;
  }

  // Current requirement for the active level
  private int currentXpNeeded() {
    return xpForLevel(level);
  }

  // Start screen ticker
  private javax.swing.Timer startTicker = new javax.swing.Timer(33, this::onStartTick);

  // DEV
  private long bootNanos = System.nanoTime();
  private boolean devInvincible = false;
  private static final boolean DEV = Boolean.parseBoolean(System.getProperty("anesk.dev", "false"))
      || "1".equals(System.getenv("ANESK_DEV"));
  private static final float stroke = 0;

  /* ---------------------- Construction ---------------------- */
  public Anesk() {
    Maps.Layout L = Maps.random(); // or Maps.get("lab-01")
    if (L != null) {
      // Example: convert walls into your game’s obstacle list
      for (int r = 0; r < L.rows; r++) {
        for (int c = 0; c < L.cols; c++) {
          if (L.wall[r][c]) {
            // translate (c, r) into world coordinates and add a wall tile/segment
          }
        }
      }
    }
    setPreferredSize(new Dimension(Config.COLS * Config.TILE, Config.ROWS * Config.TILE));
    setBackground(Config.BG);
    setFocusable(true);
    requestFocusInWindow();
    setFocusTraversalKeysEnabled(false);

    Sfx.init(); // optional; preload() calls init() anyway
    Sfx.preload(); // warms the mixer & all pools
    Skins.loadAll();
    Labyrinth.preload();

    timer = new javax.swing.Timer(TICK_START, this);
    timer.start();

    highScore = prefs.getInt("anesksnake.high", 0);
    loadProgress();
    bindKeys();

    startTicker.start();
    resetToStartScreen();
  }

  /* ------------------------ Input bind ---------------------- */
  // Controls.bind signature assumed:
  // bind(JComponent,
  // Runnable onLeft, Runnable onRight, Runnable onUp, Runnable onDown,
  // Runnable onStartOrPause, Runnable onPauseToggle, Runnable onRestart,
  // Runnable onMuteToggle, Runnable onSpeedUp, Runnable onSpeedDown,
  // Runnable onModeToggle, Runnable onSkinCycle, Runnable onResetProgress,
  // Runnable onDevTools, Runnable onMapCycle) // <-- last param
  private void bindKeys() {
    var A = Controls.actions();

    A.put(Controls.KeyAction.LEFT, () -> turn(Dir.LEFT));
    A.put(Controls.KeyAction.RIGHT, () -> turn(Dir.RIGHT));
    A.put(Controls.KeyAction.UP, () -> turn(Dir.UP));
    A.put(Controls.KeyAction.DOWN, () -> turn(Dir.DOWN));

    // SPACE / ENTER : start or pause/resume
    A.put(Controls.KeyAction.START_OR_PAUSE, () -> {
      switch (state) {
        case START, OVER -> {
          startGame();
          Sfx.play(Sfx.Id.START);
        }
        case PLAYING -> {
          state = State.PAUSED;
          paused = true;
          timer.stop();
          Sfx.play(Sfx.Id.PAUSE);
        }
        case PAUSED -> {
          state = State.PLAYING;
          paused = false;
          timer.start();
          Sfx.play(Sfx.Id.RESUME);
        }
      }
      repaint();
    });

    // P : pause toggle
    A.put(Controls.KeyAction.PAUSE_TOGGLE, () -> {
      if (state == State.PLAYING) {
        state = State.PAUSED;
        paused = true;
        timer.stop();
        Sfx.play(Sfx.Id.PAUSE);
      } else if (state == State.PAUSED) {
        state = State.PLAYING;
        paused = false;
        timer.start();
        Sfx.play(Sfx.Id.RESUME);
      }
      repaint();
    });

    // R : restart
    A.put(Controls.KeyAction.RESTART, () -> {
      startGame();
      Sfx.play(Sfx.Id.START);
    });

    // M : mute toggle
    A.put(Controls.KeyAction.MUTE_TOGGLE, () -> {
      Sfx.toggleMute();
      repaint();
    });

    // C : speed up / Z : speed down
    A.put(Controls.KeyAction.SPEED_UP, () -> changeSpeed(+1));
    A.put(Controls.KeyAction.SPEED_DOWN, () -> changeSpeed(-1));

    // TAB : mode toggle (only on START/PAUSED)
    A.put(Controls.KeyAction.MODE_TOGGLE, () -> {
      if (state == State.START || state == State.PAUSED) {
        mode = (mode == Mode.CLASSIC ? Mode.LABYRINTH : Mode.CLASSIC);
        repaint();
      }
    });

    // K : cycle unlocked skins
    A.put(Controls.KeyAction.SKIN_CYCLE, () -> {
      if (state == State.START || state == State.PAUSED) {
        Skins.Skin[] order = {
            // bases first
            Skins.Skin.MAIN, Skins.Skin.CRIMSON, Skins.Skin.ORANGE, Skins.Skin.LIME, Skins.Skin.EMERALD,
            Skins.Skin.TEAL, Skins.Skin.CYAN, Skins.Skin.COBALT, Skins.Skin.VIOLET, Skins.Skin.PINK,
            Skins.Skin.MIDNIGHT,
            // legacy/gated next
            Skins.Skin.RAINBOW, Skins.Skin.SOLAR, Skins.Skin.SILVER, Skins.Skin.GOLD,
            Skins.Skin.PRIDE, Skins.Skin.TRANS, Skins.Skin.LESBIAN, Skins.Skin.BI, Skins.Skin.PAN,
            Skins.Skin.NONBINARY, Skins.Skin.ASEXUAL, Skins.Skin.AROMANTIC, Skins.Skin.GENDERFLUID,
            Skins.Skin.INTERSEX
        };
        int i = java.util.Arrays.asList(order).indexOf(skins.get());
        for (int step = 1; step <= order.length; step++) {
          Skins.Skin cand = order[(i + step) % order.length];
          if (skins.isUnlocked(cand)) {
            skins.set(cand);
            Skins.loadAll(); // safe no-op hook
            repaint();
            break;
          }
        }
      }
    });

    // Ctrl+Shift+R : reset progress
    A.put(Controls.KeyAction.RESET_PROGRESS, this::confirmResetProgress);

    // Ctrl+Shift+D : dev tools (gated)
    A.put(Controls.KeyAction.DEV_TOOLS, () -> showDevTools());

    // T : map next (only when not actively playing)
    A.put(Controls.KeyAction.MAP_NEXT, () -> {
      if (state == State.PLAYING)
        return;
      String id = Maps.next();
      mode = Mode.LABYRINTH; // ensure labyrinth preview
      System.out.println("Map -> " + id);
      repaint();
    });

    // Shift+T : map prev (optional—define Maps.prev() if you want it)
    A.put(Controls.KeyAction.MAP_PREV, () -> {
      if (state == State.PLAYING)
        return;
      String id = Maps.prev(); // implement this if not present
      mode = Mode.LABYRINTH;
      System.out.println("Map <- " + id);
      repaint();
    });

    // ESC : back to start
    A.put(Controls.KeyAction.BACK_TO_START, this::goToStartScreen);

    Controls.bind(this, A);
  }

  /* ---------------------- Hub integration ------------------- */
  public void hubStart() {
    requestFocusInWindow();
    switch (state) {
      case START, OVER -> {
        startGame();
        Sfx.play(Sfx.Id.START);
      }
      case PAUSED -> {
        paused = false;
        state = State.PLAYING;
        timer.start();
        Sfx.play(Sfx.Id.RESUME);
        repaint();
      }
      case PLAYING -> {
        if (!timer.isRunning())
          timer.start();
      }
    }
  }

  public void hubPause() {
    if (state == State.PLAYING) {
      paused = true;
      state = State.PAUSED;
      timer.stop();
      Sfx.play(Sfx.Id.PAUSE);
      repaint();
    }
  }

  public void hubReset() {
    if (timer != null)
      timer.stop();
    resetToStartScreen();
    speedIndex = Config.START_SPEED_INDEX; // optional HUD reset
    if (timer != null)
      timer.setDelay(currentTickDelay);
    flashTicks = 0;
    repaint();
    requestFocusInWindow();
  }

  public void start() {
    hubStart();
  }

  public void pause() {
    hubPause();
  }

  public void reset() {
    hubReset();
  }

  /* --------------------- Persistence / Reset ---------------- */
  public void saveProgress() {
    prefs.putInt(PREF_LEVEL, level);
    prefs.putInt(PREF_XP, xp);
  }

  private void loadProgress() {
    level = Math.max(1, prefs.getInt(PREF_LEVEL, 1));
    xp = Math.max(0, prefs.getInt(PREF_XP, 0));
    normalizeProgress(); // ensure xp < requirement; auto-level if needed
  }

  private void confirmResetProgress() {
    Window w = SwingUtilities.getWindowAncestor(this);
    String[] options = { "Reset ALL (skins + stats)", "Reset stats only", "Cancel" };
    int choice = JOptionPane.showOptionDialog(
        w,
        "This will erase your saved progress.\nWarning: this cannot be undone.",
        "Reset progress",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null, options, options[2]);

    if (choice == 0) {
      resetProgress(true);
      queueToast("All progress reset", new Color(0xFF4D4D));
    } else if (choice == 1) {
      resetProgress(false);
      queueToast("Stats reset", new Color(0xFFB84D));
    }
  }

  private void resetProgress(boolean wipeSkins) {
    try {
      // scores / levels
      prefs.remove(PREF_LEVEL);
      prefs.remove(PREF_XP);
      prefs.remove("anesksnake.high");

      if (wipeSkins) {
        // Clear skin selection & unlocks — same keys Skins.Manager uses
        prefs.remove("anesksnake.skin");
        prefs.remove("anesksnake.skin.rainbow");
        prefs.remove("anesksnake.skin.trans");
        prefs.remove("anesksnake.skin.solar");
        prefs.remove("anesksnake.skin.silver");
        prefs.remove("anesksnake.skin.gold");
        prefs.remove("anesksnake.skin.lesbian");
        prefs.remove("anesksnake.skin.bi");
        prefs.remove("anesksnake.skin.pan");
        prefs.remove("anesksnake.skin.nonbinary");
        prefs.remove("anesksnake.skin.asexual");
        prefs.remove("anesksnake.skin.aromantic");
        prefs.remove("anesksnake.skin.genderfluid");
        prefs.remove("anesksnake.skin.intersex");
        prefs.remove("anesksnake.skin.pride");
      }
      // old keys (safe even if absent)
      prefs.remove("player_level");
      prefs.remove("player_xp");
      prefs.flush();
    } catch (Exception ignore) {
    }

    level = 1;
    xp = 0;
    highScore = 0;

    if (wipeSkins) {
      skins.load(); // reload after wipe
      skins.set(Skins.Skin.MAIN);
    }

    currentTickDelay = TICK_START;
    if (timer != null)
      timer.setDelay(currentTickDelay);
    flashTicks = 0;

    resetToStartScreen();
    repaint();
  }

  /* -------------------------- Game model -------------------- */
  private void changeSpeed(int delta) {
    int old = speedIndex;
    speedIndex = Math.max(0, Math.min(Config.SPEEDS_MS.length - 1, speedIndex + delta));
    int base = Config.SPEEDS_MS[speedIndex];
    currentTickDelay = Math.min(currentTickDelay, base);
    timer.setDelay(currentTickDelay);
    if (old != speedIndex) {
      Sfx.play(Sfx.Id.SPEED);
      repaint();
    }
  }

  private void turn(Dir d) {
    if (state != State.PLAYING)
      return;
    if ((d == Dir.LEFT && dir != Dir.RIGHT) ||
        (d == Dir.RIGHT && dir != Dir.LEFT) ||
        (d == Dir.UP && dir != Dir.DOWN) ||
        (d == Dir.DOWN && dir != Dir.UP)) {
      dir = d;
      Sfx.play(Sfx.Id.TURN);
    }
  }

  private void resetToStartScreen() {
    running = false;
    paused = false;
    state = State.START;
    score = 0;
    length = 1;
    dir = Dir.RIGHT;
    Labyrinth.clear(wall, Config.COLS, Config.ROWS);
    sx[0] = Config.COLS / 2;
    sy[0] = Config.ROWS / 2;
    spawnApple();
    bonusActive = false;
    bonusTicks = 0;
  }

  private void startGame() {
    // no need for xpNeeded anymore
    score = 0;
    highScore = Math.max(highScore, score);
    // walls
    Labyrinth.clear(wall, Config.COLS, Config.ROWS);

    // reset basics
    length = 4;
    score = 0;

    if (mode == Mode.LABYRINTH) {
      Maps.ensureActive();
      String id = Maps.activeName();
      loadLabyrinthLevelFromTxt("/assets/labs/" + id + ".txt");

    } else {
      // Classic: center start
      dir = Dir.RIGHT;
      int cx = Config.COLS / 2, cy = Config.ROWS / 2;
      if (wall[cx][cy]) {
        cx = 2;
        cy = 2;
      }
      for (int i = 0; i < length; i++) {
        sx[i] = cx - i;
        sy[i] = cy;
      }
    }

    // common init
    spawnApple();
    bonusActive = false;
    bonusTicks = 0;
    running = true;
    paused = false;
    headFlashUntil = 0;
    state = State.PLAYING;

    speedIndex = Config.START_SPEED_INDEX;
    timer.setDelay(Config.SPEEDS_MS[speedIndex]);

    timer.setInitialDelay(0); // no wait before first tick
    timer.restart(); // restarts immediately with these settings

    multTier = 1;
    multMeterMs = 0;
    multLastMs = System.currentTimeMillis();

    repaint();
  }

  private void gameOver() {
    running = false;
    state = State.OVER;
    timer.stop();
    if (score > highScore) {
      highScore = score;
      prefs.putInt("anesksnake.high", highScore);
    }
    Sfx.play(Sfx.Id.GAMEOVER);
    repaint();
  }

  private void goToStartScreen() {
    if (timer != null && timer.isRunning()) {
      timer.stop();
    }
    paused = false;
    state = State.START;
    // mode = Mode.CLASSIC; // optional
    repaint();
  }

  /* --------------------------- Tick ------------------------- */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (!running || paused || state != State.PLAYING)
      return;
    multiplierTick();

    step();
    repaint();
  }

  private void step() {
    for (int i = length - 1; i > 0; i--) {
      sx[i] = sx[i - 1];
      sy[i] = sy[i - 1];
    }
    switch (dir) {
      case LEFT -> sx[0]--;
      case RIGHT -> sx[0]++;
      case UP -> sy[0]--;
      case DOWN -> sy[0]++;
    }

    if (Config.WRAP_WALLS) {
      sx[0] = (sx[0] + Config.COLS) % Config.COLS;
      sy[0] = (sy[0] + Config.ROWS) % Config.ROWS;
    } else {
      if (sx[0] < 0 || sx[0] >= Config.COLS || sy[0] < 0 || sy[0] >= Config.ROWS) {
        gameOver();
        return;
      }
    }

    if (wall[sx[0]][sy[0]]) {
      gameOver();
      return;
    }

    for (int i = 1; i < length; i++)
      if (sx[0] == sx[i] && sy[0] == sy[i]) {
        gameOver();
        return;
      }

    if (sx[0] == appleX && sy[0] == appleY) {
      int gained = 10 * multTier;
      score += 10;
      addXp(XP_APPLE);
      multiplierOnApple();
      if (length < maxLen) {
        sx[length] = sx[length - 1];
        sy[length] = sy[length - 1];
        length++;
      }
      spawnApple();
      Sfx.play(Sfx.Id.PICKUP);
      headFlashUntil = System.nanoTime() + 580_000_000L;
      maybeUnlockSkins();

      int apples = (score / 10);
      if (apples % 5 == 0 && speedIndex < Config.SPEEDS_MS.length - 1)
        changeSpeed(+1);

      if (!bonusActive && rng.nextDouble() < Config.BONUS_SPAWN_CHANCE)
        spawnBonus();
    }

    if (bonusActive) {
      if (sx[0] == bonusX && sy[0] == bonusY) {
        score += Config.BONUS_POINTS;
        addXp(XP_RARE);
        for (int g = 0; g < 2 && length < maxLen; g++) {
          sx[length] = sx[length - 1];
          sy[length] = sy[length - 1];
          length++;
        }
        bonusActive = false;
        Sfx.play(Sfx.Id.BONUS);
      } else {
        if (--bonusTicks <= 0)
          bonusActive = false;
      }
      maybeUnlockSkins();
    }

    if (!devInvincible) {
      if (wall[sx[0]][sy[0]]) {
        gameOver();
        return;
      }
      for (int i = 1; i < length; i++) {
        if (sx[0] == sx[i] && sy[0] == sy[i]) {
          gameOver();
          return;
        }
      }
    }
  }

  /* ------------------------- Power-ups ---------------------- */
  private void onPickupPowerUp(PowerUps.Type t) {
    if (!inventory.add(t)) {
      score += PowerUps.OVERFLOW_SCORE; // full: convert to score
      // Sfx.play(Sfx.Id.CASH); // optional
    } else {
      // Sfx.play(Sfx.Id.POWERUP); // optional
    }
  }

  private void useSelectedPowerUp() {
    PowerUps.Type t = inventory.useSelected();
    if (t == null)
      return;
    switch (t) {
      case MULLIGAN -> {
        // Mulligan is auto-use; treat manual use as no-op (re-add to inventory)
        inventory.add(PowerUps.Type.MULLIGAN);
      }
      case PHASE_WALLS -> {
        effects.activate(PowerUps.Type.PHASE_WALLS);
        // Sfx.play(Sfx.Id.PHASE);
      }
    }
  }

  private void cyclePowerUpNext() {
    inventory.cycleNext();
  }

  private void cyclePowerUpPrev() {
    inventory.cyclePrev();
  }

  private boolean tryMulligan(int headBeforeX, int headBeforeY) {
    boolean had = inventory.consumeFirst(PowerUps.Type.MULLIGAN);
    if (!had)
      return false;

    Point safe = findNearestSafeTile(headBeforeX, headBeforeY);
    if (safe == null)
      return false;

    // Teleport the head to a safe tile this tick.
    sx[0] = safe.x;
    sy[0] = safe.y;
    // Treat as a normal (non-growth) move; arrays are already shifted in step().
    // (No trimTail() needed in this model.)
    // Sfx.play(Sfx.Id.MULLIGAN);
    return true;
  }

  private Point findNearestSafeTile(int originX, int originY) {
    int maxR = Math.max(Config.COLS, Config.ROWS);
    for (int r = 1; r <= maxR; r++) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dy = -r; dy <= r; dy++) {
          if (Math.abs(dx) != r && Math.abs(dy) != r)
            continue; // perimeter only
          int x = originX + dx;
          int y = originY + dy;
          if (x < 0 || y < 0 || x >= Config.COLS || y >= Config.ROWS)
            continue;
          if (wall[x][y])
            continue;
          if (occupies(x, y))
            continue;
          return new Point(x, y);
        }
      }
    }
    return null;
  }

  /* -------------------------- Spawns ------------------------ */
  private void spawnApple() {
    do {
      appleX = rng.nextInt(Config.COLS);
      appleY = rng.nextInt(Config.ROWS);
    } while (occupies(appleX, appleY) || wall[appleX][appleY] || (bonusActive && appleX == bonusX && appleY == bonusY));
  }

  private void spawnBonus() {
    int tries = 0;
    do {
      bonusX = rng.nextInt(Config.COLS);
      bonusY = rng.nextInt(Config.ROWS);
      tries++;
      if (tries > 200)
        break;
    } while (occupies(bonusX, bonusY) || wall[bonusX][bonusY] || (bonusX == appleX && bonusY == appleY));
    bonusActive = true;
    bonusTicks = Config.BONUS_LIFE_TICKS;
    Sfx.play(Sfx.Id.SPEED);
  }

  private boolean occupies(int x, int y) {
    for (int i = 0; i < length; i++)
      if (sx[i] == x && sy[i] == y)
        return true;
    return false;
  }

  /* ----------------------- Skin unlocks --------------------- */
  private void maybeUnlockSkins() {
    if ((score >= 500 || level >= 5) && skins.unlock(Skins.Skin.RAINBOW))
      announceUnlock(Skins.Skin.RAINBOW, 14);
    if ((score >= 900 || level >= 6) && skins.unlock(Skins.Skin.TRANS))
      announceUnlock(Skins.Skin.TRANS, 16);
    if ((score >= 600 || level >= 6) && skins.unlock(Skins.Skin.LESBIAN))
      announceUnlock(Skins.Skin.LESBIAN, 14);
    if ((score >= 700 || level >= 7) && skins.unlock(Skins.Skin.BI))
      announceUnlock(Skins.Skin.BI, 14);
    if ((score >= 800 || level >= 8) && skins.unlock(Skins.Skin.PAN))
      announceUnlock(Skins.Skin.PAN, 14);
    if ((score >= 1000 || level >= 9) && skins.unlock(Skins.Skin.NONBINARY))
      announceUnlock(Skins.Skin.NONBINARY, 16);
    if ((score >= 1100 || level >= 9) && skins.unlock(Skins.Skin.ASEXUAL))
      announceUnlock(Skins.Skin.ASEXUAL, 16);
    if ((score >= 1200 || level >= 10) && skins.unlock(Skins.Skin.AROMANTIC))
      announceUnlock(Skins.Skin.AROMANTIC, 16);
    if ((score >= 1300 || level >= 10) && skins.unlock(Skins.Skin.GENDERFLUID))
      announceUnlock(Skins.Skin.GENDERFLUID, 18);
    if ((score >= 1400 || level >= 11) && skins.unlock(Skins.Skin.INTERSEX))
      announceUnlock(Skins.Skin.INTERSEX, 18);
    if ((score >= 1500 || level >= 10) && skins.unlock(Skins.Skin.SOLAR))
      announceUnlock(Skins.Skin.SOLAR, 18);
    if ((score >= 1800 || level >= 12) && skins.unlock(Skins.Skin.SILVER))
      announceUnlock(Skins.Skin.SILVER, 20);
    if ((score >= 7500 || level >= 50) && skins.unlock(Skins.Skin.GOLD))
      announceUnlock(Skins.Skin.GOLD, 20);
    if ((score >= 1500 || level >= 100) && skins.unlock(Skins.Skin.PRIDE))
      announceUnlock(Skins.Skin.PRIDE, 20);
  }

  private void announceUnlock(Skins.Skin s, int ft) {
    flashTicks = ft;
    Sfx.play(Sfx.Id.LEVELUP);
    queueToast("New Skin: " + skins.label(s), Skins.Manager.accentFor(s));
  }

  /* --------------------------- Paint ------------------------ */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D gg = (Graphics2D) g.create();
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    gg.setColor(Config.BG);
    gg.fillRect(0, 0, getWidth(), getHeight());

    if (Config.SHOW_GRID) {
      gg.setColor(Config.GRID);
      for (int x = 0; x <= Config.COLS; x++)
        gg.drawLine(x * Config.TILE, 0, x * Config.TILE, Config.ROWS * Config.TILE);
      for (int y = 0; y <= Config.ROWS; y++)
        gg.drawLine(0, y * Config.TILE, Config.COLS * Config.TILE, y * Config.TILE);
    }

    gg.setColor(Config.WALL);
    for (int x = 0; x < Config.COLS; x++)
      for (int y = 0; y < Config.ROWS; y++)
        if (wall[x][y])
          gg.fillRoundRect(x * Config.TILE + 3, y * Config.TILE + 3, Config.TILE - 6, Config.TILE - 6, 6, 6);

    // apple
    gg.setColor(skins.appleColor());
    gg.fillOval(appleX * Config.TILE + 3, appleY * Config.TILE + 3, Config.TILE - 6, Config.TILE - 6);

    // bonus
    if (bonusActive) {
      int cx = bonusX * Config.TILE + Config.TILE / 2;
      int cy = bonusY * Config.TILE + Config.TILE / 2;
      int r = Config.TILE / 2 - 3;
      gg.setColor(Config.BONUS);
      Polygon diamond = new Polygon(
          new int[] { cx, cx + r, cx, cx - r },
          new int[] { cy - r, cy, cy + r, cy }, 4);
      gg.fillPolygon(diamond);
    }

    // snake
    for (int i = 0; i < length; i++) {
      boolean isHead = (i == 0);
      if (isHead) {
        if (System.nanoTime() < headFlashUntil)
          gg.setColor(Config.APPLE);
        else
          gg.setColor(skins.snakeHeadColor());
      } else {
        gg.setColor(skins.snakeBodyColor(i));
      }
      int x = sx[i] * Config.TILE, y = sy[i] * Config.TILE;
      gg.fillRoundRect(x + 2, y + 2, Config.TILE - 4, Config.TILE - 4, 6, 6);
    }

    // top HUD line
    gg.setColor(Config.TEXT);
    gg.setFont(getFont().deriveFont(Font.BOLD, 14f));
    String hud = String.format(
        "Mode: %s   Map: %s   Score: %d   High: %d   Speed: %dx%s",
        (mode == Mode.CLASSIC ? "Classic" : "Labyrinth"),
        Maps.activeName(),
        score,
        highScore,
        (speedIndex + 1),
        Sfx.isMuted() ? "  (Muted M)" : "");
    gg.drawString(hud, 10, 18);

    // overlays
    if (state == State.START) {
      drawStartScreen(gg);
    } else if (state == State.PAUSED) {
      gg.setColor(new Color(0, 0, 0, 120));
      gg.fillRect(0, 0, getWidth(), getHeight());
      drawCentered(gg, "PAUSED", 28, getHeight() / 2 - 10, true);
      drawCentered(gg, "Press Space to Resume", 16, getHeight() / 2 + 18, false);
    } else if (state == State.OVER) {
      gg.setColor(new Color(0, 0, 0, 120));
      gg.fillRect(0, 0, getWidth(), getHeight());
      drawCentered(gg, "GAME OVER", 28, getHeight() / 2 - 10, true);
      drawCentered(gg, "Press Space or R to Restart", 16, getHeight() / 2 + 18, false);
    }

    // bottom HUD
    paintHud(gg);
    paintToast(gg);
    gg.dispose();
  }

  private void drawStartScreen(Graphics2D g) {
    final int W = getWidth(), H = getHeight();

    // Background vignette
    g.setColor(new Color(0, 0, 0, 110));
    g.fillRect(0, 0, W, H);

    // Card (responsive: ~74% width, ~56% height, with sane clamps)
    int cardW = clamp((int) (W * 0.74), 560, W - 120);
    int cardH = clamp((int) (H * 0.56), 320, H - 220);
    int cardX = (W - cardW) / 2;
    int cardY = (H - cardH) / 2;

    // Glassy panel
    paintGlassPanel(g, cardX, cardY, cardW, cardH);

    // Left preview area (snake)
    int leftW = (cardW / 2) - 36;
    int leftH = cardH - 150;
    int leftX = cardX + 20;
    int leftY = cardY + 80;
    drawPreviewSnake(g, leftX, leftY, leftW, leftH);

    // Right preview area (apple) — opposite phase to the snake
    int rightW = (cardW / 2) - 36;
    int rightH = leftH;
    int rightX = cardX + cardW - rightW - 20;
    int rightY = leftY;

    // Animated title glow
    double t = (System.nanoTime() - bootNanos) / 1_000_000_000.0;
    float glow = (float) (0.6 + 0.4 * Math.sin(t * 2.2));
    g.setFont(getFont().deriveFont(Font.BOLD, 36f));
    drawNeonText(g, "NEON SNAKE", cardX + cardW / 2, cardY + 68, glow);

    // Mode / Skin / High score
    g.setFont(getFont().deriveFont(Font.PLAIN, 14f));
    g.setColor(new Color(0xE6E6FF));
    String line1 = "Mode: " + (mode == Mode.CLASSIC ? "Classic" : "Labyrinth") + "   (Tab to switch)";
    String line2 = "Skin: " + skins.label(skins.get()) + "   (X to cycle)";
    String line3 = "High Score: " + highScore;

    drawCenteredText(g, line1, cardX, cardY + 110, cardW);
    drawCenteredText(g, line2, cardX, cardY + 135, cardW);
    drawCenteredText(g, line3, cardX, cardY + 160, cardW);

    // Big Start pill
    drawPill(g, "Press  Enter  •  Space to start", cardX + 24, cardY + 190, cardW - 48, 36);

    // Controls row
    g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
    drawCenteredText(g, "Arrows/WASD move  •  P pause  •  R restart  •  M mute", cardX, cardY + 238, cardW);

    // Tiny bobbing apple
    int ax = cardX + cardW - 56, ay = cardY + 62 + (int) (Math.sin(t * 3.0) * 4);
    g.setColor(skins.appleColor());
    g.fillOval(ax, ay, 16, 16);

    String lineMap = "Map: " + Maps.activeName() + "   (T to switch)";
    drawCenteredText(g, lineMap, cardX, cardY + 95, cardW); // adjust Y if needed
  }

  private static int clamp(int v, int lo, int hi) {
    return Math.max(lo, Math.min(hi, v));
  }

  private void paintGlassPanel(Graphics2D g, int x, int y, int w, int h) {
    Graphics2D gg = (Graphics2D) g.create();
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    RoundRectangle2D.Float rr = new RoundRectangle2D.Float(x, y, w, h, 18, 18);
    gg.setPaint(new Color(20, 22, 34, 170));
    gg.fill(rr);
    gg.setStroke(new BasicStroke(2f));
    gg.setColor(new Color(255, 255, 255, 36));
    gg.draw(rr);
    gg.dispose();
  }

  private void drawNeonText(Graphics2D g, String text, int cx, int cy, float glow) {
    FontMetrics fm = g.getFontMetrics();
    int x = cx - fm.stringWidth(text) / 2;
    g.setColor(new Color(184, 132, 255, Math.min(255, (int) (140 * glow))));
    for (int r = 6; r >= 2; r -= 2) {
      g.setStroke(new BasicStroke(r));
      g.drawString(text, x, cy);
    }
    g.setColor(new Color(230, 230, 255));
    g.setStroke(new BasicStroke(1f));
    g.drawString(text, x, cy);
  }

  private void drawCenteredText(Graphics2D g, String s, int left, int y, int width) {
    FontMetrics fm = g.getFontMetrics();
    int x = left + (width - fm.stringWidth(s)) / 2;
    g.drawString(s, x, y);
  }

  private void drawPill(Graphics2D g, String label, int x, int y, int w, int h) {
    Graphics2D gg = (Graphics2D) g.create();
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    RoundRectangle2D.Float rr = new RoundRectangle2D.Float(x, y, w, h, h, h);
    gg.setPaint(new GradientPaint(x, y, new Color(90, 90, 110, 150), x, y + h, new Color(60, 60, 80, 150)));
    gg.fill(rr);
    gg.setColor(new Color(255, 255, 255, 70));
    gg.setStroke(new BasicStroke(2f));
    gg.draw(rr);
    gg.setFont(getFont().deriveFont(Font.BOLD, 14f));
    gg.setColor(new Color(230, 230, 255));
    FontMetrics fm = gg.getFontMetrics();
    int tx = x + (w - fm.stringWidth(label)) / 2;
    int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
    gg.drawString(label, tx, ty);
    gg.dispose();
  }

  private static Point pointOnRect(float t, int x, int y, int w, int h) {
    t = (t % 1f + 1f) % 1f;
    float per = 2f * (w + h);
    float d = t * per;
    if (d < w)
      return new Point(x + Math.round(d), y);
    d -= w;
    if (d < h)
      return new Point(x + w, y + Math.round(d));
    d -= h;
    if (d < w)
      return new Point(x + w - Math.round(d), y + h);
    d -= w;
    return new Point(x, y + h - Math.round(d));
  }

  private void drawPreviewSnake(Graphics2D g, int x, int y, int w, int h) {
    int r = Math.min(16, Math.max(10, Math.min(w, h) / 10));
    int pad = (int) Math.round(stroke / 2f + r / 2f);
    int rx = x + pad, ry = y + pad;
    int rw = Math.max(80, w - pad * 2), rh = Math.max(60, h - pad * 2);

    float t = (float) ((System.nanoTime() - bootNanos) / 1_000_000_000.0);
    float speed = 0.12f;
    float headT = (t * speed) % 1f;

    int segs = 12;
    float spacing = 0.045f;

    g.setColor(new Color(255, 255, 255, 22));
    g.drawRoundRect(rx, ry, rw, rh, 14, 14);

    for (int i = segs - 1; i >= 0; i--) {
      float ti = (headT - i * spacing + 1f) % 1f;
      Point p = pointOnRect(ti, rx, ry, rw, rh);
      g.setColor(i == 0 ? skins.snakeHeadColor() : skins.snakeBodyColor(i));
      g.fillRoundRect(p.x - r / 2, p.y - r / 2, r, r, 6, 6);
    }
  }

  private void paintHud(Graphics2D g) {
    final int HUD_H = 48;
    final int W = getWidth();
    final int Y = getHeight() - HUD_H;

    // right-aligned current skin label
    g.setFont(new Font("Consolas", Font.PLAIN, 12));
    g.setColor(Color.WHITE);
    String skinStr = "Skin: " + skins.label(skins.get());
    FontMetrics fm2 = g.getFontMetrics();
    g.drawString(skinStr, W - 16 - fm2.stringWidth(skinStr), Y + 30);

    // bg bar
    g.setColor(new Color(0, 0, 0, 140));
    g.fillRect(0, Y, W, HUD_H);

    // text
    g.setFont(new Font("Consolas", Font.BOLD, 18));
    g.setColor(Color.WHITE);
    g.drawString("LEVEL " + level, 16, Y + 30);

    int barW = 280, barH = 12;
    int barX = 130, barY = Y + 18;

    g.setColor(new Color(255, 255, 255, 60));
    g.drawRect(barX, barY, barW, barH);

    float pct = currentXpNeeded() > 0 ? Math.min(1f, xp / (float) currentXpNeeded()) : 1f;
    int fill = (int) (barW * pct);

    int glow = Math.max(0, Math.min(255, flashTicks * 12));
    g.setColor(new Color(127, 240, 255, 120 + glow / 3));
    g.fillRect(barX + 1, barY + 1, Math.max(0, fill - 1), barH - 1);

    g.setColor(Color.WHITE);
    g.setFont(new Font("Consolas", Font.PLAIN, 12));
    g.drawString(xp + " / " + currentXpNeeded() + " XP", barX + barW + 10, barY + barH);
    // --- Multiplier HUD (under XP bar) ---
    int mBarW = 120, mBarH = 8;
    int mBarX = barX; // align left with XP bar
    int mBarY = barY + barH + 6; // just below XP bar (fits in your 48px HUD)

    g.setColor(new Color(255, 255, 255, 60));
    g.drawRect(mBarX, mBarY, mBarW, mBarH);

    float mPct = (multTier <= 1) ? (multMeterMs / (float) MULT_METER_FULL_MS)
        : (multMeterMs / (float) MULT_METER_FULL_MS);
    int mFill = (int) (mBarW * Math.max(0f, Math.min(1f, mPct)));

    g.setColor(new Color(180, 240, 120, 160));
    g.fillRect(mBarX + 1, mBarY + 1, Math.max(0, mFill - 1), mBarH - 1);

    // label: xN
    g.setFont(new Font("Consolas", Font.BOLD, 14));
    g.setColor(Color.WHITE);
    g.drawString("x" + multTier, mBarX + mBarW + 10, mBarY + mBarH);

  }

  private void queueToast(String text, Color accent) {
    toastQ.add(new Toast(text, accent, 2600));
  }

  private void paintToast(Graphics2D g) {
    if (activeToast == null || activeToast.ageMs() >= activeToast.ms) {
      activeToast = toastQ.poll();
      if (activeToast == null)
        return;
    }
    final int age = activeToast.ageMs(), dur = activeToast.ms, FADE = 250;
    float alpha = (age < FADE) ? age / (float) FADE
        : (age > dur - FADE) ? Math.max(0f, (dur - age) / (float) FADE) : 1f;

    Graphics2D gg = (Graphics2D) g.create();
    gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gg.setComposite(AlphaComposite.SrcOver.derive(alpha));

    String text = activeToast.text;
    gg.setFont(getFont().deriveFont(Font.BOLD, 14f));
    FontMetrics fm = gg.getFontMetrics();

    int padX = 14, padY = 9;
    int w = fm.stringWidth(text) + padX * 2;
    int h = fm.getHeight() + padY * 2;
    int x = (getWidth() - w) / 2;
    int y = 52;

    gg.setColor(new Color(20, 22, 34, 200));
    gg.fillRoundRect(x, y, w, h, h, h);
    gg.setStroke(new BasicStroke(2f));
    gg.setColor(
        new Color(activeToast.accent.getRed(), activeToast.accent.getGreen(), activeToast.accent.getBlue(), 230));
    gg.drawRoundRect(x, y, w, h, h, h);

    gg.setColor(Color.WHITE);
    gg.drawString(text, x + padX, y + padY + fm.getAscent());
    gg.dispose();
  }

  private void drawCentered(Graphics2D g, String s, float size, int cy, boolean bold) {
    g.setColor(Config.TEXT);
    g.setFont(getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, size));
    FontMetrics fm = g.getFontMetrics();
    int w = fm.stringWidth(s);
    g.drawString(s, (getWidth() - w) / 2, cy);
  }

  /* ----------------------- Level / Maps --------------------- */

  private void addXp(int amount) {
    if (amount <= 0)
      return;
    xp += amount;

    // Handle multi-level jumps safely
    for (;;) {
      int need = currentXpNeeded();
      if (xp < need)
        break;
      xp -= need;
      level++;
      onLevelUp();
      maybeUnlockSkins();
    }
    saveProgress();
  }

  private void onLevelUp() {
    currentTickDelay = Math.max(TICK_FLOOR, timer.getDelay() - TICK_STEP);
    timer.setDelay(currentTickDelay);
    flashTicks = 12;
    Sfx.play(Sfx.Id.LEVELUP);
  }

  private void loadLabyrinthLevelFromTxt(String resourcePath) {
    LevelMap map = LevelMap.load(resourcePath);

    // 1) sanity: your Config size should match the map size
    if (map.w != Config.COLS || map.h != Config.ROWS) {
      throw new IllegalStateException("Map size " + map.w + "x" + map.h +
          " doesn't match Config " + Config.COLS + "x" + Config.ROWS);
    }

    // 2) copy walls/pellets into game grids
    for (int x = 0; x < Config.COLS; x++)
      for (int y = 0; y < Config.ROWS; y++)
        wall[x][y] = map.wall[x][y];

    // 3) choose a safe initial direction from spawn
    int sx0 = Math.max(0, Math.min(Config.COLS - 1, map.spawn.x));
    int sy0 = Math.max(0, Math.min(Config.ROWS - 1, map.spawn.y));
    Dir d = pickSafeDir(wall, sx0, sy0);
    if (d == null)
      d = Dir.RIGHT; // fallback

    // 4) snake head + body
    dir = d;
    sx[0] = sx0;
    sy[0] = sy0;
    int dx = (dir == Dir.RIGHT ? 1 : dir == Dir.LEFT ? -1 : 0);
    int dy = (dir == Dir.DOWN ? 1 : dir == Dir.UP ? -1 : 0);
    for (int i = 1; i < length; i++) {
      sx[i] = (sx[i - 1] - dx + Config.COLS) % Config.COLS;
      sy[i] = (sy[i - 1] - dy + Config.ROWS) % Config.ROWS;
    }
  }

  private Dir pickSafeDir(boolean[][] w, int x, int y) {
    if (free(w, (x + 1) % Config.COLS, y))
      return Dir.RIGHT;
    if (free(w, (x - 1 + Config.COLS) % Config.COLS, y))
      return Dir.LEFT;
    if (free(w, x, (y + 1) % Config.ROWS))
      return Dir.DOWN;
    if (free(w, x, (y - 1 + Config.ROWS) % Config.ROWS))
      return Dir.UP;
    return null;
  }

  private boolean free(boolean[][] w, int x, int y) {
    return !w[x][y];
  }

  /* --------------------- Start ticker callback --------------- */
  private void onStartTick(java.awt.event.ActionEvent evt) {
    if (state == State.START)
      repaint();
  }

  @SuppressWarnings("unused")
  private void showDevTools() {
    Window parent = SwingUtilities.getWindowAncestor(this);

    JPanel devPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gc = new GridBagConstraints();
    gc.insets = new Insets(6, 8, 6, 8);
    gc.anchor = GridBagConstraints.WEST;
    gc.gridx = 0;
    gc.gridy = 0;

    // --- Set Level ---
    gc.gridx = 0;
    gc.gridy++; // start a new row

    devPanel.add(new JLabel("Level:"), gc);

    // field (let it stretch)
    gc.gridx = 1;
    gc.weightx = 1.0;
    gc.fill = GridBagConstraints.HORIZONTAL;
    JTextField levelField = new JTextField(4);
    devPanel.add(levelField, gc);

    // small button (no stretch)
    gc.gridx = 2;
    gc.weightx = 0;
    gc.fill = GridBagConstraints.NONE;
    JButton setLevelBtn = new JButton("Set");
    setLevelBtn.addActionListener(e -> {
      try {
        int newLevel = Math.max(1, Integer.parseInt(levelField.getText().trim()));
        level = newLevel;
        xp = 0; // optional
        repaint();
      } catch (NumberFormatException ignore) {
      }
    });
    devPanel.add(setLevelBtn, gc);

    // >>> IMPORTANT: move to the NEXT ROW before Quick XP <<<
    gc.gridx = 0;
    gc.gridy++; // <— this was missing

    // --- Quick XP ---
    JButton xp10 = new JButton("+10 XP");
    xp10.addActionListener(e -> addXp(10));
    devPanel.add(xp10, gc);

    gc.gridx = 1;
    JButton xpToNext = new JButton("Level up");
    xpToNext.addActionListener(e -> addXp(Math.max(1, currentXpNeeded() - xp)));
    devPanel.add(xpToNext, gc);

    // --- Score field + Apply ---
    gc.gridx = 0;
    gc.gridy++;
    devPanel.add(new JLabel("Set score:"), gc);

    gc.gridx = 1;
    JTextField scoreIn = new JTextField(Integer.toString(score), 6);
    devPanel.add(scoreIn, gc);

    gc.gridx = 2;
    JButton setScore = new JButton("Apply");
    setScore.addActionListener(e -> {
      try {
        score = Math.max(0, Integer.parseInt(scoreIn.getText().trim()));
        repaint();
      } catch (NumberFormatException ignore) {
      }
    });
    devPanel.add(setScore, gc);

    // --- Force skin ---
    gc.gridx = 0;
    gc.gridy++;
    devPanel.add(new JLabel("Force skin:"), gc);

    gc.gridx = 1;
    JComboBox<Skins.Skin> skinBox = new JComboBox<>(Skins.Skin.values());
    skinBox.setSelectedItem(skins.get());
    devPanel.add(skinBox, gc);

    gc.gridx = 2;
    JButton setSkin = new JButton("Set");
    setSkin.addActionListener(e -> {
      skins.set((Skins.Skin) skinBox.getSelectedItem());
      repaint();
    });
    devPanel.add(setSkin, gc);

    // --- Unlock ALL skins ---
    gc.gridx = 0;
    gc.gridy++;
    gc.gridwidth = 3;
    JButton unlockAll = new JButton("Unlock ALL skins");
    unlockAll.addActionListener(e -> {
      for (Skins.Skin s : Skins.Skin.values())
        skins.unlock(s);
      queueToast("All skins unlocked (dev)", Color.WHITE);
    });
    devPanel.add(unlockAll, gc);
    gc.gridwidth = 1;

    // --- Invincible checkbox ---
    gc.gridx = 0;
    gc.gridy++;
    JCheckBox invChk = new JCheckBox("Invincible (ignore walls/body)");
    invChk.setSelected(devInvincible);
    devPanel.add(invChk, gc);

    // Show dialog
    int result = JOptionPane.showConfirmDialog(
        parent, devPanel, "Dev Tools",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
      devInvincible = invChk.isSelected();
    }
  }

  private void multiplierTick() {
    long now = System.currentTimeMillis();
    int dt = (int) Math.min(250, Math.max(0, now - multLastMs)); // clamp big hitches
    multLastMs = now;

    if (dt == 0)
      return;
    if (multMeterMs <= 0)
      return;

    multMeterMs -= dt;

    // If meter underflows, step down tiers gradually.
    while (multMeterMs < 0 && multTier > 1) {
      multTier--;
      multMeterMs += MULT_METER_FULL_MS; // carry underflow into the lower tier
    }
    if (multTier == 1 && multMeterMs < 0)
      multMeterMs = 0;
  }

  private void multiplierOnApple() {
    multMeterMs += MULT_GAIN_ON_APPLE_MS;

    while (multMeterMs >= MULT_METER_FULL_MS && multTier < MULT_MAX_TIER) {
      multMeterMs -= MULT_METER_FULL_MS;
      multTier++;
    }
    // Cap meter at full when at max tier
    if (multTier == MULT_MAX_TIER && multMeterMs > MULT_METER_FULL_MS) {
      multMeterMs = MULT_METER_FULL_MS;
    }
    multLastMs = System.currentTimeMillis();
  }

}
