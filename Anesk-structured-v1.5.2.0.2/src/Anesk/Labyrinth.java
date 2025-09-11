package Anesk;

import java.util.*;

public final class Labyrinth {
  private Labyrinth() {
  }

  /** Build a random "obstacles" arena (not a maze). */
  public static void build(boolean[][] wall, int cols, int rows) {
    build(wall, cols, rows, System.nanoTime());
  }

  public static void build(boolean[][] wall, int cols, int rows, long seed) {
    clear(wall, cols, rows);
    Random rng = new Random(seed);

    // ---- tuning knobs (feel free to tweak)
    double targetFill = 0.14; // ~14% of cells become walls
    int minSegLen = 2, maxSegLen = 6; // short bars
    int boxTries = (cols * rows) / 90; // attempts to place small boxes
    int plusTries = (cols * rows) / 120;// attempts to place plus shapes
    boolean keepBorderEmpty = true; // nicer for wrap or player flow

    // optional soft border clearance (1 cell margin)
    if (keepBorderEmpty) {
      for (int x = 0; x < cols; x++) {
        wall[x][0] = wall[x][rows - 1] = false;
      }
      for (int y = 0; y < rows; y++) {
        wall[0][y] = wall[cols - 1][y] = false;
      }
    }

    int area = cols * rows;
    int maxWalls = (int) (area * targetFill);
    int placed = 0;

    // ---- scatter singles to create lots of turning points
    int singles = Math.max(12, area / 140);
    for (int i = 0; i < singles && placed < maxWalls; i++) {
      int x = rng.nextInt(cols), y = rng.nextInt(rows);
      if (skipEdge(x, y, cols, rows, keepBorderEmpty))
        continue;
      if (!wall[x][y]) {
        wall[x][y] = true;
        placed++;
      }
    }

    // ---- short horizontal/vertical segments
    int segs = Math.max(14, area / 110);
    for (int i = 0; i < segs && placed < maxWalls; i++) {
      boolean vertical = rng.nextBoolean();
      int len = rng.nextInt(maxSegLen - minSegLen + 1) + minSegLen;
      int x = rng.nextInt(cols), y = rng.nextInt(rows);
      for (int k = 0; k < len && placed < maxWalls; k++) {
        int cx = (x + (vertical ? 0 : k));
        int cy = (y + (vertical ? k : 0));
        if (cx < 0 || cy < 0 || cx >= cols || cy >= rows)
          break;
        if (skipEdge(cx, cy, cols, rows, keepBorderEmpty))
          continue;
        if (!wall[cx][cy]) {
          wall[cx][cy] = true;
          placed++;
        }
      }
    }

    // ---- small boxes (2x2..4x3, etc.)
    for (int i = 0; i < boxTries && placed < maxWalls; i++) {
      int bw = rng.nextInt(3) + 2; // 2..4
      int bh = rng.nextInt(2) + 2; // 2..3
      int x = rng.nextInt(cols - bw);
      int y = rng.nextInt(rows - bh);
      boolean ok = true;
      for (int cx = x; cx < x + bw && ok; cx++)
        for (int cy = y; cy < y + bh; cy++)
          if (skipEdge(cx, cy, cols, rows, keepBorderEmpty)) {
            ok = false;
            break;
          }
      if (!ok)
        continue;
      for (int cx = x; cx < x + bw && placed < maxWalls; cx++)
        for (int cy = y; cy < y + bh && placed < maxWalls; cy++)
          if (!wall[cx][cy]) {
            wall[cx][cy] = true;
            placed++;
          }
      // cut a doorway so we don't make full cages
      int doorSide = rng.nextInt(4);
      switch (doorSide) {
        case 0 -> wall[x + rng.nextInt(bw)][Math.max(0, y - 1) + y] = false;
        case 1 -> wall[x + rng.nextInt(bw)][y + bh - 1] = false;
        case 2 -> wall[x][y + rng.nextInt(bh)] = false;
        default -> wall[x + bw - 1][y + rng.nextInt(bh)] = false;
      }
    }

    // ---- plus shapes
    for (int i = 0; i < plusTries && placed < maxWalls; i++) {
      int x = 1 + rng.nextInt(Math.max(1, cols - 2));
      int y = 1 + rng.nextInt(Math.max(1, rows - 2));
      if (skipEdge(x, y, cols, rows, keepBorderEmpty))
        continue;
      placed += putIfFree(wall, x, y);
      placed += putIfFree(wall, x + 1, y);
      placed += putIfFree(wall, x - 1, y);
      placed += putIfFree(wall, x, y + 1);
      placed += putIfFree(wall, x, y - 1);
    }

    // ---- soften: punch a handful of "gates" to reduce dead zones
    int gates = Math.max(8, area / 100);
    for (int i = 0; i < gates; i++) {
      int x = rng.nextInt(cols), y = rng.nextInt(rows);
      if (!wall[x][y])
        continue;
      int n = neighbors(wall, cols, rows, x, y);
      if (n >= 3)
        wall[x][y] = false; // open tight clusters
    }
  }

