package com.extremecraft.client.gui.player;

import com.extremecraft.config.DwConfig;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class InventoryXpOverlay {
    @SubscribeEvent
    public void onRenderScreenPost(ScreenEvent.Render.Post event) {
        if (!DwConfig.CLIENT.enableXPBarOverlay.get()) {
            return;
        }

        if (!(event.getScreen() instanceof InventoryScreen inventoryScreen)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Optional<PlayerStatsCapability> statsOpt = PlayerStatsApi.get(mc.player);
        if (statsOpt.isEmpty()) {
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        GuiGraphics graphics = event.getGuiGraphics();

        int barWidth = 110;
        int x = inventoryScreen.getGuiLeft() + 33;
        int y = Math.max(4, inventoryScreen.getGuiTop() - 12);

        int xp = stats.experience();
        int xpNeeded = Math.max(1, stats.experienceToNextLevel());
        int fill = Math.min(barWidth, Math.round((xp / (float) xpNeeded) * barWidth));

        graphics.fill(x, y, x + barWidth, y + 8, 0xCC1B202A);
        if (fill > 0) {
            graphics.fillGradient(x, y, x + fill, y + 8, 0xFF3A7AE0, 0xFF5DB0F6);
        }

        Component label = Component.literal(xp + " / " + xpNeeded + " XP");
        graphics.drawCenteredString(mc.font, label, x + (barWidth / 2), y - 10, 0xDDE6F7);
    }
}
