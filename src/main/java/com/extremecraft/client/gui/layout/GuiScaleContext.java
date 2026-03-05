package com.extremecraft.client.gui.layout;

import net.minecraft.util.Mth;

/**
 * Centralized GUI sizing/scaling context for responsive screen layouts.
 */
public record GuiScaleContext(
        int screenWidth,
        int screenHeight,
        float scale,
        int contentWidth,
        int contentHeight,
        int left,
        int top
) {
    public static GuiScaleContext from(int screenWidth, int screenHeight, int baseWidth, int baseHeight,
                                       double scaleValue, double minScale, double maxScale,
                                       int minWidth, int minHeight) {
        float scale = (float) Mth.clamp(scaleValue, minScale, maxScale);
        int contentWidth = Math.max(minWidth, Math.round(baseWidth * scale));
        int contentHeight = Math.max(minHeight, Math.round(baseHeight * scale));
        int left = (screenWidth - contentWidth) / 2;
        int top = (screenHeight - contentHeight) / 2;

        return new GuiScaleContext(screenWidth, screenHeight, scale, contentWidth, contentHeight, left, top);
    }
}
