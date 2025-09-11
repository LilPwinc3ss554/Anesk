package Anesk;

import java.awt.Color;
import java.util.prefs.Preferences;

/**
 * Skins — enum + color palettes + persisted unlocks/selection.
 * Use Skins.Manager inside your game to query/set skins & colors.
 */
public final class Skins {

  // -------------------- Public enum --------------------
  public enum Skin {
    MAIN, CRIMSON, ORANGE, LIME, EMERALD, TEAL, CYAN, COBALT, VIOLET, PINK, MIDNIGHT, RAINBOW, TRANS, SOLAR, SILVER,
    GOLD,
    LESBIAN, BI, PAN, NONBINARY, ASEXUAL, AROMANTIC, GENDERFLUID, INTERSEX, PRIDE
  }

  // -------------------- Pref keys ----------------------
  private static final String PREF_SKIN_SELECTED = "anesksnake.skin";

  private static final String PREF_SKIN_RAINBOW = "anesksnake.skin.rainbow";
  private static final String PREF_SKIN_TRANS = "anesksnake.skin.trans";
  private static final String PREF_SKIN_SOLAR = "anesksnake.skin.solar";
  private static final String PREF_SKIN_SILVER = "anesksnake.skin.silver";
  private static final String PREF_SKIN_GOLD = "anesksnake.skin.gold";

  private static final String PREF_SKIN_LESBIAN = "anesksnake.skin.lesbian";
  private static final String PREF_SKIN_BI = "anesksnake.skin.bi";
  private static final String PREF_SKIN_PAN = "anesksnake.skin.pan";
  private static final String PREF_SKIN_NONBINARY = "anesksnake.skin.nonbinary";
  private static final String PREF_SKIN_ASEXUAL = "anesksnake.skin.asexual";
  private static final String PREF_SKIN_AROMANTIC = "anesksnake.skin.aromantic";
  private static final String PREF_SKIN_GENDERFLUID = "anesksnake.skin.genderfluid";
  private static final String PREF_SKIN_INTERSEX = "anesksnake.skin.intersex";
  private static final String PREF_SKIN_PRIDE = "anesksnake.skin.pride";

  private Skins() {
  }

  // -------------------- Manager ------------------------
  public static final class Manager {
    public Skin nextUnlocked(int step) { // step = +1 (next) or -1 (previous)
      Skin[] all = Skin.values();
      int n = all.length;
      int i = skin.ordinal();
      for (int k = 1; k <= n; k++) {
        int j = Math.floorMod(i + step * k, n);
        if (isUnlocked(all[j]))
          return all[j];
      }
      return skin; // fallback
    }

    public void cycle(int step) {
      set(nextUnlocked(step));
    }

    private final Preferences prefs;

    private Skin skin = Skin.MAIN;

    // unlock flags
    private boolean uRainbow, uTrans, uSolar, uSilver, uGold, uLesbian, uBi, uPan, uNonbinary, uAsexual, uAromantic,
        uGenderfluid, uIntersex, uPride;

    public Manager(Preferences prefs) {
      this.prefs = prefs;
      load();
    }

    // -------- selection ----------
    public Skin get() {
      return skin;
    }

    /** Attempts to select; ignored if locked. */
    public void set(Skin s) {
      if (isUnlocked(s)) {
        skin = s;
        saveSelected();
      } else {
        // let caller play a "nope" sound if they want
      }
    }

    public boolean isUnlocked(Skin s) {
      return switch (s) {
        case MAIN -> true;
        // always-on bases:
        case CRIMSON, ORANGE, LIME, EMERALD, TEAL, CYAN, COBALT, VIOLET, PINK, MIDNIGHT -> true;

        // existing gated skins:
        case RAINBOW -> uRainbow;
        case TRANS -> uTrans;
        case SOLAR -> uSolar;
        case SILVER -> uSilver;
        case GOLD -> uGold;
        case LESBIAN -> uLesbian;
        case BI -> uBi;
        case PAN -> uPan;
        case NONBINARY -> uNonbinary;
        case ASEXUAL -> uAsexual;
        case AROMANTIC -> uAromantic;
        case GENDERFLUID -> uGenderfluid;
        case INTERSEX -> uIntersex;
        case PRIDE -> uPride;
      };
    }

