package Anesk;

import javax.swing.*;
import java.awt.event.*;
import java.util.EnumMap;
import java.util.Map;

/** Lightweight key binding helper with enum + map. */
public final class Controls {
  private Controls() {
  }

  /** All bindable actions. (Renamed to avoid clash with javax.swing.Action) */
  public enum KeyAction {
    LEFT, RIGHT, UP, DOWN,
    START_OR_PAUSE, PAUSE_TOGGLE, RESTART,
    MUTE_TOGGLE, SPEED_UP, SPEED_DOWN,
    MODE_TOGGLE, SKIN_CYCLE,
    RESET_PROGRESS, DEV_TOOLS,
    MAP_NEXT, MAP_PREV,
    BACK_TO_START
  }

  private static final Runnable NOOP = () -> {
  };

  public static EnumMap<KeyAction, Runnable> actions() {
    return new EnumMap<>(KeyAction.class);
  }

  /** Bind keys to actions. Missing actions are treated as NOOP. */
  public static void bind(JComponent c, Map<KeyAction, Runnable> a) {
    if (a == null)
      a = Map.of();
    InputMap im = c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap am = c.getActionMap();

    // --- directional aliases (arrows + WASD)
    map(im, am, "LEFT", a.getOrDefault(KeyAction.LEFT, NOOP));
    map(im, am, "A", a.getOrDefault(KeyAction.LEFT, NOOP));

    map(im, am, "RIGHT", a.getOrDefault(KeyAction.RIGHT, NOOP));
    map(im, am, "D", a.getOrDefault(KeyAction.RIGHT, NOOP));

    map(im, am, "UP", a.getOrDefault(KeyAction.UP, NOOP));
    map(im, am, "W", a.getOrDefault(KeyAction.UP, NOOP));

    map(im, am, "DOWN", a.getOrDefault(KeyAction.DOWN, NOOP));
    map(im, am, "S", a.getOrDefault(KeyAction.DOWN, NOOP));

    // --- core controls
    map(im, am, "SPACE", a.getOrDefault(KeyAction.START_OR_PAUSE, NOOP));
    map(im, am, "ENTER", a.getOrDefault(KeyAction.START_OR_PAUSE, NOOP));

    map(im, am, "P", a.getOrDefault(KeyAction.PAUSE_TOGGLE, NOOP));
    map(im, am, "R", a.getOrDefault(KeyAction.RESTART, NOOP));

    map(im, am, "M", a.getOrDefault(KeyAction.MUTE_TOGGLE, NOOP));
    map(im, am, "C", a.getOrDefault(KeyAction.SPEED_UP, NOOP));
    map(im, am, "Z", a.getOrDefault(KeyAction.SPEED_DOWN, NOOP));

    map(im, am, "TAB", a.getOrDefault(KeyAction.MODE_TOGGLE, NOOP));
    map(im, am, "X", a.getOrDefault(KeyAction.SKIN_CYCLE, NOOP));

    // RESET: Ctrl+Shift+R
    KeyStroke resetKs = KeyStroke.getKeyStroke(KeyEvent.VK_R,
        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    im.put(resetKs, "RESET_PROGRESS");
    am.put("RESET_PROGRESS", swingAct(a.getOrDefault(KeyAction.RESET_PROGRESS, NOOP)));

    // DEV TOOLS: Ctrl+Shift+D
    KeyStroke devKs = KeyStroke.getKeyStroke(KeyEvent.VK_D,
        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    im.put(devKs, "DEV_TOOLS");
    am.put("DEV_TOOLS", swingAct(a.getOrDefault(KeyAction.DEV_TOOLS, NOOP)));

    // MAP CYCLE: next = T, prev = Shift+T
    map(im, am, "T", a.getOrDefault(KeyAction.MAP_NEXT, NOOP));
    KeyStroke prev = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.SHIFT_DOWN_MASK);
    im.put(prev, "MAP_PREV");
    am.put("MAP_PREV", swingAct(a.getOrDefault(KeyAction.MAP_PREV, NOOP)));

    // BACK TO START: ESC
    map(im, am, "ESCAPE", a.getOrDefault(KeyAction.BACK_TO_START, NOOP));
  }

  // --- helpers -------------------------------------------------------

  private static void map(InputMap im, ActionMap am, String key, Runnable r) {
    im.put(KeyStroke.getKeyStroke(key), key);
    am.put(key, swingAct(r)); // expects javax.swing.Action
  }

  private static javax.swing.Action swingAct(Runnable r) {
    return new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        if (r != null)
          r.run();
      }
    };
  }
}
