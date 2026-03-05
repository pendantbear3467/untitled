package com.extremecraft.client;

import com.extremecraft.ExtremeCraftMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class DwKeybinds {
    public static KeyMapping OFFHAND_OVERRIDE; // bound to Right Mouse by default
    public static KeyMapping OPEN_PLAYER_MENU;

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
        OFFHAND_OVERRIDE = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".offhand_action",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                "key.categories." + ExtremeCraftMod.MODID
        );

        OPEN_PLAYER_MENU = new KeyMapping(
                "key." + ExtremeCraftMod.MODID + ".open_player_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.categories." + ExtremeCraftMod.MODID
        );

        e.register(OFFHAND_OVERRIDE);
        e.register(OPEN_PLAYER_MENU);
    }
}
