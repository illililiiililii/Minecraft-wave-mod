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
    public enum Category {
        FOOD("음식"), UTILITY("유용한 아이템"), ARMOR("방어구"), BUILDING("건축");
        private final String title;
        Category(String title) { this.title = title; }
        public String title() { return title; }
    }
    public record Product(String name, String description, long price, int unlockWave, int perWave, ItemStack[] items) {}
    public static final long[] UPGRADE_COSTS = {30, 100, 300, 850, 2000};
    public static final int[] UPGRADE_IDS = {0, 1, 2, 6, 7, 8, 9, 29};
    public static final String[] NAMES = {
            "빠른 채굴", "추가 체력", "이동 속도", "철 장비 꾸러미", "다이아 장비 꾸러미", "종결 네더라이트 장비",
            "방어 강화", "공격력 강화", "회복 능력", "정찰 강화", "빵 ×8", "구운 소고기 ×8", "황금 사과",
            "즉시 회복 물약 ×2", "횃불 ×32", "랜턴 ×8", "참나무 원목 ×32", "조약돌 ×64", "철 주괴 ×8",
            "화살 ×32", "모루", "석재 벽돌 ×64", "흑요석 ×16", "거미줄 ×8", "방패", "토템", "마법 황금 사과",
            "돌 검", "가죽 방어구 꾸러미", "넉백 강화", "눈덩이 ×16", "감속 투척 물약 ×2",
            "끈끈이 피스톤 ×4 + 레드스톤 ×16", "발사기 ×4 + 화살 ×32", "바람 구체 ×4"
    };
    private static final Product[] PRODUCTS = {
            upgrade("빠른 채굴", "채굴 속도 +20%/단계"), upgrade("추가 체력", "최대 체력 +4/단계"),
            upgrade("이동 속도", "이동 속도 +20%/단계"),
            gear("철 장비 꾸러미", "검·곡괭이·방어구", 2000, 1, 0, Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
            gear("다이아 장비 꾸러미", "검·곡괭이·방어구", 18000, 12, 0, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
            gear("종결 네더라이트 장비", "검·곡괭이·방어구", 10000000, 40, 1, Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS),
            upgrade("방어 강화", "방어력 +1/단계"), upgrade("공격력 강화", "근접 공격력 +1/단계"),
            upgrade("회복 능력", "재생 I~II (3·5단계 해금)"), upgrade("정찰 강화", "위협 정보·보스 표시 해금"),
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
                    new ItemStack(Items.WIND_CHARGE, 4))
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
            case 10, 11, 12, 13, 26 -> Category.FOOD;
            case 14, 15, 18, 19, 20, 25, 30, 31, 34 -> Category.UTILITY;
            case 3, 4, 5, 24, 27, 28 -> Category.ARMOR;
            case 16, 17, 21, 22, 23, 32, 33 -> Category.BUILDING;
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
    public static int maxLevel(int id) { return id == 9 ? 3 : UPGRADE_COSTS.length; }
    public static String nextEffect(int id, int level) {
        if (level >= maxLevel(id)) return "최대 레벨";
        int next = level + 1;
        return switch (id) {
            case 0 -> "성급함 " + next + "단계";
            case 1 -> "최대 체력 +" + (next * 4);
            case 2 -> "신속 " + next + "단계";
            case 6 -> "방어력 +" + next;
            case 7 -> "근접 공격력 +" + next;
            case 8 -> "10초마다 체력 +" + (next * 0.5) + (next >= 5 ? " · 재생 II" : next >= 3 ? " · 재생 I" : "");
            case 9 -> next == 1 ? "다음 보스 웨이브 표시" : next == 2 ? "발광 색상 범례 표시" : "적 체력 배율 표시";
            case 29 -> "근접 넉백 +" + (next * 0.2);
            default -> "";
        };
    }
    public static long cost(int product, int level) {
        Product item = product(product);
        if (item == null) return -1;
        if (upgradeSlot(product) >= 0) return level >= 0 && level < maxLevel(product) ? UPGRADE_COSTS[level] : -1;
        return item.price();
    }
    public static ItemStack[] gear(int product) {
        Product item = product(product);
        if (item == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[item.items().length];
        for (int i = 0; i < copy.length; i++) copy[i] = item.items()[i].copy();
        return copy;
    }
    public static int enemyCount(int base, int players, int wave) {
        if (players <= 0) return 0;
        long count = (long) base + (long) (players - 1) * (base + 2L) + 2L * Math.max(0, wave - 1) * players;
        return (int) Math.min(128, count);
    }
    public static long reward(int wave) { return BalanceRules.clearReward(new WaveData(), wave); }
}
