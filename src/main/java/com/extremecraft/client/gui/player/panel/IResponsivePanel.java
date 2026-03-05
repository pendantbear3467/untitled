package com.extremecraft.client.gui.player.panel;

import com.extremecraft.client.gui.layout.GuiScaleContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Contract for adaptive player menu tab panels.
 */
public interface IResponsivePanel {
    void render(GuiGraphics graphics, Font font, GuiScaleContext context, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(GuiScaleContext context, double mouseX, double mouseY, int button) {
        return false;
    }
}
