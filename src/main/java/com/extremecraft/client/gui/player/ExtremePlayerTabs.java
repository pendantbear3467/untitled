package com.extremecraft.client.gui.player;

import net.minecraft.network.chat.Component;

public final class ExtremePlayerTabs {
    public enum Tab {
        PLAYER_STATS("Stats"),
        SKILLS("Skills"),
        MAGIC("Magic"),
        DUAL_WIELD("DualWield"),
        CLASS_SKILLS("Class");

        private final Component label;

        Tab(String label) {
            this.label = Component.literal(label);
        }

        public Component label() {
            return label;
        }
    }

    private ExtremePlayerTabs() {
    }
}
