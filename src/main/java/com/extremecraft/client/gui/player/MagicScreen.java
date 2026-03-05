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
    protected void drawStandaloneContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = panelLeft + 24;
        int y = panelTop + 52;

        guiGraphics.drawString(font, Component.literal("Arcane Conduit"), x, y, 0xC399FF, false);

        int currentMana = 0;
        int maxMana = 100;
        boolean unlocked = false;
        if (minecraft != null && minecraft.player != null) {
            unlocked = ProgressApi.get(minecraft.player).map(data -> data.level() >= 5).orElse(false);
            currentMana = ProgressApi.get(minecraft.player).map(data -> Math.min(maxMana, data.level() * 4)).orElse(0);
        }

        renderManaBar(guiGraphics, x, y + 16, 220, currentMana, maxMana);
        guiGraphics.drawString(font, Component.literal("Magic Unlock: " + (unlocked ? "Unlocked" : "Locked")), x, y + 34, 0xD5D9E2, false);
        renderSpellSlots(guiGraphics, x, y + 52, 5, unlocked ? 2 : 0);
    }

    private void renderManaBar(GuiGraphics guiGraphics, int x, int y, int width, int currentMana, int maxMana) {
        int barHeight = 10;
        int fill = Math.min(width, (int) ((currentMana / (double) Math.max(1, maxMana)) * width));

        guiGraphics.drawString(font, Component.literal("Mana"), x, y - 10, 0xB78CFF, false);
        guiGraphics.fill(x, y, x + width, y + barHeight, 0xAA1E1E2B);
        guiGraphics.fillGradient(x, y, x + fill, y + barHeight, 0xFF8A5CFF, 0xFF4B2AB4);
        guiGraphics.fill(x, y, x + width, y + 1, 0x66FFFFFF);
        guiGraphics.drawString(font, Component.literal(currentMana + " / " + maxMana), x + 6, y + 1, 0xEEE7FF, false);
    }

    private void renderSpellSlots(GuiGraphics guiGraphics, int x, int y, int count, int filled) {
        guiGraphics.drawString(font, Component.literal("Spell Slots"), x, y - 12, 0xC399FF, false);

        for (int i = 0; i < count; i++) {
            int slotX = x + i * 26;
            int color = i < filled ? 0xAAA06BFF : 0xAA3C3A46;
            int rune = i < filled ? 0xFFD9C2FF : 0xFF888A93;
            guiGraphics.fill(slotX, y, slotX + 20, y + 20, color);
            guiGraphics.fill(slotX + 2, y + 2, slotX + 18, y + 18, 0x880B0D13);
            guiGraphics.drawCenteredString(font, Component.literal("R"), slotX + 10, y + 6, rune);
        }
    }
}
