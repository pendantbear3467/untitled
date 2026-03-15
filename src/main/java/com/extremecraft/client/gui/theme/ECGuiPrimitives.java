package com.extremecraft.client.gui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ECGuiPrimitives {
    private ECGuiPrimitives() {
    }

    public static void drawScreenBackdrop(GuiGraphics graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, 0xD004060A, 0xDD0A1220);
    }

    public static void drawPanelChrome(GuiGraphics graphics, int left, int top, int width, int height, int tick) {
        graphics.fill(left - 3, top - 3, left + width + 3, top + height + 3, 0x88000000);
        graphics.fill(left, top, left + width, top + height, ECGuiTheme.BG_PANEL);

        graphics.fill(left, top, left + width, top + 1, ECGuiTheme.BORDER_STRONG);
        graphics.fill(left, top + height - 1, left + width, top + height, ECGuiTheme.BORDER_STRONG);
        graphics.fill(left, top, left + 1, top + height, ECGuiTheme.BORDER_STRONG);
        graphics.fill(left + width - 1, top, left + width, top + height, ECGuiTheme.BORDER_STRONG);

        int pulse = 60 + Math.abs(28 - (tick % 56));
        int glow = (Mth.clamp(pulse, 35, 105) << 24) | 0x004EDCFF;
        graphics.fill(left + 2, top + 2, left + width - 2, top + 4, glow);

        // Subtle circuit-style separators.
        graphics.fill(left + 10, top + 8, left + width - 10, top + 9, 0x443B4F67);
        graphics.fill(left + 10, top + height - 10, left + width - 10, top + height - 9, 0x443B4F67);
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, Component label, int x, int y, int width, int accentColor) {
        graphics.drawString(font, label, x, y, ECGuiTheme.TEXT_PRIMARY, false);
        int lineY = y + 10;
        graphics.fill(x, lineY, x + width, lineY + 1, 0x55384D66);
        graphics.fill(x, lineY, x + Math.min(width, 44), lineY + 1, (0xAA << 24) | (accentColor & 0x00FFFFFF));
    }

    public static void drawTab(GuiGraphics graphics, Font font, int x, int y, int width, int height, Component label, boolean active) {
        int frame = active ? 0xFF57708E : 0xAA344559;
        int inner = active ? 0xEE1A2B3D : 0xC216202C;
        int text = active ? ECGuiTheme.TEXT_PRIMARY : ECGuiTheme.TEXT_SECONDARY;

        graphics.fill(x, y, x + width, y + height, frame);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, inner);
        if (active) {
            graphics.fill(x + 2, y + 2, x + width - 2, y + 4, 0xAA4EDCFF);
        }
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2, text);
    }

    public static void drawFramedSlot(GuiGraphics graphics, int x, int y, boolean emphasized) {
        int frame = emphasized ? 0xFF5A7899 : 0xAA425368;
        graphics.fill(x - 1, y - 1, x + 17, y + 17, frame);
        graphics.fill(x, y, x + 16, y + 16, 0xCC101822);
        graphics.fill(x + 1, y + 1, x + 15, y + 15, 0x66212B39);
    }

    public static void drawStatusChip(GuiGraphics graphics, Font font, int x, int y, Component text, int color) {
        int width = font.width(text) + 10;
        int frame = (0xB0 << 24) | (color & 0x00FFFFFF);
        int inner = 0xCC111821;
        graphics.fill(x, y, x + width, y + 12, frame);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 11, inner);
        graphics.drawString(font, text, x + 5, y + 2, color, false);
    }

    public static void drawSegmentedBar(GuiGraphics graphics, int x, int y, int width, int height, float ratio, int color) {
        int clamped = Mth.clamp(Math.round(ratio * 1000.0F), 0, 1000);
        int fill = Math.round((clamped / 1000.0F) * width);

        graphics.fill(x, y, x + width, y + height, 0xAA0E151E);
        if (fill > 0) {
            graphics.fill(x + 1, y + 1, x + fill, y + height - 1, (0xE0 << 24) | (color & 0x00FFFFFF));
        }

        int segment = 8;
        for (int sx = x + segment; sx < x + width; sx += segment) {
            graphics.fill(sx, y + 1, sx + 1, y + height - 1, 0x4418222E);
        }
    }

    public static int stateColor(boolean positive, boolean warning, boolean blocked) {
        if (positive) {
            return ECGuiTheme.STATE_READY;
        }
        if (warning) {
            return ECGuiTheme.STATE_WARN;
        }
        if (blocked) {
            return ECGuiTheme.STATE_ERROR;
        }
        return ECGuiTheme.STATE_BLOCKED;
    }
}
