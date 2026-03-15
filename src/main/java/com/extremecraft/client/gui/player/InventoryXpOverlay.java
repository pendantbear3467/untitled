package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import com.extremecraft.client.gui.theme.ECGuiTheme;
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

        ECGuiPrimitives.drawSegmentedBar(graphics, x, y, barWidth, 8, xp / (float) xpNeeded, ECGuiTheme.ACCENT_CYAN);

        Component label = Component.literal(xp + " / " + xpNeeded + " XP");
        graphics.drawCenteredString(mc.font, label, x + (barWidth / 2), y - 10, ECGuiTheme.TEXT_SECONDARY);
    }
}
