package com.extremecraft.client.gui.player;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.menu.PlayerStatsMenu;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.UpgradeSkillPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.skills.SkillsApi;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtremePlayerScreen extends AbstractContainerScreen<PlayerStatsMenu> {
    private static final int GUI_WIDTH = 286;
    private static final int GUI_HEIGHT = 190;
    private static final int TAB_WIDTH = 64;
    private static final int TAB_HEIGHT = 18;

    private static final Component TITLE = Component.literal("Extreme Player Menu");
    private static final Component NO_DATA = Component.literal("Syncing stats...");

    private static final ResourceLocation MENU_TEXTURE = ResourceLocation.tryParse(ECConstants.MODID + ":textures/gui/player_menu.png");
    private static final ResourceLocation MAGIC_SLOT_TEXTURE = ResourceLocation.tryParse(ECConstants.MODID + ":textures/gui/magic_slot.png");

    private final List<Button> statButtons = new ArrayList<>();
    private ExtremePlayerTabs.Tab activeTab = ExtremePlayerTabs.Tab.PLAYER_STATS;
    private float transition = 1.0F;

    public ExtremePlayerScreen(PlayerStatsMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, ExtremePlayerTabs.Tab.PLAYER_STATS);
    }

    public ExtremePlayerScreen(Player player, ExtremePlayerTabs.Tab tab) {
        this(new PlayerStatsMenu(0, player.getInventory()), player.getInventory(), TITLE, tab);
    }

    public ExtremePlayerScreen(PlayerStatsMenu menu, Inventory inventory, Component title, ExtremePlayerTabs.Tab tab) {
        super(menu, inventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
        this.activeTab = tab;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 12;
        this.titleLabelY = 8;

        createTabButtons();
        createSkillButtons();

        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());
        updateSkillButtonVisibility();
    }

    private void createTabButtons() {
        int tabX = leftPos + 10;
        int tabY = topPos + 28;

        for (ExtremePlayerTabs.Tab tab : ExtremePlayerTabs.Tab.values()) {
            Button button = Button.builder(tab.label(), btn -> switchTab(tab))
                    .bounds(tabX, tabY, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            addRenderableWidget(button);
            tabX += TAB_WIDTH + 2;
        }
    }

    private void createSkillButtons() {
        statButtons.clear();

        statButtons.add(addRenderableWidget(createUpgradeButton("strength", leftPos + 210, topPos + 78)));
        statButtons.add(addRenderableWidget(createUpgradeButton("agility", leftPos + 210, topPos + 98)));
        statButtons.add(addRenderableWidget(createUpgradeButton("magic", leftPos + 210, topPos + 118)));
        statButtons.add(addRenderableWidget(createUpgradeButton("defense", leftPos + 210, topPos + 138)));
    }

    private Button createUpgradeButton(String statId, int x, int y) {
        return Button.builder(Component.literal("+"), btn -> ModNetwork.CHANNEL.sendToServer(new UpgradeSkillPacket(statId)))
                .bounds(x, y, 20, 16)
                .build();
    }

    private void switchTab(ExtremePlayerTabs.Tab tab) {
        if (tab == activeTab) {
            return;
        }

        activeTab = tab;
        transition = 0.0F;
        updateSkillButtonVisibility();
    }

    private void updateSkillButtonVisibility() {
        boolean visible = activeTab == ExtremePlayerTabs.Tab.SKILL_POINTS;
        for (Button button : statButtons) {
            button.visible = visible;
            button.active = visible;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        transition = Math.min(1.0F, transition + 0.12F);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPanelBackground(graphics);
        renderActiveTabContent(graphics, mouseX, mouseY);
    }

    private void renderPanelBackground(GuiGraphics graphics) {
        if (MENU_TEXTURE != null && minecraft != null && minecraft.getResourceManager().getResource(MENU_TEXTURE).isPresent()) {
            graphics.blit(MENU_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        } else {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0141720);
            graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xE0222630);
        }

        int pulse = 70 + (int) (Math.sin((minecraft != null ? minecraft.level != null ? minecraft.level.getGameTime() : 0 : 0) * 0.12D) * 25.0D);
        int glow = (Mth.clamp(pulse, 20, 120) << 24) | 0x005EA2FF;
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 3, glow);
        graphics.fill(leftPos + 1, topPos + imageHeight - 3, leftPos + imageWidth - 1, topPos + imageHeight - 1, glow);
        graphics.fill(leftPos + 8, topPos + 50, leftPos + imageWidth - 8, topPos + imageHeight - 12, 0xAA0F1118);
    }

    private void renderActiveTabContent(GuiGraphics graphics, int mouseX, int mouseY) {
        switch (activeTab) {
            case PLAYER_STATS -> renderPlayerStatsTab(graphics);
            case MAGIC -> renderMagicTab(graphics, mouseX, mouseY);
            case DUAL_WIELD -> renderDualWieldTab(graphics, mouseX, mouseY);
            case CLASS_SKILLS -> renderClassSkillsTab(graphics);
            case SKILL_POINTS -> renderSkillPointsTab(graphics);
        }
    }

    private void renderPlayerStatsTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 58;

        Optional<PlayerStatsCapability> statsOpt = getStats();
        if (statsOpt.isEmpty()) {
            graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Class: " + stats.playerClass()), x, y, 0xE3E9F5, false);
        graphics.drawString(font, Component.literal("Skill Points: " + stats.skillPoints()), x, y + 14, 0xF0CD8E, false);
        drawStatRow(graphics, "Strength", stats.strength(), x, y + 32, 160, 0xFFBC5656);
        drawStatRow(graphics, "Agility", stats.agility(), x, y + 50, 160, 0xFF62BE7D);
        drawStatRow(graphics, "Magic", stats.magic(), x, y + 68, 160, 0xFF7E6DDA);
        drawStatRow(graphics, "Defense", stats.defense(), x, y + 86, 160, 0xFF74A4D6);
    }

    private void renderMagicTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 58;

        Optional<PlayerStatsCapability> statsOpt = getStats();
        if (statsOpt.isEmpty()) {
            graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        int maxMana = 80 + stats.magic() * 20;
        int mana = Math.min(maxMana, stats.magic() * 18);

        graphics.drawString(font, Component.literal("Mana Pool"), x, y, 0xC5A0FF, false);
        drawBar(graphics, x, y + 14, 190, 10, mana, maxMana, 0xFF6D4BC7);
        graphics.drawString(font, Component.literal(mana + " / " + maxMana), x + 8, y + 16, 0xEEE7FF, false);
        graphics.drawString(font, Component.literal("Magic Unlock: " + (stats.magicUnlocked() ? "Unlocked" : "Locked")), x, y + 30, 0xD0D6E2, false);

        int runes = stats.magicUnlocked() ? 4 : 1;
        int time = minecraft != null && minecraft.level != null ? (int) minecraft.level.getGameTime() : 0;
        for (int i = 0; i < 5; i++) {
            int slotX = x + (i * 28);
            int slotY = y + 46;
            boolean active = i < runes;
            renderRuneSlot(graphics, slotX, slotY, time, active);

            if (mouseX >= slotX && mouseX < slotX + 22 && mouseY >= slotY && mouseY < slotY + 22) {
                graphics.renderTooltip(font, Component.literal(active ? "Rune Slot " + (i + 1) : "Locked Rune"), mouseX, mouseY);
            }
        }
    }

    private void renderRuneSlot(GuiGraphics graphics, int x, int y, int time, boolean active) {
        if (MAGIC_SLOT_TEXTURE != null && minecraft != null && minecraft.getResourceManager().getResource(MAGIC_SLOT_TEXTURE).isPresent()) {
            int frame = (time / 6) % 4;
            graphics.blit(MAGIC_SLOT_TEXTURE, x, y, frame * 22, 0, 22, 22, 88, 22);
        } else {
            int pulse = 70 + Math.abs(10 - (time % 20)) * 4;
            int color = ((active ? pulse : 40) << 24) | (active ? 0x008F6FFF : 0x003B3E55);
            graphics.fill(x, y, x + 22, y + 22, color);
            graphics.drawCenteredString(font, Component.literal("R"), x + 11, y + 7, active ? 0xFFE5D6FF : 0xFF7B7C8A);
        }
    }

    private void renderDualWieldTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 58;

        if (minecraft == null || minecraft.player == null) {
            graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false);
            return;
        }

        Player player = minecraft.player;
        Optional<PlayerStatsCapability> statsOpt = getStats();
        boolean unlocked = statsOpt.map(PlayerStatsCapability::dualWieldUnlocked).orElse(false);

        graphics.drawString(font, Component.literal("Main Hand"), x, y, 0xE6E2D5, false);
        graphics.drawString(font, Component.literal("Offhand"), x + 92, y, 0xE6E2D5, false);

        int mainX = x;
        int offX = x + 92;
        int slotY = y + 14;
        drawItemSlot(graphics, mainX, slotY, player.getMainHandItem());
        drawItemSlot(graphics, offX, slotY, player.getOffhandItem());

        graphics.drawString(font, Component.literal("Dual Wield Unlock: " + (unlocked ? "Unlocked" : "Locked")), x, y + 42, 0xD0D6E2, false);

        if (isHovering(mainX - leftPos, slotY - topPos, 18, 18, mouseX, mouseY)) {
            graphics.renderTooltip(font, player.getMainHandItem(), mouseX, mouseY);
        }
        if (isHovering(offX - leftPos, slotY - topPos, 18, 18, mouseX, mouseY)) {
            graphics.renderTooltip(font, player.getOffhandItem(), mouseX, mouseY);
        }
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        graphics.fill(x, y, x + 18, y + 18, 0xAA1A1D24);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x66090B10);
        graphics.renderItem(stack, x + 1, y + 1);
    }

    private void renderClassSkillsTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 58;

        if (minecraft == null || minecraft.player == null) {
            graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false);
            return;
        }

        ProgressApi.get(minecraft.player).ifPresentOrElse(progress -> {
            graphics.drawString(font, Component.literal("Class: " + progress.currentClass()), x, y, 0xE3E9F5, false);
            graphics.drawString(font, Component.literal("Level: " + progress.level()), x, y + 14, 0xD0D6E2, false);
            graphics.drawString(font, Component.literal("Class Points: " + progress.classSkillPoints()), x, y + 28, 0xE1BF84, false);
        }, () -> graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false));

        SkillsApi.get(minecraft.player).ifPresent(skills -> {
            graphics.drawString(font, Component.literal("Mining: " + skills.getSkillLevel("mining")), x + 140, y, 0xC3CAD6, false);
            graphics.drawString(font, Component.literal("Combat: " + skills.getSkillLevel("combat")), x + 140, y + 14, 0xC3CAD6, false);
            graphics.drawString(font, Component.literal("Engineering: " + skills.getSkillLevel("engineering")), x + 140, y + 28, 0xC3CAD6, false);
            graphics.drawString(font, Component.literal("Arcane: " + skills.getSkillLevel("arcane")), x + 140, y + 42, 0xC3CAD6, false);
        });
    }

    private void renderSkillPointsTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 58;

        Optional<PlayerStatsCapability> statsOpt = getStats();
        if (statsOpt.isEmpty()) {
            graphics.drawString(font, NO_DATA, x, y, 0xD8DEE9, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Available Skill Points: " + stats.skillPoints()), x, y, 0xF0CD8E, false);

        drawSkillPointLine(graphics, "Strength", stats.strength(), x, y + 20, 180);
        drawSkillPointLine(graphics, "Agility", stats.agility(), x, y + 40, 180);
        drawSkillPointLine(graphics, "Magic Power", stats.magic(), x, y + 60, 180);
        drawSkillPointLine(graphics, "Defense", stats.defense(), x, y + 80, 180);
    }

    private void drawSkillPointLine(GuiGraphics graphics, String label, int value, int x, int y, int width) {
        graphics.drawString(font, Component.literal(label), x, y, 0xDEE3ED, false);
        drawBar(graphics, x + 70, y + 3, width - 90, 8, value, 30, 0xFF4A8CBF);
        graphics.drawString(font, Component.literal(String.valueOf(value)), x + width - 32, y, 0xE8EDF7, false);
    }

    private void drawStatRow(GuiGraphics graphics, String label, int value, int x, int y, int width, int color) {
        graphics.drawString(font, Component.literal(label + ": " + value), x, y, 0xDEE3ED, false);
        drawBar(graphics, x + 80, y + 4, width - 84, 8, value, 30, color);
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int width, int height, int value, int max, int color) {
        int safeMax = Math.max(1, max);
        int fill = Math.min(width, (int) ((value / (double) safeMax) * width));
        int alphaColor = 0xAA0E121A;
        graphics.fill(x, y, x + width, y + height, alphaColor);

        int eased = (int) (fill * transition);
        graphics.fill(x + 1, y + 1, x + eased, y + height - 1, color);
    }

    private Optional<PlayerStatsCapability> getStats() {
        if (minecraft == null || minecraft.player == null) {
            return Optional.empty();
        }
        return PlayerStatsApi.get(minecraft.player);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, TITLE, titleLabelX, titleLabelY, 0xF0D8A8, false);
        graphics.drawString(font, playerInventoryTitle, 12, inventoryLabelY, 0xBFC7D4, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
