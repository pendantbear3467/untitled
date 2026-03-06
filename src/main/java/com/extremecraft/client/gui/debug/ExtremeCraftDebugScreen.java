package com.extremecraft.client.gui.debug;

import com.extremecraft.platform.data.sync.client.PlatformDataClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ExtremeCraftDebugScreen extends Screen {
    private final Screen returnTo;

    public ExtremeCraftDebugScreen(Screen returnTo) {
        super(Component.literal("ExtremeCraft Debug"));
        this.returnTo = returnTo;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int x = width / 2 - 140;
        int y = 40;
        graphics.drawString(font, Component.literal("ExtremeCraft Data Debug"), x, y, 0xFFFFFF, false);
        y += 20;
        graphics.drawString(font, Component.literal("Materials: " + PlatformDataClientState.materials().size()), x, y, 0xD0D0D0, false);
        y += 12;
        graphics.drawString(font, Component.literal("Machines: " + PlatformDataClientState.machines().size()), x, y, 0xD0D0D0, false);
        y += 12;
        graphics.drawString(font, Component.literal("Skill Trees: " + PlatformDataClientState.skillTrees().size()), x, y, 0xD0D0D0, false);
        y += 18;
        graphics.drawString(font, Component.literal("Press ESC to return"), x, y, 0xA0A0A0, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(returnTo);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
