package dev.endlessdefense;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import java.util.UUID;

@GameTestHolder(EndlessDefense.MODID)
public final class DefenseGameTests {
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
                "endlessdefense balance enchantDrop 20");
        if (data.enchantDropPercent != 20) throw new AssertionError("Loot setting was not applied");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withPermission(4),
                "endlessdefense balance enchantDrop 10");
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
        if (BalanceRules.restMin(balance, 0) != 11 || BalanceRules.restMax(balance, 0) != 60
                || BalanceRules.restMin(balance, 20) != 19 || BalanceRules.restMax(balance, 20) != 72
                || BalanceRules.restMin(balance, 100) != 35 || BalanceRules.restMax(balance, 100) != 100)
            throw new AssertionError("Rest interval must grow with waves and remain capped");
        if (BalanceRules.nightCount(balance, 10, true) != 12 || BalanceRules.nightCount(balance, 10, false) != 10
                || BalanceRules.nightCount(balance, 128, true) != 128)
            throw new AssertionError("Night bonus incorrect");
        if (BalanceRules.initialTickets(balance, 1) != 4 || BalanceRules.initialTickets(balance, 2) != 4
                || BalanceRules.initialTickets(balance, 4) != 5 || BalanceRules.ticketCap(balance, 30, 4) != 1
                || BalanceRules.chapterGrant(balance, 6, 4) != 2 || BalanceRules.chapterGrant(balance, 7, 4) != 0)
            throw new AssertionError("Team respawn balance incorrect");
        long[] expected = {30, 100, 300, 850, 2000};
        for (int i = 0; i < expected.length; i++)
            if (ShopRules.cost(0, i) != expected[i]) throw new AssertionError("Upgrade price incorrect at " + i);
        if (ShopRules.cost(0, 5) != -1 || ShopRules.cost(9, 3) != -1
                || ShopRules.cost(5, 0) != 10000000 || ShopRules.cost(27, 0) != 80)
            throw new AssertionError("Max level or endgame price incorrect");
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
        data.award(id, 430);
        if (!data.buy(id, 0) || !data.buy(id, 0) || !data.buy(id, 0))
            throw new AssertionError("Funded upgrade purchase failed");
        if (data.account(id).coins != 0 || data.account(id).levels[0] != 3 || data.buy(id, 3))
            throw new AssertionError("Wallet or equipment pricing incorrect");
        data.started = true;
        data.gameOver = true;
        data.waveInProgress = true;
        data.respawns = 3;
        data.eliminated.add(UUID.randomUUID());
        data.teamMembers.add(id);
        CompoundTag saved = data.save(new CompoundTag(), helper.getLevel().registryAccess());
        if (!saved.getBoolean("started") || saved.getList("accounts", Tag.TAG_COMPOUND).size() != 1)
            throw new AssertionError("Run state or wallet was not serialized");
        WaveData loaded = WaveData.load(saved, helper.getLevel().registryAccess());
        if (loaded.account(id).levels[0] != 3 || loaded.respawns != 3 || loaded.eliminated.size() != 1
                || !loaded.gameOver || !loaded.waveInProgress || !loaded.teamMembers.contains(id))
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
        data.eliminated.add(id);
        data.teamMembers.add(id);
        data.resetRun();
        if (data.started || data.gameOver || data.wave != 0 || data.account(id).coins != 0)
            throw new AssertionError("Surrender reset did not clear run economy");
        if (!data.restoreSurvival.contains(id) || data.eliminated.contains(id) || !data.teamMembers.isEmpty())
            throw new AssertionError("Offline spectator recovery missing");
        helper.succeed();
    }

    @GameTest(template = "forge:empty3x3x3", timeoutTicks = 100)
    public static void legacyWalletAndEnchantedLoot(GameTestHelper helper) {
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
        ItemStack book = EnchantmentHelper.enchantItem(helper.getLevel().random,
                new ItemStack(Items.BOOK), 15, helper.getLevel().registryAccess(), java.util.Optional.empty());
        if (!book.is(Items.ENCHANTED_BOOK) || book.get(DataComponents.STORED_ENCHANTMENTS) == null)
            throw new AssertionError("Random enchanted book could not be generated");
        helper.succeed();
    }
}
