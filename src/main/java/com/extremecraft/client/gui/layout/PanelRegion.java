package com.extremecraft.client.gui.layout;

/**
 * Named rectangular region used for screen composition and hit-testing.
 */
public record PanelRegion(String id, int x, int y, int width, int height) {
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= (x + width)
                && mouseY >= y && mouseY <= (y + height);
    }
}
