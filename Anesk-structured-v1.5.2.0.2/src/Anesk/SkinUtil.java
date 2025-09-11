package Anesk;

import java.awt.Color;
import java.util.List;

/**
 * High-level skin pack selector.
 * - CLASSIC: retro snake colors
 * - PRIDE: cycles through pride palettes
 * - EXTRA: misc packs (currently SOLAR)
 */
public final class SkinUtil {

  public enum Id {
    CLASSIC, PRIDE, EXTRA
  }

  private static Id CURRENT = Id.CLASSIC;

  private static Color CURRENT_SNAKE_COLOR;
  private static Color CURRENT_APPLE_COLOR;
  private static List<Color> CURRENT_SNAKE_PALETTE; // if non-null, use per-segment palette
  private static String CURRENT_SPRITES;

  private SkinUtil() {
  }

  public static void init() {
    apply(CURRENT);
  }

  public static Id get() {
    return CURRENT;
  }

  public static void set(Id id) {
    if (id == null)
      return;
    CURRENT = id;
    apply(id);
  }

  public static Color snakeColor() {
    return CURRENT_SNAKE_COLOR;
  }

  public static Color appleColor() {
    return CURRENT_APPLE_COLOR;
  }

  public static List<Color> snakePalette() {
    return CURRENT_SNAKE_PALETTE;
  }

  public static String spritesRoot() {
    return CURRENT_SPRITES;
  }

  private static void apply(Id id) {
    switch (id) {

      case CLASSIC -> {
        // Option A (simple classic): single green snake, red apple
        CURRENT_SNAKE_COLOR = Skins.accentFor(Skins.Skin.EMERALD);
        CURRENT_APPLE_COLOR = Skins.appleColor(Skins.Skin.CRIMSON);
        CURRENT_SNAKE_PALETTE = null;
        CURRENT_SPRITES = "/assets/skins/classic/";

        // --- If you prefer multi-color classic (OPTION B), replace the four lines
        // above with:
        // CURRENT_SNAKE_PALETTE = List.of(
        // Skins.accentFor(Skins.Skin.ORANGE),
        // Skins.accentFor(Skins.Skin.LIME),
        // Skins.accentFor(Skins.Skin.CRIMSON),
        // Skins.accentFor(Skins.Skin.TEAL),
        // Skins.accentFor(Skins.Skin.CYAN),
        // Skins.accentFor(Skins.Skin.COBALT),
        // Skins.accentFor(Skins.Skin.VIOLET),
        // Skins.accentFor(Skins.Skin.PINK),
        // Skins.accentFor(Skins.Skin.MIDNIGHT)
        // );
        // CURRENT_SNAKE_COLOR = null; // renderer uses palette
        // CURRENT_APPLE_COLOR = Skins.appleColor(Skins.Skin.CRIMSON);
        // CURRENT_SPRITES = "/assets/skins/classic/";
      }

      case PRIDE -> {
        // cycle segments through multiple pride palettes
        CURRENT_SNAKE_PALETTE = List.of(
            Skins.accentFor(Skins.Skin.RAINBOW),
            Skins.accentFor(Skins.Skin.TRANS),
            Skins.accentFor(Skins.Skin.LESBIAN),
            Skins.accentFor(Skins.Skin.BI),
            Skins.accentFor(Skins.Skin.PAN),
            Skins.accentFor(Skins.Skin.NONBINARY),
            Skins.accentFor(Skins.Skin.ASEXUAL),
            Skins.accentFor(Skins.Skin.AROMANTIC),
            Skins.accentFor(Skins.Skin.GENDERFLUID),
            Skins.accentFor(Skins.Skin.INTERSEX));
        CURRENT_SNAKE_COLOR = null; // renderer should use palette instead
        CURRENT_APPLE_COLOR = Skins.appleColor(Skins.Skin.PRIDE);
        CURRENT_SPRITES = "/assets/skins/pride/";
      }

      case EXTRA -> {
        // for now: SOLAR lives here
        CURRENT_SNAKE_COLOR = Skins.accentFor(Skins.Skin.SOLAR);
        CURRENT_APPLE_COLOR = Skins.appleColor(Skins.Skin.ORANGE);
        CURRENT_SNAKE_PALETTE = null;
        CURRENT_SPRITES = "/assets/skins/extra/";
      }
    }
  }
}
