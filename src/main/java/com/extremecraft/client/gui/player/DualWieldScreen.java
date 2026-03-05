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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int x = 116;
        int y = 40;
        guiGraphics.drawString(font, Component.literal("Dual Wield System"), x, y, 0xFFD166, false);
        String offhandName = minecraft != null && minecraft.player != null
            ? minecraft.player.getOffhandItem().getDisplayName().getString()
            : "None";
        guiGraphics.drawString(font, Component.literal("Offhand Item: " + offhandName), x, y + 14, 0xFFFFFF, false);

        if (minecraft != null && minecraft.player != null) {
            PlayerDualWieldApi.get(minecraft.player).ifPresent(data ->
                    guiGraphics.drawString(font, Component.literal("Active Loadout: " + (data.activeLoadoutIndex() + 1) + " / 3 (Z to cycle)"), x, y + 62, 0xE0E0E0, false)
            );
        }

        boolean allowBlockBreak;
        try {
            allowBlockBreak = DwConfig.CLIENT.allowOffhandBlockBreaking.get();
        } catch (Exception ex) {
            allowBlockBreak = false;
        }

        guiGraphics.drawString(font, Component.literal("Offhand Block Break: " + (allowBlockBreak ? "ON" : "OFF")), x, y + 26, 0xE0E0E0, false);
        if (minecraft != null && minecraft.player != null) {
            ProgressApi.get(minecraft.player).ifPresent(data -> {
                boolean dualWieldUnlocked = data.level() >= 2;
                guiGraphics.drawString(font, Component.literal("Dual Wield Unlock: " + (dualWieldUnlocked ? "Unlocked" : "Locked")), x, y + 38, 0xE0E0E0, false);
            });
        }
        guiGraphics.drawString(font, Component.literal("Cooldown: synced with attack strength"), x, y + 50, 0xE0E0E0, false);
    }
}
