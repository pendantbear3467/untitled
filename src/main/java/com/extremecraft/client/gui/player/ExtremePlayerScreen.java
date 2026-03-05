package com.extremecraft.client.gui.player;

import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ExtremePlayerScreen extends InventoryScreen {
    protected final ExtremePlayerTabs.Tab activeTab;

    public ExtremePlayerScreen(Player player) {
        this(player, ExtremePlayerTabs.Tab.INVENTORY);
    }

    protected ExtremePlayerScreen(Player player, ExtremePlayerTabs.Tab activeTab) {
        super(player);
        this.activeTab = activeTab;
    }

    @Override
    protected void init() {
        super.init();
        ExtremePlayerTabs.addTabButtons(this, this::switchTab);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderLabels(guiGraphics, mouseX, mouseY);

        if (activeTab != ExtremePlayerTabs.Tab.INVENTORY) {
            renderTabContentBackground(guiGraphics);
            renderTabContent(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    protected void renderTabContentBackground(GuiGraphics guiGraphics) {
        int x0 = getGuiLeft() + 8;
        int y0 = getGuiTop() + 8;
        int x1 = x0 + 160;
        int y1 = y0 + 68;
        guiGraphics.fill(x0, y0, x1, y1, 0x99000000);
    }

    protected void switchTab(ExtremePlayerTabs.Tab tab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        mc.setScreen(createForTab(tab, mc.player));
    }

    protected Screen createForTab(ExtremePlayerTabs.Tab tab, Player player) {
        return switch (tab) {
            case INVENTORY -> new ExtremePlayerScreen(player);
            case DUAL_WIELD -> new DualWieldScreen(player);
            case MAGIC -> new MagicScreen(player);
            case PLAYER_STATS -> new PlayerStatsScreen(player);
            case CLASS_SYSTEM -> new ClassSystemScreen(player);
        };
    }

    protected void drawProgressSnapshot(GuiGraphics guiGraphics, int x, int y) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        ProgressApi.get(minecraft.player).ifPresentOrElse(data -> drawProgressData(guiGraphics, x, y, data),
                () -> guiGraphics.drawString(font, Component.literal("Syncing progression..."), x, y, 0xE0E0E0, false));
    }

    private void drawProgressData(GuiGraphics guiGraphics, int x, int y, PlayerProgressData data) {
        guiGraphics.drawString(font, Component.literal("Class: " + data.currentClass()), x, y, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal("Level: " + data.level()), x, y + 12, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal("Skill Points: " + data.playerSkillPoints()), x, y + 24, 0xFFFFFF, false);
    }

    public int getGuiLeft() {
        return leftPos;
    }

    public int getGuiTop() {
        return topPos;
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> T addTabWidget(T widget) {
        return addRenderableWidget(widget);
    }
}
