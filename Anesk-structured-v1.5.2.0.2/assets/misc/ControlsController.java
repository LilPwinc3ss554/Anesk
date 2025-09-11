package Anesk;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.Timer;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

public final class ControlsController {

    // --- tuning ---------------------------------------------------------
    private static final int POLL_HZ = 120; // polling rate
    private static final float AXIS_DEADZONE = 0.35f; // stick threshold
    private static final int HOLD_REFIRE_MS = 180; // min ms between repeats when holding a dir
    private static final float TRIGGER_THRESHOLD = 0.60f; // how far to press LT/RT to count
    private static final int TRIGGER_REFIRE_MS = 130; // cadence for speed up/down while held
    private static final String PREF_NAME_MATCH = "xbox";

    private final EnumMap<Controls.KeyAction, Runnable> actions;
    private final Timer timer;

    private Controller controller;
    // Axes / POV
    private Component pov;
    private Component axX, axY; // left stick
    private Component axZ, axRZ; // triggers (varies by driver)
    // Buttons
    private Component btnA, btnB, btnX, btnY, btnLB, btnRB, btnBack, btnStart, btnLS, btnRS;

    private final Map<Component, Boolean> btnPrev = new HashMap<>();

    // Direction edge/hold logic
    private int lastPOV = POV.CENTER;
    private int lastXAxis = 0; // -1,0,+1
    private int lastYAxis = 0; // -1,0,+1
    private int lastDirKey = 0; // POV.* dir last fired
    private long lastDirTimeMs = 0;

    // Trigger cadence
    private long lastLtMs = 0;
    private long lastRtMs = 0;

    public ControlsController(Map<Controls.KeyAction, Runnable> a) {
        this.actions = new EnumMap<>(Controls.KeyAction.class);
        if (a != null)
            this.actions.putAll(a);

        this.timer = new Timer(1000 / POLL_HZ, e -> poll());
        this.timer.setCoalesce(true);
    }

