package dev.endlessdefense;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;
import java.util.UUID;

/** Server-owned defense objective. Only wave mobs can damage the beacon and scripted fort walls. */
final class BaseSystem {
    private BaseSystem() {}

    static int maxHp(WaveData data) {
        long hp = data.number("coreBaseHp", 100)
                + Math.max(0L, (long) data.coreTier - 1) * data.number("coreHpStep", 50);
        return (int) Math.min(Integer.MAX_VALUE, hp);
    }

    static void beginRun(MinecraftServer server, WaveData data) {
        if (!DefenseMap.selected(server.overworld()) || !data.defenseMapBuilt) return;
        data.coreTier = 1;
        data.coreHp = maxHp(data);
        data.coreInitialized = true;
        DefenseMap.placeCore(server.overworld(), DefenseMap.core(data));
        data.setDirty();
    }

    static void ensureLegacy(MinecraftServer server, WaveData data) {
        if (!data.started || data.gameOver || data.coreInitialized || !data.defenseMapBuilt
                || !DefenseMap.selected(server.overworld())) return;
        DefenseMap.placeCore(server.overworld(), DefenseMap.core(data));
        data.coreHp = maxHp(data);
        data.coreInitialized = true;
        data.setDirty();
    }

    static String purchase(ServerPlayer player, WaveData data, int product) {
        ServerLevel level = player.server.overworld();
        if (player.serverLevel() != level) return "기지 관리는 디펜스 야생의 오버월드에서만 가능합니다.";
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt)
            return "기지 상품은 디펜스 야생 거점에서만 사용할 수 있습니다.";
        if (!data.started || data.gameOver || data.eliminated.contains(player.getUUID())
                || player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR)
            return "진행 중인 게임의 생존자만 기지를 관리할 수 있습니다.";
        if (data.wave < data.shopUnlock(product)) return "아직 해금되지 않은 상품입니다.";
        if (!level.getBlockState(DefenseMap.core(data)).is(Blocks.BEACON))
            return "중앙 핵심이 파괴되었습니다.";
        if (product == ShopRules.BASE_UPGRADE_ID && data.coreTier == Integer.MAX_VALUE)
            return "기지 단계 저장 한계에 도달했습니다.";

