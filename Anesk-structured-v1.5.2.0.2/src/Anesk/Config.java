package Anesk;

import java.awt.Color;

public final class Config {
  // Board
  public static final int TILE = 26;
  public static final int COLS = 30;
  public static final int ROWS = 25;

  // Game
  public static final int[] SPEEDS_MS = { 130, 100, 75, 55 };
  public static final int START_SPEED_INDEX = 1;
  public static final boolean WRAP_WALLS = true;
  public static final boolean SHOW_GRID = true;

  // Bonus shard
  public static final int BONUS_POINTS = 40;
  public static final int BONUS_LIFE_TICKS = 120;
  public static final double BONUS_SPAWN_CHANCE = 0.35;

  // Theme
  public static final Color BG = new Color(0x0b0b12);
  public static final Color GRID = new Color(0x222230);
  public static final Color SNAKE = new Color(0x8c00ff);
  public static final Color SNAKE_HEAD = new Color(0xb884ff);
  public static final Color APPLE = new Color(0xff2bbf);
  public static final Color BONUS = new Color(0x2bffea);
  public static final Color WALL = new Color(0x303046);
  public static final Color TEXT = new Color(0xe6e6ff);

  // SFX
  public static final boolean SOUND_ON = true;
  public static final float MASTER_DB = -6f;

  private Config() {}
}
