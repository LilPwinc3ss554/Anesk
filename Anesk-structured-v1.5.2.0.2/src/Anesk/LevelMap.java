package Anesk;

import java.awt.Point;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Loads fixed ASCII labyrinths like assets/misc/lab-01.txt */
public final class LevelMap {
    public final int w, h;
    public final boolean[][] wall; // wall[x][y]
    public final boolean[][] pellet; // pellet[x][y]
    public final Point spawn; // snake start
    public final List<Point> foods; // optional preferred food tiles

    private LevelMap(int w, int h, boolean[][] wall, boolean[][] pellet, Point spawn, List<Point> foods) {
        this.w = w;
        this.h = h;
        this.wall = wall;
        this.pellet = pellet;
        this.spawn = spawn;
        this.foods = foods;
    }

    public static LevelMap load(String resourcePath) {
        // 1) Try classpath (inside JAR or /bin/classes)
        InputStream in = LevelMap.class.getResourceAsStream(resourcePath);
        if (in == null) {
            // 2) Fallback to file path during loose dev runs
            try {
                in = new FileInputStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("Level not found on classpath or disk: " + resourcePath);
            }
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            // First non-empty line may be "WxH" like "30x25"; comments allowed after a '#'
            String line;
            int w = -1, h = -1;
            List<String> rows = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String raw = line;
                // allow "comment title" lines that start with ';' or "//"
                if (raw.trim().isEmpty() || raw.trim().startsWith(";") || raw.trim().startsWith("//"))
                    continue;

                // header "30x25" or "25x30" etc.
                String head = raw.trim();
                if (w < 0 && h < 0 && head.matches("\\d+\\s*[xX]\\s*\\d+.*")) {
                    String[] parts = head.split("[xX]");
                    w = Integer.parseInt(parts[0].trim());
                    // strip any trailing words after height
                    String rest = parts[1].trim().split("\\s+")[0];
                    h = Integer.parseInt(rest);
                    continue;
                }

                // grid line (keep exactly as-is, don't trim end spaces)
                rows.add(raw);
            }

            if (rows.isEmpty())
                throw new IllegalStateException("No grid rows in " + resourcePath);

            // If header missing, infer
            if (w < 0 || h < 0) {
                h = rows.size();
                w = rows.stream().mapToInt(String::length).max().orElse(0);
            }

            boolean[][] wall = new boolean[w][h];
            boolean[][] pellet = new boolean[w][h];
            Point spawn = null;
            List<Point> foods = new ArrayList<>();

            for (int y = 0; y < h; y++) {
                String r = (y < rows.size()) ? rows.get(y) : "";
                for (int x = 0; x < w; x++) {
                    char c = (x < r.length()) ? r.charAt(x) : ' ';
                    switch (c) {
                        case '#':
                            wall[x][y] = true;
                            break;
                        case '.':
                            pellet[x][y] = true;
                            break;
                        case 'S':
                            if (spawn == null)
                                spawn = new Point(x, y);
                            break;
                        case 'F':
                            foods.add(new Point(x, y));
                            break;
                        case '_': // fall-through
                        case ' ': // empty
                        default: // ignore unknowns
                    }
                }
            }

            // Fallback spawn if none supplied: center-ish empty tile
            if (spawn == null) {
                outer: for (int y = h / 2 - 1; y <= h / 2 + 1; y++) {
                    for (int x = w / 2 - 1; x <= w / 2 + 1; x++) {
                        if (x >= 0 && y >= 0 && x < w && y < h && !wall[x][y]) {
                            spawn = new Point(x, y);
                            break outer;
                        }
                    }
                }
                if (spawn == null)
                    spawn = new Point(1, 1);
            }

            return new LevelMap(w, h, wall, pellet, spawn, foods);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + resourcePath, e);
        }
    }
}