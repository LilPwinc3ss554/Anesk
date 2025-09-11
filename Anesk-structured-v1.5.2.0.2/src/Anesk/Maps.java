package Anesk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Labyrinth layout loader & registry.
 * - Reads ASCII maps from classpath: /Anesk/maps/*.txt
 * - Optional /Anesk/maps/index.txt to enumerate files
 * - Dev fallback: loads .txt from a filesystem folder
 * - Activates one layout, fitted to Config.COLS x Config.ROWS
 */
public final class Maps {

  // ----------- Public data model -----------
  public static final class Layout {
    public final String id; // e.g. "lab-01"
    public final int cols, rows; // native map size
    public final boolean[][] wall; // wall[r][c] == true => wall present

    Layout(String id, int cols, int rows, boolean[][] wall) {
      this.id = id;
      this.cols = cols;
      this.rows = rows;
      this.wall = wall;
    }
  }

  // ----------- Registry -----------
  private static final Map<String, Layout> BY_ID = new LinkedHashMap<>();
  private static final Random RNG = new Random();
  private static boolean LOADED = false;

  // Currently active (fitted to Config grid)
  private static String ACTIVE_ID = null;
  private static boolean[][] ACTIVE_WALLS = null; // size: Config.ROWS x Config.COLS

  private Maps() {
  }

  // ----------- Public API -----------
  // --- Drop-in: map loader for labs/*.txt ---
  static final class MapDef {
    final int w, h;
    final String key; // lab-03
    final String title; // The BCB (Broken Circuit Board)
    final boolean[][] wall; // wall[y][x]
    final java.awt.Point spawn; // from 'S' (nullable)
    final java.awt.Point fruitHint; // from 'F' (nullable)

    MapDef(int w, int h, String key, String title, boolean[][] wall,
        java.awt.Point spawn, java.awt.Point fruitHint) {
      this.w = w;
      this.h = h;
      this.key = key;
      this.title = title;
      this.wall = wall;
      this.spawn = spawn;
      this.fruitHint = fruitHint;
    }
  }

