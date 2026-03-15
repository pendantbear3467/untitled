package com.extremecraft.gui.framework;

import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import com.extremecraft.client.gui.theme.ECGuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class GuiRenderUtils {
    private GuiRenderUtils() {
    }

    public static void drawMachineBackground(GuiGraphics graphics, int left, int top, int width, int height) {
        ResourceLocation texture = resolveBackgroundTexture();
        graphics.blit(texture, left, top, 0, 0, width, height, width, height);
        ECGuiPrimitives.drawPanelChrome(graphics, left, top, width, height, 0);
        graphics.fill(left + 3, top + 3, left + width - 3, top + height - 3, 0x880A1018);
    }

    public static void drawSlotHighlight(GuiGraphics graphics, int x, int y, int time) {
        int pulse = 45 + (time % 40);
        int alpha = Math.min(170, pulse + 30);
        int glow = (alpha << 24) | (ECGuiTheme.ACCENT_CYAN & 0x00FFFFFF);
        graphics.fill(x - 1, y - 1, x + 17, y + 17, glow);
        ECGuiPrimitives.drawFramedSlot(graphics, x, y, true);
    }

    public static void drawMachineActiveAura(GuiGraphics graphics, int left, int top, int width, int height, int time) {
        int cycle = (time / 2) % 80;
        int alpha = 55 + Math.abs(40 - cycle);
        int aura = (Math.min(150, alpha) << 24) | (ECGuiTheme.ACCENT_CYAN & 0x00FFFFFF);
        graphics.fill(left + 2, top + 2, left + width - 2, top + 4, aura);
        graphics.fill(left + 2, top + height - 4, left + width - 2, top + height - 2, aura);
    }

    public static void renderTooltip(GuiGraphics graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        graphics.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
    }

    private static ResourceLocation resolveBackgroundTexture() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getResourceManager().getResource(ECGuiTextures.MACHINE_BACKGROUND).isPresent()) {
            return ECGuiTextures.MACHINE_BACKGROUND;
        }
        return ECGuiTextures.FALLBACK_PANEL;
    }
}