    /** Unlock and persist; returns true if it was newly unlocked. */
    public boolean unlock(Skin s) {
      boolean before = isUnlocked(s);
      if (before)
        return false;
      switch (s) {
        case RAINBOW -> {
          uRainbow = true;
          prefs.putBoolean(PREF_SKIN_RAINBOW, true);
        }
        case TRANS -> {
          uTrans = true;
          prefs.putBoolean(PREF_SKIN_TRANS, true);
        }
        case SOLAR -> {
          uSolar = true;
          prefs.putBoolean(PREF_SKIN_SOLAR, true);
        }
        case SILVER -> {
          uSilver = true;
          prefs.putBoolean(PREF_SKIN_SILVER, true);
        }
        case GOLD -> {
          uGold = true;
          prefs.putBoolean(PREF_SKIN_GOLD, true);
        }
        case LESBIAN -> {
          uLesbian = true;
          prefs.putBoolean(PREF_SKIN_LESBIAN, true);
        }
        case BI -> {
          uBi = true;
          prefs.putBoolean(PREF_SKIN_BI, true);
        }
        case PAN -> {
          uPan = true;
          prefs.putBoolean(PREF_SKIN_PAN, true);
        }
        case NONBINARY -> {
          uNonbinary = true;
          prefs.putBoolean(PREF_SKIN_NONBINARY, true);
        }
        case ASEXUAL -> {
          uAsexual = true;
          prefs.putBoolean(PREF_SKIN_ASEXUAL, true);
        }
        case AROMANTIC -> {
          uAromantic = true;
          prefs.putBoolean(PREF_SKIN_AROMANTIC, true);
        }
        case GENDERFLUID -> {
          uGenderfluid = true;
          prefs.putBoolean(PREF_SKIN_GENDERFLUID, true);
        }
        case INTERSEX -> {
          uIntersex = true;
          prefs.putBoolean(PREF_SKIN_INTERSEX, true);
        }
        case PRIDE -> {
          uPride = true;
          prefs.putBoolean(PREF_SKIN_PRIDE, true);
        }

        case CRIMSON, ORANGE, LIME, EMERALD, TEAL, CYAN, COBALT, VIOLET, PINK, MIDNIGHT, MAIN -> {
          /* no-op */ }
      }
      return true;
    }

    /** Human-readable label, with "(locked)" when not unlocked. */
    public String label(Skin s) {
      String base = switch (s) {
        case MAIN -> "Neon";
        case RAINBOW -> "Rainbow";
        case TRANS -> "Trans Pride";
        case SOLAR -> "Solar";
        case SILVER -> "Silver";
        case GOLD -> "Gold";
        case LESBIAN -> "Lesbian";
        case BI -> "Bi";
        case PAN -> "Pan";
        case NONBINARY -> "Nonbinary";
        case ASEXUAL -> "Asexual";
        case AROMANTIC -> "Aromantic";
        case GENDERFLUID -> "Genderfluid";
        case INTERSEX -> "Intersex";
        case PRIDE -> "Pride";

        case CRIMSON -> "Crimson";
        case ORANGE -> "Orange";
        case LIME -> "Lime";
        case EMERALD -> "Emerald";
        case TEAL -> "Teal";
        case CYAN -> "Cyan";
        case COBALT -> "Cobalt";
        case VIOLET -> "Violet";
        case PINK -> "Pink";
        case MIDNIGHT -> "Midnight";
      };
      return isUnlocked(s) ? base : base + " (locked)";
    }

    // -------- colors (API mirrors your old methods) --------