    // --- API ------------------------------------------------------------
    public void start() {
        if (controller == null) {
            controller = findPreferredController();
            if (controller != null)
                wireComponents(controller);
        }
        if (controller != null)
            timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public boolean isActive() {
        return timer.isRunning() && controller != null;
    }

    // --- polling --------------------------------------------------------
    private void poll() {
        if (controller == null)
            return;
        if (!controller.poll()) {
            controller = null;
            timer.stop();
            return;
        }

        long now = System.currentTimeMillis();
        int requestedDir = 0; // one of POV.UP/DOWN/LEFT/RIGHT, or 0

        // 1) D-Pad (POV) with diagonal collapse (UP/DOWN priority)
        if (pov != null) {
            int dir = POV.from(pov.getPollData());
            if (dir != lastPOV)
                lastPOV = dir;
            requestedDir = mergeDir(requestedDir, dir);
        }

        // 2) Left stick -> digital 4-way with deadzone (vertical priority)
        if (axX != null || axY != null) {
            int x = 0, y = 0;
            if (axX != null) {
                float v = axX.getPollData();
                x = (Math.abs(v) < AXIS_DEADZONE) ? 0 : (v < 0 ? -1 : +1);
                if (x != lastXAxis)
                    lastXAxis = x;
            }
            if (axY != null) {
                float v = axY.getPollData();
                y = (Math.abs(v) < AXIS_DEADZONE) ? 0 : (v < 0 ? -1 : +1); // negative is UP on many drivers
                if (y != lastYAxis)
                    lastYAxis = y;
            }
            int stickDir = 0;
            if (y != 0)
                stickDir = (y < 0) ? POV.UP : POV.DOWN;
            else if (x != 0)
                stickDir = (x < 0) ? POV.LEFT : POV.RIGHT;
            requestedDir = mergeDir(requestedDir, stickDir);
        }

        // 3) Fire direction with edge/hold cadence
        if (requestedDir != 0) {
            if (requestedDir != lastDirKey || (now - lastDirTimeMs) >= HOLD_REFIRE_MS) {
                fireDir(requestedDir);
                lastDirKey = requestedDir;
                lastDirTimeMs = now;
            }
        } else {
            lastDirKey = 0;
        }

        // 4) Buttons (edge-triggered)
        edge(btnA, Controls.KeyAction.START_OR_PAUSE); // A = quick start
        edge(btnStart, Controls.KeyAction.PAUSE_TOGGLE); // Menu/Start = main pause/unpause
        edge(btnBack, Controls.KeyAction.RESET_PROGRESS); // View/Back = reset

        edge(btnLB, Controls.KeyAction.MAP_PREV); // LB = labs prev
        edge(btnRB, Controls.KeyAction.MAP_NEXT); // RB = labs next

        edge(btnX, Controls.KeyAction.SKIN_CYCLE); // X = skin cycle
        edge(btnY, Controls.KeyAction.MODE_TOGGLE); // Y = (free for now, keep as mode toggle)

        // Optional: bind others later for powerups
        // edge(btnB, Controls.KeyAction.<SOMETHING>);
        // edge(btnRS, Controls.KeyAction.MUTE_TOGGLE);
        // edge(btnLS, Controls.KeyAction.DEV_TOOLS);

        // 5) Triggers -> speed control (rate-limited while held)
        handleTriggers(now);
    }

    private void handleTriggers(long now) {
        // Common case: separate LT/RT on Z and RZ
        if (axZ != null && axRZ != null) {
            float lt = axZ.getPollData();
            float rt = axRZ.getPollData();
            if (lt > TRIGGER_THRESHOLD && (now - lastLtMs) >= TRIGGER_REFIRE_MS) {
                press(Controls.KeyAction.SPEED_DOWN);
                lastLtMs = now;
            }
            if (rt > TRIGGER_THRESHOLD && (now - lastRtMs) >= TRIGGER_REFIRE_MS) {
                press(Controls.KeyAction.SPEED_UP);
                lastRtMs = now;
            }
            return;
        }

        // Fallback: some drivers expose both triggers on a single axis (Z)
        if (axZ != null) {
            float z = axZ.getPollData(); // often -1..+1 where one trigger is negative, the other positive
            if (z < -TRIGGER_THRESHOLD && (now - lastLtMs) >= TRIGGER_REFIRE_MS) {
                press(Controls.KeyAction.SPEED_DOWN); // treat negative as LT
                lastLtMs = now;
            } else if (z > TRIGGER_THRESHOLD && (now - lastRtMs) >= TRIGGER_REFIRE_MS) {
                press(Controls.KeyAction.SPEED_UP); // treat positive as RT
                lastRtMs = now;
            }
        }
    }

    // --- helpers --------------------------------------------------------
    private static int mergeDir(int base, int add) {
        if (add == 0)
            return base;
        if (base == 0)
            return add;
        // If both vertical and horizontal requested at once, prefer vertical.
        if ((base == POV.LEFT || base == POV.RIGHT) && (add == POV.UP || add == POV.DOWN))
            return add;
        return base;
    }

    private void fireDir(int dir) {
        switch (dir) {
            case POV.UP -> press(Controls.KeyAction.UP);
            case POV.DOWN -> press(Controls.KeyAction.DOWN);
            case POV.LEFT -> press(Controls.KeyAction.LEFT);
            case POV.RIGHT -> press(Controls.KeyAction.RIGHT);
            default -> {
            }
        }
    }

    private void press(Controls.KeyAction a) {
        Runnable r = actions.get(a);
        if (r != null)
            r.run();
    }

    private void edge(Component c, Controls.KeyAction a) {
        if (c == null)
            return;
        boolean now = c.getPollData() > 0.5f; // buttons are usually 0.0 or 1.0
        boolean was = btnPrev.getOrDefault(c, false);
        if (now && !was)
            press(a);
        btnPrev.put(c, now);
    }

    private Controller findPreferredController() {
        ControllerEnvironment ce = ControllerEnvironment.getDefaultEnvironment();
        Controller fallback = null;
        for (Controller c : ce.getControllers()) {
            Controller.Type t = c.getType();
            if (t != Controller.Type.GAMEPAD && t != Controller.Type.STICK)
                continue;
            String name = (c.getName() == null) ? "" : c.getName().toLowerCase();
            if (name.contains(PREF_NAME_MATCH))
                return c; // prefer Xbox
            if (fallback == null)
                fallback = c;
        }
        return fallback;
    }

    private void wireComponents(Controller c) {
        pov = axX = axY = axZ = axRZ = null;
        btnA = btnB = btnX = btnY = btnLB = btnRB = btnBack = btnStart = btnLS = btnRS = null;

        btnPrev.clear();
        lastPOV = POV.CENTER;
        lastXAxis = lastYAxis = 0;
        lastDirKey = 0;
        lastDirTimeMs = 0;
        lastLtMs = lastRtMs = 0;

        for (Component comp : c.getComponents()) {
            var id = comp.getIdentifier();
            String nm = id.getName();
            if (Objects.equals(id, Component.Identifier.Axis.POV)) {
                pov = comp;
                continue;
            }
            if (Objects.equals(id, Component.Identifier.Axis.X)) {
                axX = comp;
                continue;
            }
            if (Objects.equals(id, Component.Identifier.Axis.Y)) {
                axY = comp;
                continue;
            }
            if (Objects.equals(id, Component.Identifier.Axis.Z)) {
                axZ = comp;
                continue;
            }
            if (Objects.equals(id, Component.Identifier.Axis.RZ)) {
                axRZ = comp;
                continue;
            }

            if (nm == null)
                continue;
            // Typical Xbox button indices (can vary slightly by driver)
            switch (nm) {
                case "0" -> btnA = comp; // A
                case "1" -> btnB = comp; // B
                case "2" -> btnX = comp; // X
                case "3" -> btnY = comp; // Y
                case "4" -> btnLB = comp; // LB
                case "5" -> btnRB = comp; // RB
                case "6" -> btnBack = comp; // View/Back (reset)
                case "7" -> btnStart = comp; // Menu/Start (pause)
                case "8" -> btnLS = comp; // L3
                case "9" -> btnRS = comp; // R3
                default -> {
                }
            }
        }
    }

    private static final class POV {
        static final int CENTER = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4;

        static int from(float f) {
            if (f == Component.POV.CENTER)
                return CENTER;
            if (f == Component.POV.UP)
                return UP;
            if (f == Component.POV.DOWN)
                return DOWN;
            if (f == Component.POV.LEFT)
                return LEFT;
            if (f == Component.POV.RIGHT)
                return RIGHT;
            // Collapse diagonals to vertical (classic Snake feel)
            if (f == Component.POV.UP_LEFT || f == Component.POV.UP_RIGHT)
                return UP;
            if (f == Component.POV.DOWN_LEFT || f == Component.POV.DOWN_RIGHT)
                return DOWN;
            return CENTER;
        }
    }
}
