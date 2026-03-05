package com.extremecraft.client.gui.player;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class MagicScreen extends ExtremePlayerScreen {
    public MagicScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.MAGIC);
    }

    @Override
    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiLeft() + 14;
        int y = getGuiTop() + 14;
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
