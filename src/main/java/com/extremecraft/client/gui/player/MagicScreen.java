package com.extremecraft.client.gui.player;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MagicScreen extends StandalonePlayerScreen {
    public MagicScreen(Player player) {
        super(player, Component.literal("Magic"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int x = 116;
        int y = 40;
        guiGraphics.drawString(font, Component.literal("Magic System"), x, y, 0xA0C4FF, false);
        guiGraphics.drawString(font, Component.literal("Mana: 0 / 100"), x, y + 14, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.literal("Spell Slots: [ ] [ ] [ ]"), x, y + 26, 0xE0E0E0, false);

        if (minecraft != null && minecraft.player != null) {
            ProgressApi.get(minecraft.player).ifPresent(data -> {
                boolean magicUnlocked = data.level() >= 5;
                guiGraphics.drawString(font, Component.literal("Magic Unlock: " + (magicUnlocked ? "Unlocked" : "Locked")), x, y + 38, 0xE0E0E0, false);
            });
        }

        guiGraphics.drawString(font, Component.literal("Equipped Spell: None"), x, y + 50, 0xE0E0E0, false);
    }
}
