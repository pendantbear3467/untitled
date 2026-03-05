package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.layout.GuiScaleContext;
import com.extremecraft.config.DwConfig;
import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtremePlayerScreen extends Screen {
    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(ECConstants.MODID, "textures/gui/extreme_player_menu.png");
    private static final ResourceLocation MAGIC_SLOT = new ResourceLocation(ECConstants.MODID, "textures/gui/magic_slot.png");

    private static final int BASE_TEXTURE_WIDTH = 306;
    private static final int BASE_TEXTURE_HEIGHT = 206;

    private static final int TAB_WIDTH = 58;
    private static final int TAB_HEIGHT = 18;

    private final Player player;
    private final Screen returnScreen;
    private final SkillTreeScreenPanel skillTreePanel;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Button> statButtons = new ArrayList<>();

    private ExtremePlayerTabs.Tab activeTab = ExtremePlayerTabs.Tab.PLAYER_STATS;
    private String activeSkillTree = "combat";

    private int guiWidth;
    private int guiHeight;
    private int leftPos;
    private int topPos;
    private GuiScaleContext layoutContext;

    public ExtremePlayerScreen(Player player, Screen returnScreen) {
        super(Component.literal("ExtremeCraft Player"));
        this.player = player;
        this.returnScreen = returnScreen;
        this.skillTreePanel = new SkillTreeScreenPanel(this::getStats);
    }

    public ExtremePlayerScreen(Player player, ExtremePlayerTabs.Tab defaultTab) {
        this(player, Minecraft.getInstance().screen instanceof InventoryScreen inv ? inv : null);
        this.activeTab = defaultTab;
    }

    @Override
    protected void init() {
        tabButtons.clear();
        statButtons.clear();

        for (ExtremePlayerTabs.Tab tab : ExtremePlayerTabs.Tab.values()) {
            final ExtremePlayerTabs.Tab captured = tab;
            Button button = Button.builder(tab.label(), b -> setActiveTab(captured))
                    .bounds(0, 0, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        statButtons.add(addRenderableWidget(statButton("vitality")));
        statButtons.add(addRenderableWidget(statButton("strength")));
        statButtons.add(addRenderableWidget(statButton("agility")));
        statButtons.add(addRenderableWidget(statButton("endurance")));
        statButtons.add(addRenderableWidget(statButton("intelligence")));
        statButtons.add(addRenderableWidget(statButton("luck")));

        recalculateLayout();
        updateStatButtonVisibility();
        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());
    }

    private void setActiveTab(ExtremePlayerTabs.Tab tab) {
        if (activeTab == tab) {
            return;
        }

        activeTab = tab;
        recalculateLayout();
        updateStatButtonVisibility();
    }

    private Button statButton(String stat) {
        return Button.builder(Component.literal("+"), b -> ModNetwork.CHANNEL.sendToServer(new UpgradeStatPacket(stat)))
                .bounds(0, 0, 18, 16)
                .build();
    }

    private void recalculateLayout() {
        if (minecraft == null) {
            return;
        }

        layoutContext = GuiScaleContext.from(
                width,
                height,
                activeTab.preferredWidth(),
                activeTab.preferredHeight(),
                DwConfig.CLIENT.guiScaleMultiplier.get(),
                0.75D,
                1.75D,
                320,
                214
        );

        guiWidth = layoutContext.contentWidth();
        guiHeight = layoutContext.contentHeight();
        leftPos = layoutContext.left();
        topPos = layoutContext.top();

        int tabsStartX = leftPos + 10;
        int tabsY = topPos + 8;
        for (int i = 0; i < tabButtons.size(); i++) {
            Button button = tabButtons.get(i);
            button.setX(tabsStartX + (i * (TAB_WIDTH + 4)));
            button.setY(tabsY);
        }

        int rowY = topPos + 89;
        int buttonX = leftPos + 214;
        for (int i = 0; i < statButtons.size(); i++) {
            Button button = statButtons.get(i);
            button.setX(buttonX);
            button.setY(rowY + (i * 24));
        }
    }

    private void updateStatButtonVisibility() {
        boolean visible = activeTab == ExtremePlayerTabs.Tab.PLAYER_STATS;
        for (Button b : statButtons) {
            b.visible = visible;
            b.active = visible;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderFrame(graphics);

        switch (activeTab) {
            case PLAYER_STATS -> renderStatsTab(graphics);
            case SKILLS -> renderSkillsTab(graphics, mouseX, mouseY);
            case MAGIC -> renderMagicTab(graphics, mouseX, mouseY);
            case DUAL_WIELD -> renderDualWieldTab(graphics, mouseX, mouseY);
            case CLASS_SKILLS -> renderClassTab(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFrame(GuiGraphics graphics) {
        if (minecraft != null && minecraft.getResourceManager().getResource(BG_TEXTURE).isPresent()) {
            graphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, guiWidth, guiHeight, BASE_TEXTURE_WIDTH, BASE_TEXTURE_HEIGHT);
        } else {
            graphics.fill(leftPos, topPos, leftPos + guiWidth, topPos + guiHeight, 0xF01A1B22);
            graphics.fill(leftPos + 2, topPos + 2, leftPos + guiWidth - 2, topPos + guiHeight - 2, 0xE0282B35);
        }

        int pulse = 80 + (int) (Math.sin((player.tickCount + (minecraft == null ? 0.0F : minecraft.getFrameTime())) * 0.07F) * 30);
        int glow = (Mth.clamp(pulse, 30, 140) << 24) | 0x00468DFF;
        graphics.fill(leftPos + 2, topPos + 2, leftPos + guiWidth - 2, topPos + 4, glow);

        graphics.drawString(font, Component.literal("ExtremeCraft Progression"), leftPos + 10, topPos + 34, 0xF3D6A0, false);
        graphics.fill(leftPos + 8, topPos + 54, leftPos + guiWidth - 8, topPos + guiHeight - 10, 0xAA0C0F16);
    }

    private void renderStatsTab(GuiGraphics graphics) {
        Optional<PlayerStatsCapability> statsOpt = getStats();
        int x = leftPos + 16;
        int y = topPos + 62;

        if (statsOpt.isEmpty()) {
            graphics.drawString(font, Component.literal("Syncing stats..."), x, y, 0xD5D9E2, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Level: " + stats.level()), x, y, 0xEAEFF7, false);
        graphics.drawString(font, Component.literal("Stat Points: " + stats.statPoints()), x + 80, y, 0xF1C98F, false);
        graphics.drawString(font, Component.literal("Skill Points: " + stats.skillPoints()), x + 182, y, 0xCFB5FF, false);

        drawXpBar(graphics, x, y + 14, 220, stats.experience(), stats.experienceToNextLevel(), 0xFF4B8AE2, 0xFF6AAEF2);

        int rowStartY = y + 40;
        drawPrimaryRow(graphics, "Vitality", stats.vitality(), x, rowStartY);
        drawPrimaryRow(graphics, "Strength", stats.strength(), x, rowStartY + 24);
        drawPrimaryRow(graphics, "Agility", stats.agility(), x, rowStartY + 48);
        drawPrimaryRow(graphics, "Endurance", stats.endurance(), x, rowStartY + 72);
        drawPrimaryRow(graphics, "Intelligence", stats.intelligence(), x, rowStartY + 96);
        drawPrimaryRow(graphics, "Luck", stats.luck(), x, rowStartY + 120);

        int rightPanelX = leftPos + guiWidth - 108;
        int rightPanelY = y + 40;
        graphics.drawString(font, Component.literal("HP " + stats.maxHealth()), rightPanelX, rightPanelY, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Mana " + stats.maxMana()), rightPanelX, rightPanelY + 14, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Crit " + (int) (stats.critChance() * 100) + "%"), rightPanelX, rightPanelY + 28, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Move " + String.format("%.2f", stats.movementSpeed())), rightPanelX, rightPanelY + 42, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Magic " + stats.magicPower()), rightPanelX, rightPanelY + 56, 0xE6EAF2, false);
    }

    private void drawPrimaryRow(GuiGraphics graphics, String label, int value, int x, int y) {
        graphics.drawString(font, Component.literal(label), x, y + 3, 0xD9DEEA, false);

        int barLeft = x + 72;
        int barWidth = 104;
        graphics.fill(barLeft, y + 4, barLeft + barWidth, y + 11, 0xAA212735);
        int fill = Math.min(barWidth - 1, (int) ((Math.min(40, value) / 40.0F) * (barWidth - 1)));
        graphics.fill(barLeft + 1, y + 5, barLeft + 1 + fill, y + 10, 0xFF4B7FC9);

        graphics.drawString(font, Component.literal(String.valueOf(value)), x + 182, y + 3, 0xF0F4FA, false);
    }

    private void drawXpBar(GuiGraphics graphics, int x, int y, int width, int xp, int xpNeeded, int leftColor, int rightColor) {
        int clampedNeeded = Math.max(1, xpNeeded);
        int fill = Math.min(width, Math.round((xp / (float) clampedNeeded) * width));

        graphics.fill(x, y, x + width, y + 8, 0xAA1A1F2A);
        if (fill > 0) {
            graphics.fillGradient(x, y, x + fill, y + 8, leftColor, rightColor);
        }

        Component xpText = Component.literal(xp + " / " + clampedNeeded + " XP");
        graphics.drawCenteredString(font, xpText, x + width / 2, y - 10, 0xDCE6F6);
    }

    private void renderSkillsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 14;
        int y = topPos + 62;
        int w = guiWidth - 28;
        int h = guiHeight - 76;

        graphics.drawString(font, Component.literal("Skill Tree"), x, y - 10, 0xE8C78D, false);

        int treeButtonX = x;
        int treeButtonY = y + h - 12;
        for (String treeId : SkillTreeManager.treeIds()) {
            int color = treeId.equals(activeSkillTree) ? 0xFFD6B37A : 0xFF9EA7B6;
            graphics.drawString(font, Component.literal("[" + treeId + "]"), treeButtonX, treeButtonY, color, false);
            treeButtonX += 62;
        }

        skillTreePanel.render(graphics, font, x, y, w, h - 16, activeSkillTree, mouseX, mouseY);
    }

    private void renderMagicTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        Optional<PlayerStatsCapability> statsOpt = getStats();
        if (statsOpt.isEmpty()) {
            graphics.drawString(font, Component.literal("Syncing magic..."), x, y, 0xD5D9E2, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Mana: " + stats.mana() + " / " + stats.maxMana()), x, y, 0xCFAEFF, false);
        graphics.drawString(font, Component.literal("Magic Power: " + stats.magicPower()), x, y + 14, 0xCFAEFF, false);

        int time = player.tickCount;
        for (int i = 0; i < 8; i++) {
            int sx = x + (i % 4) * 28;
            int sy = y + 38 + (i / 4) * 28;

            if (minecraft != null && minecraft.getResourceManager().getResource(MAGIC_SLOT).isPresent()) {
                int frame = (time / 6) % 4;
                graphics.blit(MAGIC_SLOT, sx, sy, frame * 22, 0, 22, 22, 88, 22);
            } else {
                int pulse = (80 + Math.abs(10 - ((time + i * 3) % 20)) * 6) << 24;
                graphics.fill(sx, sy, sx + 22, sy + 22, pulse | 0x003E5C96);
                graphics.drawCenteredString(font, Component.literal("R"), sx + 11, sy + 7, 0xFFE8D6FF);
            }

            if (mouseX >= sx && mouseX <= sx + 22 && mouseY >= sy && mouseY <= sy + 22) {
                graphics.renderTooltip(font, Component.literal("Rune Slot " + (i + 1)), mouseX, mouseY);
            }
        }
    }

    private void renderDualWieldTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        graphics.drawString(font, Component.literal("Main Hand"), x, y, 0xD8DEE9, false);
        graphics.drawString(font, Component.literal("Offhand"), x + 120, y, 0xD8DEE9, false);

        drawItemSlot(graphics, x, y + 14, main);
        drawItemSlot(graphics, x + 120, y + 14, off);

        if (mouseX >= x && mouseX <= x + 18 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, main, mouseX, mouseY);
        }
        if (mouseX >= x + 120 && mouseX <= x + 138 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, off, mouseX, mouseY);
        }

        graphics.drawString(font, Component.literal("Right-click entity: offhand attack"), x, y + 46, 0xB5C3D8, false);
        graphics.drawString(font, Component.literal("Left-click: main hand (vanilla)"), x, y + 60, 0xB5C3D8, false);
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        graphics.fill(x, y, x + 18, y + 18, 0xAA1F2432);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x660A0C11);
        graphics.renderItem(stack, x + 1, y + 1);
    }

    private void renderClassTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 62;

        graphics.drawString(font, Component.literal("Class System"), x, y, 0xE3CDA0, false);
        graphics.drawString(font, Component.literal("Current class perks apply from progression data."), x, y + 16, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("UI for class switching/perk trees is pending."), x, y + 30, 0xC8CFDB, false);
    }

    private Optional<PlayerStatsCapability> getStats() {
        return PlayerStatsApi.get(player);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == ExtremePlayerTabs.Tab.SKILLS && button == InputConstants.MOUSE_BUTTON_LEFT) {
            int panelX = leftPos + 14;
            int panelY = topPos + 62;
            int panelW = guiWidth - 28;
            int panelH = guiHeight - 76;
            if (skillTreePanel.mouseClicked(panelX, panelY, panelW, panelH - 16, activeSkillTree, mouseX, mouseY)) {
                return true;
            }

            int treeButtonX = panelX;
            int treeButtonY = panelY + panelH - 12;
            for (String treeId : SkillTreeManager.treeIds()) {
                if (mouseX >= treeButtonX && mouseX <= treeButtonX + 56 && mouseY >= treeButtonY && mouseY <= treeButtonY + 10) {
                    activeSkillTree = treeId;
                    return true;
                }
                treeButtonX += 62;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(returnScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
