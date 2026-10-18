package dev.endlessdefense;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import java.util.Arrays;
import java.util.Comparator;

/** Stable product IDs: 0–5 preserve saves from 0.3. */
public final class ShopRules {
    private ShopRules() {}
    public static final int TURRET_ID = 35, TURRET_UPGRADE_ID = 36, TURRET_REPAIR_ID = 37;
    public static final int BASE_UPGRADE_ID = 38, CORE_REPAIR_ID = 39, WALL_REPAIR_ID = 40;
    public static final int MAX_TURRETS = 4;
    public static final long TURRET_PRICE = 400, TURRET_REPAIR_PRICE = 200;
    public enum Category {
        FOOD("음식"), UTILITY("유용한 아이템"), ARMOR("방어구"), BUILDING("건축");
        private final String title;
        Category(String title) { this.title = title; }
        public String title() { return title; }
    }
    public record Product(String name, String description, long price, int unlockWave, int perWave, ItemStack[] items) {}
    public static final int[] UPGRADE_IDS = {0, 1, 2, 6, 7, 8, 9, 29};
    public static final String[] NAMES = {
            "빠른 채굴", "추가 체력", "이동 속도", "철 장비 꾸러미", "다이아 장비 꾸러미", "종결 네더라이트 장비",
            "방어 강화", "공격력 강화", "회복 능력", "정찰 강화", "빵 ×8", "구운 소고기 ×8", "황금 사과",
            "즉시 회복 물약 ×2", "횃불 ×32", "랜턴 ×8", "참나무 원목 ×32", "조약돌 ×64", "철 주괴 ×8",
            "화살 ×32", "모루", "석재 벽돌 ×64", "흑요석 ×16", "거미줄 ×8", "방패", "토템", "마법 황금 사과",
            "돌 검", "가죽 방어구 꾸러미", "넉백 강화", "눈덩이 ×16", "감속 투척 물약 ×2",
            "끈끈이 피스톤 ×4 + 레드스톤 ×16", "발사기 ×4 + 화살 ×32", "바람 구체 ×4",
            "자동 사냥 포탑", "포탑 강화", "포탑 수리",
            "기지 강화", "중앙 핵심 수리", "성벽 수리",
            "구운 감자 ×16", "황금 당근 ×8", "꿀병 ×4", "활", "쇠뇌", "분광 화살 ×24",
            "물 양동이", "엔더 진주 ×2", "철 도끼", "다이아 검", "비계 ×32",
            "철창 ×32", "TNT ×4", "달콤한 열매 덤불 ×16", "뾰족한 점적석 ×32"
    };
    private static final Product[] PRODUCTS = {
            upgrade("빠른 채굴", "채굴 속도 +20%/단계"), upgrade("추가 체력", "최대 체력 +4/단계"),
            upgrade("이동 속도", "이동 속도 +20%/단계"),
            gear("철 장비 꾸러미", "검·곡괭이·방어구", 2000, 1, 0, Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
            gear("다이아 장비 꾸러미", "검·곡괭이·방어구", 18000, 12, 0, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
            gear("종결 네더라이트 장비", "검·곡괭이·방어구", 10000000, 40, 1, Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS),
            upgrade("방어 강화", "방어력 +1/단계"), upgrade("공격력 강화", "근접 공격력 +1/단계"),
            upgrade("회복 능력", "지속 회복 +0.5/단계 · 재생 I~II 해금"), upgrade("정찰 강화", "위협 정보·보스 표시 · 웨이브 적 피해 +1%/단계"),
            supply("빵 ×8", "초반 식량", 30, 1, 0, new ItemStack(Items.BREAD, 8)),
            supply("구운 소고기 ×8", "고효율 식량", 60, 1, 0, new ItemStack(Items.COOKED_BEEF, 8)),
            supply("황금 사과", "전투 생존", 350, 5, 2, new ItemStack(Items.GOLDEN_APPLE)),
            supply("즉시 회복 물약 ×2", "체력 회복", 280, 3, 2, healingPotions()),
            supply("횃불 ×32", "야간 조명", 40, 1, 0, new ItemStack(Items.TORCH, 32)),
            supply("랜턴 ×8", "기지 조명", 130, 3, 0, new ItemStack(Items.LANTERN, 8)),
            supply("참나무 원목 ×32", "기본 건축·제작", 50, 1, 0, new ItemStack(Items.OAK_LOG, 32)),
            supply("조약돌 ×64", "기본 건축·수리", 50, 1, 0, new ItemStack(Items.COBBLESTONE, 64)),
            supply("철 주괴 ×8", "제작·수리 재료", 210, 3, 0, new ItemStack(Items.IRON_INGOT, 8)),
            supply("화살 ×32", "원거리 탄약", 90, 1, 0, new ItemStack(Items.ARROW, 32)),
            supply("모루", "장비 수리 시설", 500, 5, 1, new ItemStack(Items.ANVIL)),
            supply("석재 벽돌 ×64", "벽·바리케이드", 100, 1, 0, new ItemStack(Items.STONE_BRICKS, 64)),
            supply("흑요석 ×16", "고급 방벽", 650, 12, 2, new ItemStack(Items.OBSIDIAN, 16)),
            supply("거미줄 ×8", "적 이동 지연 함정", 520, 10, 2, new ItemStack(Items.COBWEB, 8)),
            supply("방패", "초반 방어", 200, 1, 0, new ItemStack(Items.SHIELD)),
            supply("토템", "제한적 긴급 생존", 3200, 20, 1, new ItemStack(Items.TOTEM_OF_UNDYING)),
            supply("마법 황금 사과", "후반 긴급 생존", 5500, 30, 1, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),
            supply("돌 검", "초반 기본 무기", 80, 1, 0, new ItemStack(Items.STONE_SWORD)),
            gear("가죽 방어구 꾸러미", "초반 방어구 4부위", 240, 1, 0,
                    Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS),
            upgrade("넉백 강화", "근접 공격 넉백 +0.2/단계"),
            supply("눈덩이 ×16", "저렴한 투척물", 65, 1, 0, new ItemStack(Items.SNOWBALL, 16)),
            supply("감속 투척 물약 ×2", "진입로의 적을 느리게 함", 460, 4, 2,
                    PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SLOWNESS),
                    PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SLOWNESS)),
            supply("끈끈이 피스톤 ×4 + 레드스톤 ×16", "움직이는 바리케이드 재료", 390, 5, 2,
                    new ItemStack(Items.STICKY_PISTON, 4), new ItemStack(Items.REDSTONE, 16)),
            supply("발사기 ×4 + 화살 ×32", "자동 방어 장치 재료", 440, 5, 2,
                    new ItemStack(Items.DISPENSER, 4), new ItemStack(Items.ARROW, 32)),
            supply("바람 구체 ×4", "적을 밀어내는 방어용 투척물", 420, 4, 2,
                    new ItemStack(Items.WIND_CHARGE, 4)),
            supply("자동 사냥 포탑", "디펜스 야생 거점에 약한 팀 포탑 설치 · 최대 4기", TURRET_PRICE, 1, 0,
                    new ItemStack(Items.DISPENSER)),
            supply("포탑 강화", "팀 포탑 중 최저 레벨 1기 강화 · 단계 제한 없음", 0, 1, 0,
                    new ItemStack(Items.CROSSBOW)),
            supply("포탑 수리", "파손된 팀 포탑 1기 재가동 · 아래 상자는 보존", TURRET_REPAIR_PRICE, 1, 0,
                    new ItemStack(Items.ANVIL)),
            supply("기지 강화", "중앙 핵심 최대 체력 +50 · 성벽 파괴 지연", 0, 1, 0, new ItemStack(Items.BEACON)),
            supply("중앙 핵심 수리", "핵심 체력 40 회복", 250, 1, 2, new ItemStack(Items.IRON_INGOT)),
            supply("성벽 수리", "거점 성벽 최대 24블록 복구", 300, 1, 2, new ItemStack(Items.STONE_BRICKS)),
            supply("구운 감자 ×16", "값싼 식량", 45, 1, 0, new ItemStack(Items.BAKED_POTATO, 16)),
            supply("황금 당근 ×8", "포만감 높은 식량", 150, 3, 0, new ItemStack(Items.GOLDEN_CARROT, 8)),
            supply("꿀병 ×4", "독 해제용 음료", 120, 3, 0, new ItemStack(Items.HONEY_BOTTLE, 4)),
            supply("활", "초반 원거리 무기", 250, 1, 0, new ItemStack(Items.BOW)),
            supply("쇠뇌", "기지 방어용 원거리 무기", 850, 5, 0, new ItemStack(Items.CROSSBOW)),
            supply("분광 화살 ×24", "적 식별용 화살", 600, 10, 2, new ItemStack(Items.SPECTRAL_ARROW, 24)),
            supply("물 양동이", "소화·이동·방어", 180, 1, 0, new ItemStack(Items.WATER_BUCKET)),
            supply("엔더 진주 ×2", "긴급 탈출", 900, 12, 2, new ItemStack(Items.ENDER_PEARL, 2)),
            supply("철 도끼", "근접전·벌목", 420, 3, 0, new ItemStack(Items.IRON_AXE)),
            supply("다이아 검", "후반 무기", 3500, 12, 0, new ItemStack(Items.DIAMOND_SWORD)),
            supply("비계 ×32", "높은 성벽 보수", 180, 1, 0, new ItemStack(Items.SCAFFOLDING, 32)),
            supply("철창 ×32", "방어 시설 재료", 250, 3, 0, new ItemStack(Items.IRON_BARS, 32)),
            supply("TNT ×4", "공성 몹 저지", 950, 10, 2, new ItemStack(Items.TNT, 4)),
            supply("달콤한 열매 덤불 ×16", "접근 지연 식물", 110, 1, 0, new ItemStack(Items.SWEET_BERRIES, 16)),
            supply("뾰족한 점적석 ×32", "낙하 함정 재료", 130, 3, 0, new ItemStack(Items.POINTED_DRIPSTONE, 32))
    };

