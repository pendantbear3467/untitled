package com.extremecraft.gui.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntSupplier;

public class AnimatedFluidTank {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final IntSupplier amount;
    private final IntSupplier capacity;
    private final Component fluidName;

    public AnimatedFluidTank(int x, int y, int width, int height, IntSupplier amount, IntSupplier capacity, Component fluidName) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.amount = amount;
        this.capacity = capacity;
        this.fluidName = fluidName;
    }

    public void render(GuiGraphics graphics, int left, int top, int tickCount) {
        graphics.fill(left + x, top + y, left + x + width, top + y + height, 0x662A2E35);

        int cap = Math.max(1, capacity.getAsInt());
        int filled = Math.max(0, Math.min(height, (int) ((amount.getAsInt() / (double) cap) * height)));
        if (filled > 0) {
            int wobble = Math.max(0, 10 - Math.abs(10 - (tickCount % 20)));
            int fluidColor = 0xFF2A8FFF + (wobble << 8);
            graphics.fill(left + x + 1, top + y + height - filled, left + x + width - 1, top + y + height - 1, fluidColor);
        }
    }

    public boolean isMouseOver(int left, int top, int mouseX, int mouseY) {
        return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
    }

    public List<Component> tooltip() {
        return List.of(
                fluidName,
                Component.literal(amount.getAsInt() + " / " + Math.max(1, capacity.getAsInt()) + " mB")
        );
    }
}
