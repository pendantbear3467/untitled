package com.extremecraft.client.gui.player;

import net.minecraft.network.chat.Component;

public final class ExtremePlayerTabs {
    public enum Tab {
        PLAYER_STATS("Stats", 338, 252),
        SKILLS("Skills", 420, 312),
        MAGIC("Magic", 338, 236),
        DUAL_WIELD("DualWield", 320, 214),
        CLASS_SKILLS("Class", 338, 236),
        MODULES("Tech", 360, 244);

        private final Component label;
        private final int preferredWidth;
        private final int preferredHeight;

        Tab(String label, int preferredWidth, int preferredHeight) {
            this.label = Component.literal(label);
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        public Component label() {
            return label;
        }

        public int preferredWidth() {
            return preferredWidth;
        }

        public int preferredHeight() {
            return preferredHeight;
        }
    }

    private ExtremePlayerTabs() {
    }
}