  static MapDef loadLab(String file) {
    // file like "/labs/lab-03.txt"
    java.util.List<String> lines = new java.util.ArrayList<>();
    try (var in = Anesk.class.getResourceAsStream(file);
        var br = new java.io.BufferedReader(
            new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
      for (String s; (s = br.readLine()) != null;)
        lines.add(s);
    } catch (Exception e) {
      throw new RuntimeException("Missing " + file, e);
    }

    // header: "30x25 lab-03 - The BCB ..."
    String header = lines.get(0);
    var m = java.util.regex.Pattern
        .compile("\\s*(\\d+)x(\\d+)\\s+([\\w-]+)(?:\\s*-\\s*(.*))?")
        .matcher(header);
    if (!m.matches())
      throw new IllegalArgumentException("Bad header: " + header);
    int W = Integer.parseInt(m.group(1));
    int H = Integer.parseInt(m.group(2));
    String key = m.group(3);
    String title = m.group(4) == null ? "" : m.group(4);

    // next H lines are the grid
    boolean[][] wall = new boolean[H][W];
    java.awt.Point spawn = null, fruit = null;

    for (int y = 0; y < H; y++) {
      String row = lines.get(1 + y);
      if (row.length() < W)
        throw new IllegalArgumentException("Row " + y + " too short");
      for (int x = 0; x < W; x++) {
        char c = row.charAt(x);
        if (c == '#')
          wall[y][x] = true;
        else if (c == 'S')
          spawn = new java.awt.Point(x, y);
        else if (c == 'F')
          fruit = new java.awt.Point(x, y);
        // spaces and any other chars are passable
      }
    }
    return new MapDef(W, H, key, title, wall, spawn, fruit);
  }

  /** Ids you can list in menus or debug tools. */
  public static List<String> ids() {
    loadAll();
    return new ArrayList<>(BY_ID.keySet());
  }

  /** Get a stored layout by id (its native size, not fitted). */
  public static Layout get(String id) {
    loadAll();
    return BY_ID.get(id);
  }

  /** Pick a random stored layout (native). */
  public static Layout random() {
    loadAll();
    if (BY_ID.isEmpty())
      return null;
    int i = RNG.nextInt(BY_ID.size());
    return BY_ID.values().stream().skip(i).findFirst().orElse(null);
  }

  /** Activate a layout and fit it to Config grid. Returns true if success. */
  public static boolean activate(String id) {
    loadAll();
    Layout L = BY_ID.get(id);
    if (L == null)
      return false;
    ACTIVE_WALLS = fitToConfig(L);
    ACTIVE_ID = id;
    System.out.println("[Labyrinth] Activated '" + id + "' -> fitted to " + Config.COLS + "x" + Config.ROWS);
    return true;
  }

  /**
   * Ensure something is active (lab-01 if present, else first, else generated
   * arena).
   */
  public static void ensureActive() {
    if (ACTIVE_WALLS != null)
      return;
    loadAll();
    if (BY_ID.containsKey("lab-01")) {
      activate("lab-01");
    } else if (!BY_ID.isEmpty()) {
      activate(BY_ID.keySet().iterator().next());
    } else {
      // nothing found anywhere: generate a simple border arena sized to Config
      String id = "generated-arena";
      Layout arena = generatedArena(id, Config.COLS, Config.ROWS);
      BY_ID.put(id, arena);
      activate(id);
    }
  }

  /** Name of the active layout (or "(none)"). */
  public static String activeName() {
    return (ACTIVE_ID != null ? ACTIVE_ID : "(none)");
  }

  /** Query the fitted wall grid (safe even before activation). */
  public static boolean isWall(int col, int row) {
    ensureActive();
    if (col < 0 || col >= Config.COLS || row < 0 || row >= Config.ROWS)
      return false;
    return ACTIVE_WALLS[row][col];
  }

  /** Cycle to next layout id and activate it. Returns the new id (or null). */
  public static String next() {
    loadAll();
    if (BY_ID.isEmpty())
      return null;
    List<String> list = new ArrayList<>(BY_ID.keySet());
    int idx = (ACTIVE_ID == null) ? -1 : list.indexOf(ACTIVE_ID);
    String nid = list.get((idx + 1 + list.size()) % list.size());
    activate(nid);
    return nid;
  }

  // ----------- Loading -----------

  /** Preload everything once (idempotent). */
  public static void loadAll() {
    if (LOADED)
      return;

    // 1) Try to discover map files from a packaged index
    List<String> names = readIndexFromClasspath();

    // 2) If there is no index, fall back to a small fixed list you maintain in code
    if (names.isEmpty()) {
      names = List.of("lab-01.txt", "lab-02.txt", "lab-03.txt"); // keep in sync with your folder
    }

    // 3) Load each from classpath
    for (String filename : names) {
      Layout L = loadOneFromClasspath(filename);
      if (L != null)
        BY_ID.put(L.id, L);
    }

    // 4) Optional dev: also look on filesystem during source runs
    BY_ID.putAll(loadAllFromFilesystemFallback("admin/Anesk/Anesk/maps"));

    LOADED = true;
    System.out.println("[Labyrinth] Preloaded " + BY_ID.size() + " maps: " + BY_ID.keySet());
  }

  /**
   * Optional index to avoid hardcoding the list; stored at /Anesk/maps/index.txt
   */
  private static List<String> readIndexFromClasspath() {
    try (InputStream in = Maps.class.getResourceAsStream("/assets/labs/index.txt\"")) {
      if (in == null)
        return List.of();
      try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        return br.lines()
            .map(String::trim)
            .filter(s -> !s.isEmpty() && !s.startsWith(";") && s.endsWith(".txt"))
            .collect(Collectors.toList());
      }
    } catch (IOException ioe) {
      return List.of();
    }
  }

