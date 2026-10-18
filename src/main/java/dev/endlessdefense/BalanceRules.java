package dev.endlessdefense;

/** Central formulas. World-owned tuning values live in WaveData. */
public final class BalanceRules {
    private BalanceRules() {}

    public static long clearReward(WaveData data, int wave) {
        int earlyLast = data.number("earlyLastWave", 5);
        int midLast = Math.max(earlyLast + 1, data.number("midLastWave", 20));
        long base;
        if (wave <= earlyLast) base = data.earlyReward + (wave - 1L) * data.earlyRewardStep;
        else if (wave <= midLast) base = data.earlyReward + (earlyLast - 1L) * data.earlyRewardStep
                + (wave - (long) earlyLast) * data.midRewardStep;
        else base = data.earlyReward + (earlyLast - 1L) * data.earlyRewardStep
                + (midLast - (long) earlyLast) * data.midRewardStep
                + (long) Math.floor(Math.sqrt(wave - (long) midLast)) * data.lateRewardStep;
        return base + (wave >= 1 && wave <= data.earlyBonusThroughWave ? data.earlyBonusAmount : 0);
    }

    public static int restMin(WaveData data, int clearedWave) {
        return Math.min(data.number("restMinCap", 90), data.number("restBaseMin", 45)
                + Math.max(0, clearedWave) / 5 * data.restMinStep);
    }

    public static int restMax(WaveData data, int clearedWave) {
        return Math.max(restMin(data, clearedWave),
                Math.min(data.restMaxCap, data.number("restBaseMax", 120)
                        + Math.max(0, clearedWave) / 5 * data.restMaxStep));
    }

    public static int nightCount(WaveData data, int base, boolean night) {
        if (!night || data.nightPercent <= 0 || data.nightCap <= 0) return base;
        int extra = Math.max(1, (int) Math.round(base * data.nightPercent / 100.0));
        return Math.min(data.number("maxWaveMobs", 128), base + Math.min(extra, data.nightCap));
    }

    public static int initialTickets(WaveData data, int players) {
        return Math.min(data.respawnEarlyCap, 3 + (Math.max(1, players) + 1) / 2);
    }

    public static int ticketCap(WaveData data, int wave, int players) {
        if (wave >= data.respawnLateWave) return Math.max(1, (players + 3) / 5);
        if (wave >= 3 * data.respawnGrantInterval) return Math.max(2, (players + 1) / 2);
        return initialTickets(data, players);
    }

    public static double healthMultiplier(int wave) {
        return Math.min(3.4, 1.0 + Math.max(0, wave - 1) * 0.085);
    }
    public static double healthMultiplier(WaveData data, int wave) {
        return healthMultiplier(wave, data.number("healthGrowth", 85), data.number("healthCap", 340));
    }
    public static double healthMultiplier(int wave, int growthPermille, int capPercent) {
        return Math.min(capPercent / 100.0, 1.0 + Math.max(0, wave - 1) * growthPermille / 1000.0);
    }
}
