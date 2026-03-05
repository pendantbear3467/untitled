package com.extremecraft.client.gui.player;

import net.minecraft.world.entity.player.Player;

/**
 * Legacy wrapper that now opens the unified player menu on the Skill Points tab.
 */
public class ProgressionScreen extends ExtremePlayerScreen {
    public ProgressionScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.SKILL_POINTS);
    }
}
