package dev.endlessdefense;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** World-owned progress, accounts and balance settings. */
public final class WaveData extends SavedData {
    private static final Factory<WaveData> FACTORY = new Factory<>(WaveData::new, WaveData::load, DataFixTypes.LEVEL);
    public int wave, highestWave;
    public boolean started;
    public boolean gameOver;
    public boolean waveInProgress;
    public boolean defenseMapBuilt;
    public boolean coreInitialized;
    public int defenseMapX, defenseMapY, defenseMapZ;
    public int coreTier = 1, coreHp = 100;
    public final int[] turretLevels = new int[ShopRules.MAX_TURRETS];
    public int earlyBonusThroughWave;
    public int respawns, lastGrantWave;
    public final Set<UUID> eliminated = new HashSet<>(), pendingRespawn = new HashSet<>();
    public final Set<UUID> restoreSurvival = new HashSet<>();
    public final Set<UUID> teamMembers = new HashSet<>();
    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<String, Integer> numericSettings = new HashMap<>();
    public int baseMobs = 4, healthPercent = 100, damagePercent = 100, awareness = 2, armorBonus = 0, gearChance = 15;
    public int earlyReward = 100, earlyRewardStep = 30, midRewardStep = 60, lateRewardStep = 90;
    public int killRewardEvery = 4, killBounty = 8, bossReward = 150;
    public int restMinStep = 2, restMaxStep = 3, restMaxCap = 180;
    public int earlyBonusAmount = 250, infuserChancePercent = 1;
    public int nightPercent = 15, nightCap = 16;
    public int respawnEarlyCap = 8, respawnLateWave = 30, respawnGrantInterval = 5;

