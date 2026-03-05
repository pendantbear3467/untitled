package com.extremecraft.client.gui.player;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ProgressionScreen extends StandalonePlayerScreen {
    public ProgressionScreen(Player player) {
        super(player, Component.literal("Progression"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int x = 116;
        int y = 40;
        guiGraphics.drawString(font, Component.literal("Progression / Class"), x, y, 0xC8E6C9, false);
        if (minecraft != null && minecraft.player != null) {
            ProgressApi.get(minecraft.player).ifPresentOrElse(data -> {
                        guiGraphics.drawString(font, Component.literal("Class: " + data.currentClass()), x, y + 14, 0xFFFFFF, false);
                        guiGraphics.drawString(font, Component.literal("Level: " + data.level()), x, y + 28, 0xFFFFFF, false);
                        guiGraphics.drawString(font, Component.literal("XP: " + data.xp() + " / " + com.extremecraft.progression.PlayerProgressData.xpToNextLevel(data.level())), x, y + 42, 0xE0E0E0, false);
                        guiGraphics.drawString(font, Component.literal("Player Skill Points: " + data.playerSkillPoints()), x, y + 56, 0xE0E0E0, false);
                        guiGraphics.drawString(font, Component.literal("Class Skill Points: " + data.classSkillPoints()), x, y + 70, 0xE0E0E0, false);
                    },
                    () -> guiGraphics.drawString(font, Component.literal("Syncing progression..."), x, y + 14, 0xE0E0E0, false)
            );
        }
    }
}