    private static Product upgrade(String name, String description) { return new Product(name, description, 0, 1, 0, new ItemStack[0]); }
    private static ItemStack[] healingPotions() {
        return new ItemStack[] {PotionContents.createItemStack(Items.POTION, Potions.HEALING),
                PotionContents.createItemStack(Items.POTION, Potions.HEALING)};
    }
    private static Product supply(String name, String description, long price, int wave, int cap, ItemStack... items) {
        return new Product(name, description, price, wave, cap, items);
    }
    private static Product gear(String name, String description, long price, int wave, int cap, net.minecraft.world.item.Item... items) {
        ItemStack[] stacks = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) stacks[i] = new ItemStack(items[i]);
        return supply(name, description, price, wave, cap, stacks);
    }
    public static Product product(int id) { return id >= 0 && id < PRODUCTS.length ? PRODUCTS[id] : null; }
    public static Category category(int id) {
        if (upgradeSlot(id) >= 0 || product(id) == null) return null;
        return switch (id) {
            case 10, 11, 12, 13, 26, 41, 42, 43 -> Category.FOOD;
            case 14, 15, 18, 19, 20, 25, 30, 31, 34, 46, 47, 48 -> Category.UTILITY;
            case 3, 4, 5, 24, 27, 28, 44, 45, 49, 50 -> Category.ARMOR;
            case 16, 17, 21, 22, 23, 32, 33, TURRET_ID, TURRET_UPGRADE_ID, TURRET_REPAIR_ID,
                    BASE_UPGRADE_ID, CORE_REPAIR_ID, WALL_REPAIR_ID, 51, 52, 53, 54, 55 -> Category.BUILDING;
            default -> null;
        };
    }
    public static int[] catalog(Category category) {
        return Arrays.stream(java.util.stream.IntStream.range(0, PRODUCTS.length).toArray())
                .filter(id -> category(id) == category).boxed()
                .sorted(Comparator.<Integer>comparingLong(id -> PRODUCTS[id].price()).thenComparingInt(id -> id))
                .mapToInt(Integer::intValue).toArray();
    }
    public static int upgradeSlot(int id) {
        for (int i = 0; i < UPGRADE_IDS.length; i++) if (UPGRADE_IDS[i] == id) return i;
        return -1;
    }
    public static int maxLevel(int id) { return Integer.MAX_VALUE; }
    public static String nextEffect(int id, int level) {
        if (level == Integer.MAX_VALUE) return "레벨 저장 한계";
        int next = level + 1;
        return switch (id) {
            case 0 -> "성급함 " + next + "단계";
            case 1 -> "최대 체력 +" + (next * 4L);
            case 2 -> "신속 " + next + "단계";
            case 6 -> "방어력 +" + next;
            case 7 -> "근접 공격력 +" + next;
            case 8 -> "10초마다 체력 +" + (next * 0.5) + (next >= 5 ? " · 재생 II" : next >= 3 ? " · 재생 I" : "");
            case 9 -> next == 1 ? "다음 보스 웨이브 표시 · 피해 +1%" : next == 2 ? "발광 색상 범례 · 피해 +2%"
                    : next == 3 ? "적 체력 배율 · 피해 +3%" : "웨이브 적 대상 피해 +" + next + "%";
            case 29 -> "근접 넉백 +" + (next * 0.2);
            default -> "";
        };
    }
    public static long cost(int product, int level) {
        Product item = product(product);
        if (item == null) return -1;
        if (product == TURRET_UPGRADE_ID) return turretUpgradeCost(level, 250, 80);
        if (upgradeSlot(product) >= 0) return upgradeCost(level, 30, 150);
        return item.price();
    }
    public static long upgradeCost(int level, int base, int growthPercent) {
        if (level < 0 || level == Integer.MAX_VALUE) return -1;
        double value = base * Math.pow(1.0 + growthPercent / 100.0, level);
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1, (long) Math.ceil(value));
    }
    public static long turretUpgradeCost(int level, int base, int growthPercent) {
        return level >= 1 && level < Integer.MAX_VALUE ? upgradeCost(level - 1, base, growthPercent) : -1;
    }
    public static float turretDamage(int level, int baseTenths, int stepTenths) {
        return (float) ((baseTenths + Math.max(0L, (long) level - 1) * stepTenths) / 10.0);
    }
    public static int turretInterval(int level, int base, int step) {
        return (int) Math.max(10L, base - Math.max(0L, (long) level - 1) * step);
    }
    public static int turretRange(int level, int base, int step) {
        return (int) Math.min(64L, base + Math.max(0L, (long) level - 1) * step);
    }
    public static int lowestTurretLevel(int[] levels) {
        int lowest = Integer.MAX_VALUE;
        for (int level : levels) if (level > 0) lowest = Math.min(lowest, level);
        return lowest;
    }
    public static int turretCount(int[] levels) {
        int count = 0;
        for (int level : levels) if (level > 0) count++;
        return count;
    }
    public static ItemStack[] gear(int product) {
        Product item = product(product);
        if (item == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[item.items().length];
        for (int i = 0; i < copy.length; i++) copy[i] = item.items()[i].copy();
        return copy;
    }
    public static int enemyCount(int base, int players, int wave) {
        return enemyCount(base, players, wave, 64);
    }
    public static int enemyCount(int base, int players, int wave, int cap) {
        return enemyCount(base, players, wave, cap, 2, 16);
    }
    public static int enemyCount(int base, int players, int wave, int cap, int growthPerRoot, int growthCap) {
        if (players <= 0) return 0;
        long growth = Math.min(growthCap, (long) Math.floor(Math.sqrt(Math.max(0, wave - 1))) * growthPerRoot);
        long count = (long) base + (long) (players - 1) * (base + 2L)
                + growth * (players + 1L) / 2L;
        return (int) Math.min(cap, count);
    }
    public static long reward(int wave) { return BalanceRules.clearReward(new WaveData(), wave); }
}
