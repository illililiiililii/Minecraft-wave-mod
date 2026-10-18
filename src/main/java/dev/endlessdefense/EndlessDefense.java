package dev.endlessdefense;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod(EndlessDefense.MODID)
public final class EndlessDefense {
    public static final String MODID = "endlessdefense";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(MODID, "main"))
            .networkProtocolVersion(5).simpleChannel();
    private static final Map<MinecraftServer, Session> SESSIONS = new WeakHashMap<>();

    public EndlessDefense(FMLJavaModLoadingContext context) {
        ModMobs.TYPES.register(context.getModEventBus());
        CHANNEL.messageBuilder(ReadyPacket.class, 0)
                .encoder((message, buffer) -> buffer.writeBoolean(message.desired))
                .decoder(buffer -> new ReadyPacket(buffer.readBoolean()))
                .consumerMainThread(ReadyPacket::handle).add();
        CHANNEL.messageBuilder(StatusPacket.class, 1)
                .encoder((message, buffer) -> {
                    buffer.writeVarInt(message.wave);
                    buffer.writeVarInt(message.highest);
                    buffer.writeVarInt(message.phase);
                    buffer.writeVarInt(message.countdown);
                    buffer.writeBoolean(message.selfReady);
                    buffer.writeVarInt(message.ready);
                    buffer.writeVarInt(message.total);
                    buffer.writeVarInt(message.remaining);
                    buffer.writeUtf(message.roster, 2048);
                    buffer.writeBoolean(message.canConfigure);
                    buffer.writeVarInt(message.baseMobs);
                    buffer.writeVarInt(message.healthPercent);
                    buffer.writeVarInt(message.damagePercent);
                    buffer.writeVarInt(message.awareness);
                    buffer.writeVarInt(message.armorBonus);
                    buffer.writeVarInt(message.gearChance);
                    buffer.writeVarLong(message.coins);
                    for (int level : message.upgrades) buffer.writeVarInt(level);
                    for (int count : message.stock) buffer.writeVarInt(count);
                    buffer.writeVarInt(message.respawns);
                    buffer.writeBoolean(message.night);
                    buffer.writeVarInt(message.surrenderVotes);
                    buffer.writeBoolean(message.selfSurrender);
                })
                .decoder(buffer -> new StatusPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(2048),
                        buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                        buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarLong(),
                        readUpgrades(buffer),
                        readStock(buffer), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean()))
                .consumerMainThread(StatusPacket::handle).add();
        CHANNEL.messageBuilder(ConfigPacket.class, 2)
                .encoder((message, buffer) -> { buffer.writeVarInt(message.option); buffer.writeVarInt(message.value); })
                .decoder(buffer -> new ConfigPacket(buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread(ConfigPacket::handle).add();
        CHANNEL.messageBuilder(ShopPacket.class, 3)
                .encoder((message, buffer) -> buffer.writeVarInt(message.product))
                .decoder(buffer -> new ShopPacket(buffer.readVarInt()))
                .consumerMainThread(ShopPacket::handle).add();
        CHANNEL.messageBuilder(SurrenderPacket.class, 4)
                .encoder((message, buffer) -> buffer.writeBoolean(message.vote))
                .decoder(buffer -> new SurrenderPacket(buffer.readBoolean()))
                .consumerMainThread(SurrenderPacket::handle).add();
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Endless Survival Defense loaded");
    }

    private static Session session(MinecraftServer server) {
        return SESSIONS.computeIfAbsent(server, ignored -> new Session(WaveData.get(server).started, server));
    }

    public static void requestReady(boolean desired) {
        CHANNEL.send(new ReadyPacket(desired), PacketDistributor.SERVER.noArg());
    }

    public static void requestConfig(int option, int value) {
        CHANNEL.send(new ConfigPacket(option, value), PacketDistributor.SERVER.noArg());
    }

    public static void requestPurchase(int product) {
        CHANNEL.send(new ShopPacket(product), PacketDistributor.SERVER.noArg());
    }
    public static void requestSurrender(boolean vote) {
        CHANNEL.send(new SurrenderPacket(vote), PacketDistributor.SERVER.noArg());
    }

    private record SurrenderPacket(boolean vote) {
        private static void handle(SurrenderPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player != null) voteSurrender(player.server, player, message.vote);
        }
    }

    private record ReadyPacket(boolean desired) {
        private static void handle(ReadyPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            MinecraftServer server = player.server;
            Session game = session(server);
            if (game.phase != Phase.LOBBY) {
                player.displayClientMessage(Component.literal("첫 게임이 시작되어 준비 상태를 바꿀 수 없습니다."), true);
                return;
            }
            boolean changed = message.desired ? game.ready.add(player.getUUID()) : game.ready.remove(player.getUUID());
            if (!changed) return; // Duplicate or delayed click packets are idempotent.
            announce(server, player.getName().getString() + (message.desired ? " 준비 완료" : " 준비 취소"));
            sync(server);
            if (allReady(server, game)) startRun(server, game);
        }
    }

    private record ShopPacket(int product) {
        private static void handle(ShopPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player == null || message.product < 0 || message.product >= ShopRules.NAMES.length) return;
            WaveData data = WaveData.get(player.server);
            if (!data.buy(player.getUUID(), message.product)) {
                player.sendSystemMessage(Component.literal("[Endless Defense] 노드가 부족하거나 해금·재고·레벨 조건을 충족하지 못했습니다."));
                sync(player.server);
                return;
            }
            if (ShopRules.upgradeSlot(message.product) >= 0) applyBenefits(player, data.account(player.getUUID()));
            else for (ItemStack stack : ShopRules.gear(message.product)) {
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
            player.sendSystemMessage(Component.literal("[Endless Defense] 구매 완료: " + ShopRules.NAMES[message.product]));
            sync(player.server);
        }
    }

    private record ConfigPacket(int option, int value) {
        private static void handle(ConfigPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) return;
            String[] names = {"count", "health", "damage", "awareness", "armor", "gear"};
            if (message.option < 0 || message.option >= names.length) return;
            if (!setSetting(WaveData.get(player.server), names[message.option], message.value)) return;
            player.sendSystemMessage(Component.literal("[Endless Defense] 설정 저장: " + names[message.option]
                    + " = " + message.value + " (다음 웨이브부터 적용)"));
            sync(player.server);
        }
    }

    public record StatusPacket(int wave, int highest, int phase, int countdown, boolean selfReady, int ready, int total,
                               int remaining, String roster, boolean canConfigure, int baseMobs, int healthPercent,
                               int damagePercent, int awareness, int armorBonus, int gearChance, long coins, int[] upgrades,
                               int[] stock, int respawns, boolean night, int surrenderVotes, boolean selfSurrender) {
        public StatusPacket {
            upgrades = java.util.Arrays.copyOf(upgrades, ShopRules.UPGRADE_IDS.length);
            stock = java.util.Arrays.copyOf(stock, ShopRules.NAMES.length);
        }

        private static void handle(StatusPacket message, CustomPayloadEvent.Context context) {
            if (context.isClientSide()) ClientVisuals.accept(message);
        }
    }

    private static int[] readUpgrades(net.minecraft.network.FriendlyByteBuf buffer) {
        int[] upgrades = new int[ShopRules.UPGRADE_IDS.length];
        for (int i = 0; i < upgrades.length; i++) upgrades[i] = buffer.readVarInt();
        return upgrades;
    }

    private static int[] readStock(net.minecraft.network.FriendlyByteBuf buffer) {
        int[] stock = new int[ShopRules.NAMES.length];
        for (int i = 0; i < stock.length; i++) stock[i] = buffer.readVarInt();
        return stock;
    }

    private static boolean allReady(MinecraftServer server, Session game) {
        var players = server.getPlayerList().getPlayers();
        return !players.isEmpty() && players.stream().allMatch(player -> game.ready.contains(player.getUUID()));
    }

    private static java.util.List<ServerPlayer> participants(MinecraftServer server, WaveData data) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !data.eliminated.contains(player.getUUID())
                        && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR)
                .toList();
    }

    private static void sync(MinecraftServer server) {
        Session game = session(server);
        WaveData data = WaveData.get(server);
        var players = server.getPlayerList().getPlayers();
        StringBuilder names = new StringBuilder();
        for (ServerPlayer player : players) {
            if (!names.isEmpty()) names.append("  ");
            names.append(game.ready.contains(player.getUUID()) ? "[✓] " : "[ ] ").append(player.getName().getString());
        }
        String roster = names.length() > 1800 ? names.substring(0, 1800) : names.toString();
        int shownRespawns = game.phase == Phase.WAITING && !game.retrying
                ? BalanceRules.ticketCap(data, data.wave + 1, Math.max(1, participants(server, data).size()))
                : data.respawns;
        for (ServerPlayer player : players) {
            WaveData.Account account = data.account(player.getUUID());
            CHANNEL.send(new StatusPacket(data.wave, data.highestWave, game.phase.ordinal(),
                    game.phase == Phase.WAITING && game.waitSeconds <= 10 ? game.waitSeconds : -1,
                    game.ready.contains(player.getUUID()), game.ready.size(), players.size(), game.mobs.size(), roster,
                    player.hasPermissions(2), data.baseMobs, data.healthPercent, data.damagePercent,
                    data.awareness, data.armorBonus, data.gearChance, account.coins, account.levels.clone(),
                    account.stockWave == data.wave ? account.stock.clone() : new int[ShopRules.NAMES.length],
                    shownRespawns, game.night, game.surrenderVotes.size(), game.surrenderVotes.contains(player.getUUID())),
                    PacketDistributor.PLAYER.with(player));
        }
    }

    private static void startRun(MinecraftServer server, Session game) {
        if (game.phase != Phase.LOBBY || !allReady(server, game)) return;
        DefenseMap.ensure(server);
        WaveData data = WaveData.get(server);
        data.started = true;
        data.earlyBonusThroughWave = 2 + server.overworld().random.nextInt(4);
        data.respawns = BalanceRules.initialTickets(data, server.getPlayerList().getPlayerCount());
        data.eliminated.clear();
        data.pendingRespawn.clear();
        data.teamMembers.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) data.teamMembers.add(player.getUUID());
        data.setDirty();
        game.ready.clear();
        BlockPos spawn = server.overworld().getSharedSpawnPos();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getInventory().clearContent();
            player.getEnderChestInventory().clearContent();
            player.teleportTo(server.overworld(), spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
        announce(server, "전원 준비 완료! 소지품을 비우고 월드 스폰에 모였습니다. "
                + data.earlyBonusThroughWave + "웨이브까지 클리어 보너스 +" + data.earlyBonusAmount
                + " 노드. 곧 첫 웨이브가 시작됩니다.");
        scheduleWave(server, game);
    }

    private static void scheduleWave(MinecraftServer server, Session game) {
        game.phase = Phase.WAITING;
        game.night = false;
        WaveData data = WaveData.get(server);
        int min = BalanceRules.restMin(data, data.wave);
        int max = BalanceRules.restMax(data, data.wave);
        game.waitSeconds = min + server.overworld().random.nextInt(max - min + 1);
        cue(server, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8f, 0.8f);
        sync(server);
    }

    private static void voteSurrender(MinecraftServer server, ServerPlayer player, boolean vote) {
        Session game = session(server);
        if (game.phase == Phase.LOBBY) return;
        if (vote) game.surrenderVotes.add(player.getUUID());
        else game.surrenderVotes.remove(player.getUUID());
        announce(server, player.getName().getString() + (vote ? " 종료 찬성" : " 종료 투표 취소")
                + " (" + game.surrenderVotes.size() + "/" + server.getPlayerList().getPlayerCount() + ")");
        trySurrender(server, game);
    }

    private static void trySurrender(MinecraftServer server, Session game) {
        var players = server.getPlayerList().getPlayers();
        if (!players.isEmpty() && game.phase != Phase.LOBBY
                && players.stream().filter(p -> game.surrenderVotes.contains(p.getUUID())).count() > players.size() / 2) endRun(server, game);
        else sync(server);
    }

    private static void endRun(MinecraftServer server, Session game) {
        if (game.phase == Phase.LOBBY) return;
        WaveData data = WaveData.get(server);
        for (UUID id : game.mobs) {
            Entity mob = findEntity(server, id);
            if (mob != null) mob.discard();
            removeGlowTeam(server, id);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            clearBenefits(player, data.account(player.getUUID()));
            if (data.eliminated.contains(player.getUUID())) player.setGameMode(GameType.SURVIVAL);
        }
        game.mobs.clear(); game.bossMobs.clear(); game.kills.clear(); game.infusions.clear(); game.ready.clear(); game.surrenderVotes.clear();
        game.night = false;
        game.phase = Phase.LOBBY;
        data.resetRun();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) data.restoreSurvival.remove(player.getUUID());
        data.setDirty();
        announce(server, "팀이 웨이브를 포기해 이번 게임을 종료했습니다. 최고 기록은 유지되고 노드·업그레이드는 초기화됩니다.");
        title(server, "게임 종료", "다시 시작하려면 전원 준비", 10, 60, 10);
        cue(server, SoundEvents.WITHER_DEATH, 0.65f, 1.2f);
        sync(server);
    }

    private static void gameOver(MinecraftServer server, Session game) {
        if (game.phase == Phase.GAME_OVER) return;
        for (UUID id : game.mobs) {
            Entity mob = findEntity(server, id);
            if (mob != null) mob.discard();
            removeGlowTeam(server, id);
        }
        game.mobs.clear(); game.bossMobs.clear(); game.kills.clear(); game.infusions.clear();
        game.phase = Phase.GAME_OVER;
        WaveData data = WaveData.get(server);
        data.gameOver = true;
        data.waveInProgress = false;
        data.setDirty();
        announce(server, "팀 전멸! 게임이 멈췄습니다. H 화면의 종료 투표 또는 OP /endlessdefense stop으로 새 게임을 준비할 수 있습니다.");
        title(server, "게임 오버", "팀 부활 횟수를 모두 사용했습니다", 10, 80, 20);
        cue(server, SoundEvents.WITHER_DEATH, 0.8f, 0.8f);
        sync(server);
    }

    private static void clearBenefits(ServerPlayer player, WaveData.Account account) {
        removeOwnedEffect(player, MobEffects.DIG_SPEED, account.levels[0]);
        removeOwnedEffect(player, MobEffects.HEALTH_BOOST, account.levels[1]);
        removeOwnedEffect(player, MobEffects.MOVEMENT_SPEED, account.levels[2]);
        removeOwnedEffect(player, MobEffects.REGENERATION, account.levels[5] >= 5 ? 2 : account.levels[5] >= 3 ? 1 : 0);
        permanentAttribute(player, Attributes.ARMOR, "shop_armor", 0);
        permanentAttribute(player, Attributes.ATTACK_DAMAGE, "shop_attack", 0);
        permanentAttribute(player, Attributes.ATTACK_KNOCKBACK, "shop_knockback", 0);
    }

    private static void removeOwnedEffect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> type, int level) {
        MobEffectInstance current = player.getEffect(type);
        if (level > 0 && current != null && current.isInfiniteDuration() && current.getAmplifier() == level - 1)
            player.removeEffect(type);
    }

    private static void startWave(MinecraftServer server, Session game) {
        if (game.phase != Phase.WAITING) return;
        WaveData data = WaveData.get(server);
        var players = participants(server, data);
        if (players.isEmpty()) return;
        game.phase = Phase.ACTIVE;
        data.wave++;
        data.waveInProgress = true;
        if (!game.retrying) data.respawns = BalanceRules.ticketCap(data, data.wave, players.size());
        game.retrying = false;
        data.highestWave = Math.max(data.highestWave, data.wave);
        data.setDirty();
        boolean bossWave = data.wave % 5 == 0;
        game.night = isNight(server.overworld());
        int total = BalanceRules.nightCount(data,
                ShopRules.enemyCount(data.baseMobs, players.size(), data.wave), game.night);
        int count = bossWave ? total - 1 : total;
        announce(server, data.wave + "웨이브 시작! " + (game.night ? "야간 추가 적! " : "")
                + (bossWave ? "철갑 수호자 출현! " : "") + "예정 적: " + total + " / 팀 부활 " + data.respawns);
        title(server, data.wave + "웨이브 시작!", bossWave ? "철갑 수호자 출현" : "적을 모두 처치하세요", 10, 50, 10);
        cue(server, SoundEvents.RAID_HORN.value(), 1.0f, bossWave ? 0.7f : 1.0f);
        game.kills.clear();
        game.bossMobs.clear();
        game.infusions.clear();
        game.bossKilled = false;
        game.escaped = false;
        game.spawning = true;
        for (int i = 0; i < total; i++) {
            boolean boss = bossWave && i == count;
            ServerPlayer target = players.get(i % players.size());
            ServerLevel level = target.serverLevel();
            EntityType<? extends Monster> type = boss ? ModMobs.IRON_BULWARK.get()
                    : data.wave >= 8 && i % 7 == 0 ? ModMobs.IRON_BULWARK.get()
                    : data.wave >= 4 && i % 4 == 0 ? ModMobs.ASH_RUNNER.get()
                    : data.wave >= 3 && i % 3 == 0 ? EntityType.SKELETON : EntityType.ZOMBIE;
            BlockPos top = findSpawn(level, target, players, data, i);
            if (top == null) {
                LOGGER.warn("Wave {} spawn search failed near {} in {}", data.wave, target.blockPosition(), level.dimension().location());
                game.escaped = true;
                continue;
            }
            Monster mob = type.create(level);
            if (mob == null) { game.escaped = true; continue; }
            mob.moveTo(top.getX() + 0.5, top.getY(), top.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
            mob.setPersistenceRequired();
            applyDifficulty(mob, data, boss, level);
            mob.addTag("endlessdefense_wave");
            if (boss) mob.addTag("endlessdefense_boss");
            boolean infuser = !boss && level.random.nextInt(100) < data.infuserChancePercent;
            if (infuser) mob.addTag("endlessdefense_infuser");
            mob.setGlowingTag(true);
            if (boss) {
                mob.setCustomName(Component.literal("철갑 수호자 · " + data.wave + "웨이브"));
            } else if (infuser) mob.setCustomName(Component.literal("마력 주입자 · " + mob.getType().getDescription().getString()));
            else mob.setCustomName(mob.getType().getDescription());
            mob.setCustomNameVisible(true);
            if (level.addFreshEntity(mob)) {
                game.mobs.add(mob.getUUID());
                if (boss) game.bossMobs.add(mob.getUUID());
                assignGlow(mob);
            } else game.escaped = true;
        }
        game.spawning = false;
        game.ticks = 0;
        sync(server);
        if (game.mobs.isEmpty()) {
            retryWave(server, game, "적 생성 실패");
        }
    }

    private static void retryWave(MinecraftServer server, Session game, String reason) {
        WaveData data = WaveData.get(server);
        for (UUID id : game.mobs) {
            Entity mob = findEntity(server, id);
            if (mob != null) mob.discard();
            removeGlowTeam(server, id);
        }
        game.mobs.clear(); game.bossMobs.clear(); game.kills.clear(); game.infusions.clear();
        game.bossKilled = false;
        game.retrying = true;
        data.wave = Math.max(0, data.wave - 1);
        data.waveInProgress = false;
        data.setDirty();
        announce(server, reason + ": 보상 없이 같은 웨이브를 다시 준비합니다.");
        scheduleWave(server, game);
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000L;
        return time >= 13000 && time < 23000;
    }

    private static BlockPos findSpawn(ServerLevel level, ServerPlayer target,
                                      java.util.List<ServerPlayer> players, WaveData data, int index) {
        BlockPos lane = DefenseMap.laneSpawn(level, target, players, data, index);
        if (lane != null) return lane;
        // Prefer accessible spaces near the target's elevation, including caves and tree canopies.
        for (int attempt = 0; attempt < 80; attempt++) {
            int dx = (level.random.nextBoolean() ? 1 : -1) * (9 + level.random.nextInt(24));
            int dz = (level.random.nextBoolean() ? 1 : -1) * (9 + level.random.nextInt(24));
            BlockPos candidate = target.blockPosition().offset(dx, level.random.nextInt(9) - 4, dz);
            for (int down = 0; down <= 4; down++) {
                BlockPos spot = candidate.below(down);
                if (safeSpawn(level, spot, players, data)) return spot;
            }
        }
        for (int attempt = 0; attempt < 80; attempt++) {
            int dx = (level.random.nextBoolean() ? 1 : -1) * (12 + level.random.nextInt(36));
            int dz = (level.random.nextBoolean() ? 1 : -1) * (12 + level.random.nextInt(36));
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    target.blockPosition().offset(dx, 0, dz));
            if (level.getBlockState(top).is(Blocks.SNOW)) top = top.above();
            if (safeSpawn(level, top, players, data)) return top;
        }
        // An evenly spaced ring handles steep snowy slopes that random samples can miss.
        for (int radius = 12; radius <= 48; radius += 4)
            for (int direction = 0; direction < 16; direction++) {
                double angle = direction * Math.PI / 8;
                int x = target.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
                int z = target.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(x, target.getBlockY(), z));
                if (level.getBlockState(top).is(Blocks.SNOW)) top = top.above();
                if (safeSpawn(level, top, players, data)) return top;
            }
        return null;
    }

    static boolean hasSafeSupport(ServerLevel level, BlockPos floor) {
        var state = level.getBlockState(floor);
        if (state.isFaceSturdy(level, floor, Direction.UP) || state.is(BlockTags.LEAVES)) return true;
        BlockPos below = floor.below();
        return state.is(Blocks.SNOW) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    private static boolean safeSpawn(ServerLevel level, BlockPos spot,
                                     java.util.List<ServerPlayer> players, WaveData data) {
        if (data.defenseMapBuilt && level == level.getServer().overworld()
                && Math.abs(spot.getX() - data.defenseMapX) <= 20
                && Math.abs(spot.getZ() - data.defenseMapZ) <= 20) return false;
        if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()
                || !level.getFluidState(spot).isEmpty() || !hasSafeSupport(level, spot.below())
                || !level.getEntitiesOfClass(Monster.class, new AABB(spot).inflate(0.6)).isEmpty()) return false;
        return players.stream().noneMatch(p -> p.serverLevel() == level
                && p.distanceToSqr(spot.getX() + .5, spot.getY(), spot.getZ() + .5) < 100);
    }

    private static void breachSoftBlock(MinecraftServer server, Session game) {
        WaveData data = WaveData.get(server);
        for (ServerPlayer player : participants(server, data)) {
            ServerLevel level = player.serverLevel();
            int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    player.blockPosition()).getY();
            if (player.getBlockY() > surface - 5) continue;
            Monster nearby = null;
            double best = 24 * 24;
            for (UUID id : game.mobs) {
                Entity entity = level.getEntity(id);
                if (!(entity instanceof Monster mob)) continue;
                double distance = mob.distanceToSqr(player);
                if (distance > 25 && distance < best) { best = distance; nearby = mob; }
            }
            if (nearby == null) continue;
            int dx = Integer.compare(player.getBlockX(), nearby.getBlockX());
            int dy = Integer.compare(player.getBlockY(), nearby.getBlockY());
            int dz = Integer.compare(player.getBlockZ(), nearby.getBlockZ());
            BlockPos obstruction = nearby.blockPosition().offset(dx, dy > 0 ? 1 : 0, dz);
            var state = level.getBlockState(obstruction);
            if (level.getBlockEntity(obstruction) != null || state.getDestroySpeed(level, obstruction) < 0
                    || state.getDestroySpeed(level, obstruction) > 3
                    || !(state.is(BlockTags.DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE))) continue;
            level.destroyBlock(obstruction, true, nearby);
        }
    }

    private static void applyDifficulty(Monster mob, WaveData data, boolean boss, ServerLevel level) {
        double waveHealth = BalanceRules.healthMultiplier(data.wave);
        double waveDamage = Math.min(1.0 + (data.wave - 1) * 0.05, 2.0);
        setAttribute(mob, Attributes.MAX_HEALTH, Math.min(300, mob.getAttributeBaseValue(Attributes.MAX_HEALTH)
                * data.healthPercent / 100.0 * waveHealth * (boss ? 1.8 : 1)));
        setAttribute(mob, Attributes.ATTACK_DAMAGE, Math.min(30, mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)
                * data.damagePercent / 100.0 * waveDamage * (boss ? 1.3 : 1)));
        setAttribute(mob, Attributes.ARMOR, Math.min(30, mob.getAttributeBaseValue(Attributes.ARMOR)
                + data.armorBonus + Math.min(data.wave / 5, 6)));
        setAttribute(mob, Attributes.FOLLOW_RANGE, 16 + data.awareness * 8);
        setAttribute(mob, Attributes.MOVEMENT_SPEED, Math.min(0.48,
                mob.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * (1 + (data.awareness - 2) * 0.06)));
        mob.setHealth(mob.getMaxHealth());
        if (mob instanceof Skeleton) mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        else if (boss || level.random.nextInt(100) < data.gearChance)
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(boss ? Items.IRON_AXE : Items.STONE_SWORD));
        // A helmet keeps vanilla zombie/skeleton wave units from clearing themselves in daylight.
        boolean upgradedHelmet = boss || level.random.nextInt(100) < data.gearChance;
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(upgradedHelmet ? Items.IRON_HELMET : Items.LEATHER_HELMET));
        mob.setDropChance(EquipmentSlot.HEAD, 0.0f);
        if (boss || level.random.nextInt(100) < data.gearChance)
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(boss ? Items.IRON_CHESTPLATE : Items.LEATHER_CHESTPLATE));
    }

    private static void setAttribute(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        var instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static void finishWave(MinecraftServer server, Session game) {
        if (game.phase != Phase.ACTIVE) return;
        if (game.escaped) { retryWave(server, game, "웨이브 적 추적 실패"); return; }
        int wave = WaveData.get(server).wave;
        WaveData data = WaveData.get(server);
        long reward = BalanceRules.clearReward(data, wave);
        for (ServerPlayer player : participants(server, data)) data.award(player.getUUID(), reward);
        for (var entry : game.kills.entrySet()) data.award(entry.getKey(), (entry.getValue() / data.killRewardEvery) * (long) data.killBounty);
        if (game.bossKilled) for (ServerPlayer player : participants(server, data)) data.award(player.getUUID(), data.bossReward);
        for (var entry : game.infusions.entrySet()) {
            WaveData.Account account = data.account(entry.getKey());
            account.pendingInfusions = Math.min(32, account.pendingInfusions + entry.getValue());
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                player.sendSystemMessage(Component.literal("마력 주입 " + entry.getValue()
                        + "회 획득! 무기를 주 손에 들면 무작위 인챈트가 적용됩니다."));
                applyPendingInfusions(player, data);
            }
        }
        // A cleared wave begins a fresh team life pool. Spectators return for the next wave.
        data.restoreSurvival.addAll(data.eliminated);
        for (UUID id : data.eliminated) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) player.setGameMode(GameType.SURVIVAL);
                data.restoreSurvival.remove(id);
            }
        }
        data.eliminated.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (player.isAlive() && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR)
                player.setHealth(player.getMaxHealth());
        data.waveInProgress = false;
        data.setDirty();
        announce(server, wave + "웨이브 클리어! 기본 +" + reward + " 노드"
                + (game.bossKilled ? ", 보스 +" + data.bossReward : "") + " · 체력 회복. 다음 웨이브는 자동 시작됩니다.");
        title(server, wave + "웨이브 클리어!", "체력 회복 · 기본 +" + reward + " 노드", 10, 50, 10);
        cue(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        scheduleWave(server, game);
    }

    private static void cue(MinecraftServer server, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            player.playNotifySound(sound, SoundSource.RECORDS, volume, pitch);
    }

    private static void title(MinecraftServer server, String main, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(main)));
        }
    }

    private static void applyBenefits(ServerPlayer player, WaveData.Account account) {
        persistentEffect(player, MobEffects.DIG_SPEED, account.levels[0]);
        persistentEffect(player, MobEffects.HEALTH_BOOST, account.levels[1]);
        persistentEffect(player, MobEffects.MOVEMENT_SPEED, account.levels[2]);
        int regen = account.levels[5] >= 5 ? 2 : account.levels[5] >= 3 ? 1 : 0;
        persistentEffect(player, MobEffects.REGENERATION, regen);
        permanentAttribute(player, Attributes.ARMOR, "shop_armor", account.levels[3]);
        permanentAttribute(player, Attributes.ATTACK_DAMAGE, "shop_attack", account.levels[4]);
        permanentAttribute(player, Attributes.ATTACK_KNOCKBACK, "shop_knockback", account.levels[7] * 0.2);
        if (account.levels[5] > 0 && player.isAlive()
                && player.getHealth() < player.getMaxHealth())
            player.heal(account.levels[5] * 0.5f);
    }

    private static void applyPendingInfusions(ServerPlayer player, WaveData data) {
        WaveData.Account account = data.account(player.getUUID());
        if (account.pendingInfusions <= 0) return;
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.is(ItemTags.WEAPON_ENCHANTABLE) || weapon.is(ItemTags.BOW_ENCHANTABLE)
                || weapon.is(ItemTags.CROSSBOW_ENCHANTABLE) || weapon.is(ItemTags.TRIDENT_ENCHANTABLE)
                || weapon.is(ItemTags.MACE_ENCHANTABLE))) return;
        var choices = infusionOptions(weapon, player.registryAccess());
        if (choices.isEmpty()) return;
        var chosen = choices.get(player.getRandom().nextInt(choices.size()));
        int level = Math.min(chosen.value().getMaxLevel(), 1 + Math.max(0, data.wave - 1) / 12);
        weapon.enchant(chosen, level);
        account.pendingInfusions--;
        data.setDirty();
        player.sendSystemMessage(Component.literal("무기에 " + chosen.value().description().getString()
                + " " + level + " 인챈트가 적용됐습니다. 남은 마력 주입 " + account.pendingInfusions + "회"));
        player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    static java.util.List<Holder.Reference<Enchantment>> infusionOptions(ItemStack weapon, RegistryAccess registries) {
        if (!(weapon.is(ItemTags.WEAPON_ENCHANTABLE) || weapon.is(ItemTags.BOW_ENCHANTABLE)
                || weapon.is(ItemTags.CROSSBOW_ENCHANTABLE) || weapon.is(ItemTags.TRIDENT_ENCHANTABLE)
                || weapon.is(ItemTags.MACE_ENCHANTABLE))) return java.util.List.of();
        var existing = EnchantmentHelper.getEnchantmentsForCrafting(weapon);
        return registries.registryOrThrow(Registries.ENCHANTMENT).holders()
                .filter(enchantment -> enchantment.is(EnchantmentTags.IN_ENCHANTING_TABLE)
                        && enchantment.value().canEnchant(weapon)
                        && !existing.keySet().contains(enchantment)
                        && existing.keySet().stream().allMatch(other -> Enchantment.areCompatible(enchantment, other)))
                .toList();
    }

    private static void persistentEffect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int level) {
        MobEffectInstance current = player.getEffect(effect);
        if (level <= 0) return;
        if (current == null || current.getAmplifier() != level - 1 || !current.isInfiniteDuration())
            player.addEffect(new MobEffectInstance(effect, -1, level - 1, true, false));
    }

    private static void permanentAttribute(ServerPlayer player,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String key, double amount) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, key);
        var old = instance.getModifier(id);
        if (old != null && old.amount() == amount) return;
        if (old != null) instance.removeModifier(id);
        if (amount > 0) instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void announce(MinecraftServer server, String text) {
        server.getPlayerList().broadcastSystemMessage(Component.literal("[Endless Defense] " + text), false);
    }

    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Session game = session(player.server);
            WaveData data = WaveData.get(player.server);
            game.ready.remove(player.getUUID());
            game.surrenderVotes.remove(player.getUUID());
            if (data.restoreSurvival.remove(player.getUUID())) {
                player.setGameMode(GameType.SURVIVAL);
                data.setDirty();
            }
            if (data.eliminated.contains(player.getUUID())) player.setGameMode(GameType.SPECTATOR);
            if (data.started && !data.gameOver && data.teamMembers.add(player.getUUID())) data.setDirty();
            if (data.pendingRespawn.remove(player.getUUID())) data.setDirty();
            applyBenefits(player, data.account(player.getUUID()));
            applyPendingInfusions(player, data);
            player.sendSystemMessage(Component.literal("[Endless Defense] G: 최초 준비, H: 진행 상황, K: 상점"));
            sync(player.server);
        }
    }

    @SubscribeEvent public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            session(player.server).ready.remove(player.getUUID());
            Session game = session(player.server);
            game.surrenderVotes.remove(player.getUUID());
            trySurrender(player.server, game);
        }
    }

    @SubscribeEvent public void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            Session game = session(level.getServer());
            WaveData data = WaveData.get(level.getServer());
            if (event.getEntity() instanceof ServerPlayer player && game.phase != Phase.LOBBY
                    && !data.pendingRespawn.contains(player.getUUID()) && !data.eliminated.contains(player.getUUID())) {
                int tick = level.getServer().getTickCount();
                boolean recentRescue = tick - game.lastRescueTick.getOrDefault(player.getUUID(), -1000) < 20;
                if (data.respawns > 0 || recentRescue) {
                    if (!recentRescue) {
                        data.respawns--;
                        game.lastRescueTick.put(player.getUUID(), tick);
                        announce(level.getServer(), player.getName().getString()
                                + " 불사의 토템 효과로 생존! 팀 부활 남은 횟수 " + data.respawns);
                        data.setDirty();
                        sync(level.getServer());
                    }
                    player.setHealth(1.0f);
                    player.invulnerableTime = 20;
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                    level.broadcastEntityEvent(player, (byte) 35);
                    event.setCanceled(true);
                    return;
                } else {
                    data.eliminated.add(player.getUUID());
                    announce(level.getServer(), player.getName().getString()
                            + " 사망 · 팀 부활 소진, 관전 모드로 전환됩니다.");
                }
                data.setDirty();
                sync(level.getServer());
                if (!data.teamMembers.isEmpty() && data.teamMembers.stream().allMatch(data.eliminated::contains))
                    gameOver(level.getServer(), game);
            }
            if (game.phase == Phase.ACTIVE && game.mobs.remove(event.getEntity().getUUID())) {
                removeGlowTeam(level.getServer(), event.getEntity().getUUID());
                boolean boss = game.bossMobs.remove(event.getEntity().getUUID());
                if (boss) game.bossKilled = true;
                Entity attacker = event.getSource().getEntity();
                if (attacker instanceof ServerPlayer killer && !data.eliminated.contains(killer.getUUID())) {
                    game.kills.merge(killer.getUUID(), 1, Integer::sum);
                    if (event.getEntity().getTags().contains("endlessdefense_infuser"))
                        game.infusions.merge(killer.getUUID(), 1, Integer::sum);
                }
                if (game.mobs.isEmpty()) finishWave(level.getServer(), game);
                else sync(level.getServer());
            }
        }
    }

    @SubscribeEvent public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WaveData data = WaveData.get(player.server);
        if (data.eliminated.contains(player.getUUID())) player.setGameMode(GameType.SPECTATOR);
        else if (data.pendingRespawn.remove(player.getUUID())) data.setDirty();
        applyBenefits(player, data.account(player.getUUID()));
        sync(player.server);
    }

    @SubscribeEvent public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            applyBenefits(player, WaveData.get(player.server).account(player.getUUID()));
    }

    @SubscribeEvent public void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Monster mob)) return;
        if (mob.getTags().contains("endlessdefense_wave")) {
            Session game = session(level.getServer());
            if (!game.spawning && !game.mobs.contains(mob.getUUID())) { mob.discard(); return; }
        }
        boolean waveOrCustom = mob.getTags().contains("endlessdefense_wave")
                || mob.getType() == ModMobs.ASH_RUNNER.get() || mob.getType() == ModMobs.IRON_BULWARK.get();
        if (mob.getTags().contains("endlessdefense_wave")) {
            if (mob.getCustomName() == null) mob.setCustomName(mob.getType().getDescription());
            mob.setCustomNameVisible(true);
        }
        if (waveOrCustom) assignGlow(mob);
    }

    static void assignGlow(Monster mob) {
        mob.setGlowingTag(true);
        boolean wave = mob.getTags().contains("endlessdefense_wave");
        String name = wave && mob.getTags().contains("endlessdefense_boss") ? "ed_wave_boss"
                : wave && mob.getTags().contains("endlessdefense_infuser") ? "ed_wave_infuser"
                : mob.getType() == ModMobs.IRON_BULWARK.get() ? wave ? "ed_wave_bulwark" : "ed_bulwark"
                : mob.getType() == ModMobs.ASH_RUNNER.get() ? wave ? "ed_wave_runner" : "ed_runner"
                : mob instanceof Skeleton ? "ed_wave_skeleton" : "ed_wave_zombie";
        ChatFormatting color = switch (name) {
            case "ed_wave_boss" -> ChatFormatting.RED;
            case "ed_wave_infuser" -> ChatFormatting.LIGHT_PURPLE;
            case "ed_wave_bulwark", "ed_bulwark" -> ChatFormatting.GOLD;
            case "ed_wave_runner", "ed_runner" -> ChatFormatting.AQUA;
            case "ed_wave_skeleton" -> ChatFormatting.YELLOW;
            default -> ChatFormatting.WHITE;
        };
        Scoreboard scoreboard = mob.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) team = scoreboard.addPlayerTeam(name);
        team.setColor(color);
        scoreboard.addPlayerToTeam(mob.getStringUUID(), team);
    }

    private static void removeGlowTeam(MinecraftServer server, UUID id) {
        Scoreboard scoreboard = server.overworld().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(id.toString());
        if (team != null && team.getName().startsWith("ed_")) scoreboard.removePlayerFromTeam(id.toString(), team);
    }

    @SubscribeEvent public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        MinecraftServer server = event.getServer();
        DefenseMap.ensure(server);
        Session game = session(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            applyPendingInfusions(player, WaveData.get(server));
        if (server.getTickCount() % 200 == 0)
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                applyBenefits(player, WaveData.get(server).account(player.getUUID()));
        if (game.phase == Phase.ACTIVE)
            for (UUID id : game.mobs) {
                Entity entity = findEntity(server, id);
                if (entity instanceof Monster mob) {
                    mob.setGlowingTag(true);
                    if (mob.getTeam() == null || !mob.getTeam().getName().startsWith("ed_wave_")) assignGlow(mob);
                }
            }
        if (game.phase == Phase.LOBBY) {
            if (allReady(server, game)) startRun(server, game);
            else sync(server);
            return;
        }
        if (game.phase == Phase.WAITING) {
            if (participants(server, WaveData.get(server)).isEmpty()) { sync(server); return; }
            game.waitSeconds--;
            if (game.waitSeconds > 0 && game.waitSeconds <= 10)
                title(server, Integer.toString(game.waitSeconds), "다음 웨이브까지", 0, 21, 0);
            if (game.waitSeconds > 0 && game.waitSeconds <= 10)
                cue(server, SoundEvents.NOTE_BLOCK_HAT.value(), 0.9f, game.waitSeconds <= 3 ? 1.5f : 1.0f);
            if (game.waitSeconds <= 0) startWave(server, game);
            else sync(server);
            return;
        }
        if (game.phase == Phase.GAME_OVER) { sync(server); return; }
        if (participants(server, WaveData.get(server)).isEmpty()) { sync(server); return; }
        game.ticks += 20;
        if (game.ticks % 100 == 0) breachSoftBlock(server, game);
        if (game.ticks % 100 == 0) game.mobs.removeIf(id -> {
            Entity entity = findEntity(server, id);
            if (entity == null || !entity.isAlive()) {
                game.escaped = true;
                removeGlowTeam(server, id);
                return true;
            }
            return false;
        });
        if (game.mobs.isEmpty()) {
            if (game.escaped) retryWave(server, game, "웨이브 적 소실");
            else finishWave(server, game);
        }
        else if (game.ticks >= 20 * 60 * 20) {
            retryWave(server, game, "웨이브 시간 초과");
        } else sync(server);
    }

    private static Entity findEntity(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    @SubscribeEvent public void onCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("endlessdefense")
                .then(Commands.literal("surrender").requires(source -> source.getEntity() instanceof ServerPlayer)
                        .executes(ctx -> { voteSurrender(ctx.getSource().getServer(), ctx.getSource().getPlayerOrException(), true); return 1; }))
                .then(Commands.literal("stop").requires(source -> source.hasPermission(2))
                        .executes(ctx -> { endRun(ctx.getSource().getServer(), session(ctx.getSource().getServer())); return 1; }))
                .then(Commands.literal("status").executes(ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    Session game = session(server);
                    WaveData data = WaveData.get(server);
                    ctx.getSource().sendSuccess(() -> Component.literal("웨이브 " + data.wave + " / 최고 " + data.highestWave
                            + " / " + (game.phase == Phase.ACTIVE ? "전투 중, 남은 적 " + game.mobs.size()
                            : game.phase == Phase.WAITING ? "다음 웨이브 대기 중"
                            : "시작 준비 중, " + game.ready.size() + "/" + server.getPlayerList().getPlayerCount() + "명 준비")), false);
                    return 1;
                }))
                .then(Commands.literal("config").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("show").executes(ctx -> {
                            WaveData d = WaveData.get(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("count=" + d.baseMobs + " health=" + d.healthPercent
                                    + " damage=" + d.damagePercent + " awareness=" + d.awareness + " armor=" + d.armorBonus
                                    + " gear=" + d.gearChance), false);
                            return 1;
                        }))
                        .then(setting("count", 1, 128))
                        .then(setting("health", 25, 500))
                        .then(setting("damage", 25, 500))
                        .then(setting("awareness", 1, 5))
                        .then(setting("armor", 0, 20))
                        .then(setting("gear", 0, 100)))
                .then(Commands.literal("balance").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("show").executes(ctx -> {
                            WaveData d = WaveData.get(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("early=" + d.earlyReward + " earlyStep=" + d.earlyRewardStep
                                    + " mid=" + d.midRewardStep + " late=" + d.lateRewardStep + " killEvery=" + d.killRewardEvery
                                    + " killBounty=" + d.killBounty + " boss=" + d.bossReward
                                    + " night=" + d.nightPercent + " nightCap=" + d.nightCap + " respawnCap=" + d.respawnEarlyCap
                                    + " lateWave=" + d.respawnLateWave
                                    + " restMinStep=" + d.restMinStep + " restMaxStep=" + d.restMaxStep
                                    + " restMaxCap=" + d.restMaxCap + " earlyBonus=" + d.earlyBonusAmount
                                    + " earlyBonusThrough=" + d.earlyBonusThroughWave
                                    + " infuser=" + d.infuserChancePercent), false);
                            return 1;
                        }))
                        .then(setting("early", 10, 1000)).then(setting("earlyStep", 0, 200))
                        .then(setting("mid", 0, 300)).then(setting("late", 0, 500))
                        .then(setting("killEvery", 1, 50)).then(setting("killBounty", 0, 100))
                        .then(setting("boss", 0, 2000))
                        .then(setting("night", 0, 25)).then(setting("nightCap", 0, 32))
                        .then(setting("respawnCap", 1, 20)).then(setting("lateWave", 10, 200))
                        .then(setting("restMinStep", 0, 10)).then(setting("restMaxStep", 0, 15))
                        .then(setting("restMaxCap", 60, 180)).then(setting("earlyBonus", 0, 1000))
                        .then(setting("infuser", 0, 10)).then(setting("enchantDrop", 0, 10)));
        event.getDispatcher().register(root);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> setting(
            String name, int min, int max) {
        return Commands.literal(name).then(Commands.argument("value", IntegerArgumentType.integer(min, max)).executes(ctx -> {
            MinecraftServer server = ctx.getSource().getServer();
            WaveData data = WaveData.get(server);
            int value = IntegerArgumentType.getInteger(ctx, "value");
            if (!setSetting(data, name, value)) return 0;
            ctx.getSource().sendSuccess(() -> Component.literal("설정 저장: " + name + " = " + value
                    + " (다음 해당 계산부터 적용)"), true);
            sync(server);
            return value;
        }));
    }

    private static boolean setSetting(WaveData data, String name, int value) {
        boolean valid = switch (name) {
            case "count" -> value >= 1 && value <= 128;
            case "health", "damage" -> value >= 25 && value <= 500;
            case "awareness" -> value >= 1 && value <= 5;
            case "armor" -> value >= 0 && value <= 20;
            case "gear" -> value >= 0 && value <= 100;
            case "early" -> value >= 10 && value <= 1000;
            case "earlyStep" -> value >= 0 && value <= 200;
            case "mid" -> value >= 0 && value <= 300;
            case "late" -> value >= 0 && value <= 500;
            case "killEvery" -> value >= 1 && value <= 50;
            case "killBounty" -> value >= 0 && value <= 100;
            case "boss" -> value >= 0 && value <= 2000;
            case "night" -> value >= 0 && value <= 25;
            case "nightCap" -> value >= 0 && value <= 32;
            case "respawnCap" -> value >= 1 && value <= 20;
            case "lateWave" -> value >= 10 && value <= 200;
            case "restMinStep" -> value >= 0 && value <= 10;
            case "restMaxStep" -> value >= 0 && value <= 15;
            case "restMaxCap" -> value >= 60 && value <= 180;
            case "earlyBonus" -> value >= 0 && value <= 1000;
            case "infuser", "enchantDrop" -> value >= 0 && value <= 10;
            default -> false;
        };
        if (!valid) return false;
        switch (name) {
            case "count" -> data.baseMobs = value;
            case "health" -> data.healthPercent = value;
            case "damage" -> data.damagePercent = value;
            case "awareness" -> data.awareness = value;
            case "armor" -> data.armorBonus = value;
            case "gear" -> data.gearChance = value;
            case "early" -> data.earlyReward = value;
            case "earlyStep" -> data.earlyRewardStep = value;
            case "mid" -> data.midRewardStep = value;
            case "late" -> data.lateRewardStep = value;
            case "killEvery" -> data.killRewardEvery = value;
            case "killBounty" -> data.killBounty = value;
            case "boss" -> data.bossReward = value;
            case "night" -> data.nightPercent = value;
            case "nightCap" -> data.nightCap = value;
            case "respawnCap" -> data.respawnEarlyCap = value;
            case "lateWave" -> data.respawnLateWave = value;
            case "restMinStep" -> data.restMinStep = value;
            case "restMaxStep" -> data.restMaxStep = value;
            case "restMaxCap" -> data.restMaxCap = value;
            case "earlyBonus" -> data.earlyBonusAmount = value;
            case "infuser", "enchantDrop" -> data.infuserChancePercent = value;
        }
        data.setDirty();
        return true;
    }

    private enum Phase { LOBBY, WAITING, ACTIVE, GAME_OVER }

    private static final class Session {
        int ticks;
        int waitSeconds;
        Phase phase;
        boolean night;
        boolean escaped;
        boolean spawning;
        boolean bossKilled;
        boolean retrying;
        final Set<UUID> ready = new HashSet<>();
        final Set<UUID> surrenderVotes = new HashSet<>();
        final Set<UUID> mobs = new HashSet<>();
        final Set<UUID> bossMobs = new HashSet<>();
        final Map<UUID, Integer> kills = new java.util.HashMap<>();
        final Map<UUID, Integer> infusions = new java.util.HashMap<>();
        final Map<UUID, Integer> lastRescueTick = new java.util.HashMap<>();

        Session(boolean started, MinecraftServer server) {
            WaveData data = WaveData.get(server);
            if (started && data.waveInProgress && !data.gameOver) {
                data.wave = Math.max(0, data.wave - 1);
                data.waveInProgress = false;
                data.setDirty();
                retrying = true;
            }
            phase = !started ? Phase.LOBBY : data.gameOver ? Phase.GAME_OVER : Phase.WAITING;
            if (phase == Phase.WAITING) {
                int min = BalanceRules.restMin(data, data.wave);
                waitSeconds = min + server.overworld().random.nextInt(BalanceRules.restMax(data, data.wave) - min + 1);
            }
        }
    }
}
