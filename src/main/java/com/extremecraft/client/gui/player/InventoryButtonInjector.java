package com.extremecraft.client.gui.player;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InventoryButtonInjector {
    private static final ResourceLocation ICON = new ResourceLocation(ECConstants.MODID, "textures/gui/extremecraft_icon.png");

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());

        int x = inventoryScreen.getGuiLeft() - 26;
        int y = inventoryScreen.getGuiTop() + 8;

        ImageButton button = new ImageButton(x, y, 20, 20, 0, 0, 20, ICON, 20, 40,
                btn -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.setScreen(new ExtremePlayerScreen(mc.player, inventoryScreen));
                    }
                });

        event.addListener(button);
    }
}
