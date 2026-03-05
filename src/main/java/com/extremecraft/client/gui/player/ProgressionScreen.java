package com.extremecraft.client.gui.player;

import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ProgressionScreen extends StandalonePlayerScreen {
    public ProgressionScreen(Player player) {
        super(player, Component.literal("Progression"));
    }

    @Override
    protected void drawStandaloneContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = panelLeft + 24;
        int y = panelTop + 52;

        guiGraphics.drawString(font, Component.literal("Class Progression"), x, y, 0xF4C66B, false);

        if (minecraft == null || minecraft.player == null) {
            guiGraphics.drawString(font, Component.literal("Syncing progression..."), x, y + 16, 0xA5AFBF, false);
            return;
        }

        ProgressApi.get(minecraft.player).ifPresentOrElse(data -> {
            guiGraphics.drawString(font, Component.literal("Class: " + data.currentClass()), x, y + 16, 0xD5D9E2, false);
            guiGraphics.drawString(font, Component.literal("Level: " + data.level()), x, y + 30, 0xD5D9E2, false);
            guiGraphics.drawString(font, Component.literal("Player SP: " + data.playerSkillPoints()), x, y + 44, 0xC7B588, false);
            guiGraphics.drawString(font, Component.literal("Class SP: " + data.classSkillPoints()), x, y + 58, 0xC7B588, false);
            renderProgressBar(guiGraphics, x, y + 78, 220, data);
        }, () -> guiGraphics.drawString(font, Component.literal("Syncing progression..."), x, y + 16, 0xA5AFBF, false));
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, PlayerProgressData data) {
        int barHeight = 12;
        int maxXp = Math.max(1, PlayerProgressData.xpToNextLevel(data.level()));
        int fill = Math.min(width, (int) ((data.xp() / (double) maxXp) * width));

        guiGraphics.drawString(font, Component.literal("XP"), x, y - 12, 0xF4C66B, false);
        guiGraphics.fill(x, y, x + width, y + barHeight, 0xAA22252C);
        guiGraphics.fillGradient(x, y, x + fill, y + barHeight, 0xFFE2B657, 0xFFB27A2A);
        guiGraphics.fill(x, y, x + width, y + 1, 0x66FFFFFF);
        guiGraphics.drawString(font, Component.literal(data.xp() + " / " + maxXp), x + 6, y + 2, 0xF7F2E3, false);
    }
}
