package com.extremecraft.client.gui.player;

import net.minecraft.world.entity.player.Player;

/**
 * Legacy wrapper that now opens the unified player menu on the Dual Wield tab.
 */
public class DualWieldScreen extends ExtremePlayerScreen {
    public DualWieldScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.DUAL_WIELD);
    }
}
