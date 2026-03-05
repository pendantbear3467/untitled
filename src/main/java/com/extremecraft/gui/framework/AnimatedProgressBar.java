package com.extremecraft.gui.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;

public class AnimatedProgressBar {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final IntSupplier scaled;
    private final IntSupplier current;
    private final IntSupplier max;

    public AnimatedProgressBar(int x, int y, int width, int height, IntSupplier scaled, IntSupplier current, IntSupplier max) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.scaled = scaled;
        this.current = current;
        this.max = max;
    }

    public void render(GuiGraphics graphics, int left, int top, int tickCount, boolean active) {
        graphics.fill(left + x, top + y, left + x + width, top + y + height, 0x660F1926);
        int filled = Math.max(0, Math.min(width, scaled.getAsInt()));
        if (filled <= 0) {
            return;
        }

        int pulse = active ? ((tickCount % 24) / 3) : 0;
        int accent = 0xFF34B6FF + (pulse << 16);
        graphics.fill(left + x + 1, top + y + 1, left + x + filled, top + y + height - 1, accent);
    }

    public boolean isMouseOver(int left, int top, int mouseX, int mouseY) {
        return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
    }

    public List<Component> tooltip() {
        return List.of(
                Component.translatable("gui.extremecraft.progress"),
                Component.literal(current.getAsInt() + " / " + Math.max(1, max.getAsInt()))
        );
    }
}
