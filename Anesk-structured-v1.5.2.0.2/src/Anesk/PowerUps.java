package Anesk;

import java.util.*;

public final class PowerUps {

    public enum Type {
        MULLIGAN, PHASE_WALLS
    }

    public static final int INVENTORY_CAP = 5;
    public static final int PHASE_MOVES = 3; // how many moves phase lasts
    public static final int OVERFLOW_SCORE = 100;

    /** Inventory of up to 5 power-ups (includes Mulligans). */
    public static final class Inventory {
        private final List<Type> slots = new ArrayList<>(INVENTORY_CAP);
        private int selected = 0; // index into slots (0..size-1)

        /** Try to add; returns true if added, false if full. */
        public boolean add(Type t) {
            if (slots.size() >= INVENTORY_CAP)
                return false;
            slots.add(t);
            // keep selection stable
            return true;
        }

        /** Use/consume the selected slot. Returns the Type used or null if none. */
        public Type useSelected() {
            if (slots.isEmpty())
                return null;
            Type t = slots.remove(selected);
            if (selected >= slots.size())
                selected = Math.max(0, slots.size() - 1);
            return t;
        }

        /** Remove first occurrence of given type; returns true if removed. */
        public boolean consumeFirst(Type t) {
            int i = slots.indexOf(t);
            if (i >= 0) {
                slots.remove(i);
                if (selected >= slots.size())
                    selected = Math.max(0, slots.size() - 1);
                return true;
            }
            return false;
        }

        public void cycleNext() {
            if (slots.isEmpty())
                return;
            selected = (selected + 1) % slots.size();
        }

        public void cyclePrev() {
            if (slots.isEmpty())
                return;
            selected = (selected - 1 + slots.size()) % slots.size();
        }

        public int size() {
            return slots.size();
        }

        public int selectedIndex() {
            return selected;
        }

        public List<Type> list() {
            return List.copyOf(slots);
        }

        /** Convenience: how many Mulligans do we have? */
        public int countOf(Type t) {
            int n = 0;
            for (Type x : slots)
                if (x == t)
                    n++;
            return n;
        }
    }

    /** Timed effects active on the snake. */
    public static final class Effects {
        private int phaseMoves = 0;

        public void activate(Type t) {
            if (t == Type.PHASE_WALLS) {
                phaseMoves = Math.max(phaseMoves, PHASE_MOVES);
            }
        }

        public boolean isPhaseOn() {
            return phaseMoves > 0;
        }

        public void onSuccessfulMove() {
            if (phaseMoves > 0)
                phaseMoves--;
        }

        public int phaseRemaining() {
            return phaseMoves;
        }
    }
}
