package com.extremecraft.client.gui.player;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ClassSystemScreen extends ExtremePlayerScreen {
    public ClassSystemScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.CLASS_SYSTEM);
    }

    @Override
    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiLeft() + 14;
        int y = getGuiTop() + 16;

        guiGraphics.drawString(font, Component.literal("Class System"), x, y, 0xFFE082, false);
        ProgressApi.get(minecraft.player).ifPresent(data -> {
            guiGraphics.drawString(font, Component.literal("Current: " + data.currentClass()), x, y + 14, 0xFFFFFF, false);
            guiGraphics.drawString(font, Component.literal("Available Classes:"), x, y + 28, 0xE0E0E0, false);
            guiGraphics.drawString(font, Component.literal("Engineer | Warrior | Mage | Explorer"), x, y + 40, 0xE0E0E0, false);
            guiGraphics.drawString(font, Component.literal("Unlocked: " + String.join(", ", data.unlockedClasses())), x, y + 54, 0xA5D6A7, false);
        });
    }
}
