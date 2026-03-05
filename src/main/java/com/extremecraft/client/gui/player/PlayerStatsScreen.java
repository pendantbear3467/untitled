package com.extremecraft.client.gui.player;

import com.extremecraft.skills.SkillsApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class PlayerStatsScreen extends ExtremePlayerScreen {
    public PlayerStatsScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.PLAYER_STATS);
    }

    @Override
    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiLeft() + 14;
        int y = getGuiTop() + 16;

        guiGraphics.drawString(font, Component.literal("Player Stats"), x, y, 0xC8E6C9, false);
        drawProgressSnapshot(guiGraphics, x, y + 14);

        SkillsApi.get(minecraft.player).ifPresent(skills -> {
            guiGraphics.drawString(font, Component.literal("Mining: " + skills.getSkillLevel("mining")), x, y + 54, 0xE0E0E0, false);
            guiGraphics.drawString(font, Component.literal("Combat: " + skills.getSkillLevel("combat")), x + 76, y + 54, 0xE0E0E0, false);
            guiGraphics.drawString(font, Component.literal("Engineering: " + skills.getSkillLevel("engineering")), x, y + 66, 0xE0E0E0, false);
            guiGraphics.drawString(font, Component.literal("Arcane: " + skills.getSkillLevel("arcane")), x + 110, y + 66, 0xE0E0E0, false);
        });
    }
}
