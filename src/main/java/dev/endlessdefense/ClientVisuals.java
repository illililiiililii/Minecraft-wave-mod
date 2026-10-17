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
import net.minecraft.world.entity.monster.Zombie;
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
                    graphics.drawString(mc.font, "팀 부활 " + status.respawns() + "회" + (status.night() ? "  ·  야간 웨이브" : "")
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
                            || mob.getType() == ModMobs.IRON_BULWARK.get())) return;
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
            graphics.drawString(font, "팀 공유 부활: " + status.respawns() + "회" + (status.night() ? " · 야간" : ""),
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
        private static final int PAGE_SIZE = 6;
        private final Button[] buyButtons = new Button[PAGE_SIZE];
        private int page;
        private ShopScreen() { super(Component.literal("Endless Defense 상점")); }

        @Override protected void init() {
            int y0 = height / 2 - 66;
            for (int i = 0; i < buyButtons.length; i++) {
                final int row = i;
                buyButtons[i] = Button.builder(Component.literal("구매"), button -> EndlessDefense.requestPurchase(page * PAGE_SIZE + row))
                        .bounds(width / 2 + 118, y0 + i * 23, 48, 20).build();
                addRenderableWidget(buyButtons[i]);
            }
            addRenderableWidget(Button.builder(Component.literal("◀ 이전"), button -> page = Math.max(0, page - 1))
                    .bounds(width / 2 - 118, height / 2 + 78, 75, 20).build());
            addRenderableWidget(Button.builder(Component.literal("다음 ▶"), button -> page = Math.min((ShopRules.NAMES.length - 1) / PAGE_SIZE, page + 1))
                    .bounds(width / 2 + 43, height / 2 + 78, 75, 20).build());
            addRenderableWidget(Button.builder(Component.literal("돌아가기"), button -> onClose())
                    .bounds(width / 2 - 70, height / 2 + 102, 140, 20).build());
        }

        @Override public void onClose() { Minecraft.getInstance().setScreen(new StatusScreen()); }

        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawCenteredString(font, "상점 · 보유 노드 " + status.coins() + " · " + (page + 1)
                    + "/" + ((ShopRules.NAMES.length + PAGE_SIZE - 1) / PAGE_SIZE), width / 2, height / 2 - 85, 0xFFD480);
            int y0 = height / 2 - 66;
            for (int i = 0; i < buyButtons.length; i++) {
                int id = page * PAGE_SIZE + i;
                if (id >= ShopRules.NAMES.length) { buyButtons[i].visible = false; continue; }
                buyButtons[i].visible = true;
                ShopRules.Product item = ShopRules.product(id);
                int slot = ShopRules.upgradeSlot(id);
                int level = slot >= 0 ? status.upgrades()[slot] : 0;
                long cost = ShopRules.cost(id, level);
                int remaining = item.perWave() > 0 ? item.perWave() - status.stock()[id] : -1;
                boolean unlocked = status.wave() >= item.unlockWave();
                buyButtons[i].active = unlocked && cost >= 0 && status.coins() >= cost && remaining != 0;
                String headline = item.name() + (slot >= 0 ? " " + level + "/" + ShopRules.maxLevel(id) : "")
                        + " · " + (cost < 0 ? "최대" : cost + " 노드");
                graphics.drawString(font, headline, width / 2 - 166, y0 + i * 23 + 1, 0xFFFFFF, true);
                String detail = slot >= 0 ? "다음 단계: " + ShopRules.nextEffect(id, level) : item.description();
                if (!unlocked) detail += " · " + item.unlockWave() + "웨이브 해금";
                else if (remaining >= 0) detail += " · 남은 재고 " + remaining;
                graphics.drawString(font, detail, width / 2 - 166, y0 + i * 23 + 11, 0xAAAAAA, true);
            }
            graphics.drawCenteredString(font, "지속 효과는 사망·재접속 후에도 유지", width / 2, height / 2 + 67, 0xAAAAAA);
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
