package dev.endlessdefense;

/** Central formulas. World-owned tuning values live in WaveData. */
public final class BalanceRules {
    private BalanceRules() {}

    public static long clearReward(WaveData data, int wave) {
        long base;
        if (wave <= 5) base = data.earlyReward + (wave - 1L) * data.earlyRewardStep;
        else if (wave <= 20) base = data.earlyReward + 4L * data.earlyRewardStep + (wave - 5L) * data.midRewardStep;
        else base = data.earlyReward + 4L * data.earlyRewardStep + 15L * data.midRewardStep
                + (long) Math.floor(Math.sqrt(wave - 20L)) * data.lateRewardStep;
        return base + (wave >= 1 && wave <= data.earlyBonusThroughWave ? data.earlyBonusAmount : 0);
    }

    public static int restMin(WaveData data, int clearedWave) {
        return Math.min(90, 45 + Math.max(0, clearedWave) / 5 * data.restMinStep);
    }

    public static int restMax(WaveData data, int clearedWave) {
        return Math.max(restMin(data, clearedWave),
                Math.min(data.restMaxCap, 120 + Math.max(0, clearedWave) / 5 * data.restMaxStep));
    }

    public static int nightCount(WaveData data, int base, boolean night) {
        if (!night) return base;
        int extra = Math.max(1, (int) Math.round(base * data.nightPercent / 100.0));
        return Math.min(128, base + Math.min(extra, data.nightCap));
    }

    public static int initialTickets(WaveData data, int players) {
        return Math.min(data.respawnEarlyCap, 3 + (Math.max(1, players) + 1) / 2);
    }

    public static int ticketCap(WaveData data, int wave, int players) {
        if (wave >= data.respawnLateWave) return Math.max(1, (players + 3) / 5);
        if (wave >= 15) return Math.max(2, (players + 1) / 2);
        return initialTickets(data, players);
    }

    public static double healthMultiplier(int wave) {
        return Math.min(3.4, 1.0 + Math.max(0, wave - 1) * 0.085);
    }
}
