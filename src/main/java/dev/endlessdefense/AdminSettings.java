package dev.endlessdefense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared, validated numeric settings catalog for the OP screen and server packets. */
final class AdminSettings {
    enum Group {
        COMBAT("전투"), ECONOMY("보상"), TIMING("시간·부활"), TURRET("포탑"),
        PRICES("상품 가격"), CONDITIONS("상품 조건");
        private final String title;
        Group(String title) { this.title = title; }
        String title() { return title; }
    }
    record Entry(Group group, String key, String label, int min, int max, int step, int defaultValue) {}
    static final int PAGE_SIZE = 5;
    static final List<Entry> ENTRIES;
    private static final Map<String, Integer> INDEX;

    static {
        List<Entry> entries = new ArrayList<>();
        add(entries, Group.COMBAT, "count", "기본 적 수", 1, 128, 1, 4);
        add(entries, Group.COMBAT, "health", "기본 체력 %", 25, 500, 25, 100);
        add(entries, Group.COMBAT, "damage", "기본 공격력 %", 25, 500, 25, 100);
        add(entries, Group.COMBAT, "awareness", "탐지 단계", 1, 5, 1, 2);
        add(entries, Group.COMBAT, "armor", "추가 방어력", 0, 20, 1, 0);
        add(entries, Group.COMBAT, "gear", "장비 확률 %", 0, 100, 5, 15);
        add(entries, Group.COMBAT, "infuser", "마력 주입 몹 %", 0, 100, 1, 1);
        add(entries, Group.COMBAT, "skeletonWave", "스켈레톤 해금 웨이브", 1, 200, 1, 3);
        add(entries, Group.COMBAT, "ashWave", "잿불 추적자 해금 웨이브", 1, 200, 1, 4);
        add(entries, Group.COMBAT, "ironWave", "철갑 수호자 해금 웨이브", 1, 200, 1, 8);
        add(entries, Group.COMBAT, "skeletonStride", "스켈레톤 배치 간격", 1, 20, 1, 3);
        add(entries, Group.COMBAT, "ashStride", "잿불 추적자 배치 간격", 1, 20, 1, 4);
        add(entries, Group.COMBAT, "ironStride", "철갑 수호자 배치 간격", 1, 20, 1, 7);
        add(entries, Group.COMBAT, "bossInterval", "보스 웨이브 간격", 1, 50, 1, 5);
        add(entries, Group.COMBAT, "healthGrowth", "웨이브 체력 증가 ‰", 0, 500, 5, 85);
        add(entries, Group.COMBAT, "healthCap", "체력 배율 상한 %", 100, 1000, 20, 340);
        add(entries, Group.COMBAT, "damageGrowth", "웨이브 공격 증가 %", 0, 50, 1, 5);
        add(entries, Group.COMBAT, "damageCap", "공격 배율 상한 %", 100, 500, 10, 200);
        add(entries, Group.COMBAT, "bossHealth", "보스 체력 배율 %", 100, 500, 10, 180);
        add(entries, Group.COMBAT, "bossDamage", "보스 공격 배율 %", 100, 500, 10, 130);
        add(entries, Group.COMBAT, "maxWaveMobs", "웨이브 적 상한", 16, 128, 4, 128);
        add(entries, Group.COMBAT, "mobHealthCap", "개체 체력 상한", 20, 2000, 20, 300);
        add(entries, Group.COMBAT, "mobDamageCap", "개체 공격력 상한", 1, 200, 1, 30);
        add(entries, Group.COMBAT, "mobArmorCap", "개체 방어력 상한", 0, 100, 1, 30);
        add(entries, Group.COMBAT, "armorWaveStep", "방어력 증가 웨이브 간격", 1, 50, 1, 5);
        add(entries, Group.COMBAT, "armorWaveCap", "웨이브 추가 방어력 상한", 0, 50, 1, 6);
        add(entries, Group.COMBAT, "night", "야간 추가 %", 0, 25, 1, 15);
        add(entries, Group.COMBAT, "nightCap", "야간 추가 상한", 0, 32, 1, 16);

        add(entries, Group.ECONOMY, "early", "초반 기본 보상", 10, 1000, 10, 100);
        add(entries, Group.ECONOMY, "earlyLastWave", "초반 보상 마지막 웨이브", 1, 30, 1, 5);
        add(entries, Group.ECONOMY, "midLastWave", "중반 보상 마지막 웨이브", 2, 200, 1, 20);
        add(entries, Group.ECONOMY, "earlyStep", "초반 증가량", 0, 200, 5, 30);
        add(entries, Group.ECONOMY, "mid", "중반 증가량", 0, 300, 5, 60);
        add(entries, Group.ECONOMY, "late", "후반 증가량", 0, 500, 5, 90);
        add(entries, Group.ECONOMY, "killEvery", "처치 보상 간격", 1, 50, 1, 4);
        add(entries, Group.ECONOMY, "killBounty", "처치 보상 노드", 0, 100, 1, 8);
        add(entries, Group.ECONOMY, "boss", "보스 보상 노드", 0, 2000, 25, 150);
        add(entries, Group.ECONOMY, "earlyBonus", "초반 추가 보상", 0, 1000, 25, 250);
        add(entries, Group.ECONOMY, "earlyBonusMinWave", "추가 보상 최소 종료 웨이브", 1, 30, 1, 2);
        add(entries, Group.ECONOMY, "earlyBonusMaxWave", "추가 보상 최대 종료 웨이브", 1, 30, 1, 5);
        add(entries, Group.ECONOMY, "upgradeBase", "능력 강화 첫 비용", 1, 100000, 10, 30);
        add(entries, Group.ECONOMY, "upgradeGrowth", "능력 강화 비용 증가 %", 1, 500, 5, 150);

        add(entries, Group.TIMING, "restBaseMin", "휴식 기본 최소 초", 10, 180, 5, 45);
        add(entries, Group.TIMING, "restBaseMax", "휴식 기본 최대 초", 10, 300, 5, 120);
        add(entries, Group.TIMING, "restMinCap", "휴식 최소 상한 초", 10, 300, 5, 90);
        add(entries, Group.TIMING, "restMaxCap", "휴식 최대 상한 초", 60, 300, 5, 180);
        add(entries, Group.TIMING, "restMinStep", "5웨이브당 최소 증가", 0, 10, 1, 2);
        add(entries, Group.TIMING, "restMaxStep", "5웨이브당 최대 증가", 0, 15, 1, 3);
        add(entries, Group.TIMING, "respawnCap", "초반 부활 상한", 1, 20, 1, 8);
        add(entries, Group.TIMING, "lateWave", "후반 부활 시작 웨이브", 10, 200, 5, 30);
        add(entries, Group.TIMING, "respawnGrantInterval", "중반 부활 감소 기준 ÷3", 2, 20, 1, 5);

        add(entries, Group.TURRET, "turretUpgradeBase", "포탑 첫 강화 비용", 1, 100000, 25, 250);
        add(entries, Group.TURRET, "turretUpgradeGrowth", "포탑 강화 비용 증가 %", 1, 500, 5, 80);
        add(entries, Group.TURRET, "turretDamage", "포탑 기본 공격 ×10", 1, 200, 1, 10);
        add(entries, Group.TURRET, "turretDamageStep", "단계당 공격 ×10", 1, 200, 1, 10);
        add(entries, Group.TURRET, "turretInterval", "기본 발사 간격 틱", 10, 200, 5, 80);
        add(entries, Group.TURRET, "turretIntervalStep", "단계당 간격 감소 틱", 0, 30, 1, 12);
        add(entries, Group.TURRET, "turretRange", "기본 사거리 블록", 4, 64, 1, 14);
        add(entries, Group.TURRET, "turretRangeStep", "단계당 사거리 증가", 0, 12, 1, 3);

        for (int id = 0; id < ShopRules.NAMES.length; id++) {
            ShopRules.Product product = ShopRules.product(id);
            if (product == null) continue;
            if (ShopRules.upgradeSlot(id) < 0 && id != ShopRules.TURRET_UPGRADE_ID) {
                Group group = id == ShopRules.TURRET_ID || id == ShopRules.TURRET_REPAIR_ID
                        ? Group.TURRET : Group.PRICES;
                add(entries, group, "price:" + id, ShopRules.NAMES[id] + " 가격", 1, 100000000, 25,
                        (int) product.price());
            }
            add(entries, Group.CONDITIONS, "unlock:" + id, ShopRules.NAMES[id] + " 해금 웨이브",
                    0, 200, 1, product.unlockWave());
            if (ShopRules.upgradeSlot(id) < 0 && id < ShopRules.TURRET_ID)
                add(entries, Group.CONDITIONS, "cap:" + id, ShopRules.NAMES[id] + " 웨이브 재고",
                        0, 64, 1, product.perWave());
        }
        ENTRIES = List.copyOf(entries);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < ENTRIES.size(); i++) index.put(ENTRIES.get(i).key(), i);
        INDEX = Map.copyOf(index);
    }

    private AdminSettings() {}

    private static void add(List<Entry> entries, Group group, String key, String label,
                            int min, int max, int step, int defaultValue) {
        entries.add(new Entry(group, key, label, min, max, step, defaultValue));
    }

    static int[] defaults() {
        int[] values = new int[ENTRIES.size()];
        for (int i = 0; i < values.length; i++) values[i] = ENTRIES.get(i).defaultValue();
        return values;
    }

    static int[] values(WaveData data) {
        int[] values = new int[ENTRIES.size()];
        for (int i = 0; i < values.length; i++) values[i] = read(data, ENTRIES.get(i));
        return values;
    }

    static int value(int[] values, String key) {
        Integer index = INDEX.get(key);
        if (index == null) return 0;
        return index < values.length ? values[index] : ENTRIES.get(index).defaultValue();
    }

    static List<Integer> indices(Group group) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < ENTRIES.size(); i++) if (ENTRIES.get(i).group() == group) result.add(i);
        return result;
    }

    static boolean set(WaveData data, int index, int value) {
        if (index < 0 || index >= ENTRIES.size()) return false;
        Entry entry = ENTRIES.get(index);
        if (value < entry.min() || value > entry.max()) return false;
        String key = entry.key();
        switch (key) {
            case "count" -> data.baseMobs = value;
            case "health" -> data.healthPercent = value;
            case "damage" -> data.damagePercent = value;
            case "awareness" -> data.awareness = value;
            case "armor" -> data.armorBonus = value;
            case "gear" -> data.gearChance = value;
            case "infuser" -> data.infuserChancePercent = value;
            case "early" -> data.earlyReward = value;
            case "earlyStep" -> data.earlyRewardStep = value;
            case "mid" -> data.midRewardStep = value;
            case "late" -> data.lateRewardStep = value;
            case "killEvery" -> data.killRewardEvery = value;
            case "killBounty" -> data.killBounty = value;
            case "boss" -> data.bossReward = value;
            case "earlyBonus" -> data.earlyBonusAmount = value;
            case "restMinStep" -> data.restMinStep = value;
            case "restMaxStep" -> data.restMaxStep = value;
            case "restMaxCap" -> data.restMaxCap = value;
            case "night" -> data.nightPercent = value;
            case "nightCap" -> data.nightCap = value;
            case "respawnCap" -> data.respawnEarlyCap = value;
            case "lateWave" -> data.respawnLateWave = value;
            case "respawnGrantInterval" -> data.respawnGrantInterval = value;
            default -> data.setNumber(key, value);
        }
        data.setDirty();
        return true;
    }

    private static int read(WaveData data, Entry entry) {
        return switch (entry.key()) {
            case "count" -> data.baseMobs;
            case "health" -> data.healthPercent;
            case "damage" -> data.damagePercent;
            case "awareness" -> data.awareness;
            case "armor" -> data.armorBonus;
            case "gear" -> data.gearChance;
            case "infuser" -> data.infuserChancePercent;
            case "early" -> data.earlyReward;
            case "earlyStep" -> data.earlyRewardStep;
            case "mid" -> data.midRewardStep;
            case "late" -> data.lateRewardStep;
            case "killEvery" -> data.killRewardEvery;
            case "killBounty" -> data.killBounty;
            case "boss" -> data.bossReward;
            case "earlyBonus" -> data.earlyBonusAmount;
            case "restMinStep" -> data.restMinStep;
            case "restMaxStep" -> data.restMaxStep;
            case "restMaxCap" -> data.restMaxCap;
            case "night" -> data.nightPercent;
            case "nightCap" -> data.nightCap;
            case "respawnCap" -> data.respawnEarlyCap;
            case "lateWave" -> data.respawnLateWave;
            case "respawnGrantInterval" -> data.respawnGrantInterval;
            default -> data.number(entry.key(), entry.defaultValue());
        };
    }
}
