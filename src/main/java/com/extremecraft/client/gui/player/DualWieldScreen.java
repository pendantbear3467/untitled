package com.extremecraft.client.gui.player;

import com.extremecraft.config.DwConfig;
import com.extremecraft.progression.capability.PlayerProgressCapabilityApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DualWieldScreen extends ExtremePlayerScreen {
    public DualWieldScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.DUAL_WIELD);
    }

    @Override
    protected void renderTabContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getGuiLeft() + 14;
        int y = getGuiTop() + 14;
        guiGraphics.drawString(font, Component.literal("Dual Wield System"), x, y, 0xFFD166, false);
        guiGraphics.drawString(font, Component.literal("Offhand Item: " + menu.getCarried().getDisplayName().getString()), x, y + 14, 0xFFFFFF, false);

        boolean allowBlockBreak;
        try {
            allowBlockBreak = DwConfig.CLIENT.allowOffhandBlockBreaking.get();
        } catch (Exception ex) {
            allowBlockBreak = false;
        }

        guiGraphics.drawString(font, Component.literal("Offhand Block Break: " + (allowBlockBreak ? "ON" : "OFF")), x, y + 26, 0xE0E0E0, false);
        PlayerProgressCapabilityApi.get(minecraft.player).ifPresent(data ->
                guiGraphics.drawString(font, Component.literal("Dual Wield Unlock: " + (data.dualWieldUnlocked() ? "Unlocked" : "Locked")), x, y + 38, 0xE0E0E0, false)
        );
        guiGraphics.drawString(font, Component.literal("Cooldown: synced with attack strength"), x, y + 50, 0xE0E0E0, false);
    }
}
