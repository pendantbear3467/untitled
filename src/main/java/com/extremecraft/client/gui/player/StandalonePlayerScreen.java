package com.extremecraft.client.gui.player;

import com.extremecraft.progression.skilltree.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Shared base for standalone player systems screens that are no longer tied to the vanilla inventory GUI.
 */
public abstract class StandalonePlayerScreen extends Screen {
    protected final Player player;

    protected StandalonePlayerScreen(Player player, Component title) {
        super(title);
        this.player = player;
    }

    @Override
    protected void init() {
        int x = 10;
        int y = 10;
        int w = 80;
        int h = 20;

        addRenderableWidget(Button.builder(Component.literal("Skill"), b -> Minecraft.getInstance().setScreen(new SkillTreeScreen()))
                .bounds(x, y, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Magic"), b -> Minecraft.getInstance().setScreen(new MagicScreen(player)))
                .bounds(x, y + 24, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Dual"), b -> Minecraft.getInstance().setScreen(new DualWieldScreen(player)))
                .bounds(x, y + 48, w, h)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Progress"), b -> Minecraft.getInstance().setScreen(new ProgressionScreen(player)))
                .bounds(x, y + 72, w, h)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.fill(100, 8, this.width - 8, this.height - 8, 0xAA101015);
        guiGraphics.drawCenteredString(this.font, this.title, (100 + this.width - 8) / 2, 14, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