        long price = product == ShopRules.BASE_UPGRADE_ID
                ? ShopRules.upgradeCost(data.coreTier - 1,
                        data.number("baseUpgradeBase", 600), data.number("baseUpgradeGrowth", 80))
                : data.shopPrice(product);
        if (price < 0) return "기지 단계 저장 한계에 도달했습니다.";
        WaveData.Account account = data.account(player.getUUID());
        if (account.stockWave != data.wave) {
            account.stockWave = data.wave;
            java.util.Arrays.fill(account.stock, 0);
        }
        int cap = data.shopCap(product);
        if (cap > 0 && account.stock[product] >= cap) return "이번 웨이브의 수리 재고를 모두 사용했습니다.";
        if (account.coins < price) return "노드가 부족합니다. 필요 노드: " + price;
        if (product == ShopRules.CORE_REPAIR_ID && data.coreHp >= maxHp(data))
            return "중앙 핵심 체력이 이미 가득 찼습니다.";
        if (product == ShopRules.WALL_REPAIR_ID) {
            int repaired = DefenseMap.repairWalls(level, data, data.number("wallRepairCount", 24));
            if (repaired == 0) return "수리할 빈 성벽 자리가 없습니다.";
            account.coins -= price;
            if (cap > 0) account.stock[product]++;
            data.setDirty();
            return "성벽 " + repaired + "블록 수리 완료";
        }
        if (product == ShopRules.BASE_UPGRADE_ID) {
            data.coreTier++;
            data.coreHp = maxHp(data);
        } else if (product == ShopRules.CORE_REPAIR_ID) {
            data.coreHp = (int) Math.min(maxHp(data),
                    (long) data.coreHp + data.number("coreRepairAmount", 40));
        } else return "알 수 없는 기지 상품입니다.";
        account.coins -= price;
        if (cap > 0) account.stock[product]++;
        data.setDirty();
        return "구매 완료: " + ShopRules.NAMES[product] + " · 핵심 " + data.coreHp + "/" + maxHp(data)
                + " · 기지 단계 " + data.coreTier;
    }

    /** Called once each second during a wave; returns true when the objective is destroyed. */
    static boolean tick(MinecraftServer server, WaveData data, Set<UUID> waveMobs) {
        ServerLevel level = server.overworld();
        if (!DefenseMap.selected(level) || !data.defenseMapBuilt) return false;
        BlockPos core = DefenseMap.core(data);
        if (data.coreHp <= 0 || !level.getBlockState(core).is(Blocks.BEACON)) return true;
        int damage = 0, broken = 0;
        long breachPeriod = 20L * data.number("wallBreachSeconds", 5) * Math.min(100, data.coreTier);
        boolean breachNow = server.getTickCount() % breachPeriod < 20;
        for (UUID id : waveMobs) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Monster mob) || !mob.isAlive()) continue;
            double distance = mob.distanceToSqr(core.getX() + .5, core.getY() + .5, core.getZ() + .5);
            if (distance <= 10 && Math.abs(mob.getY() - core.getY()) < 3) {
                damage += mob.getTags().contains("endlessdefense_boss") ? data.number("coreBossDamage", 4)
                        : mob instanceof ModMobs.SiegeBrute || mob instanceof ModMobs.DreadKnight
                        ? data.number("coreSiegeDamage", 2) : data.number("coreNormalDamage", 1);
                continue;
            }
            if (distance > 48 * 48) continue;
            if ((mob instanceof ModMobs.SiegeBrute || mob instanceof ModMobs.DreadKnight)
                    && (mob.getTarget() == null || mob.distanceToSqr(mob.getTarget()) > 12 * 12))
                mob.getNavigation().moveTo(core.getX() + .5, core.getY(), core.getZ() + .5, 1.0);
            if (breachNow && broken < data.number("wallBreakCap", 2) && breachWall(level, data, mob)) broken++;
        }
        if (damage > 0) {
            data.coreHp = Math.max(0, data.coreHp - Math.min(data.number("coreDamageCap", 12), damage));
            data.setDirty();
            if (data.coreHp == 0) {
                level.removeBlock(core, false);
                return true;
            }
        }
        return false;
    }

    private static boolean breachWall(ServerLevel level, WaveData data, Monster mob) {
        int dx = mob.getBlockX() - data.defenseMapX, dz = mob.getBlockZ() - data.defenseMapZ;
        boolean xSide = Math.abs(dx) >= Math.abs(dz);
        int outward = xSide ? Math.abs(dx) : Math.abs(dz);
        if (outward < 17 || outward > 22) return false;
        int lateral = xSide ? dz : dx;
        if (Math.abs(lateral) <= 2 || Math.abs(lateral) > 17) return false;
        int x = data.defenseMapX + (xSide ? Integer.signum(dx) * 18 : lateral);
        int z = data.defenseMapZ + (xSide ? lateral : Integer.signum(dz) * 18);
        for (int h = 1; h <= 2; h++) {
            BlockPos wall = new BlockPos(x, data.defenseMapY + h, z);
            if (level.getBlockState(wall).is(Blocks.STONE_BRICKS)
                    || level.getBlockState(wall).is(Blocks.MOSSY_STONE_BRICKS)
                    || level.getBlockState(wall).is(Blocks.STONE_BRICK_WALL)) {
                level.destroyBlock(wall, false, mob);
                return true;
            }
        }
        BlockPos wall = new BlockPos(x, data.defenseMapY, z);
        if (level.getBlockState(wall).is(Blocks.STONE_BRICKS)
                || level.getBlockState(wall).is(Blocks.MOSSY_STONE_BRICKS)) {
            level.destroyBlock(wall, false, mob);
            return true;
        }
        return false;
    }
}
