package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.BaseExtremeScreen;
import com.extremecraft.progression.skilltree.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Shared base for standalone player systems screens that are no longer tied to the vanilla inventory GUI.
 */
public abstract class StandalonePlayerScreen extends BaseExtremeScreen {
    protected final Player player;

    protected StandalonePlayerScreen(Player player, Component title) {
        super(title);
        this.player = player;
        this.panelWidth = 286;
        this.panelHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        int x = panelLeft + 10;
        int y = panelTop + 10;
        int w = 64;
        int h = 20;

        addRenderableWidget(Button.builder(Component.literal("Skill"), b -> Minecraft.getInstance().setScreen(new SkillTreeScreen()))
                .bounds(x, y, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Magic"), b -> Minecraft.getInstance().setScreen(new MagicScreen(player)))
                .bounds(x + 70, y, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Dual"), b -> Minecraft.getInstance().setScreen(new DualWieldScreen(player)))
                .bounds(x + 140, y, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Progress"), b -> Minecraft.getInstance().setScreen(new ProgressionScreen(player)))
                .bounds(x + 210, y, 66, h)
                .build());
    }

    @Override
    protected void drawContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int titleX = panelLeft + (panelWidth / 2);
        guiGraphics.drawCenteredString(this.font, this.title, titleX, panelTop + 18, 0xF2D28C);
        guiGraphics.fill(panelLeft + 10, panelTop + 36, panelLeft + panelWidth - 10, panelTop + panelHeight - 12, 0x7710121A);
        drawStandaloneContent(guiGraphics, mouseX, mouseY, partialTick);
    }

    protected abstract void drawStandaloneContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);
}
