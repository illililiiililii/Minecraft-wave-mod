package dev.endlessdefense;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.UUID;

@GameTestHolder(EndlessDefense.MODID)
public final class DefenseGameTests {
    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void optionalDefenseWorldPreset(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var presets = registries.registryOrThrow(Registries.WORLD_PRESET);
        var key = ResourceKey.create(Registries.WORLD_PRESET,
                ResourceLocation.fromNamespaceAndPath(EndlessDefense.MODID, "defense"));
        WorldPreset preset = presets.get(key);
        if (preset == null || !presets.getHolderOrThrow(key).is(WorldPresetTags.NORMAL))
            throw new AssertionError("Defense world type is missing from normal world creation choices");
        var overworld = preset.overworld().orElseThrow();
        var typeKey = overworld.type().unwrapKey().orElseThrow();
        if (!typeKey.location().equals(ResourceLocation.fromNamespaceAndPath(EndlessDefense.MODID, "defense_overworld")))
            throw new AssertionError("Defense world type marker is missing");
        if (!(overworld.generator() instanceof NoiseBasedChunkGenerator noise)
                || !noise.generatorSettings().is(NoiseGeneratorSettings.OVERWORLD))
            throw new AssertionError("Defense map must keep vanilla survival terrain generation");
        if (DefenseMap.selected(helper.getLevel()))
            throw new AssertionError("Ordinary GameTest world was mistaken for a defense world");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 200)
    public static void defenseCourtyardAndApproach(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(128, 1, 0));
        int x = origin.getX(), y = 64, z = origin.getZ();
        DefenseMap.build(level, x, y, z);
        if (!level.getBlockState(new BlockPos(x, y - 1, z)).is(Blocks.GLOWSTONE)
                || !level.getBlockState(new BlockPos(x, y, z)).isAir())
            throw new AssertionError("Defense courtyard floor or clearing missing");
        for (int side : new int[] {-18, 18}) {
            if (!level.getBlockState(new BlockPos(x + side, y, z)).isAir()
                    || !level.getBlockState(new BlockPos(x, y, z + side)).isAir()
                    || !level.getBlockState(new BlockPos(x + side, y, z + 7)).is(Blocks.STONE_BRICKS)
                    || !level.getBlockState(new BlockPos(x + 7, y, z + side)).is(Blocks.STONE_BRICKS))
                throw new AssertionError("Defense wall or one of four gates missing");
        }
        boolean approach = false;
        for (int height = y - 13; height < y; height++)
            approach |= level.getBlockState(new BlockPos(x + 25, height, z)).is(Blocks.GRAVEL);
        if (!approach || !level.getBlockState(new BlockPos(x + 15, y + 4, z + 15)).is(Blocks.POLISHED_ANDESITE))
            throw new AssertionError("Defense approach or watchtower missing");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void registeredMobsAndOperatorSettings(GameTestHelper helper) {
        var runner = helper.spawn(ModMobs.ASH_RUNNER.get(), 1, 1, 1);
        var bulwark = helper.spawn(ModMobs.IRON_BULWARK.get(), 2, 1, 1);
        if (!runner.isAlive() || !bulwark.isAlive()) throw new AssertionError("Custom mobs did not spawn");
        if (!runner.isCurrentlyGlowing() || !bulwark.isCurrentlyGlowing())
            throw new AssertionError("Custom monsters are not permanently marked glowing");
        if (runner.getTeam() == null || runner.getTeam().getColor() != ChatFormatting.AQUA
                || bulwark.getTeam() == null || bulwark.getTeam().getColor() != ChatFormatting.GOLD)
            throw new AssertionError("Monster glow team colors missing");
        var wild = helper.spawn(EntityType.ZOMBIE, 0, 1, 0);
        if (wild.isCurrentlyGlowing()) throw new AssertionError("Wild zombie must not glow");
        var skeleton = helper.spawn(EntityType.SKELETON, 0, 1, 2);
        if (wild.isCustomNameVisible() || skeleton.isCustomNameVisible())
            throw new AssertionError("Wild zombie and skeleton must not get wave health nameplates");
        if (skeleton.isCurrentlyGlowing()) throw new AssertionError("Wild skeleton must not glow");
        wild.addTag("endlessdefense_wave");
        EndlessDefense.assignGlow(wild);
        if (wild.getTeam() == null || !wild.getTeam().getName().startsWith("ed_wave_"))
            throw new AssertionError("Wave monster lacks a client-visible wave team for its health bar");
        wild.addTag("endlessdefense_infuser");
        EndlessDefense.assignGlow(wild);
        if (wild.getTeam().getColor() != ChatFormatting.LIGHT_PURPLE)
            throw new AssertionError("Infuser must have a distinct purple outline");
        wild.removeTag("endlessdefense_wave");
        if (runner.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) <= bulwark.getAttributeBaseValue(Attributes.MOVEMENT_SPEED))
            throw new AssertionError("Runner should be faster than bulwark");
        if (bulwark.getAttributeBaseValue(Attributes.ARMOR) < 8)
            throw new AssertionError("Bulwark armor attribute missing");
        var cow = helper.spawn(EntityType.COW, 1, 1, 2);
        if (!runner.doHurtTarget(cow) || !cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
            throw new AssertionError("Runner's slowing attack did not apply");

        var server = helper.getLevel().getServer();
        var data = WaveData.get(server);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense config count 12");
        if (data.baseMobs != 12) throw new AssertionError("Operator setting command did not apply");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense config count 4");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance night 20");
        if (data.nightPercent != 20) throw new AssertionError("Balance command did not persist night ratio");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance night 15");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance infuser 5");
        if (data.infuserChancePercent != 5) throw new AssertionError("Infuser spawn setting was not applied");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance infuser 1");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void settingsRejectUnauthorizedAndOutOfRange(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var data = WaveData.get(server);
        int original = data.baseMobs;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(0),
                "endlessdefense config count 12");
        if (data.baseMobs != original) throw new AssertionError("Non-OP changed settings");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense config count 129");
        if (data.baseMobs != original) throw new AssertionError("Out-of-range setting was accepted");
        int originalNight = data.nightPercent;
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(0),
                "endlessdefense balance night 25");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance night 26");
        if (data.nightPercent != originalNight) throw new AssertionError("Balance permission or range check failed");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void shopCostsAndPlayerScaling(GameTestHelper helper) {
        if (ShopRules.enemyCount(4, 1, 1) != 4 || ShopRules.enemyCount(4, 2, 1) != 10
                || ShopRules.enemyCount(4, 4, 1) != 22)
            throw new AssertionError("Player count scaling incorrect");
        if (ShopRules.enemyCount(4, 2, 2) != 14 || ShopRules.enemyCount(4, 8, 1000) != 128)
            throw new AssertionError("Wave scaling or cap incorrect");
        WaveData balance = new WaveData();
        if (ShopRules.reward(1) != 100 || ShopRules.reward(2) != 130 || ShopRules.reward(5) != 220
                || ShopRules.reward(20) != 1120 || ShopRules.reward(21) != 1210)
            throw new AssertionError("Wave reward balance incorrect");
        if (BalanceRules.restMin(balance, 0) != 45 || BalanceRules.restMax(balance, 0) != 120
                || BalanceRules.restMin(balance, 20) != 53 || BalanceRules.restMax(balance, 20) != 132
                || BalanceRules.restMin(balance, 100) != 85 || BalanceRules.restMax(balance, 100) != 180)
            throw new AssertionError("Rest interval must grow with waves and remain capped");
        balance.earlyBonusThroughWave = 3;
        if (BalanceRules.clearReward(balance, 1) != 350 || BalanceRules.clearReward(balance, 2) != 380
                || BalanceRules.clearReward(balance, 3) != 410 || BalanceRules.clearReward(balance, 4) != 190)
            throw new AssertionError("Random-duration opening rewards are not limited to selected rounds");
        if (BalanceRules.nightCount(balance, 10, true) != 12 || BalanceRules.nightCount(balance, 10, false) != 10
                || BalanceRules.nightCount(balance, 128, true) != 128)
            throw new AssertionError("Night bonus incorrect");
        if (BalanceRules.initialTickets(balance, 1) != 4 || BalanceRules.initialTickets(balance, 2) != 4
                || BalanceRules.initialTickets(balance, 4) != 5 || BalanceRules.ticketCap(balance, 30, 4) != 1
                || BalanceRules.ticketCap(balance, 1, 1) != 4 || BalanceRules.ticketCap(balance, 15, 4) != 2
                || BalanceRules.ticketCap(balance, 30, 1) != 1)
            throw new AssertionError("Team respawn balance incorrect");
        boolean[] covered = new boolean[ShopRules.NAMES.length];
        for (int id : ShopRules.UPGRADE_IDS) covered[id] = true;
        for (ShopRules.Category category : ShopRules.Category.values()) {
            long previous = -1;
            for (int id : ShopRules.catalog(category)) {
                if (covered[id] || ShopRules.category(id) != category || ShopRules.cost(id, 0) < previous)
                    throw new AssertionError("Kiosk category or price order incorrect");
                covered[id] = true;
                previous = ShopRules.cost(id, 0);
            }
        }
        for (boolean found : covered) if (!found) throw new AssertionError("Shop product missing from kiosk");
        if (!ShopRules.NAMES[7].equals("공격력 강화") || !ShopRules.nextEffect(7, 1).contains("+2"))
            throw new AssertionError("Attack upgrade is not visible or scaled");
        long[] expected = {30, 75, 188, 469, 1172};
        for (int i = 0; i < expected.length; i++)
            if (ShopRules.cost(0, i) != expected[i]) throw new AssertionError("Upgrade price incorrect at " + i);
        if (ShopRules.cost(0, 5) <= expected[4] || ShopRules.cost(9, 3) <= expected[2]
                || ShopRules.cost(5, 0) != 10000000 || ShopRules.cost(27, 0) != 80
                || ShopRules.cost(29, 0) != 30 || ShopRules.cost(29, 5) <= expected[4]
                || ShopRules.category(31) != ShopRules.Category.UTILITY
                || ShopRules.category(32) != ShopRules.Category.BUILDING)
            throw new AssertionError("Unlimited upgrade or endgame price incorrect");
        if (!DefaultAttributes.getSupplier(EntityType.PLAYER).hasAttribute(Attributes.ATTACK_KNOCKBACK))
            throw new AssertionError("Player has no attack knockback attribute for the new upgrade");
        if (ShopRules.gear(13).length != 2 || ShopRules.gear(13)[0].get(DataComponents.POTION_CONTENTS) == null)
            throw new AssertionError("Healing potion lacks potion contents");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void shopWalletPurchaseAndSave(GameTestHelper helper) {
        WaveData data = new WaveData();
        UUID id = UUID.randomUUID();
        data.wave = 1;
        if (data.buy(id, 0) || data.buy(id, -1)) throw new AssertionError("Unfunded or invalid purchase accepted");
        data.award(id, 293);
        if (!data.buy(id, 0) || !data.buy(id, 0) || !data.buy(id, 0))
            throw new AssertionError("Funded upgrade purchase failed");
        if (data.account(id).coins != 0 || data.account(id).levels[0] != 3 || data.buy(id, 3))
            throw new AssertionError("Wallet or equipment pricing incorrect");
        data.started = true;
        data.gameOver = true;
        data.waveInProgress = true;
        data.respawns = 3;
        data.earlyBonusThroughWave = 4;
        data.account(id).pendingInfusions = 2;
        data.defenseMapBuilt = true;
        data.defenseMapX = 123;
        data.defenseMapY = 68;
        data.defenseMapZ = -456;
        data.eliminated.add(UUID.randomUUID());
        data.teamMembers.add(id);
        CompoundTag saved = data.save(new CompoundTag(), helper.getLevel().registryAccess());
        if (!saved.getBoolean("started") || saved.getList("accounts", Tag.TAG_COMPOUND).size() != 1)
            throw new AssertionError("Run state or wallet was not serialized");
        WaveData loaded = WaveData.load(saved, helper.getLevel().registryAccess());
        if (loaded.account(id).levels[0] != 3 || loaded.respawns != 3 || loaded.eliminated.size() != 1
                || !loaded.gameOver || !loaded.waveInProgress || !loaded.teamMembers.contains(id)
                || !loaded.defenseMapBuilt || loaded.defenseMapX != 123
                || loaded.defenseMapY != 68 || loaded.defenseMapZ != -456
                || loaded.earlyBonusThroughWave != 4 || loaded.account(id).pendingInfusions != 2)
            throw new AssertionError("Wallet or respawn state was not restored");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void limitedStockAndReset(GameTestHelper helper) {
        WaveData data = new WaveData();
        UUID id = UUID.randomUUID();
        data.wave = 5;
        data.award(id, 1000);
        if (!data.buy(id, 12) || !data.buy(id, 12) || data.buy(id, 12))
            throw new AssertionError("Per-wave stock limit failed");
        data.wave = 6;
        data.award(id, 350);
        if (!data.buy(id, 12)) throw new AssertionError("Stock did not refresh on next wave");
        data.started = true;
        data.gameOver = true;
        data.earlyBonusThroughWave = 5;
        data.account(id).pendingInfusions = 2;
        data.eliminated.add(id);
        data.teamMembers.add(id);
        data.resetRun();
        if (data.started || data.gameOver || data.wave != 0 || data.account(id).coins != 0
                || data.earlyBonusThroughWave != 0 || data.account(id).pendingInfusions != 0)
            throw new AssertionError("Surrender reset did not clear run economy");
        if (!data.restoreSurvival.contains(id) || data.eliminated.contains(id) || !data.teamMembers.isEmpty())
            throw new AssertionError("Offline spectator recovery missing");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void legacyWalletAndDirectWeaponInfusion(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        WaveData old = new WaveData();
        old.award(id, 40);
        CompoundTag legacy = old.save(new CompoundTag(), helper.getLevel().registryAccess());
        legacy.remove("economyVersion");
        legacy.putInt("earlyReward", 7);
        legacy.putInt("midRewardStep", 2);
        WaveData migrated = WaveData.load(legacy, helper.getLevel().registryAccess());
        if (migrated.account(id).coins != 400 || migrated.earlyReward != 100 || migrated.midRewardStep != 60)
            throw new AssertionError("Legacy economy was not migrated once");
        CompoundTag current = migrated.save(new CompoundTag(), helper.getLevel().registryAccess());
        if (WaveData.load(current, helper.getLevel().registryAccess()).account(id).coins != 400)
            throw new AssertionError("Currency migration applied twice");
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        var choices = EndlessDefense.infusionOptions(sword, helper.getLevel().registryAccess());
        var repeatable = choices.stream().filter(enchantment -> enchantment.value().getMaxLevel() > 1).findFirst();
        if (repeatable.isEmpty()) throw new AssertionError("Weapon infusion has no repeatable enchantments");
        sword.enchant(repeatable.get(), 1);
        if (sword.get(DataComponents.ENCHANTMENTS) == null
                || !EndlessDefense.infusionOptions(sword, helper.getLevel().registryAccess()).contains(repeatable.get())
                || !EndlessDefense.infusionOptions(new ItemStack(Items.BREAD), helper.getLevel().registryAccess()).isEmpty())
            throw new AssertionError("Repeatable weapon enchantment or compatibility filter failed");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void snowySpawnSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos snow = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlock(snow.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(snow, Blocks.SNOW.defaultBlockState(), 3);
        if (!EndlessDefense.hasSafeSupport(level, snow))
            throw new AssertionError("Thin snow over solid ground should allow wave spawns");
        level.setBlock(snow, Blocks.POWDER_SNOW.defaultBlockState(), 3);
        if (EndlessDefense.hasSafeSupport(level, snow))
            throw new AssertionError("Powder snow must not count as safe spawn support");
        helper.succeed();
    }
}
