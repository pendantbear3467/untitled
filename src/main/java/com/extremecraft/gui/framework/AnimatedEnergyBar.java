package com.extremecraft.gui.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;

public class AnimatedEnergyBar {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final IntSupplier scaled;
    private final IntSupplier stored;
    private final IntSupplier capacity;

    public AnimatedEnergyBar(int x, int y, int width, int height, IntSupplier scaled, IntSupplier stored, IntSupplier capacity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scaled = scaled;
        this.stored = stored;
        this.capacity = capacity;
    }

    public void render(GuiGraphics graphics, int left, int top, int tickCount) {
        graphics.fill(left + x, top + y, left + x + width, top + y + height, 0x660A131F);
        int filled = Math.max(0, Math.min(height, scaled.getAsInt()));
        if (filled <= 0) {
            return;
        }

        int wave = tickCount % 20;
        int coreColor = 0xFF00D2FF + (Math.max(0, 8 - Math.abs(10 - wave)) << 16);
        graphics.fill(left + x + 1, top + y + height - filled, left + x + width - 1, top + y + height - 1, coreColor);
    }

    public boolean isMouseOver(int left, int top, int mouseX, int mouseY) {
        return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
    }

    public List<Component> tooltip() {
        return List.of(
                Component.translatable("gui.extremecraft.energy"),
                Component.literal(stored.getAsInt() + " FE / " + Math.max(1, capacity.getAsInt()) + " FE")
        );
    }
}
