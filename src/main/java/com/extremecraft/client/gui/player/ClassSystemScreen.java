package com.extremecraft.client.gui.player;

import net.minecraft.world.entity.player.Player;

/**
 * Legacy wrapper that now opens the unified player menu on the Class/Skills tab.
 */
public class ClassSystemScreen extends ExtremePlayerScreen {
    public ClassSystemScreen(Player player) {
        super(player, ExtremePlayerTabs.Tab.CLASS_SKILLS);
    }
}