  /**
   * Pick a random free cell AND a safe initial direction (neighbor also free).
   */
  public static int[] randomSafeSpawn(boolean[][] wall, int cols, int rows, long seed) {
    Random rng = new Random(seed);
    for (int tries = 0; tries < 500; tries++) {
      int x = rng.nextInt(cols), y = rng.nextInt(rows);
      if (wall[x][y])
        continue;
      int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
      // shuffle
      for (int i = 0; i < dirs.length; i++) {
        int j = rng.nextInt(dirs.length);
        int[] t = dirs[i];
        dirs[i] = dirs[j];
        dirs[j] = t;
      }
      for (int[] d : dirs) {
        int nx = (x + d[0] + cols) % cols;
        int ny = (y + d[1] + rows) % rows;
        if (!wall[nx][ny])
          return new int[] { x, y, dirIndex(d[0], d[1]) };
      }
    }
    return new int[] { cols / 2, rows / 2, 0 };
  }

  // -------------------- helpers --------------------

  /** Clear (no walls). */
  public static void clear(boolean[][] wall, int cols, int rows) {
    for (int x = 0; x < cols; x++)
      java.util.Arrays.fill(wall[x], false);
  }

  private static boolean skipEdge(int x, int y, int cols, int rows, boolean keepBorderEmpty) {
    if (!keepBorderEmpty)
      return false;
    return (x == 0 || y == 0 || x == cols - 1 || y == rows - 1);
  }

  private static int putIfFree(boolean[][] wall, int x, int y) {
    if (x < 0 || y < 0 || x >= wall.length || y >= wall[0].length)
      return 0;
    if (!wall[x][y]) {
      wall[x][y] = true;
      return 1;
    }
    return 0;
  }

  private static int neighbors(boolean[][] wall, int cols, int rows, int x, int y) {
    int n = 0;
    if (x > 0 && wall[x - 1][y])
      n++;
    if (x < cols - 1 && wall[x + 1][y])
      n++;
    if (y > 0 && wall[x][y - 1])
      n++;
    if (y < rows - 1 && wall[x][y + 1])
      n++;
    return n;
  }

  private static int dirIndex(int dx, int dy) {
    if (dx == 1 && dy == 0)
      return 0; // RIGHT
    if (dx == -1 && dy == 0)
      return 1; // LEFT
    if (dx == 0 && dy == 1)
      return 2; // DOWN
    return 3; // UP
  }

  public static void preload() {
    // Load and cache all labyrinth maps once at startup
    Maps.loadAll();

    // Optional: log what was loaded for debugging
    System.out.println("[Labyrinth] Preloaded " + Maps.ids().size() + " maps: " + Maps.ids());
  }

  public static LevelMap loadFromFile(String resourcePath) {
    LevelMap map = LevelMap.load(resourcePath);

    // Optional debug
    System.out.printf("[Labyrinth] Loaded %dx%d map from %s%n", map.w, map.h, resourcePath);

    return map;
  }
}