    public static WaveData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "endlessdefense");
    }

    static WaveData load(CompoundTag tag, HolderLookup.Provider lookup) {
        WaveData d = new WaveData();
        d.wave = Math.max(0, tag.getInt("wave"));
        d.highestWave = Math.max(d.wave, tag.getInt("highestWave"));
        d.started = tag.getBoolean("started") || d.wave > 0; // 0.2/0.3 worlds must not wipe inventories again.
        d.gameOver = tag.getBoolean("gameOver");
        d.waveInProgress = tag.getBoolean("waveInProgress");
        d.defenseMapBuilt = tag.getBoolean("defenseMapBuilt");
        d.coreInitialized = tag.getBoolean("coreInitialized");
        d.defenseMapX = tag.getInt("defenseMapX");
        d.defenseMapY = tag.getInt("defenseMapY");
        d.defenseMapZ = tag.getInt("defenseMapZ");
        d.coreTier = bounded(tag, "coreTier", 1, 1, Integer.MAX_VALUE);
        d.coreHp = Math.max(0, tag.contains("coreHp") ? tag.getInt("coreHp") : 100);
        for (int i = 0; i < d.turretLevels.length; i++)
            d.turretLevels[i] = Math.max(0, tag.getInt("turretLevel" + i));
        d.earlyBonusThroughWave = Math.max(0, Math.min(30, tag.getInt("earlyBonusThroughWave")));
        boolean legacyEconomy = tag.getInt("economyVersion") < 1;
        d.respawns = Math.max(0, tag.getInt("respawns"));
        d.lastGrantWave = Math.max(0, tag.getInt("lastGrantWave"));
        readIds(tag, "eliminated", d.eliminated);
        readIds(tag, "pendingRespawn", d.pendingRespawn);
        readIds(tag, "restoreSurvival", d.restoreSurvival);
        readIds(tag, "teamMembers", d.teamMembers);
        ListTag savedAccounts = tag.getList("accounts", Tag.TAG_COMPOUND);
        for (int i = 0; i < savedAccounts.size(); i++) {
            CompoundTag entry = savedAccounts.getCompound(i);
            if (!entry.hasUUID("id")) continue;
            Account a = new Account();
            long savedCoins = Math.max(0, entry.getLong("coins"));
            a.coins = legacyEconomy ? (savedCoins > Long.MAX_VALUE / 10 ? Long.MAX_VALUE : savedCoins * 10) : savedCoins;
            for (int j = 0; j < a.levels.length; j++)
                a.levels[j] = Math.max(0, entry.getInt("upgrade" + j));
            a.stockWave = Math.max(0, entry.getInt("stockWave"));
            a.pendingInfusions = Math.max(0, Math.min(32, entry.getInt("pendingInfusions")));
            for (int j = 0; j < a.stock.length; j++) a.stock[j] = Math.max(0, entry.getInt("stock" + j));
            d.accounts.put(entry.getUUID("id"), a);
        }
        d.baseMobs = bounded(tag, "baseMobs", 4, 1, 128);
        d.healthPercent = bounded(tag, "healthPercent", 100, 25, 500);
        d.damagePercent = bounded(tag, "damagePercent", 100, 25, 500);
        d.awareness = bounded(tag, "awareness", 2, 1, 5);
        d.armorBonus = bounded(tag, "armorBonus", 0, 0, 20);
        d.gearChance = bounded(tag, "gearChance", 15, 0, 100);
        d.earlyReward = legacyEconomy && tag.contains("earlyReward")
                ? tag.getInt("earlyReward") == 7 ? 100 : Math.min(1000, Math.max(10, tag.getInt("earlyReward") * 10))
                : bounded(tag, "earlyReward", 100, 10, 1000);
        d.earlyRewardStep = bounded(tag, "earlyRewardStep", 30, 0, 200);
        d.midRewardStep = legacyEconomy && tag.contains("midRewardStep")
                ? tag.getInt("midRewardStep") == 2 ? 60 : Math.min(300, Math.max(0, tag.getInt("midRewardStep") * 20))
                : bounded(tag, "midRewardStep", 60, 0, 300);
        d.lateRewardStep = legacyEconomy && tag.contains("lateRewardStep")
                ? tag.getInt("lateRewardStep") == 4 ? 90 : Math.min(500, Math.max(0, tag.getInt("lateRewardStep") * 18))
                : bounded(tag, "lateRewardStep", 90, 0, 500);
        d.killRewardEvery = bounded(tag, "killRewardEvery", 4, 1, 50);
        d.killBounty = bounded(tag, "killBounty", 8, 0, 100);
        d.bossReward = legacyEconomy && tag.contains("bossReward")
                ? tag.getInt("bossReward") == 18 ? 150 : Math.min(2000, Math.max(0, tag.getInt("bossReward") * 8))
                : bounded(tag, "bossReward", 150, 0, 2000);
        d.restMinStep = bounded(tag, "restMinStep", 2, 0, 10);
        d.restMaxStep = bounded(tag, "restMaxStep", 3, 0, 15);
        d.restMaxCap = bounded(tag, "restMaxCap", 180, 60, 300);
        d.earlyBonusAmount = bounded(tag, "earlyBonusAmount", 250, 0, 1000);
        d.infuserChancePercent = bounded(tag, "infuserChancePercent", 1, 0, 100);
        d.nightPercent = bounded(tag, "nightPercent", 15, 0, 25);
        d.nightCap = bounded(tag, "nightCap", 16, 0, 32);
        d.respawnEarlyCap = bounded(tag, "respawnEarlyCap", 8, 1, 20);
        d.respawnLateWave = bounded(tag, "respawnLateWave", 30, 10, 200);
        d.respawnGrantInterval = bounded(tag, "respawnGrantInterval", 5, 2, 20);
        CompoundTag settings = tag.getCompound("numericSettings");
        for (AdminSettings.Entry entry : AdminSettings.ENTRIES) {
            if (!settings.contains(entry.key())) continue;
            int value = settings.getInt(entry.key());
            if (value >= entry.min() && value <= entry.max())
                d.numericSettings.put(entry.key(), value);
        }
        return d;
    }
    private static int bounded(CompoundTag tag, String key, int fallback, int min, int max) {
        int value = tag.getInt(key);
        return tag.contains(key) && value >= min && value <= max ? value : fallback;
    }
    private static void readIds(CompoundTag tag, String key, Set<UUID> dest) {
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) if (list.getCompound(i).hasUUID("id")) dest.add(list.getCompound(i).getUUID("id"));
    }
    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) { CompoundTag e = new CompoundTag(); e.putUUID("id", id); list.add(e); }
        return list;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putInt("wave", wave); tag.putInt("highestWave", highestWave); tag.putBoolean("started", started);
        tag.putBoolean("gameOver", gameOver);
        tag.putBoolean("waveInProgress", waveInProgress);
        tag.putBoolean("defenseMapBuilt", defenseMapBuilt);
        tag.putBoolean("coreInitialized", coreInitialized);
        tag.putInt("defenseMapX", defenseMapX);
        tag.putInt("defenseMapY", defenseMapY);
        tag.putInt("defenseMapZ", defenseMapZ);
        tag.putInt("coreTier", coreTier); tag.putInt("coreHp", coreHp);
        for (int i = 0; i < turretLevels.length; i++) tag.putInt("turretLevel" + i, turretLevels[i]);
        tag.putInt("earlyBonusThroughWave", earlyBonusThroughWave);
        tag.putInt("economyVersion", 1);
        tag.putInt("respawns", respawns); tag.putInt("lastGrantWave", lastGrantWave);
        tag.put("eliminated", writeIds(eliminated)); tag.put("pendingRespawn", writeIds(pendingRespawn));
        tag.put("restoreSurvival", writeIds(restoreSurvival));
        tag.put("teamMembers", writeIds(teamMembers));
        ListTag savedAccounts = new ListTag();
        accounts.forEach((id, a) -> {
            CompoundTag e = new CompoundTag(); e.putUUID("id", id); e.putLong("coins", a.coins);
            for (int i = 0; i < a.levels.length; i++) e.putInt("upgrade" + i, a.levels[i]);
            e.putInt("stockWave", a.stockWave);
            e.putInt("pendingInfusions", a.pendingInfusions);
            for (int i = 0; i < a.stock.length; i++) e.putInt("stock" + i, a.stock[i]);
            savedAccounts.add(e);
        });
        tag.put("accounts", savedAccounts);
        tag.putInt("baseMobs", baseMobs); tag.putInt("healthPercent", healthPercent); tag.putInt("damagePercent", damagePercent);
        tag.putInt("awareness", awareness); tag.putInt("armorBonus", armorBonus); tag.putInt("gearChance", gearChance);
        tag.putInt("earlyReward", earlyReward); tag.putInt("earlyRewardStep", earlyRewardStep);
        tag.putInt("midRewardStep", midRewardStep); tag.putInt("lateRewardStep", lateRewardStep);
        tag.putInt("killRewardEvery", killRewardEvery); tag.putInt("killBounty", killBounty);
        tag.putInt("bossReward", bossReward);
        tag.putInt("restMinStep", restMinStep); tag.putInt("restMaxStep", restMaxStep);
        tag.putInt("restMaxCap", restMaxCap);
        tag.putInt("earlyBonusAmount", earlyBonusAmount);
        tag.putInt("infuserChancePercent", infuserChancePercent);
        tag.putInt("nightPercent", nightPercent); tag.putInt("nightCap", nightCap);
        tag.putInt("respawnEarlyCap", respawnEarlyCap); tag.putInt("respawnLateWave", respawnLateWave);
        tag.putInt("respawnGrantInterval", respawnGrantInterval);
        CompoundTag settings = new CompoundTag();
        numericSettings.forEach(settings::putInt);
        tag.put("numericSettings", settings);
        return tag;
    }

    public Account account(UUID id) { return accounts.computeIfAbsent(id, ignored -> new Account()); }
    public int number(String key, int fallback) { return numericSettings.getOrDefault(key, fallback); }
    public void setNumber(String key, int value) { numericSettings.put(key, value); setDirty(); }
    public int shopPrice(int id) {
        ShopRules.Product item = ShopRules.product(id);
        return item == null ? 0 : number("price:" + id, (int) item.price());
    }
    public int shopUnlock(int id) {
        ShopRules.Product item = ShopRules.product(id);
        return item == null ? Integer.MAX_VALUE : number("unlock:" + id, item.unlockWave());
    }
    public int shopCap(int id) {
        ShopRules.Product item = ShopRules.product(id);
        return item == null ? 0 : number("cap:" + id, item.perWave());
    }
    public void resetRun() {
        wave = 0;
        earlyBonusThroughWave = 0;
        started = false;
        gameOver = false;
        waveInProgress = false;
        respawns = 0;
        lastGrantWave = 0;
        restoreSurvival.addAll(eliminated);
        eliminated.clear();
        pendingRespawn.clear();
        teamMembers.clear();
        Arrays.fill(turretLevels, 0);
        coreTier = 1;
        coreHp = 100;
        coreInitialized = false;
        accounts.clear();
        setDirty();
    }
    public void award(UUID id, long amount) {
        if (amount <= 0) return;
        Account a = account(id);
        a.coins = amount > Long.MAX_VALUE - a.coins ? Long.MAX_VALUE : a.coins + amount;
        setDirty();
    }
    public boolean buy(UUID id, int product) {
        if (product == ShopRules.TURRET_ID || product == ShopRules.TURRET_UPGRADE_ID
                || product == ShopRules.TURRET_REPAIR_ID || product == ShopRules.BASE_UPGRADE_ID
                || product == ShopRules.CORE_REPAIR_ID || product == ShopRules.WALL_REPAIR_ID) return false;
        ShopRules.Product item = ShopRules.product(product);
        if (item == null || wave < shopUnlock(product) || eliminated.contains(id)) return false;
        Account a = account(id);
        int slot = ShopRules.upgradeSlot(product);
        long price = slot >= 0 ? ShopRules.upgradeCost(a.levels[slot],
                number("upgradeBase", 30), number("upgradeGrowth", 150)) : shopPrice(product);
        if (price < 0 || a.coins < price) return false;
        if (a.stockWave != wave) { a.stockWave = wave; Arrays.fill(a.stock, 0); }
        int cap = shopCap(product);
        if (cap > 0 && a.stock[product] >= cap) return false;
        a.coins -= price;
        if (slot >= 0) a.levels[slot]++;
        if (cap > 0) a.stock[product]++;
        setDirty();
        return true;
    }
    public static final class Account {
        public long coins;
        public int pendingInfusions;
        public final int[] levels = new int[ShopRules.UPGRADE_IDS.length];
        public int stockWave;
        public final int[] stock = new int[ShopRules.NAMES.length];
    }
}