  private static Layout loadOneFromClasspath(String filename) {
    String path = "/assets/labs/" + filename;
    try (InputStream in = Maps.class.getResourceAsStream(path)) {
      if (in == null)
        return null;
      List<String> lines;
      try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        lines = br.lines().collect(Collectors.toList());
      }
      return parseAsciiGrid(filenameToId(filename), lines);
    } catch (IOException ioe) {
      return null;
    }
  }

  @SuppressWarnings("unused")
  private static Map<String, Layout> loadAllFromFilesystemFallback(String folderPath) {
    Map<String, Layout> found = new LinkedHashMap<>();
    File dir = new File(folderPath);
    File[] files = dir.isDirectory() ? dir.listFiles((d, n) -> n.endsWith(".txt")) : null;
    if (files == null)
      return found;

    for (File f : files) {
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
        List<String> lines = br.lines().collect(Collectors.toList());
        Layout L = parseAsciiGrid(filenameToId(f.getName()), lines);
        if (L != null)
          found.put(L.id, L);
      } catch (IOException ignored) {
      }
    }
    return found;
  }

  private static String filenameToId(String name) {
    int dot = name.lastIndexOf('.');
    return (dot > 0 ? name.substring(0, dot) : name).trim();
  }

  // ----------- Parsing & fitting -----------

  /** Parse rows of '#'/'.' into a boolean wall grid (keeps the native size). */
  private static Layout parseAsciiGrid(String id, List<String> raw) {
    // filter comments & blank lines; ignore a header like "30x25 example"
    List<String> rows = raw.stream()
        .map(Maps::rstrip)
        .filter(s -> !s.isEmpty() && !s.startsWith(";"))
        .filter(s -> looksLikeRow(s)) // ignore e.g. "30x25 example"
        .collect(Collectors.toList());
    if (rows.isEmpty())
      return null;

    int cols = rows.get(0).length();
    for (String r : rows) {
      if (r.length() != cols)
        throw new IllegalArgumentException("Inconsistent row width in map: " + id);
    }

    int R = rows.size();
    boolean[][] wall = new boolean[R][cols];
    for (int r = 0; r < R; r++) {
      String line = rows.get(r);
      for (int c = 0; c < cols; c++) {
        char ch = line.charAt(c);
        wall[r][c] = (ch == '#'); // anything else is empty ('.', 'f', spaces)
      }
    }
    return new Layout(id, cols, R, wall);
  }

  /**
   * Fit a native layout into Config grid by centering, cropping or padding with
   * empty.
   */
  private static boolean[][] fitToConfig(Layout L) {
    boolean[][] out = new boolean[Config.ROWS][Config.COLS];

    // center-align
    int offC = Math.max(0, (Config.COLS - L.cols) / 2);
    int offR = Math.max(0, (Config.ROWS - L.rows) / 2);

    for (int r = 0; r < Config.ROWS; r++) {
      int sr = r - offR;
      for (int c = 0; c < Config.COLS; c++) {
        int sc = c - offC;
        boolean v = (sr >= 0 && sr < L.rows && sc >= 0 && sc < L.cols) && L.wall[sr][sc];
        out[r][c] = v;
      }
    }
    return out;
  }

  // Simple generated arena with border walls (for emergencies)
  private static Layout generatedArena(String id, int cols, int rows) {
    boolean[][] w = new boolean[rows][cols];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        w[r][c] = (r == 0 || r == rows - 1 || c == 0 || c == cols - 1);
      }
    }
    return new Layout(id, cols, rows, w);
  }

  // Java doesn’t have rstrip on String; tiny helper:
  private static String rstrip(String s) {
    int i = s.length() - 1;
    while (i >= 0 && Character.isWhitespace(s.charAt(i)))
      i--;
    return s.substring(0, i + 1);
  }

  private static boolean looksLikeRow(String s) {
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if ("#.^v<>SF \t".indexOf(ch) == -1)
        return false; // allow gates + spawn/food
    }
    return s.length() >= 3;
  }

  public static String prev() {
    throw new UnsupportedOperationException("Unimplemented method 'prev'");
  }
}
