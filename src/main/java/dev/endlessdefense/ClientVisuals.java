package dev.endlessdefense;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = EndlessDefense.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVisuals {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final KeyMapping READY = new KeyMapping("key.endlessdefense.ready", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.endlessdefense");
    private static final KeyMapping PANEL = new KeyMapping("key.endlessdefense.panel", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, "key.categories.endlessdefense");
    private static final KeyMapping SHOP = new KeyMapping("key.endlessdefense.shop", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.endlessdefense");
    private static final KeyMapping INFUSE = new KeyMapping("key.endlessdefense.infuse", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.endlessdefense");
    private static EndlessDefense.StatusPacket status = new EndlessDefense.StatusPacket(
            0, 0, 0, -1, false, 0, 0, 0, "", false, 4, 100, 100, 2, 0, 15,
            1, false, new int[ShopRules.MAX_TURRETS], 0, 0, 0, 0, 0, 0,
            new int[ShopRules.UPGRADE_IDS.length], new int[ShopRules.NAMES.length],
            AdminSettings.defaults(), 0, false, 0, false);

    public static void accept(EndlessDefense.StatusPacket newStatus) { status = newStatus; }

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(READY);
        event.register(PANEL);
        event.register(SHOP);
        event.register(INFUSE);
    }

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobs.ASH_RUNNER.get(), AshRenderer::new);
        event.registerEntityRenderer(ModMobs.IRON_BULWARK.get(), BulwarkRenderer::new);
        event.registerEntityRenderer(ModMobs.FROST_WARDEN.get(), FrostRenderer::new);
        event.registerEntityRenderer(ModMobs.SIEGE_BRUTE.get(), SiegeRenderer::new);
        event.registerEntityRenderer(ModMobs.DREAD_KNIGHT.get(), DreadRenderer::new);
    }

    @SubscribeEvent public static void overlay(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.VANILLA_ROOT,
                ResourceLocation.fromNamespaceAndPath(EndlessDefense.MODID, "status_hud"),
                (graphics, tracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.options.hideGui) return;
                    int panelWidth = Math.min(312, mc.getWindow().getGuiScaledWidth() - 16);
                    int x = mc.getWindow().getGuiScaledWidth() - panelWidth - 8, y = 8;
                    graphics.fill(x - 4, y - 4, x + panelWidth,
                            y + 72 + Math.min(3, status.upgrades()[6]) * 11, 0x990A1119);
                    graphics.drawString(mc.font, "Endless Defense  |  웨이브 " + status.wave()
                            + "  최고 " + status.highest(), x, y, 0xFFD480, true);
                    String state = status.phase() == 3 ? "게임 오버 · 종료 투표 가능"
                            : status.phase() == 2 ? "전투 중 · 남은 적 " + status.remaining()
                            : status.phase() == 1 ? status.countdown() > 0 ? "다음 웨이브 " + status.countdown() + "초" : "다음 웨이브 대기 중"
                            : "시작 준비 · " + status.ready() + "/" + status.total() + "명";
                    graphics.drawString(mc.font, state, x, y + 11, 0xFFFFFF, true);
                    graphics.drawString(mc.font, "노드 " + status.coins() + " · G 준비  H 상태  K 상점  J 주입",
                            x, y + 22, 0xAAAAAA, true);
                    graphics.drawString(mc.font, (status.phase() == 1 ? "다음 웨이브 팀 부활 " : "팀 부활 ")
                                    + status.respawns() + "회" + (status.night() ? "  ·  야간 웨이브" : "")
                                    + (status.surrenderVotes() > 0 ? "  ·  종료 " + status.surrenderVotes() + "/" + status.total() : ""),
                            x, y + 33, 0xFFCCAA, true);
                    if (status.defenseWorld()) graphics.drawString(mc.font,
                            "핵심 " + status.coreHp() + "/" + status.coreMaxHp()
                                    + " · 포탑 " + ShopRules.turretCount(status.turretLevels()) + "/4",
                            x, y + 44, 0x99DDFF, true);
                    graphics.drawString(mc.font, "마력 주입 " + status.pendingInfusions() + "회 · 주 손 장비 + J",
                            x, y + 55, 0xBB99FF, true);
                    graphics.drawString(mc.font, status.phase() == 0 ? "H 메뉴에서 웨이브 포기 투표 가능" : "⚠ H 메뉴: 웨이브 포기 투표",
                            x, y + 66, 0xFF8888, true);
                    if (status.upgrades()[6] >= 1)
                        graphics.drawString(mc.font, "정찰: 다음 보스 " + (((status.wave() / setting(status, "bossInterval", 5)) + 1)
                                * setting(status, "bossInterval", 5)) + "웨이브", x, y + 77, 0x99DDFF, true);
                    if (status.upgrades()[6] >= 2)
                        graphics.drawString(mc.font, "빨강 보스 · 보라 주입자 · 금색 수호자", x, y + 88, 0x99DDFF, true);
                    if (status.upgrades()[6] >= 3)
                        graphics.drawString(mc.font, "현재 적 체력 배율 ×" + String.format(java.util.Locale.ROOT, "%.1f",
                                BalanceRules.healthMultiplier(Math.max(1, status.wave()),
                                        AdminSettings.value(status.adminValues(), "healthGrowth"),
                                        AdminSettings.value(status.adminValues(), "healthCap"))),
                                x, y + 99, 0x99DDFF, true);
                });
    }

    private static int setting(EndlessDefense.StatusPacket state, String key, int fallback) {
        int value = AdminSettings.value(state.adminValues(), key);
        return value > 0 ? value : fallback;
    }

    @Mod.EventBusSubscriber(modid = EndlessDefense.MODID, value = Dist.CLIENT)
    public static final class RuntimeEvents {
        @SubscribeEvent public static void nameTag(RenderNameTagEvent event) {
            if (!(event.getEntity() instanceof LivingEntity mob)
                    || mob.getTeam() == null || !mob.getTeam().getName().startsWith("ed_wave_")) return;
            float max = Math.max(1, mob.getMaxHealth());
            int filled = Math.max(0, Math.min(10, Math.round(mob.getHealth() / max * 10)));
            int color = filled > 6 ? 0x55FF55 : filled > 3 ? 0xFFFF55 : 0xFF5555;
            Component bar = Component.literal("▰".repeat(filled) + "▱".repeat(10 - filled)
                    + " " + (int) Math.ceil(mob.getHealth()) + "/" + (int) Math.ceil(max) + " ♥ ")
                    .withColor(color).append(event.getOriginalContent());
            event.setContent(bar);
            event.setResult(Event.Result.ALLOW);
        }

        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().getConnection() == null) return;
            if (Minecraft.getInstance().screen == null) {
                while (READY.consumeClick()) {
                    if (status.phase() == 0) EndlessDefense.requestReady(!status.selfReady());
                }
                while (PANEL.consumeClick()) Minecraft.getInstance().setScreen(new StatusScreen());
                while (SHOP.consumeClick()) Minecraft.getInstance().setScreen(new ShopScreen());
                while (INFUSE.consumeClick()) EndlessDefense.requestInfusion();
            }
        }
    }

    private static final class StatusScreen extends Screen {
        private Button readyButton;
        private Button surrenderButton;
        private StatusScreen() { super(Component.literal("Endless Survival Defense")); }

        @Override protected void init() {
            readyButton = Button.builder(Component.literal("준비/취소"), button -> {
                if (status.phase() == 0) EndlessDefense.requestReady(!status.selfReady());
            }).bounds(width / 2 - 80, height / 2 + 45, 160, 20).build();
            addRenderableWidget(readyButton);
            surrenderButton = Button.builder(Component.literal("⚠ 웨이브 포기 투표"), button ->
                    EndlessDefense.requestSurrender(!status.selfSurrender()))
                    .bounds(width / 2 - 100, height / 2 + 98, 200, 20).build();
            addRenderableWidget(surrenderButton);
            addRenderableWidget(Button.builder(Component.literal("상점 열기"), button -> Minecraft.getInstance().setScreen(new ShopScreen()))
                    .bounds(width / 2 - 80, height / 2 + 70, 160, 20).build());
            addRenderableWidget(Button.builder(Component.literal("닫기"), button -> onClose())
                    .bounds(width / 2 + 88, height / 2 + 45, 95, 20).build());
            if (status.canConfigure()) addRenderableWidget(Button.builder(Component.literal("관리자 설정"),
                    button -> Minecraft.getInstance().setScreen(new AdminScreen()))
                    .bounds(width / 2 - 183, height / 2 + 45, 95, 20).build());
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            readyButton.active = status.phase() == 0;
            readyButton.setMessage(Component.literal(status.phase() != 0 ? "자동 웨이브 진행 중" : status.selfReady() ? "준비 취소" : "준비 완료"));
            surrenderButton.active = status.phase() != 0;
            surrenderButton.setMessage(Component.literal(status.selfSurrender() ? "⚠ 포기 투표 취소" : "⚠ 웨이브 포기 투표 "
                    + status.surrenderVotes() + "/" + status.total()));
            int x = width / 2 - 105;
            int y = height / 2 - 72;
            graphics.drawString(font, "웨이브: " + status.wave() + "  /  최고: " + status.highest(), x, y, 0xFFFFFF, true);
            graphics.drawString(font, status.phase() == 3 ? "게임 오버 · 종료 투표 가능"
                    : status.phase() == 2 ? "남은 적: " + status.remaining()
                    : status.phase() == 1 ? "다음 웨이브 대기 중" : "준비 인원: " + status.ready() + "/" + status.total(), x, y + 14, 0xFFFFFF, true);
            graphics.drawString(font, "보유 노드: " + status.coins(), x, y + 25, 0xFFFFAA, true);
            graphics.drawString(font, "마력 주입 " + status.pendingInfusions() + "회 · 주 손 장비 + J",
                    x, y - 24, 0xBB99FF, true);
            if (status.defenseWorld()) graphics.drawString(font,
                    "기지 핵심 " + status.coreHp() + "/" + status.coreMaxHp() + " · 단계 " + status.coreTier()
                            + " · 포탑 " + ShopRules.turretCount(status.turretLevels()) + "/4",
                    x, y - 12, 0x99DDFF, true);
            graphics.drawString(font, (status.phase() == 1 ? "다음 웨이브 팀 부활: " : "팀 공유 부활: ")
                            + status.respawns() + "회" + (status.night() ? " · 야간" : ""),
                    x, y + 36, 0xFFCCAA, true);
            graphics.drawString(font, "참가자 준비 상태", x, y + 49, 0xFFD480, true);
            String[] players = status.roster().isBlank() ? new String[]{"접속 중인 플레이어 없음"}
                    : status.roster().split("  ");
            for (int i = 0; i < Math.min(players.length, 4); i++)
                graphics.drawString(font, players[i], x, y + 62 + i * 11, 0xDDDDDD, true);
            if (players.length > 4) graphics.drawString(font, "+" + (players.length - 4) + "명 더", x, y + 106,
                    0xAAAAAA, true);
            graphics.drawCenteredString(font, "포기 투표가 과반이면 현재 게임을 종료합니다", width / 2,
                    height / 2 + 86, 0xFF8888);
        }
    }

    private static final class ShopScreen extends Screen {
        private static final ShopRules.Category[] TABS = ShopRules.Category.values();
        private static final int[][] CATALOGS = java.util.Arrays.stream(TABS).map(ShopRules::catalog).toArray(int[][]::new);
        private final Button[] upgradeButtons = new Button[4];
        private final Button[] itemButtons = new Button[4];
        private final Button[] tabButtons = new Button[TABS.length];
        private Button previousUpgrade, nextUpgrade, previousItem, nextItem;
        private int tab, itemPage, upgradeOffset, visibleCards, cardWidth, left, top;
        private boolean unavailable;
        private ShopScreen() { super(Component.literal("Endless Defense 상점")); }

        @Override protected void init() {
            try {
                initShop();
            } catch (RuntimeException exception) {
                showUnavailable(exception);
            }
        }

        private void initShop() {
            top = Math.max(8, (height - 218) / 2);
            visibleCards = Math.min(4, Math.max(2, (width - 56) / 88));
            cardWidth = Math.min(108, (width - 56) / visibleCards);
            left = (width - visibleCards * cardWidth) / 2;
            for (int i = 0; i < upgradeButtons.length; i++) {
                final int slot = i;
                int x = left + i * cardWidth + 5;
                upgradeButtons[i] = Button.builder(Component.literal("구매"), button -> {
                    int id = upgradeId(slot);
                    if (id >= 0) EndlessDefense.requestPurchase(id);
                }).bounds(x, top + 84, cardWidth - 10, 19).build();
                addRenderableWidget(upgradeButtons[i]);
                itemButtons[i] = Button.builder(Component.literal("구매"), button -> {
                    int id = itemId(slot);
                    if (id >= 0) EndlessDefense.requestPurchase(id);
                }).bounds(x, top + 198, cardWidth - 10, 19).build();
                addRenderableWidget(itemButtons[i]);
            }
            previousUpgrade = addRenderableWidget(Button.builder(Component.literal("‹"), button ->
                    upgradeOffset = Math.max(0, upgradeOffset - visibleCards)).bounds(left - 22, top + 55, 20, 20).build());
            nextUpgrade = addRenderableWidget(Button.builder(Component.literal("›"), button ->
                    upgradeOffset = Math.min(ShopRules.UPGRADE_IDS.length - 1, upgradeOffset + visibleCards))
                    .bounds(left + visibleCards * cardWidth + 2, top + 55, 20, 20).build());
            for (int i = 0; i < TABS.length; i++) {
                final int chosen = i;
                int w = (width - 16) / TABS.length;
                tabButtons[i] = addRenderableWidget(Button.builder(Component.literal(TABS[i].title()), button -> {
                    tab = chosen;
                    itemPage = 0;
                }).bounds(8 + i * w, top + 111, w - 2, 20).build());
            }
            previousItem = addRenderableWidget(Button.builder(Component.literal("‹"), button ->
                    itemPage = Math.max(0, itemPage - 1)).bounds(left - 22, top + 169, 20, 20).build());
            nextItem = addRenderableWidget(Button.builder(Component.literal("›"), button ->
                    itemPage = Math.min((catalog().length - 1) / visibleCards, itemPage + 1))
                    .bounds(left + visibleCards * cardWidth + 2, top + 169, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("돌아가기"), button -> onClose())
                    .bounds(width - 65, top, 58, 18).build());
        }

        @Override public void onClose() { Minecraft.getInstance().setScreen(new StatusScreen()); }

        private int upgradeId(int slot) {
            int index = upgradeOffset + slot;
            return slot < visibleCards && index < ShopRules.UPGRADE_IDS.length ? ShopRules.UPGRADE_IDS[index] : -1;
        }

        private int itemId(int slot) {
            int index = itemPage * visibleCards + slot;
            int[] entries = catalog();
            return slot < visibleCards && index < entries.length ? entries[index] : -1;
        }

        private int[] catalog() {
            return java.util.Arrays.stream(CATALOGS[tab])
                    .filter(id -> status.defenseWorld() || id < ShopRules.TURRET_ID)
                    .boxed().sorted(java.util.Comparator.<Integer>comparingLong(id -> {
                        long value = price(id);
                        return value < 0 ? Long.MAX_VALUE : value;
                    }).thenComparingInt(id -> id)).mapToInt(Integer::intValue).toArray();
        }

        private long price(int id) {
            if (id == ShopRules.TURRET_UPGRADE_ID)
                return ShopRules.turretUpgradeCost(ShopRules.lowestTurretLevel(status.turretLevels()),
                        setting("turretUpgradeBase"), setting("turretUpgradeGrowth"));
            if (id == ShopRules.BASE_UPGRADE_ID)
                return ShopRules.upgradeCost(status.coreTier() - 1,
                        setting("baseUpgradeBase"), setting("baseUpgradeGrowth"));
            int slot = ShopRules.upgradeSlot(id);
            return slot >= 0 ? ShopRules.upgradeCost(status.upgrades()[slot],
                    setting("upgradeBase"), setting("upgradeGrowth")) : setting("price:" + id);
        }

        private int setting(String key) {
            return AdminSettings.value(status.adminValues(), key);
        }

        private static ItemStack icon(int id) {
            return switch (id) {
                case 0 -> new ItemStack(Items.DIAMOND_PICKAXE);
                case 1 -> new ItemStack(Items.GOLDEN_APPLE);
                case 2 -> new ItemStack(Items.SUGAR);
                case 6 -> new ItemStack(Items.IRON_CHESTPLATE);
                case 7 -> new ItemStack(Items.IRON_SWORD);
                case 8 -> new ItemStack(Items.GLISTERING_MELON_SLICE);
                case 9 -> new ItemStack(Items.SPYGLASS);
                case 29 -> new ItemStack(Items.WIND_CHARGE);
                default -> {
                    ShopRules.Product product = ShopRules.product(id);
                    yield product == null || product.items().length == 0
                            ? new ItemStack(Items.BARRIER) : product.items()[0];
                }
            };
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (unavailable) {
                renderUnavailable(graphics);
                return;
            }
            try {
                renderShop(graphics, mouseX, mouseY, partialTick);
            } catch (RuntimeException exception) {
                showUnavailable(exception);
                renderUnavailable(graphics);
            }
        }

        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (unavailable) return true;
            try {
                return super.mouseClicked(mouseX, mouseY, button);
            } catch (RuntimeException exception) {
                showUnavailable(exception);
                return true;
            }
        }

        private void showUnavailable(RuntimeException exception) {
            if (unavailable) return;
            unavailable = true;
            LOGGER.error("Endless Defense shop screen failed", exception);
        }

        private void renderUnavailable(GuiGraphics graphics) {
            graphics.fill(0, 0, width, height, 0xEE101820);
            graphics.drawCenteredString(font, "상점 정보를 불러올 수 없음", width / 2, height / 2 - 10, 0xFF8888);
            graphics.drawCenteredString(font, "ESC를 눌러 돌아가세요 · 자세한 오류는 latest.log 확인",
                    width / 2, height / 2 + 8, 0xFFFFFF);
        }

        private void renderShop(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            upgradeOffset = Math.min(upgradeOffset, ((ShopRules.UPGRADE_IDS.length - 1) / visibleCards) * visibleCards);
            int catalogLength = catalog().length;
            itemPage = Math.min(itemPage, (catalogLength - 1) / visibleCards);
            previousUpgrade.active = upgradeOffset > 0;
            nextUpgrade.active = upgradeOffset + visibleCards < ShopRules.UPGRADE_IDS.length;
            previousItem.active = itemPage > 0;
            nextItem.active = (itemPage + 1) * visibleCards < catalogLength;
            for (int i = 0; i < TABS.length; i++)
                tabButtons[i].setMessage(Component.literal((tab == i ? "● " : "") + TABS[i].title()));
            for (int i = 0; i < upgradeButtons.length; i++) {
                int upgrade = upgradeId(i), item = itemId(i);
                upgradeButtons[i].visible = upgrade >= 0;
                itemButtons[i].visible = item >= 0;
                if (upgrade >= 0) updatePurchaseButton(upgradeButtons[i], upgrade);
                if (item >= 0) updatePurchaseButton(itemButtons[i], item);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, "상점 · 보유 노드 " + status.coins(), left, top + 4, 0xFFD480, true);
            graphics.drawCenteredString(font, "지속 업그레이드 " + (upgradeOffset / visibleCards + 1) + "/"
                    + ((ShopRules.UPGRADE_IDS.length - 1) / visibleCards + 1), width / 2, top + 20, 0x99DDFF);
            for (int i = 0; i < visibleCards; i++) {
                int id = upgradeId(i);
                if (id >= 0) drawCard(graphics, id, left + i * cardWidth, top + 30, true);
            }
            graphics.fill(8 + tab * ((width - 16) / TABS.length), top + 132,
                    8 + (tab + 1) * ((width - 16) / TABS.length) - 2, top + 134, 0xFFFFD480);
            graphics.drawCenteredString(font, "가격 낮은 순 · " + (itemPage + 1) + "/"
                    + ((catalogLength - 1) / visibleCards + 1), width / 2, top + 137, 0xAAAAAA);
            for (int i = 0; i < visibleCards; i++) {
                int id = itemId(i);
                if (id >= 0) drawCard(graphics, id, left + i * cardWidth, top + 147, false);
            }
            for (int i = 0; i < visibleCards; i++) {
                int x = left + i * cardWidth;
                int id = mouseX >= x && mouseX < x + cardWidth ?
                        mouseY >= top + 30 && mouseY < top + 104 ? upgradeId(i) :
                        mouseY >= top + 147 && mouseY < top + 218 ? itemId(i) : -1 : -1;
                if (id >= 0) {
                    int slot = ShopRules.upgradeSlot(id);
                    String detail = slot >= 0 ? ShopRules.nextEffect(id, status.upgrades()[slot])
                            : ShopRules.product(id).description();
                    if (id == ShopRules.TURRET_ID) detail += " · 공격 "
                            + ShopRules.turretDamage(1, setting("turretDamage"), setting("turretDamageStep"))
                            + " · 사거리 " + ShopRules.turretRange(1, setting("turretRange"), setting("turretRangeStep"))
                            + " · " + ShopRules.turretInterval(1, setting("turretInterval"), setting("turretIntervalStep"))
                            + "틱마다 발사";
                    if (id == ShopRules.TURRET_UPGRADE_ID) {
                        int lowest = ShopRules.lowestTurretLevel(status.turretLevels());
                        if (lowest < Integer.MAX_VALUE) {
                            int next = lowest + 1;
                            detail += " · 다음 " + next + "단계: 공격 "
                                    + ShopRules.turretDamage(next, setting("turretDamage"), setting("turretDamageStep"))
                                    + " · 사거리 " + ShopRules.turretRange(next, setting("turretRange"), setting("turretRangeStep"))
                                    + " · " + ShopRules.turretInterval(next, setting("turretInterval"), setting("turretIntervalStep"))
                                    + "틱마다 발사";
                        }
                    }
                    if (id == ShopRules.BASE_UPGRADE_ID)
                        detail += " · 다음 핵심 최대 체력 " + (setting("coreBaseHp")
                                + (long) status.coreTier() * setting("coreHpStep"));
                    graphics.renderTooltip(font, Component.literal(ShopRules.NAMES[id] + " · " + detail), mouseX, mouseY);
                    break;
                }
            }
        }

        private void updatePurchaseButton(Button button, int id) {
            ShopRules.Product product = ShopRules.product(id);
            long price = price(id);
            int cap = setting("cap:" + id);
            int stock = cap > 0 ? cap - status.stock()[id] : 1;
            boolean unlocked = status.wave() >= setting("unlock:" + id);
            boolean turret = id >= ShopRules.TURRET_ID && id <= ShopRules.TURRET_REPAIR_ID;
            boolean base = id >= ShopRules.BASE_UPGRADE_ID && id <= ShopRules.WALL_REPAIR_ID;
            boolean available = id == ShopRules.TURRET_ID ? ShopRules.turretCount(status.turretLevels()) < ShopRules.MAX_TURRETS
                    : id == ShopRules.TURRET_UPGRADE_ID ? price >= 0 : id != ShopRules.TURRET_REPAIR_ID || status.brokenTurrets() > 0;
            if (id == ShopRules.CORE_REPAIR_ID) available = status.coreHp() < status.coreMaxHp();
            button.active = unlocked && price >= 0 && status.coins() >= price && stock > 0
                    && ((!turret && !base) || status.defenseWorld() && available);
            button.setMessage(Component.literal(!unlocked ? "잠김" : (turret || base) && !available ? "불가"
                    : price < 0 ? "최대" : stock == 0 ? "품절" : "구매"));
        }

        private void drawCard(GuiGraphics graphics, int id, int x, int y, boolean upgrade) {
            ShopRules.Product product = ShopRules.product(id);
            int slot = ShopRules.upgradeSlot(id);
            int level = slot >= 0 ? status.upgrades()[slot] : 0;
            long price = price(id);
            int textColor = status.wave() < setting("unlock:" + id) ? 0x888888 : 0xFFFFFF;
            graphics.fill(x + 2, y, x + cardWidth - 2, y + (upgrade ? 52 : 49), 0xBB15232E);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(product.name(), cardWidth - 8),
                    x + cardWidth / 2, y + 3, textColor);
            graphics.renderItem(icon(id), x + cardWidth / 2 - 8, y + (upgrade ? 15 : 13));
            if (upgrade)
                graphics.drawCenteredString(font, "레벨 " + level,
                        x + cardWidth / 2, y + 33, 0x99DDFF);
            else if (id >= ShopRules.TURRET_ID && id <= ShopRules.TURRET_REPAIR_ID)
                graphics.drawCenteredString(font, "포탑 " + ShopRules.turretCount(status.turretLevels()) + "/4"
                                + (status.brokenTurrets() > 0 ? " · 파손 " + status.brokenTurrets() : ""),
                        x + cardWidth / 2, y + 30, 0x99DDFF);
            else if (id >= ShopRules.BASE_UPGRADE_ID && id <= ShopRules.WALL_REPAIR_ID)
                graphics.drawCenteredString(font, "핵심 " + status.coreHp() + "/" + status.coreMaxHp(),
                        x + cardWidth / 2, y + 30, 0x99DDFF);
            else if (setting("cap:" + id) > 0)
                graphics.drawCenteredString(font, "재고 " + Math.max(0, setting("cap:" + id) - status.stock()[id]),
                        x + cardWidth / 2, y + 30, 0xAAAAAA);
            String priceLabel = id == ShopRules.TURRET_UPGRADE_ID && price < 0
                    && ShopRules.turretCount(status.turretLevels()) == 0 ? "포탑 필요"
                    : price < 0 ? "저장 한계" : price + " 노드";
            graphics.drawCenteredString(font, priceLabel,
                    x + cardWidth / 2, y + (upgrade ? 44 : 41), 0xFFD480);
        }
    }

    private static final class AdminScreen extends Screen {
        private final Button[] rows = new Button[AdminSettings.PAGE_SIZE];
        private final Button[] minus = new Button[AdminSettings.PAGE_SIZE];
        private final Button[] plus = new Button[AdminSettings.PAGE_SIZE];
        private Button previous, next;
        private EditBox input;
        private AdminSettings.Group group = AdminSettings.Group.COMBAT;
        private int page, selected = -1, top;
        private String inputError = "";
        private AdminScreen() { super(Component.literal("Endless Defense 관리자 설정")); }

        @Override protected void init() {
            if (!status.canConfigure()) { onClose(); return; }
            top = Math.max(4, (height - 230) / 2);
            AdminSettings.Group[] groups = AdminSettings.Group.values();
            int tabWidth = (width - 12) / groups.length;
            for (int i = 0; i < groups.length; i++) {
                final AdminSettings.Group chosen = groups[i];
                addRenderableWidget(Button.builder(Component.literal(chosen.title()), button -> {
                    group = chosen;
                    page = 0;
                    selected = -1;
                    input.setValue("");
                }).bounds(6 + i * tabWidth, top, tabWidth - 2, 18).build());
            }
            for (int i = 0; i < rows.length; i++) {
                final int row = i;
                int y = top + 40 + i * 22;
                rows[i] = addRenderableWidget(Button.builder(Component.empty(), button -> select(entryAt(row)))
                        .bounds(width / 2 - 150, y, 198, 20).build());
                minus[i] = addRenderableWidget(Button.builder(Component.literal("−"), button -> change(row, -1))
                        .bounds(width / 2 + 52, y, 24, 20).build());
                plus[i] = addRenderableWidget(Button.builder(Component.literal("+"), button -> change(row, 1))
                        .bounds(width / 2 + 80, y, 24, 20).build());
            }
            previous = addRenderableWidget(Button.builder(Component.literal("‹ 이전"), button -> page = Math.max(0, page - 1))
                    .bounds(width / 2 - 112, top + 152, 70, 20).build());
            next = addRenderableWidget(Button.builder(Component.literal("다음 ›"), button -> page = Math.min(pageCount() - 1, page + 1))
                    .bounds(width / 2 + 42, top + 152, 70, 20).build());
            input = addRenderableWidget(new EditBox(font, width / 2 - 55, top + 177, 110, 19,
                    Component.literal("설정값 입력")));
            input.setFilter(value -> value.matches("-?\\d*"));
            if (selected >= 0) input.setValue(Integer.toString(status.adminValues()[selected]));
            addRenderableWidget(Button.builder(Component.literal("적용"), button -> applyInput())
                    .bounds(width / 2 + 60, top + 177, 52, 19).build());
            addRenderableWidget(Button.builder(Component.literal("돌아가기"), button -> onClose())
                    .bounds(width / 2 - 80, top + 202, 160, 20).build());
        }

        private int pageCount() {
            return Math.max(1, (AdminSettings.indices(group).size() + AdminSettings.PAGE_SIZE - 1)
                    / AdminSettings.PAGE_SIZE);
        }

        private int entryAt(int row) {
            var entries = AdminSettings.indices(group);
            int position = page * AdminSettings.PAGE_SIZE + row;
            return position < entries.size() ? entries.get(position) : -1;
        }

        private void select(int index) {
            if (index < 0) return;
            selected = index;
            input.setValue(Integer.toString(status.adminValues()[index]));
            inputError = "";
        }

        private void change(int row, int direction) {
            int index = entryAt(row);
            if (index < 0 || !status.canConfigure()) return;
            AdminSettings.Entry entry = AdminSettings.ENTRIES.get(index);
            long target = (long) status.adminValues()[index] + (long) direction * entry.step();
            int value = (int) Math.max(entry.min(), Math.min(entry.max(), target));
            select(index);
            input.setValue(Integer.toString(value));
            if (value != status.adminValues()[index]) EndlessDefense.requestConfig(index, value);
        }

        private void applyInput() {
            if (selected < 0 || !status.canConfigure()) return;
            try {
                int value = Integer.parseInt(input.getValue());
                AdminSettings.Entry entry = AdminSettings.ENTRIES.get(selected);
                if (value < entry.min() || value > entry.max()) {
                    inputError = "허용 범위: " + entry.min() + "~" + entry.max();
                    return;
                }
                EndlessDefense.requestConfig(selected, value);
                inputError = "";
            } catch (NumberFormatException exception) {
                inputError = "정수를 입력하세요";
            }
        }

        @Override public void onClose() { Minecraft.getInstance().setScreen(new StatusScreen()); }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            previous.active = page > 0;
            next.active = page + 1 < pageCount();
            for (int i = 0; i < rows.length; i++) {
                int index = entryAt(i);
                rows[i].visible = minus[i].visible = plus[i].visible = index >= 0;
                if (index < 0) continue;
                AdminSettings.Entry entry = AdminSettings.ENTRIES.get(index);
                rows[i].setMessage(Component.literal(font.plainSubstrByWidth(
                        (index == selected ? "● " : "") + entry.label() + ": " + status.adminValues()[index], 186)));
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, group.title() + " 설정 · " + (page + 1) + "/" + pageCount(),
                    width / 2, top + 24, 0xFFD480);
            graphics.drawCenteredString(font, selected < 0 ? "행을 선택한 뒤 숫자를 입력하세요" : inputError.isEmpty()
                    ? "선택값 입력 후 적용 · 변경은 다음 계산부터" : inputError,
                    width / 2, top + 225, inputError.isEmpty() ? 0xAAAAAA : 0xFF7777);
        }
    }

    private static final class AshRenderer extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
                EndlessDefense.MODID, "textures/entity/ash_runner.png");
        private AshRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(Zombie entity) { return TEXTURE; }
    }

    private static final class BulwarkRenderer extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
                EndlessDefense.MODID, "textures/entity/iron_bulwark.png");
        private BulwarkRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(Zombie entity) { return TEXTURE; }
    }

    private static final class FrostRenderer extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/drowned.png");
        private FrostRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(Zombie entity) { return TEXTURE; }
    }

    private static final class SiegeRenderer extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/husk.png");
        private SiegeRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(Zombie entity) { return TEXTURE; }
    }

    private static final class DreadRenderer extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
        private DreadRenderer(EntityRendererProvider.Context context) { super(context); }
        @Override public ResourceLocation getTextureLocation(Zombie entity) { return TEXTURE; }
    }
}