    public static Color accentFor(Skin s) {
      return switch (s) {
        case RAINBOW -> Color.WHITE;
        case SOLAR -> new Color(0xEF7701);
        case SILVER -> new Color(0xC0C0C0);
        case GOLD -> new Color(0xFFE177);

        // pride
        case TRANS -> new Color(0xFFFFFF);
        case LESBIAN -> new Color(0xFF9A56);
        case BI -> new Color(0x9B4F96);
        case PAN -> new Color(0xFFD800);
        case NONBINARY -> new Color(0x9C59D1);
        case ASEXUAL -> new Color(0x800080);
        case AROMANTIC -> new Color(0x3DA542);
        case GENDERFLUID -> new Color(0xBE18D6);
        case INTERSEX -> new Color(0x7A00AC);
        case PRIDE -> new Color(0xE40303);

        // new bases (accent = lighter of base)
        case CRIMSON -> lighten(c(0xE53935), 0.35f);
        case ORANGE -> lighten(c(0xFF8C00), 0.25f);
        case LIME -> lighten(c(0xB8FF00), 0.15f);
        case EMERALD -> lighten(c(0x2ECC71), 0.20f);
        case TEAL -> lighten(c(0x14B8A6), 0.20f);
        case CYAN -> lighten(c(0x00E5FF), 0.10f);
        case COBALT -> lighten(c(0x0047AB), 0.35f);
        case VIOLET -> lighten(c(0x7C4DFF), 0.25f);
        case PINK -> lighten(c(0xFF5DA8), 0.15f);
        case MIDNIGHT -> lighten(c(0x0B1226), 0.50f);
        default -> new Color(0xB884FF);
      };
    }

    public Color appleColor() {
      return appleColor(skin);
    }

    public static Color appleColor(Skin skin) {
      switch (skin) {
        case TRANS -> {
          Color[] pal = { c(0x5BCFFA), c(0xF5A9B8), c(0xFFFFFF), c(0xF5A9B8), c(0x5BCFFA) };
          return cyclePalette(pal, 2.0);
        }
        case RAINBOW -> {
          float base = (System.nanoTime() / 1_000_000_000f) * 0.15f;
          float h = (base % 1f);
          return Color.getHSBColor(h, 0.9f, 1f);
        }
        case LESBIAN -> {
          Color[] pal = { c(0xD52D00), c(0xFF9A56), c(0xFFFFFF), c(0xD362A4), c(0xA30262) };
          return cyclePalette(pal, 2.2);
        }
        case BI -> {
          Color[] pal = { c(0xD60270), c(0xD60270), c(0x9B4F96), c(0x0038A8), c(0x0038A8) };
          return cyclePalette(pal, 2.0);
        }
        case PAN -> {
          Color[] pal = { c(0xFF218C), c(0xFFD800), c(0x21B1FF) };
          return cyclePalette(pal, 1.8);
        }
        case NONBINARY -> {
          Color[] pal = { c(0xFCF434), c(0xFFFFFF), c(0x9C59D1), c(0x2C2C2C) };
          return cyclePalette(pal, 2.2);
        }
        case ASEXUAL -> {
          Color[] pal = { c(0x000000), c(0xA4A4A4), c(0xFFFFFF), c(0x800080) };
          return cyclePalette(pal, 2.0);
        }
        case AROMANTIC -> {
          Color[] pal = { c(0x3DA542), c(0xA7D379), c(0xFFFFFF), c(0xA9A9A9), c(0x000000) };
          return cyclePalette(pal, 2.4);
        }
        case GENDERFLUID -> {
          Color[] pal = { c(0xFF75A2), c(0xFFFFFF), c(0xBE18D6), c(0x000000), c(0x333EB4) };
          return cyclePalette(pal, 2.4);
        }
        case INTERSEX -> {
          Color[] pal = { c(0xFFDD00), c(0x7A00AC) };
          return cyclePalette(pal, 1.4);
        }
        case SOLAR -> {
          return c(0xFFB200);
        }
        case SILVER -> {
          return c(0xEDEDED);
        }
        case GOLD -> {
          float t = (System.nanoTime() / 1_000_000_000f) * 0.45f;
          float b = 0.88f + 0.10f * (float) Math.sin(t * (float) Math.PI * 2);
          return Color.getHSBColor(0.12f, 0.85f, b);
        }
        case PRIDE -> {
          Color[] pal = { c(0xE40303), c(0xFF8C00), c(0xFFED00), c(0x008026), c(0x004DFF), c(0x750787) };
          return cyclePalette(pal, 2.4);
        }

        // New bases: subtle shimmer between dark -> base -> light
        case CRIMSON -> {
          Color b = c(0xE53935);
          return cyclePalette(new Color[] { darken(b, 0.25f), b, lighten(b, 0.22f) }, 2.0);
        }
        case ORANGE -> {
          Color b = c(0xFF8C00);
          return cyclePalette(new Color[] { darken(b, 0.20f), b, lighten(b, 0.18f) }, 1.9);
        }
        case LIME -> {
          Color b = c(0xB8FF00);
          return cyclePalette(new Color[] { darken(b, 0.15f), b, lighten(b, 0.15f) }, 1.8);
        }
        case EMERALD -> {
          Color b = c(0x2ECC71);
          return cyclePalette(new Color[] { darken(b, 0.18f), b, lighten(b, 0.20f) }, 2.1);
        }
        case TEAL -> {
          Color b = c(0x14B8A6);
          return cyclePalette(new Color[] { darken(b, 0.18f), b, lighten(b, 0.18f) }, 2.0);
        }
        case CYAN -> {
          Color b = c(0x00E5FF);
          return cyclePalette(new Color[] { darken(b, 0.12f), b, lighten(b, 0.12f) }, 1.7);
        }
        case COBALT -> {
          Color b = c(0x0047AB);
          return cyclePalette(new Color[] { darken(b, 0.20f), b, lighten(b, 0.25f) }, 2.2);
        }
        case VIOLET -> {
          Color b = c(0x7C4DFF);
          return cyclePalette(new Color[] { darken(b, 0.18f), b, lighten(b, 0.22f) }, 2.0);
        }
        case PINK -> {
          Color b = c(0xFF5DA8);
          return cyclePalette(new Color[] { darken(b, 0.12f), b, lighten(b, 0.18f) }, 1.9);
        }
        case MIDNIGHT -> {
          Color b = c(0x0B1226);
          return cyclePalette(new Color[] { b, lighten(b, 0.20f), b }, 2.3);
        }

        default -> {
          return c(0xFF2BBF);
        } // MAIN fallback
      }
    }

