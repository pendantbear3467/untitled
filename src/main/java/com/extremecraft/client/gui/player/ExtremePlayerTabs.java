package com.extremecraft.client.gui.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.Consumer;

public final class ExtremePlayerTabs {
    public enum Tab {
        INVENTORY("Inventory"),
        DUAL_WIELD("Dual Wield"),
        MAGIC("Magic"),
        PLAYER_STATS("Player Stats"),
        CLASS_SYSTEM("Class System");

        private final String title;

        Tab(String title) {
            this.title = title;
        }

        public Component label() {
            return Component.literal(title);
        }
    }

    private static boolean hooksRegistered = false;

    private ExtremePlayerTabs() {
    }

    public static void registerHooks() {
        if (hooksRegistered) {
            return;
        }

        MinecraftForge.EVENT_BUS.addListener(ExtremePlayerTabs::onOpenInventory);
        hooksRegistered = true;
    }

    private static void onOpenInventory(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof InventoryScreen && !(event.getScreen() instanceof ExtremePlayerScreen)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                event.setNewScreen(new ExtremePlayerScreen(mc.player));
            }
        }
    }

    public static void addTabButtons(ExtremePlayerScreen screen, Consumer<Tab> onTabClicked) {
        int startX = screen.getGuiLeft() - 78;
        int startY = screen.getGuiTop() + 6;
        int buttonHeight = 20;

        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int y = startY + i * (buttonHeight + 2);
            Button button = Button.builder(tab.label(), btn -> onTabClicked.accept(tab))
                    .bounds(startX, y, 74, buttonHeight)
                    .build();
            screen.addTabWidget(button);
        }
    }
}
