package com.extremecraft.gui;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machines.pulverizer.PulverizerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PulverizerScreen extends AbstractContainerScreen<PulverizerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");

    public PulverizerScreen(PulverizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (menu.isCrafting()) {
            int progress = menu.getProgressScaled();
            graphics.blit(TEXTURE, x + 79, y + 34, 176, 14, progress + 1, 16);
        }

        int energy = menu.getEnergyScaled();
        if (energy > 0) {
            graphics.fill(x + 8, y + 70 - energy, x + 13, y + 70, 0xFF00CCFF);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
