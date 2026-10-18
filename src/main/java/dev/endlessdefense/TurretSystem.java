package dev.endlessdefense;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Team-owned courtyard turrets. All purchases, targeting and loot transfers run on the server thread. */
final class TurretSystem {
    // The four existing watchtower roofs provide clear sight over the courtyard walls.
    private static final int[][] OFFSETS = {{-15, -15}, {15, -15}, {-15, 15}, {15, 15}};
    private static final Map<UUID, BlockPos> FIRING = new HashMap<>();

    private TurretSystem() {}

    static String purchase(ServerPlayer player, WaveData data, int product) {
        ServerLevel level = player.server.overworld();
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt)
            return "포탑은 디펜스 야생 거점에서만 사용할 수 있습니다.";
        if (!data.started || data.gameOver || data.eliminated.contains(player.getUUID()))
            return "진행 중인 게임의 생존자만 포탑을 구매할 수 있습니다.";
        ShopRules.Product offer = ShopRules.product(product);
        if (offer == null || data.wave < data.shopUnlock(product))
            return "이 상품은 아직 해금되지 않았습니다.";

        int slot = -1;
        long price;
        if (product == ShopRules.TURRET_ID) {
            if (ShopRules.turretCount(data.turretLevels) >= ShopRules.MAX_TURRETS)
                return "포탑은 최대 4기까지 설치할 수 있습니다.";
            double nearest = Double.MAX_VALUE;
            for (int i = 0; i < data.turretLevels.length; i++) {
                BlockPos base = base(data, i);
                if (data.turretLevels[i] != 0 || !canPlace(level, base, false)) continue;
                double distance = player.distanceToSqr(Vec3.atCenterOf(base));
                if (distance < nearest) { nearest = distance; slot = i; }
            }
            if (slot < 0) return "빈 포탑 자리가 없습니다. 거점의 네 설치 지점을 비워 주세요.";
            price = data.shopPrice(product);
        } else if (product == ShopRules.TURRET_UPGRADE_ID) {
            int lowest = Integer.MAX_VALUE;
            for (int i = 0; i < data.turretLevels.length; i++)
                if (data.turretLevels[i] > 0 && data.turretLevels[i] < lowest) {
                    lowest = data.turretLevels[i]; slot = i;
                }
            if (slot < 0 || lowest == Integer.MAX_VALUE) return "강화할 포탑이 없습니다.";
            price = ShopRules.turretUpgradeCost(lowest,
                    data.number("turretUpgradeBase", 250), data.number("turretUpgradeGrowth", 80));
        } else if (product == ShopRules.TURRET_REPAIR_ID) {
            double nearest = Double.MAX_VALUE;
            for (int i = 0; i < data.turretLevels.length; i++) {
                BlockPos base = base(data, i);
                if (data.turretLevels[i] == 0 || operational(level, base) || !canPlace(level, base, true)) continue;
                double distance = player.distanceToSqr(Vec3.atCenterOf(base));
                if (distance < nearest) { nearest = distance; slot = i; }
            }
            if (slot < 0) return "수리 가능한 포탑이 없습니다. 파손된 자리에 다른 블록이 있다면 치워 주세요.";
            price = data.shopPrice(product);
        } else return "알 수 없는 포탑 상품입니다.";

