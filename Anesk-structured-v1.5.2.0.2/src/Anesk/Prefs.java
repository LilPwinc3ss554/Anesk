package Anesk;

import java.util.prefs.Preferences;

public final class Prefs {
  private Prefs() {}
  private static Preferences prefs;

  /** Call once on startup BEFORE using any getters. */
  public static void init() {
    if (prefs == null) {
      // Use a stable node name so prefs survive refactors/packages.
      prefs = Preferences.userRoot().node("Anesk");
    }

    // Seed defaults once
    if (prefs.get("skin", null) == null) {
      prefs.put("skin", Skins.Skin.MAIN.name()); // default skin
    }
    if (prefs.get("sound", null) == null) {
      prefs.putBoolean("sound", true);           // sound on by default
    }
  }

  // --- Skin (enum-backed) ---
  public static Skins.Skin getSkin() {
    ensureInit();
    String v = prefs.get("skin", Skins.Skin.MAIN.name());
    try {
      return Skins.Skin.valueOf(v);
    } catch (IllegalArgumentException ex) {
      return Skins.Skin.MAIN; // fallback if someone saved an unknown string
    }
  }

  public static void setSkin(Skins.Skin skin) {
    ensureInit();
    prefs.put("skin", skin.name());
  }

  // --- Sound ---
  public static boolean isSoundOn() {
    ensureInit();
    return prefs.getBoolean("sound", true);
  }

  public static void setSoundOn(boolean on) {
    ensureInit();
    prefs.putBoolean("sound", on);
  }

  // --- helper ---
  private static void ensureInit() {
    if (prefs == null) init();
  }
}
