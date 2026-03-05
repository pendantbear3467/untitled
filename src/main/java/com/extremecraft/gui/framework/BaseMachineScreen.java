package com.extremecraft.gui.framework;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseMachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final List<int[]> highlightedSlots = new ArrayList<>();
    protected AnimatedProgressBar progressBar;
    protected AnimatedEnergyBar energyBar;
    protected AnimatedFluidTank fluidTank;

    protected BaseMachineScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected void addMachineSlotHighlight(int slotX, int slotY) {
        highlightedSlots.add(new int[]{slotX, slotY});
    }

    protected boolean isMachineActive() {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        int animTime = animationTime();

        GuiRenderUtils.drawMachineBackground(graphics, left, top, imageWidth, imageHeight);

        if (isMachineActive()) {
            GuiRenderUtils.drawMachineActiveAura(graphics, left, top, imageWidth, imageHeight, animTime);
        }

        for (int[] slot : highlightedSlots) {
            GuiRenderUtils.drawSlotHighlight(graphics, left + slot[0], top + slot[1], animTime);
        }

        if (progressBar != null) {
            progressBar.render(graphics, left, top, animTime, isMachineActive());
        }
        if (energyBar != null) {
            energyBar.render(graphics, left, top, animTime);
        }
        if (fluidTank != null) {
            fluidTank.render(graphics, left, top, animTime);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        if (progressBar != null && progressBar.isMouseOver(left, top, mouseX, mouseY)) {
            GuiRenderUtils.renderTooltip(graphics, font, progressBar.tooltip(), mouseX, mouseY);
            return;
        }

        if (energyBar != null && energyBar.isMouseOver(left, top, mouseX, mouseY)) {
            GuiRenderUtils.renderTooltip(graphics, font, energyBar.tooltip(), mouseX, mouseY);
            return;
        }

        if (fluidTank != null && fluidTank.isMouseOver(left, top, mouseX, mouseY)) {
            GuiRenderUtils.renderTooltip(graphics, font, fluidTank.tooltip(), mouseX, mouseY);
            return;
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    private int animationTime() {
        return (int) (System.currentTimeMillis() / 50L);
    }
}