        WaveData.Account account = data.account(player.getUUID());
        if (price < 0) return "포탑 레벨 저장 한계에 도달했습니다.";
        if (account.coins < price) return "노드가 부족합니다. 필요 노드: " + price;
        if ((product == ShopRules.TURRET_ID || product == ShopRules.TURRET_REPAIR_ID)
                && !place(level, base(data, slot), slot, product == ShopRules.TURRET_REPAIR_ID))
            return "포탑 설치에 실패했습니다. 설치 자리를 확인해 주세요.";
        account.coins -= price;
        if (product == ShopRules.TURRET_ID) data.turretLevels[slot] = 1;
        else if (product == ShopRules.TURRET_UPGRADE_ID) data.turretLevels[slot]++;
        data.setDirty();
        return "구매 완료: " + ShopRules.NAMES[product] + " · " + (slot + 1) + "번 포탑 레벨 " + data.turretLevels[slot];
    }

    static void tick(MinecraftServer server, WaveData data, Set<UUID> waveMobs) {
        ServerLevel level = server.overworld();
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt || waveMobs.isEmpty()) return;
        int tick = server.getTickCount();
        for (int i = 0; i < data.turretLevels.length; i++) {
            int levelValue = data.turretLevels[i];
            if (levelValue <= 0) continue;
            int interval = ShopRules.turretInterval(levelValue,
                    data.number("turretInterval", 80), data.number("turretIntervalStep", 12));
            if ((tick + i * 7) % interval != 0) continue;
            BlockPos base = base(data, i);
            if (!level.isLoaded(base) || !operational(level, base)) continue;
            Vec3 muzzle = Vec3.atCenterOf(base.above(2)).add(0, 0.65, 0);
            int range = ShopRules.turretRange(levelValue,
                    data.number("turretRange", 14), data.number("turretRangeStep", 3));
            Monster target = null;
            double nearest = (double) range * range;
            for (Monster mob : level.getEntitiesOfClass(Monster.class, new AABB(base).inflate(range))) {
                if (!waveMobs.contains(mob.getUUID()) || !mob.isAlive()) continue;
                Vec3 eye = mob.getEyePosition();
                double distance = muzzle.distanceToSqr(eye);
                if (distance >= nearest || level.clip(new ClipContext(muzzle, eye,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                        CollisionContext.empty())).getType() != HitResult.Type.MISS) continue;
                target = mob;
                nearest = distance;
            }
            if (target == null) continue;
            Vec3 end = target.getEyePosition();
            for (int step = 1; step <= 7; step++) {
                Vec3 point = muzzle.lerp(end, step / 8.0);
                level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, base, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 0.4f, 1.1f);
            FIRING.put(target.getUUID(), base);
            try {
                target.hurt(level.damageSources().magic(), ShopRules.turretDamage(levelValue,
                        data.number("turretDamage", 10), data.number("turretDamageStep", 10)));
            } finally {
                FIRING.remove(target.getUUID());
            }
        }
    }

    static void collectDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        BlockPos base = FIRING.get(event.getEntity().getUUID());
        if (base == null || !(level.getBlockEntity(base) instanceof ChestBlockEntity chest)) return;
        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemEntity drop = drops.next();
            ItemStack remaining = drop.getItem().copy();
            insert(chest, remaining);
            if (remaining.isEmpty()) drops.remove();
            else drop.setItem(remaining);
        }
        chest.setChanged();
    }

    static void deactivate(MinecraftServer server, WaveData data) {
        ServerLevel level = server.overworld();
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt) return;
        for (int i = 0; i < data.turretLevels.length; i++) {
            if (data.turretLevels[i] <= 0) continue;
            BlockPos base = base(data, i);
            if (level.getBlockState(base.above(2)).is(Blocks.DISPENSER)) level.removeBlock(base.above(2), false);
            if (level.getBlockState(base.above()).is(Blocks.IRON_BARS)) level.removeBlock(base.above(), false);
        }
        // Chests and their loot remain in the world after a run ends.
    }

    static int brokenCount(ServerLevel level, WaveData data) {
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt) return 0;
        int count = 0;
        for (int i = 0; i < data.turretLevels.length; i++) {
            BlockPos base = base(data, i);
            if (data.turretLevels[i] > 0 && level.isLoaded(base) && !operational(level, base)) count++;
        }
        return count;
    }

    private static BlockPos base(WaveData data, int slot) {
        return new BlockPos(data.defenseMapX + OFFSETS[slot][0], data.defenseMapY + 5,
                data.defenseMapZ + OFFSETS[slot][1]);
    }

    private static boolean operational(ServerLevel level, BlockPos base) {
        return level.getBlockEntity(base) instanceof ChestBlockEntity
                && level.getBlockState(base.above()).is(Blocks.IRON_BARS)
                && level.getBlockState(base.above(2)).is(Blocks.DISPENSER);
    }

    private static boolean canPlace(ServerLevel level, BlockPos base, boolean repair) {
        if (!level.isLoaded(base) || !level.getBlockState(base.below()).isSolidRender(level, base.below())) return false;
        return (level.getBlockState(base).isAir() || level.getBlockState(base).is(Blocks.CHEST))
                && (level.getBlockState(base.above()).isAir()
                    || repair && level.getBlockState(base.above()).is(Blocks.IRON_BARS))
                && (level.getBlockState(base.above(2)).isAir()
                    || repair && level.getBlockState(base.above(2)).is(Blocks.DISPENSER));
    }

    private static boolean place(ServerLevel level, BlockPos base, int slot, boolean repair) {
        if (!canPlace(level, base, repair)) return false;
        if (level.getBlockState(base).isAir() && !level.setBlock(base, Blocks.CHEST.defaultBlockState(), 3)) return false;
        if (level.getBlockState(base.above()).isAir()
                && !level.setBlock(base.above(), Blocks.IRON_BARS.defaultBlockState(), 3)) return false;
        Direction facing = OFFSETS[slot][0] < 0 ? Direction.WEST : Direction.EAST;
        if (level.getBlockState(base.above(2)).isAir()
                && !level.setBlock(base.above(2), Blocks.DISPENSER.defaultBlockState()
                        .setValue(DispenserBlock.FACING, facing), 3)) return false;
        return operational(level, base);
    }

    private static void insert(Container chest, ItemStack stack) {
        for (int i = 0; i < chest.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack held = chest.getItem(i);
            if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
                int moved = Math.min(stack.getCount(), Math.min(held.getMaxStackSize(), chest.getMaxStackSize()) - held.getCount());
                if (moved > 0) { held.grow(moved); stack.shrink(moved); chest.setItem(i, held); }
            }
        }
        for (int i = 0; i < chest.getContainerSize() && !stack.isEmpty(); i++)
            if (chest.getItem(i).isEmpty())
                chest.setItem(i, stack.split(Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), chest.getMaxStackSize()))));
    }
}
