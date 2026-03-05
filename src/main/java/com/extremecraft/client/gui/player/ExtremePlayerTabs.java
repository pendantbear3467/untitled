package com.extremecraft.client.gui.player;

import net.minecraft.network.chat.Component;

public final class ExtremePlayerTabs {
    public enum Tab {
        PLAYER_STATS("Stats"),
        MAGIC("Magic"),
        DUAL_WIELD("Dual Wield"),
        CLASS_SKILLS("Class"),
        SKILL_POINTS("Skills");

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
