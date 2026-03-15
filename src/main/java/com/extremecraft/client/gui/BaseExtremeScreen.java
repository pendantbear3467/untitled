package com.extremecraft.client.gui;

import com.extremecraft.core.ECConstants;
import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Reusable themed screen shell for ExtremeCraft RPG-tech interfaces.
 */
public abstract class BaseExtremeScreen extends Screen {
    protected static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/gui/gui_panel.png");

    protected int panelWidth = 256;
    protected int panelHeight = 180;
    protected int panelLeft;
    protected int panelTop;

    protected BaseExtremeScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        layoutPanel();
    }

    protected final void layoutPanel() {
        panelLeft = (this.width - panelWidth) / 2;
        panelTop = (this.height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ECGuiPrimitives.drawScreenBackdrop(guiGraphics, this.width, this.height);
        drawBackdrop(guiGraphics);
        drawPanel(guiGraphics);
        drawGradientOverlay(guiGraphics);
        drawContent(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawTooltips(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected void drawBackdrop(GuiGraphics guiGraphics) {
        guiGraphics.fill(panelLeft - 8, panelTop - 8, panelLeft + panelWidth + 8, panelTop + panelHeight + 8, 0x66000000);
    }

    protected void drawPanel(GuiGraphics guiGraphics) {
        if (minecraft != null && minecraft.getResourceManager().getResource(PANEL_TEXTURE).isPresent()) {
            guiGraphics.blit(PANEL_TEXTURE, panelLeft, panelTop, 0, 0, panelWidth, panelHeight, 256, 256);
        }
        ECGuiPrimitives.drawPanelChrome(guiGraphics, panelLeft, panelTop, panelWidth, panelHeight, minecraft == null || minecraft.player == null ? 0 : minecraft.player.tickCount);
    }

    protected void drawGradientOverlay(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(panelLeft + 4, panelTop + 4, panelLeft + panelWidth - 4, panelTop + panelHeight - 4,
                0x22006B9E, 0x331B0C29);
    }

    protected abstract void drawContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    protected void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
