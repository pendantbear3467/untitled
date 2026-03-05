package com.extremecraft.client.gui.player;

import com.extremecraft.combat.dualwield.PlayerDualWieldApi;
import com.extremecraft.config.DwConfig;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class DualWieldScreen extends StandalonePlayerScreen {
    public DualWieldScreen(Player player) {
        super(player, Component.literal("Dual Wield"));
    }

    @Override
    protected void drawStandaloneContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = panelLeft + 24;
        int y = panelTop + 52;

        guiGraphics.drawString(font, Component.literal("Weapon Loadouts"), x, y, 0xF4C66B, false);

        renderWeaponSlot(guiGraphics, x, y + 16, "Main", minecraft != null && minecraft.player != null
                ? minecraft.player.getMainHandItem().getDisplayName().getString() : "Empty");
        renderWeaponSlot(guiGraphics, x + 126, y + 16, "Off", minecraft != null && minecraft.player != null
                ? minecraft.player.getOffhandItem().getDisplayName().getString() : "Empty");

        int active = PlayerDualWieldApi.get(player).map(data -> data.activeLoadoutIndex()).orElse(0);
        renderLoadout(guiGraphics, x, y + 62, 0, active);
        renderLoadout(guiGraphics, x + 76, y + 62, 1, active);
        renderLoadout(guiGraphics, x + 152, y + 62, 2, active);

        boolean allowBlockBreak;
        try {
            allowBlockBreak = DwConfig.CLIENT.allowOffhandBlockBreaking.get();
        } catch (Exception ex) {
            allowBlockBreak = false;
        }

        boolean unlocked = ProgressApi.get(player).map(data -> data.level() >= 2).orElse(false);
        guiGraphics.drawString(font, Component.literal("Dual Wield Unlock: " + (unlocked ? "Unlocked" : "Locked")), x, y + 92, 0xD5D9E2, false);
        guiGraphics.drawString(font, Component.literal("Offhand Block Break: " + (allowBlockBreak ? "ON" : "OFF")), x, y + 106, 0xAAB2C0, false);
    }

    private void renderWeaponSlot(GuiGraphics guiGraphics, int x, int y, String label, String itemName) {
        guiGraphics.fill(x, y, x + 112, y + 38, 0xAA1C2028);
        guiGraphics.fill(x + 2, y + 2, x + 110, y + 36, 0x660A0C11);
        guiGraphics.drawString(font, Component.literal(label + " Hand"), x + 6, y + 5, 0xF4C66B, false);
        guiGraphics.drawString(font, Component.literal(itemName), x + 6, y + 20, 0xE6EAF2, false);
    }

    private void renderLoadout(GuiGraphics guiGraphics, int x, int y, int index, int active) {
        boolean isActive = index == active;
        int box = isActive ? 0xCCB88A3E : 0xAA2A2F39;
        int text = isActive ? 0xFFF2D28C : 0xFFD0D4DD;

        guiGraphics.fill(x, y, x + 68, y + 20, box);
        guiGraphics.drawCenteredString(font, Component.literal("Loadout " + (index + 1)), x + 34, y + 6, text);
    }
}
