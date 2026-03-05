package com.extremecraft.client.gui.player;

import net.minecraft.world.entity.player.Player;

/**
 * Legacy wrapper that now opens the unified player menu on the Magic tab.
 */
public class MagicScreen extends ExtremePlayerScreen {
    public MagicScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.MAGIC);
    }
}
