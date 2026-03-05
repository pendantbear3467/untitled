package com.extremecraft.client.gui.player;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ProgressionScreen extends ExtremePlayerScreen {
    public ProgressionScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.PROGRESSION);
    }

    @Override
    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiLeft() + 14;
        int y = getGuiTop() + 14;
        guiGraphics.drawString(font, Component.literal("Progression / Class"), x, y, 0xC8E6C9, false);
        drawProgressSnapshot(guiGraphics, x, y + 14);
        guiGraphics.drawString(font, Component.literal("Skill Tree: [framework ready]"), x, y + 50, 0xE0E0E0, false);
    }
}
