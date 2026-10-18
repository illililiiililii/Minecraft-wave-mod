package dev.endlessdefense;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.List;

/** One-time, survival-compatible fortification for the optional Defense Survival world type. */
final class DefenseMap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(EndlessDefense.MODID, "defense_overworld");
    private DefenseMap() {}

    static boolean selected(ServerLevel level) {
        return level.dimensionTypeRegistration().unwrapKey()
                .map(key -> key.location().equals(TYPE)).orElse(false);
    }

    static void ensure(MinecraftServer server) {
        ServerLevel level = server.overworld();
        WaveData data = WaveData.get(server);
        if (data.defenseMapBuilt || !selected(level)) return;
        BlockPos origin = level.getSharedSpawnPos();
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
        int y = Math.max(level.getMinBuildHeight() + 8, Math.min(surface.getY(), level.getMaxBuildHeight() - 12));
        build(level, surface.getX(), y, surface.getZ());
        data.defenseMapX = surface.getX();
        data.defenseMapY = y;
        data.defenseMapZ = surface.getZ();
        data.defenseMapBuilt = true;
        data.setDirty();
        level.setDefaultSpawnPos(new BlockPos(surface.getX() + 4, y - 1, surface.getZ()), 0);
        LOGGER.info("Defense Survival courtyard created at {}, {}, {}", surface.getX(), y, surface.getZ());
    }

    static void build(ServerLevel level, int cx, int y, int cz) {
        // Clear an open courtyard, then support it over caves or uneven ground.
        for (int dx = -19; dx <= 19; dx++) for (int dz = -19; dz <= 19; dz++) {
            int x = cx + dx, z = cz + dz;
            for (int h = 0; h <= 20; h++) place(level, x, y + h, z, Blocks.AIR);
            for (int h = -4; h <= -2; h++) {
                BlockPos pos = new BlockPos(x, y + h, z);
                if (level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty())
                    place(level, x, y + h, z, Blocks.STONE);
            }
            Block floor = dx % 8 == 0 && dz % 8 == 0 ? Blocks.GLOWSTONE
                    : Math.abs(dx) <= 3 && Math.abs(dz) <= 3 ? Blocks.POLISHED_ANDESITE
                    : Math.abs(dx) <= 2 || Math.abs(dz) <= 2 ? Blocks.STONE_BRICKS
                    : (dx * 31 + dz * 17) % 11 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.COBBLESTONE;
            place(level, x, y - 1, z, floor);
        }

        // Four five-block gates and corner towers create clear lines of approach.
        for (int d = -18; d <= 18; d++) {
            if (Math.abs(d) <= 2) continue;
            for (int h = 0; h <= 2; h++) {
                place(level, cx + d, y + h, cz - 18, Blocks.STONE_BRICKS);
                place(level, cx + d, y + h, cz + 18, Blocks.STONE_BRICKS);
                place(level, cx - 18, y + h, cz + d, Blocks.STONE_BRICKS);
                place(level, cx + 18, y + h, cz + d, Blocks.STONE_BRICKS);
            }
            if ((d & 1) == 0) {
                place(level, cx + d, y + 3, cz - 18, Blocks.STONE_BRICK_WALL);
                place(level, cx + d, y + 3, cz + 18, Blocks.STONE_BRICK_WALL);
                place(level, cx - 18, y + 3, cz + d, Blocks.STONE_BRICK_WALL);
                place(level, cx + 18, y + 3, cz + d, Blocks.STONE_BRICK_WALL);
            }
        }
        for (int sx : new int[] {-1, 1}) for (int sz : new int[] {-1, 1}) {
            int tx = cx + sx * 15, tz = cz + sz * 15;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                for (int h = 0; h <= 3; h++) place(level, tx + dx, y + h, tz + dz, Blocks.STONE_BRICKS);
                place(level, tx + dx, y + 4, tz + dz, Blocks.POLISHED_ANDESITE);
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2)
                    place(level, tx + dx, y + 5, tz + dz, Blocks.STONE_BRICK_WALL);
            }
            int lx = cx + sx * 12;
            Direction facing = sx > 0 ? Direction.WEST : Direction.EAST;
            for (int h = 1; h <= 4; h++)
                level.setBlock(new BlockPos(lx, y + h, tz),
                        Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing), 2);
        }
        for (int dx : new int[] {-6, 6}) for (int dz : new int[] {-6, 6}) {
            for (int h = 0; h < 3; h++) place(level, cx + dx, y + h, cz + dz, Blocks.OAK_FENCE);
            place(level, cx + dx, y + 3, cz + dz, Blocks.LANTERN);
        }
        for (int side = 0; side < 4; side++) buildApproach(level, cx, y, cz, side);
        placeCore(level, new BlockPos(cx, y, cz));
    }

    static BlockPos core(WaveData data) {
        return new BlockPos(data.defenseMapX, data.defenseMapY, data.defenseMapZ);
    }

    static void placeCore(ServerLevel level, BlockPos core) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            place(level, core.getX() + dx, core.getY() - 1, core.getZ() + dz, Blocks.IRON_BLOCK);
        place(level, core.getX(), core.getY(), core.getZ(), Blocks.BEACON);
    }

    static int repairWalls(ServerLevel level, WaveData data, int limit) {
        int repaired = 0, cx = data.defenseMapX, cy = data.defenseMapY, cz = data.defenseMapZ;
        for (int d = -18; d <= 18 && repaired < limit; d++) {
            if (Math.abs(d) <= 2) continue;
            for (int h = 0; h <= 2 && repaired < limit; h++) {
                BlockPos[] positions = {new BlockPos(cx + d, cy + h, cz - 18),
                        new BlockPos(cx + d, cy + h, cz + 18),
                        new BlockPos(cx - 18, cy + h, cz + d),
                        new BlockPos(cx + 18, cy + h, cz + d)};
                for (BlockPos pos : positions) {
                    if (repaired >= limit) break;
                    if (level.getBlockState(pos).isAir() && level.setBlock(pos, Blocks.STONE_BRICKS.defaultBlockState(), 3)) repaired++;
                }
            }
        }
        return repaired;
    }

    private static void buildApproach(ServerLevel level, int cx, int y, int cz, int side) {
        int pathY = y;
        for (int distance = 19; distance <= 30; distance++) {
            int centerX = cx + (side == 0 ? distance : side == 2 ? -distance : 0);
            int centerZ = cz + (side == 1 ? distance : side == 3 ? -distance : 0);
            int naturalY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(centerX, y, centerZ)).getY();
            pathY += Integer.compare(naturalY, pathY);
            for (int lateral = -2; lateral <= 2; lateral++) {
                int x = centerX + (side % 2 == 1 ? lateral : 0);
                int z = centerZ + (side % 2 == 0 ? lateral : 0);
                for (int h = 0; h <= 4; h++) place(level, x, pathY + h, z, Blocks.AIR);
                for (int h = -3; h <= -2; h++) {
                    BlockPos below = new BlockPos(x, pathY + h, z);
                    if (level.getBlockState(below).isAir() || !level.getFluidState(below).isEmpty())
                        place(level, x, pathY + h, z, Blocks.STONE);
                }
                place(level, x, pathY - 1, z, distance < 23 ? Blocks.STONE_BRICKS : Blocks.GRAVEL);
            }
        }
    }

    static BlockPos laneSpawn(ServerLevel level, ServerPlayer target, List<ServerPlayer> players, WaveData data, int index) {
        if (!data.defenseMapBuilt || level != target.serverLevel()) return null;
        double ox = target.getX() - data.defenseMapX, oz = target.getZ() - data.defenseMapZ;
        if (ox * ox + oz * oz > 72 * 72) return null;
        for (int attempt = 0; attempt < 24; attempt++) {
            int side = (index + attempt / 6) % 4;
            int distance = 27 + level.random.nextInt(12);
            int lateral = level.random.nextInt(17) - 8;
            int x = data.defenseMapX + (side == 0 ? distance : side == 2 ? -distance : lateral);
            int z = data.defenseMapZ + (side == 1 ? distance : side == 3 ? -distance : lateral);
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, data.defenseMapY, z));
            BlockPos spot = level.getBlockState(surface).is(Blocks.SNOW) ? surface.above() : surface;
            if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()
                    || !EndlessDefense.hasSafeSupport(level, spot.below())
                    || !level.getFluidState(spot).isEmpty()
                    || players.stream().anyMatch(p -> p.serverLevel() == level &&
                    p.distanceToSqr(spot.getX() + .5, spot.getY(), spot.getZ() + .5) < 100)
                    || !level.getEntitiesOfClass(Monster.class, new AABB(spot).inflate(1.25)).isEmpty()) continue;
            return spot;
        }
        return null;
    }

    private static void place(ServerLevel level, int x, int y, int z, Block block) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getBlockState(pos).is(block)) level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
