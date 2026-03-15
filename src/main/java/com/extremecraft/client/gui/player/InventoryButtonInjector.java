package com.extremecraft.client.gui.player;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InventoryButtonInjector {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "textures/gui/extremecraft_icon.png");

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());

        int x = inventoryScreen.getGuiLeft() - 26;
        int y = inventoryScreen.getGuiTop() + 8;

        event.addListener(new ThemedInventoryButton(
                x,
                y,
                20,
                20,
                Component.literal("EC"),
                ICON,
                btn -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.setScreen(new ExtremePlayerScreen(mc.player, inventoryScreen));
                    }
                }
        ));

        event.addListener(new ThemedInventoryButton(
                x,
                y + 24,
                20,
                20,
                Component.literal("DW"),
                null,
                btn -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.setScreen(new DualWieldScreen(mc.player, inventoryScreen));
                    }
                }
        ));
    }

    private static final class ThemedInventoryButton extends Button {
        private final ResourceLocation icon;

        private ThemedInventoryButton(int x, int y, int width, int height, Component label, ResourceLocation icon, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.icon = icon;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int frame = this.active ? (this.isHoveredOrFocused() ? 0xFF6887A8 : 0xFF4C6885) : 0xFF37465A;
            int inner = this.active ? 0xDD111A25 : 0xAA1A1F2A;
            int text = this.active ? 0xFFE6F1FF : 0xFF9FB1C6;

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, frame);
            guiGraphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, inner);
            guiGraphics.fill(this.getX() + 2, this.getY() + 2, this.getX() + this.width - 2, this.getY() + 4, this.isHoveredOrFocused() ? 0xAA4EDCFF : 0x66416A93);

            Minecraft mc = Minecraft.getInstance();
            boolean drewIcon = false;
            if (icon != null && mc.getResourceManager().getResource(icon).isPresent()) {
                guiGraphics.blit(icon, this.getX() + 2, this.getY() + 2, 0, 0, 16, 16, 16, 16);
                drewIcon = true;
            }

            if (drewIcon && !this.getMessage().getString().isBlank()) {
                guiGraphics.drawString(mc.font, this.getMessage(), this.getX() + 11, this.getY() + 6, text, false);
            } else {
                guiGraphics.drawCenteredString(mc.font, this.getMessage(), this.getX() + (this.width / 2), this.getY() + 6, text);
            }
        }
    }
}
