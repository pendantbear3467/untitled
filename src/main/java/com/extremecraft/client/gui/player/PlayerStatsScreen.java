package com.extremecraft.client.gui.player;

import net.minecraft.world.entity.player.Player;

/**
 * Legacy wrapper that now opens the unified player menu on the Player Stats tab.
 */
public class PlayerStatsScreen extends ExtremePlayerScreen {
    public PlayerStatsScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.PLAYER_STATS);
    }
}