    public Color snakeBodyColor(int idx) {
      return snakeBodyColor(skin, idx);
    }

    public static Color snakeBodyColor(Skin skin, int idx) {
      switch (skin) {
        case RAINBOW -> {
          float base = (System.nanoTime() / 1_000_000_000f) * 0.12f;
          float h = (base + idx * 0.06f) % 1f;
          return Color.getHSBColor(h, 0.85f, 1f);
        }
        case TRANS -> {
          Color[] bands = { c(0x5bcffa), c(0xf5a9b8), c(0xffffff), c(0xf5a9b8), c(0x5bcffa) };
          return bands[Math.floorMod(idx, bands.length)];
        }
        case SOLAR -> {
          float t = (System.nanoTime() / 1_000_000_000f) * 0.10f;
          float h = 0.10f + 0.055f * (float) Math.sin((t + idx * 0.18f) * Math.PI * 2);
          return Color.getHSBColor(h, 0.95f, 1f);
        }
        case SILVER -> {
          float t = (System.nanoTime() / 1_000_000_000f) * 0.5f;
          float b = 0.75f + 0.20f * (float) Math.sin((t + idx * 0.15f) * Math.PI * 2);
          return Color.getHSBColor(0f, 0f, b);
        }
        case GOLD -> {
          float t = (System.nanoTime() / 1_000_000_000f) * 0.55f;
          float b = 0.78f + 0.18f * (float) Math.sin((t + idx * 0.12f) * (float) Math.PI * 2);
          return Color.getHSBColor(0.12f, 0.85f, b);
        }
        case LESBIAN -> {
          Color[] b = { c(0xD52D00), c(0xFF9A56), c(0xFFFFFF), c(0xD362A4), c(0xA30262) };
          return b[Math.floorMod(idx, b.length)];
        }
        case BI -> {
          Color[] b = { c(0xD60270), c(0xD60270), c(0x9B4F96), c(0x0038A8), c(0x0038A8) };
          return b[Math.floorMod(idx, b.length)];
        }
        case PAN -> {
          Color[] b = { c(0xFF218C), c(0xFFD800), c(0x21B1FF) };
          return b[Math.floorMod(idx, b.length)];
        }
        case NONBINARY -> {
          Color[] b = { c(0xFCF434), c(0xFFFFFF), c(0x9C59D1), c(0x2C2C2C) };
          return b[Math.floorMod(idx, b.length)];
        }
        case ASEXUAL -> {
          Color[] b = { c(0x000000), c(0xA4A4A4), c(0xFFFFFF), c(0x800080) };
          return b[Math.floorMod(idx, b.length)];
        }
        case AROMANTIC -> {
          Color[] b = { c(0x3DA542), c(0xA7D379), c(0xFFFFFF), c(0xA9A9A9), c(0x000000) };
          return b[Math.floorMod(idx, b.length)];
        }
        case GENDERFLUID -> {
          Color[] b = { c(0xFF75A2), c(0xFFFFFF), c(0xBE18D6), c(0x000000), c(0x333EB4) };
          return b[Math.floorMod(idx, b.length)];
        }
        case INTERSEX -> {
          Color[] b = { c(0xFFDD00), c(0x7A00AC) };
          return b[Math.floorMod(idx, b.length)];
        }
        case PRIDE -> {
          Color[] b = { c(0xE40303), c(0xFF8C00), c(0xFFED00), c(0x008026), c(0x004DFF), c(0x750787) };
          return b[Math.floorMod(idx, b.length)];
        }

        // New bases: repeating dark -> base -> light -> base bands
        case CRIMSON -> {
          Color base = c(0xE53935);
          Color[] b = { darken(base, 0.25f), base, lighten(base, 0.20f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case ORANGE -> {
          Color base = c(0xFF8C00);
          Color[] b = { darken(base, 0.20f), base, lighten(base, 0.18f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case LIME -> {
          Color base = c(0xB8FF00);
          Color[] b = { darken(base, 0.15f), base, lighten(base, 0.15f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case EMERALD -> {
          Color base = c(0x2ECC71);
          Color[] b = { darken(base, 0.18f), base, lighten(base, 0.20f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case TEAL -> {
          Color base = c(0x14B8A6);
          Color[] b = { darken(base, 0.18f), base, lighten(base, 0.18f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case CYAN -> {
          Color base = c(0x00E5FF);
          Color[] b = { darken(base, 0.12f), base, lighten(base, 0.12f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case COBALT -> {
          Color base = c(0x0047AB);
          Color[] b = { darken(base, 0.20f), base, lighten(base, 0.25f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case VIOLET -> {
          Color base = c(0x7C4DFF);
          Color[] b = { darken(base, 0.18f), base, lighten(base, 0.22f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case PINK -> {
          Color base = c(0xFF5DA8);
          Color[] b = { darken(base, 0.12f), base, lighten(base, 0.18f), base };
          return b[Math.floorMod(idx, b.length)];
        }
        case MIDNIGHT -> {
          Color base = c(0x0B1226);
          Color[] b = { base, lighten(base, 0.20f), base, darken(base, 0.10f) };
          return b[Math.floorMod(idx, b.length)];
        }

        default -> {
          return c(0x8c00ff);
        } // MAIN
      }
    }

    public Color snakeHeadColor() {
      return snakeHeadColor(skin);
    }

    public static Color snakeHeadColor(Skin skin) {
      return switch (skin) {
        case RAINBOW -> Color.WHITE;
        case TRANS -> c(0xffffff);
        case SOLAR -> c(0xef7701);
        case SILVER -> c(0xc0c0c0);
        case GOLD -> c(0xFFE177);
        case LESBIAN -> c(0xFF9A56);
        case BI -> c(0x9B4F96);
        case PAN -> c(0xFFD800);
        case NONBINARY -> c(0x9C59D1);
        case ASEXUAL -> c(0x800080);
        case AROMANTIC -> c(0x3DA542);
        case GENDERFLUID -> c(0xBE18D6);
        case INTERSEX -> c(0x7A00AC);
        case PRIDE -> c(0xE40303);

        case CRIMSON -> lighten(c(0xE53935), 0.28f);
        case ORANGE -> lighten(c(0xFF8C00), 0.22f);
        case LIME -> lighten(c(0xB8FF00), 0.22f);
        case EMERALD -> lighten(c(0x2ECC71), 0.25f);
        case TEAL -> lighten(c(0x14B8A6), 0.25f);
        case CYAN -> lighten(c(0x00E5FF), 0.18f);
        case COBALT -> lighten(c(0x0047AB), 0.35f);
        case VIOLET -> lighten(c(0x7C4DFF), 0.28f);
        case PINK -> lighten(c(0xFF5DA8), 0.20f);
        case MIDNIGHT -> lighten(c(0x0B1226), 0.45f);

        default -> c(0xb884ff);
      };
    }

    // -------- persistence ----------
    public void load() {
      int sel = prefs.getInt(PREF_SKIN_SELECTED, 0);
      skin = (sel >= 0 && sel < Skin.values().length) ? Skin.values()[sel] : Skin.MAIN;

      uRainbow = prefs.getBoolean(PREF_SKIN_RAINBOW, false);
      uTrans = prefs.getBoolean(PREF_SKIN_TRANS, false);
      uSolar = prefs.getBoolean(PREF_SKIN_SOLAR, false);
      uSilver = prefs.getBoolean(PREF_SKIN_SILVER, false);
      uGold = prefs.getBoolean(PREF_SKIN_GOLD, false);
      uLesbian = prefs.getBoolean(PREF_SKIN_LESBIAN, false);
      uBi = prefs.getBoolean(PREF_SKIN_BI, false);
      uPan = prefs.getBoolean(PREF_SKIN_PAN, false);
      uNonbinary = prefs.getBoolean(PREF_SKIN_NONBINARY, false);
      uAsexual = prefs.getBoolean(PREF_SKIN_ASEXUAL, false);
      uAromantic = prefs.getBoolean(PREF_SKIN_AROMANTIC, false);
      uGenderfluid = prefs.getBoolean(PREF_SKIN_GENDERFLUID, false);
      uIntersex = prefs.getBoolean(PREF_SKIN_INTERSEX, false);
      uPride = prefs.getBoolean(PREF_SKIN_PRIDE, false);

      if (!isUnlocked(skin))
        skin = Skin.MAIN;
    }

    public void saveSelected() {
      prefs.putInt(PREF_SKIN_SELECTED, skin.ordinal());
    }
  }

  // -------------------- color helpers --------------------
  private static Color c(int hex) {
    return new Color(hex);
  }

  private static Color lerp(Color a, Color b, float t) {
    t = Math.max(0f, Math.min(1f, t));
    int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
    int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
    int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
    return new Color(r, g, bl);
  }

  private static float smooth01(float x) {
    x = Math.max(0f, Math.min(1f, x));
    return 0.5f - 0.5f * (float) Math.cos(x * Math.PI);
  }

  private static Color cyclePalette(Color[] pal, double secondsPerLoop) {
    if (pal == null || pal.length == 0)
      return c(0xff2bbf);
    double t = (System.nanoTime() / 1_000_000_000.0) / secondsPerLoop;
    double pos = t * pal.length;
    int i = (int) Math.floor(pos) % pal.length;
    int j = (i + 1) % pal.length;
    float k = smooth01((float) (pos - Math.floor(pos)));
    return lerp(pal[i], pal[j], k);
  }

  private static Color lighten(Color base, float amt) {
    return lerp(base, Color.WHITE, amt);
  }

  private static Color darken(Color base, float amt) {
    return lerp(base, Color.BLACK, amt);
  }

  private static final Object SKINS_LOCK = new Object();
  private static boolean SKINS_LOADED = false;

  public static void loadAll() {
    synchronized (SKINS_LOCK) {
      if (SKINS_LOADED)
        return;

      SKINS_LOADED = true;
    }
  }

  public static Color accentFor(Skin emerald) {
    throw new UnsupportedOperationException("Unimplemented method 'accentFor'");
  }

  public static Color appleColor(Skin crimson) {
    throw new UnsupportedOperationException("Unimplemented method 'appleColor'");
  }
}
