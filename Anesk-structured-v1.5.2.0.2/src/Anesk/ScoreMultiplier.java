package Anesk;

public final class ScoreMultiplier {
    // tunables (move your current constants here)
    public static final int MAX_TIER = 5;
    public static final int METER_FULL_MS = 4000;
    public static final int DECAY_PER_SEC_MS = 1200; // drains per second when idle
    public static final int GAIN_ON_APPLE_MS = 1400; // meter gain on pickup

    private int tier = 1;
    private int meterMs = 0; // 0..METER_FULL_MS
    private long lastMs = System.currentTimeMillis();

    public void tick() {
        long now = System.currentTimeMillis();
        int dt = (int) Math.max(0, Math.min(250, now - lastMs));
        lastMs = now;

        if (tier > 1 || meterMs > 0) {
            int drain = (int) (DECAY_PER_SEC_MS * (dt / 1000.0));
            meterMs -= drain;
            while (meterMs < 0 && tier > 1) {
                tier--;
                meterMs += METER_FULL_MS;
            }
            if (meterMs < 0)
                meterMs = 0;
        }
    }

    public void onApple() {
        meterMs += GAIN_ON_APPLE_MS;
        while (meterMs >= METER_FULL_MS && tier < MAX_TIER) {
            meterMs -= METER_FULL_MS;
            tier++;
        }
        if (tier == MAX_TIER && meterMs > METER_FULL_MS)
            meterMs = METER_FULL_MS;
        lastMs = System.currentTimeMillis();
    }

    public int tier() {
        return tier;
    } // 1..MAX_TIER

    public double meter01() {
        return Math.min(1.0, Math.max(0.0, meterMs / (double) METER_FULL_MS));
    }
}
