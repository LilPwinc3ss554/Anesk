package Anesk;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class Sfx {
  public enum Id {
    PICKUP, LEVELUP, GAMEOVER, TURN, START, PAUSE, RESUME, SPEED, BONUS
  }

  private static boolean enabled = Config.SOUND_ON;
  private static final float MASTER_DB = Config.MASTER_DB;

  private static final class ClipPool {
    private final List<Clip> clips = new ArrayList<>();
    private int next = 0;

    ClipPool(String resourcePath, int voices) {
      for (int i = 0; i < voices; i++) {
        Clip c = loadClip(resourcePath);
        if (c != null)
          clips.add(c);
      }
    }

    void play() {
      if (!enabled || clips.isEmpty())
        return;
      try {
        Clip c = clips.get(next);
        next = (next + 1) % clips.size();
        if (c.isRunning())
          c.stop();
        c.setFramePosition(0);
        setGainDb(c, MASTER_DB + (float) (Math.random() * 1.5 - 0.75f)); // ±0.75 dB
        c.start();
      } catch (Exception ignored) {
      }
    }

    void warmup() {
      for (Clip c : clips) { // assuming you already keep an array of clips
        if (c == null)
          continue;
        try {
          // mute, nudge the mixer, then restore
          int oldPos = c.getFramePosition();
          setGainDb(c, -80f); // hard mute (your Sfx.setGainDb helper)
          c.setFramePosition(0);
          c.start();
          c.stop();
          c.flush();
          c.setFramePosition(oldPos);
        } catch (Exception ignore) {
          // don't fail startup just because one device/clip is quirky
        }
      }
    }

  }

  private static final Map<Id, ClipPool> pools = new EnumMap<>(Id.class);
  private static boolean initialized = false;

  public static void preload() {
    init(); // builds pools if not already
    for (ClipPool p : pools.values()) {
      p.warmup();
    }
  }

  public static void init() {
    if (initialized)
      return;
    initialized = true;
    pools.put(Id.PICKUP, new ClipPool("pickup.wav", 6));
    pools.put(Id.LEVELUP, new ClipPool("levelup.wav", 3));
    pools.put(Id.GAMEOVER, new ClipPool("gameover.wav", 2));
    pools.put(Id.TURN, new ClipPool("turn.wav", 4));
    pools.put(Id.START, new ClipPool("start.wav", 2));
    pools.put(Id.PAUSE, new ClipPool("pause.wav", 2));
    pools.put(Id.RESUME, new ClipPool("resume.wav", 2));
    pools.put(Id.SPEED, new ClipPool("speed.wav", 2));
    pools.put(Id.BONUS, new ClipPool("bonus.wav", 2));
  }

  public static void play(Id id) {
    ClipPool p = pools.get(id);
    if (p != null)
      p.play();
  }

  public static void toggleMute() {
    enabled = !enabled;
  }

  public static boolean isMuted() {
    return !enabled;
  }

  // --------- helpers ---------
  private static Clip loadClip(String name) {
    // classpath candidates (use Sfx.class to decouple from Anesk)
    String[] candidates = new String[] {
        "/assets/sounds/" + name,
        "/sounds/" + name,
        "/assets/sounds/" + name
    };
    for (String path : candidates) {
      try {
        var url = Sfx.class.getResource(path);
        if (url == null)
          continue;
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(url.openStream()))) {
          Clip clip = AudioSystem.getClip();
          clip.open(ais);
          setGainDb(clip, MASTER_DB);
          System.out.println("[SFX] loaded " + path);
          return clip;
        }
      } catch (Exception ignore) {
      }
    }

    // last-chance filesystem for dev runs
    String[] fs = { "Anesk/sounds/" + name, "sounds/" + name, "assets/sounds/" + name };
    for (String p : fs) {
      try {
        File f = new File(p);
        if (!f.exists())
          continue;
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(f)) {
          Clip clip = AudioSystem.getClip();
          clip.open(ais);
          setGainDb(clip, MASTER_DB);
          System.out.println("[SFX] loaded file " + f.getPath());
          return clip;
        }
      } catch (Exception ignore) {
      }
    }

    System.out.println("[SFX] NOT FOUND: " + name);
    return null;
  }

  private static void setGainDb(Clip c, float db) {
    if (c.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
      FloatControl gain = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
      db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
      gain.setValue(db);
    }
  }

  private Sfx() {
  }
}
