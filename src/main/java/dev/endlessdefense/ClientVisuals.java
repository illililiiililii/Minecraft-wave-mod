package dev.endlessdefense;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
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

@Mod.EventBusSubscriber(modid = EndlessDefense.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientVisuals {
    private static final KeyMapping READY = new KeyMapping("key.endlessdefense.ready", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G, "key.categories.endlessdefense");
    private static final KeyMapping PANEL = new KeyMapping("key.endlessdefense.panel", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, "key.categories.endlessdefense");
    private static final KeyMapping SHOP = new KeyMapping("key.endlessdefense.shop", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.endlessdefense");
    private static EndlessDefense.StatusPacket status = new EndlessDefense.StatusPacket(
            0, 0, 0, -1, false, 0, 0, 0, "", false, 4, 100, 100, 2, 0, 15, 0,
            new int[ShopRules.UPGRADE_IDS.length], new int[ShopRules.NAMES.length], 0, false, 0, false);

    public static void accept(EndlessDefense.StatusPacket newStatus) { status = newStatus; }

    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
        event.register(READY);
        event.register(PANEL);
        event.register(SHOP);
    }

    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMobs.ASH_RUNNER.get(), AshRenderer::new);
        event.registerEntityRenderer(ModMobs.IRON_BULWARK.get(), BulwarkRenderer::new);
    }

    @SubscribeEvent public static void overlay(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.VANILLA_ROOT,
                ResourceLocation.fromNamespaceAndPath(EndlessDefense.MODID, "status_hud"),
                (graphics, tracker) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.options.hideGui) return;
                    int x = 8, y = 8;
                    graphics.fill(x - 4, y - 4, x + 285, y + 62 + status.upgrades()[6] * 11, 0x990A1119);
                    graphics.drawString(mc.font, "Endless Defense  |  웨이브 " + status.wave()
                            + "  최고 " + status.highest(), x, y, 0xFFD480, true);
                    String state = status.phase() == 3 ? "게임 오버 · 종료 투표 가능"
                            : status.phase() == 2 ? "전투 중 · 남은 적 " + status.remaining()
                            : status.phase() == 1 ? status.countdown() > 0 ? "다음 웨이브 " + status.countdown() + "초" : "다음 웨이브 대기 중"
                            : "시작 준비 · " + status.ready() + "/" + status.total() + "명";
                    graphics.drawString(mc.font, state, x, y + 11, 0xFFFFFF, true);
                    graphics.drawString(mc.font, "노드 " + status.coins() + "  ·  G 준비  H 상태  K 상점", x, y + 22, 0xAAAAAA, true);
                    graphics.drawString(mc.font, (status.phase() == 1 ? "다음 웨이브 팀 부활 " : "팀 부활 ")
                                    + status.respawns() + "회" + (status.night() ? "  ·  야간 웨이브" : "")
                                    + (status.surrenderVotes() > 0 ? "  ·  종료 " + status.surrenderVotes() + "/" + status.total() : ""),
                            x, y + 33, 0xFFCCAA, true);
                    graphics.drawString(mc.font, status.phase() == 0 ? "H 메뉴에서 웨이브 포기 투표 가능" : "⚠ H 메뉴: 웨이브 포기 투표",
                            x, y + 44, 0xFF8888, true);
                    if (status.upgrades()[6] >= 1)
                        graphics.drawString(mc.font, "정찰: 다음 보스 " + (((status.wave() / 5) + 1) * 5) + "웨이브", x, y + 55, 0x99DDFF, true);
                    if (status.upgrades()[6] >= 2)
                        graphics.drawString(mc.font, "빨강 보스 · 금색 수호자 · 하늘색 추적자", x, y + 66, 0x99DDFF, true);
                    if (status.upgrades()[6] >= 3)
                        graphics.drawString(mc.font, "현재 적 체력 배율 ×" + String.format(java.util.Locale.ROOT, "%.1f",
                                BalanceRules.healthMultiplier(Math.max(1, status.wave()))), x, y + 77, 0x99DDFF, true);
                });
    }

    @Mod.EventBusSubscriber(modid = EndlessDefense.MODID, value = Dist.CLIENT)
    public static final class RuntimeEvents {
        @SubscribeEvent public static void nameTag(RenderNameTagEvent event) {
            if (!(event.getEntity() instanceof LivingEntity mob) ||
                    !(mob.getTags().contains("endlessdefense_wave") || mob.getType() == ModMobs.ASH_RUNNER.get()
                            || mob.getType() == ModMobs.IRON_BULWARK.get()
                            || mob.getType() == EntityType.ZOMBIE || mob.getType() == EntityType.SKELETON)) return;
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
        private ShopScreen() { super(Component.literal("Endless Defense 상점")); }

        @Override protected void init() {
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
                    itemPage = Math.min((CATALOGS[tab].length - 1) / visibleCards, itemPage + 1))
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
            return slot < visibleCards && index < CATALOGS[tab].length ? CATALOGS[tab][index] : -1;
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
                default -> ShopRules.product(id).items()[0];
            };
        }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            upgradeOffset = Math.min(upgradeOffset, ((ShopRules.UPGRADE_IDS.length - 1) / visibleCards) * visibleCards);
            itemPage = Math.min(itemPage, (CATALOGS[tab].length - 1) / visibleCards);
            previousUpgrade.active = upgradeOffset > 0;
            nextUpgrade.active = upgradeOffset + visibleCards < ShopRules.UPGRADE_IDS.length;
            previousItem.active = itemPage > 0;
            nextItem.active = (itemPage + 1) * visibleCards < CATALOGS[tab].length;
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
                    + ((CATALOGS[tab].length - 1) / visibleCards + 1), width / 2, top + 137, 0xAAAAAA);
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
                    graphics.renderTooltip(font, Component.literal(ShopRules.NAMES[id] + " · " + detail), mouseX, mouseY);
                    break;
                }
            }
        }

        private void updatePurchaseButton(Button button, int id) {
            ShopRules.Product product = ShopRules.product(id);
            int slot = ShopRules.upgradeSlot(id);
            long price = ShopRules.cost(id, slot >= 0 ? status.upgrades()[slot] : 0);
            int stock = product.perWave() > 0 ? product.perWave() - status.stock()[id] : 1;
            boolean unlocked = status.wave() >= product.unlockWave();
            button.active = unlocked && price >= 0 && status.coins() >= price && stock > 0;
            button.setMessage(Component.literal(price < 0 ? "최대" : !unlocked ? "잠김" : stock == 0 ? "품절" : "구매"));
        }

        private void drawCard(GuiGraphics graphics, int id, int x, int y, boolean upgrade) {
            ShopRules.Product product = ShopRules.product(id);
            int slot = ShopRules.upgradeSlot(id);
            int level = slot >= 0 ? status.upgrades()[slot] : 0;
            long price = ShopRules.cost(id, level);
            int textColor = status.wave() < product.unlockWave() ? 0x888888 : 0xFFFFFF;
            graphics.fill(x + 2, y, x + cardWidth - 2, y + (upgrade ? 52 : 49), 0xBB15232E);
            graphics.drawCenteredString(font, font.plainSubstrByWidth(product.name(), cardWidth - 8),
                    x + cardWidth / 2, y + 3, textColor);
            graphics.renderItem(icon(id), x + cardWidth / 2 - 8, y + (upgrade ? 15 : 13));
            if (upgrade)
                graphics.drawCenteredString(font, "레벨 " + level + "/" + ShopRules.maxLevel(id),
                        x + cardWidth / 2, y + 33, 0x99DDFF);
            else if (product.perWave() > 0)
                graphics.drawCenteredString(font, "재고 " + Math.max(0, product.perWave() - status.stock()[id]),
                        x + cardWidth / 2, y + 30, 0xAAAAAA);
            graphics.drawCenteredString(font, price < 0 ? "최대 레벨" : price + " 노드",
                    x + cardWidth / 2, y + (upgrade ? 44 : 41), 0xFFD480);
        }
    }

    private static final class AdminScreen extends Screen {
        private static final String[] LABELS = {"기본 적 수", "체력 %", "공격력 %", "탐지 단계", "추가 방어력", "장비 확률 %"};
        private static final int[] STEPS = {1, 25, 25, 1, 1, 5};
        private static final int[] MIN = {1, 25, 25, 1, 0, 0};
        private static final int[] MAX = {128, 500, 500, 5, 20, 100};

        private AdminScreen() { super(Component.literal("Endless Defense 관리자 설정")); }

        @Override protected void init() {
            if (!status.canConfigure()) { onClose(); return; }
            int y0 = height / 2 - 75;
            for (int i = 0; i < LABELS.length; i++) {
                final int option = i;
                final int y = y0 + i * 24;
                addRenderableWidget(Button.builder(Component.literal("−"), button -> change(option, -STEPS[option]))
                        .bounds(width / 2 + 55, y, 22, 20).build());
                addRenderableWidget(Button.builder(Component.literal("+"), button -> change(option, STEPS[option]))
                        .bounds(width / 2 + 82, y, 22, 20).build());
            }
            addRenderableWidget(Button.builder(Component.literal("돌아가기"), button -> onClose())
                    .bounds(width / 2 - 80, height / 2 + 80, 160, 20).build());
        }

        private void change(int option, int delta) {
            if (!status.canConfigure()) return;
            int[] values = {status.baseMobs(), status.healthPercent(), status.damagePercent(),
                    status.awareness(), status.armorBonus(), status.gearChance()};
            int value = Math.max(MIN[option], Math.min(MAX[option], values[option] + delta));
            if (value != values[option]) EndlessDefense.requestConfig(option, value);
        }

        @Override public void onClose() { Minecraft.getInstance().setScreen(new StatusScreen()); }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, title, width / 2, height / 2 - 88, 0xFFD480);
            int[] values = {status.baseMobs(), status.healthPercent(), status.damagePercent(),
                    status.awareness(), status.armorBonus(), status.gearChance()};
            int y0 = height / 2 - 75;
            for (int i = 0; i < LABELS.length; i++) {
                graphics.drawString(font, LABELS[i] + ": " + values[i], width / 2 - 105, y0 + i * 24 + 6,
                        0xFFFFFF, true);
            }
            graphics.drawCenteredString(font, "변경 사항은 다음 웨이브부터 적용", width / 2, height / 2 + 61, 0xAAAAAA);
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
}
